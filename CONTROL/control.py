#!/usr/bin/env python3

import click
import docker
import json
import logging
import psutil
import sys
import toml
import uuid

from docker.errors import ImageNotFound
from docker.types import Ulimit
from os import listdir
from os.path import isfile, join
from requests.exceptions import ReadTimeout

DOMAIN = 'graphbenchmark.com'
IMG_VERSION = ':latest'
DB_LIST = ["orientdb", "janusgraph", "neo4j", "arangodb", "sqlgpg"]

log = logging.getLogger(__name__)
log_lvls = {
    'CRITICAL': logging.CRITICAL,
    'ERROR':    logging.ERROR,
    'WARNING':  logging.WARNING,
    'INFO':     logging.INFO,
    'DEBUG':    logging.DEBUG,
}

# volume mounting options
VOPT = lambda opt: opt


def fatal(*args, **kwargs):
    log.fatal(*args, **kwargs)
    sys.exit(1)


@click.group()
@click.option('--log_level', type=click.Choice(list(log_lvls.keys())), default='DEBUG')
@click.option('--selinux', is_flag=True, help='Enable support for SELinux.')
def main(log_level, selinux):
    level = log_lvls[log_level]
    log.setLevel(level)

    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(level)
    formatter = logging.Formatter('%(asctime)s| %(levelname)-8s - %(message)s', '%H:%M:%S')
    handler.setFormatter(formatter)
    log.addHandler(handler)

    if selinux:
        global VOPT
        log.info('Enabling support for SELinux, labeling shells and runtime folder.')
        # NOTE: it should be enough to do this just once,
        #  but we prefer avoiding 'check and set' logic and do it everytime.
        # https://docs.docker.com/storage/bind-mounts/#configure-the-selinux-label
        VOPT = lambda opt: f'{opt},z'


@main.command()
@click.option('-c', '--config', type=click.File('w'), required=True, help="Configuration file.")
@click.option('-d', '--dataset_dir', type=click.Path(exists=True, file_okay=False, resolve_path=True),
              required=False, help="Dataset directory")
@click.option('-s', '--shell_dir', type=click.Path(exists=True, file_okay=False, resolve_path=True),
              required=False, default='../SHELLS/dist', help="Direcotory containing the compiled shells")
@click.option('-r', '--runtime_dir', type=click.Path(exists=True, file_okay=False, resolve_path=True),
              required=True, help="Runtime directory")
@click.option('--database', multiple=True,
              required=False, default=DB_LIST, help="List of databases for which to instantiate the configuration")
def generate_config(config, dataset_dir=None, shell_dir=None, runtime_dir=None, database=DB_LIST):
    log.info("Genereting default config for current site")

    client = docker.from_env()
    db_confs = {
            "orientdb": {
                'image': "graphbenchmark.com/orientdb:latest",
                'jvm_opts': "-XX:+UseG1GC -XX:MaxDirectMemorySize=512m",
            },

            "neo4j": {'image': "graphbenchmark.com/neo4j:latest"},
            "janusgraph": {'image': "graphbenchmark.com/janusgraph:latest"},

            "arangodb": {'image': "graphbenchmark.com/arangodb:latest"},
            "sqlgpg": {'image': "graphbenchmark.com/sqlgpg:latest"},
        }


    conf = {
        # Default sampling rules are compatible with tinkerpop-modern_mod.json
        'sampling': {
            'nodes': 3,
            'node_labels': 2,
            'node_props': 3,
            'edges': 3,
            'edge_labels': 2,
            'edge_props': 0,
            'paths': 3,
            'paths_max_len': 5,
        },


        # 'databases': {
        #     tag[len(DOMAIN)+1:-len(IMG_VERSION)]: {"image": tag}
        #     for tag in chain(*(i.tags for i in client.images.list()))
        #     if tag.startswith(DOMAIN) and tag.endswith(IMG_VERSION)
        # },

        # TODO: consider database specific ENV
        'databases': { _n : _c for _n, _c in  db_confs.items() if _n in database },
        'datasets': {},
        'queries': [],
        'warmup': [],
        'loader': 'com.graphbenchmark.queries.mgm.Load',
        'sampler': 'com.graphbenchmark.queries.mgm.Sampler',
        'mapsample': 'com.graphbenchmark.queries.mgm.MapSample',
        'sample_id': str(uuid.uuid4()),
        'modes': ['SINGLE_SHOT', 'BATCH', 'CONCURRENT'],
        'iterations': 3,
        'threads': 3,                       # How many concurrent clients to spawn

        'timeout': {
            'single_shot':  60 * 30,
            'batch':        60 * 60 * 3,
            'concurrent':   60 * 60,
            'load':         60 * 60 * 24,
            'extra_container': 60 * 10,     # Effevtive timeout: (query|load) + this
            'consecutive': 3,               # -1 disables it
        },

        # v.1: -Xms1G -Xmn128M -Xmx120G -XX:+UseG1GC
        # 'jvm_opts': '-XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap',
        'jvm_opts': '-XX:+UseG1GC -XX:+UnlockExperimentalVMOptions',

        # https://docker-py.readthedocs.io/en/stable/containers.html
        'cnt_opts': {
            # required for numactl
            'security_opt': ['seccomp=unconfined'],
            'mem_limit': int(psutil.virtual_memory().total * 0.99),
            # https://linuxhint.com/understanding_vm_swappiness/
            'mem_swappiness': 1,
            # Imported from v.1
            # https://support.hpe.com/hpesc/public/docDisplay?docId=emr_na-c02239033
            'ulimits': {'memlock': {'hard': -1, 'soft': -1}},
            # Imported from v.1
            # FIXME: @ml why do we need this?
            'cap_add': ['IPC_LOCK'],
        }
    }

    if dataset_dir:
        log.debug("Building dataset list")
        for f in listdir(dataset_dir):
            if not isfile(join(dataset_dir, f)):
                continue
            # If the dataset is inside the runtime dir user virtual path
            conf['datasets'][f] = {
                'path': f'/runtime/data/{f}' if dataset_dir.startswith(runtime_dir) else join(dataset_dir, f),
                'uid_field': 'uid'
            }

    if len(conf['databases']):
        log.debug("Get available queries and default 'actions'")
        db = next(iter(conf['databases']))
        shell = get_shell(db, shell_dir)

        args = ['-g', '{}']
        if log.getEffectiveLevel() <= logging.WARNING:
            args += ['-d']
        cnt = client.containers.run(conf['databases'][db]["image"], args, volumes={
            shell: {'bind': '/shell.jar', 'mode': VOPT('ro')},
            runtime_dir: {'bind': '/runtime', 'mode': VOPT('rw')},
        }, detach=True)

        data = json.loads(grep_last_json(cexec_must(cnt, timeout=120)))
        conf['loader'] = data['_loader']
        conf['sampler'] = data['_sampler']
        conf['mapsample'] = data['_mapsample']
        conf['queries'] = [q for q, c in data.items() if q[0] != '_' and c['common']]

    log.debug(f"Saving configuration to {config.name}")
    toml.dump(conf, config)


def get_shell(database_name, shell_dir):
    shells = [join(shell_dir, f) for f in listdir(shell_dir)
              if isfile(join(shell_dir, f)) and f.startswith(f'shell-{database_name}-')]
    if len(shells) != 1:
        fatal(f'Found {len(shells)} shell[s] found for {database_name}')
    return shells[0]


def grep_last_json(msg):
    for l in reversed(msg.splitlines()):
        if l and l[0] == '{':
            return l


def cexec(cnt, timeout=None, rm=True, *args, **kwargs):
    """ Executes a container and wait for it to complete or timeout.
        Panics when an Exception is thrown or the command fails.

        Returns (stdout, hasTimedout)
    """
    to = False
    try:
        x = cnt.wait(*args, timeout=timeout, **kwargs)
        if x['StatusCode']:
            log.error(f'Error in query. Code: {x["StatusCode"]}')
            raise Exception()
    except ReadTimeout:
        # Container timeout
        log.warning("Container TIMEOUT")
        x.kill()
        to = True
    except Exception:
        log.fatal(cnt.logs().decode("utf-8"))
        fatal('Irrecoverable error')

    res = cnt.logs(stderr=False).decode("utf-8")

    if rm:
        cnt.remove(force=True)
    return res, to


def cexec_must(*args, **kwargs):
    """ Executes container, like cexec, but panics on timeout """
    res, to = cexec(*args, **kwargs)
    if to:
        fatal("The container timedout")
    return res


def cexec_query(cnt, timeout, session_id, run_conf, rm=True, *args, **kwargs):
    """ Executes container, like cexec, but looks for timeout in result line """
    res, to = cexec(cnt, timeout, rm, *args, **kwargs)
    if to:
        log.warn(f"Container timedout, this should never happen. <<{run_conf}>>")
        return [f"ERROR_CONTAINER_TO;{session_id};{run_conf}"], True

    # Cleanup stream
    res = list(filter(lambda l: l.startswith(session_id), res.splitlines()))

    # Status field is in column 11, values: (OK, TIMEOUT, OOM)

    # Timeout and OOM will log only one line!
    nok = to or (res and (lambda x: len(x) >= 12 and x[11] != 'OK')(res[-1].split(";")))

    return res, nok


def validate_conf(conf, commit_suffix):
    errors = []
    if not conf['jvm_opts'].strip():
        errors.append('"jvm_opts" cannot be empty.')

    # TODO: implement the other checks

    # Sampling
    samp = conf['sampling']
    if samp.get('node_props', 0) and not samp.get('node_labels', 0):
        errors.append('"node_props" sampling requires "edge_labels" sampling')

    if samp.get('edge_props', 0) and not samp.get('edge_labels', 0):
        errors.append('"edge_props" sampling requires "edge_labels" sampling')

    # Commit
    if commit_suffix:
        if len(conf.get('queries', [])) != 1:
            errors.append('Commit operation supported only for 1 query at a time.')
        if conf['modes'] != ['SINGLE_SHOT']:
            errors.append('Commit operation supported only for in SINGLE_SHOT mode.')

    if errors:
        log.fatal('\n'.join(errors))
        fatal('Provided configuration is not valid.')


def fix_base_cnt(opts):
    # Fix ulimits
    opts['ulimits'] = [Ulimit(name=name, **param) for name, param in opts.get('ulimits', {}).items()]

    # Inject common options
    opts['detach'] = True
    opts['auto_remove'] = False

    return opts


def validate_sample(sample, conf, fallback=False):
    """ Ensure that samples are compatible with the configuration. """

    ok = True
    new_conf = {}
    for k, v in conf['sampling'].items():
        if k == 'paths_max_len':
            continue

        new_conf[k] = min(v, len(sample.get(k, [])))
        if v > len(sample.get(k, [])):
            ok = False

    if not ok:
        if not fallback:
            log.fatal('Config not compatible with sample file. Suggested "sampling" conf:')
            fatal(json.dumps(new_conf))
        log.warning('Config not compatible with sample file. Fallingback to:')
        log.warning(json.dumps(new_conf))
        return new_conf
    return conf['sampling']


def validate_query_conf(qconf, conf, commit_suffix):
    missing_queries = set(conf['queries']) - {k for k in qconf.keys() if k[0] != '_'}
    if missing_queries:
        fatal(f"The following queries are not implemented: {missing_queries}")
    if commit_suffix and len(qconf[conf['queries'][0]]['configurations']) != 1:
        fatal('Commit operation supported only for queries with one singel configuration.')

    missing_warmup = set(conf['warmup']) - {k for k in qconf.keys() if k[0] != '_'}
    if missing_warmup:
        fatal(f"The following warmup queries are not implemented: {missing_warmup}")

    log.debug(f"Runnig with query version: {qconf['_queries_version']}")


def exp_filter(session_id):
    return lambda l: l.startswith(session_id)


def log_res(results, res, nok):
    for rec in res:
        print(rec, file=results, flush=True)
    if nok:
        log.warn("Timeout or OOM >> " + res[-1])
    return nok


@main.command()
@click.option('-c', '--config', type=click.File('r'), required=True, help="Configuration file.")
@click.option('-r', '--runtime_dir', type=click.Path(exists=True, file_okay=False, resolve_path=True),
              required=True, help="Runtime directory")
@click.option('-s', '--shell_dir', type=click.Path(exists=True, file_okay=False, resolve_path=True),
              required=True, default='../SHELLS/dist', help="Direcotory containing the compiled shells")
@click.option('-o', '--results', type=click.File('a+'), default='results.csv', help="Results file.")
@click.option('--data_suffix', default='', help='Data images suffix')
@click.option('--commit_suffix', default='', help='Commit images suffix')
@click.option('--load_only', default=False, is_flag=True, help='Load only (no sample patch)')
@click.option('--sample_only', default=False, is_flag=True, help='Sample only (no patch)')
@click.option('--sample_fallback', default=False, is_flag=True, help='Adapt sample spec to available sample')
def run_benchmark(config, runtime_dir, shell_dir, results, data_suffix, commit_suffix, load_only, sample_only, sample_fallback):
    SESSION_ID = str(uuid.uuid4())
    CMD_ID = 0
    log.info(f"Running benchmark: {SESSION_ID}")

    log.debug(f"Loading configuration from {config.name}")
    conf = toml.load(config)
    validate_conf(conf, commit_suffix)

    BASE_ENV_ALL = [f"GDB_JVM_OPTS={conf['jvm_opts']}"]
    BASE_CNT = fix_base_cnt(conf['cnt_opts'])
    BASE_ARGS = []
    if log.getEffectiveLevel() <= logging.WARNING:
        log.debug('Shells are running with -d')
        BASE_ARGS = ['-d']

    qconf = None                    # Query execution configuration (we shall wait for sampling)
    client = docker.from_env()      # Docker client (for all commands)

    # TODO: estimate the number of tests and then show some "progress" bar.

    log.debug("Main benchamark loop")
    for ds_name, ds in conf['datasets'].items():


        # See Venv.SAMPLE_DIR and SampleManager.samplePath()
        # /runtime/samples/tinkerpop-modern_mod.json-390f6c58-a954-43a7-a18f-c2d33c84cc0c.json
        sample_path = join(runtime_dir, 'samples', f"{ds_name}-{conf['sample_id']}.json")

        log.info(f"Current dataset is {ds_name}")
        for database in conf['databases']:
            db_image = conf['databases'][database]['image']

            BASE_ENV = BASE_ENV_ALL
            jvm_opts_override = conf['databases'][database].get('jvm_opts')
            if jvm_opts_override:
                log.info(f'Overriding java options: {jvm_opts_override}')
                BASE_ENV = [e for e in BASE_ENV if not e.startswith('GDB_JVM_OPTS=')]
                BASE_ENV += [f"GDB_JVM_OPTS={jvm_opts_override}"]

            log.info(f"Current database is {database}")
            shell = get_shell(database, shell_dir)

            vols = {
                shell: {'bind': '/shell.jar', 'mode': VOPT('ro')},
                runtime_dir: {'bind': '/runtime/', 'mode': VOPT('rw')},
            }

            # Mount dataset iif outside the runtime data directory
            if not ds['path'].startswith('/runtime/data/'):
                vols[ds['path']] = {'bind': ds['path'], 'mode': VOPT('ro')}

            # Check if img for datase-sample exists
            data_image = f'data.{DOMAIN}/{database}_{ds_name}_{conf["sample_id"]}{data_suffix}'.replace(' ', '-')
            try:
                client.images.get(data_image)
            except ImageNotFound:
                # If usign data_suffix, we do not now hot it was created: give_up
                if data_suffix:
                    fatal(f'Missing data image: {data_image}')

                # Load if it is not already
                raw_data_image = f'data.{DOMAIN}/{database}_{ds_name}'.replace(' ', '-')
                try:
                    client.images.get(raw_data_image)
                except ImageNotFound:
                    log.info(f'Loading {ds_name} into {database}')
                    env = BASE_ENV + ['GDB_SAFE_SHUTDOWN=true']
                    args = [
                        '-p', json.dumps([{}]),
                        '-x', json.dumps({
                            'query': conf['loader'],
                            'dataset': ds,
                            'sample_id': conf['sample_id'],
                            'session_id': SESSION_ID,
                            'cmd_id': CMD_ID,
                            'timeout': conf['timeout']['load'],
                        }),
                        *BASE_ARGS,
                    ]
                    CMD_ID += 1
                    cnt = client.containers.run(db_image, args, environment=env, volumes=vols, **BASE_CNT)
                    load_timeout = conf['timeout']['load'] + conf['timeout']['extra_container']
                    if log_res(results,
                               *cexec_query(cnt,
                                            timeout=load_timeout,
                                            rm=False,
                                            session_id=SESSION_ID,
                                            run_conf={'loading': raw_data_image})):
                        fatal(f"Timeout loading creating {data_image}")
                    log.info(f'Commiting {database} with {ds_name} as {data_image}')
                    cnt.commit(raw_data_image)
                    cnt.remove()

                if load_only:
                    continue

                # Create sample if it did not exists
                if not isfile(sample_path):
                    log.info(f'Creating sample {conf["sample_id"]} for {ds_name}')
                    args = [
                        '-p', json.dumps([conf['sampling']]),     # This is needed if we need to create a new sample.
                        '-x', json.dumps({
                            'query': conf['sampler'],
                            'dataset': ds,
                            'sample_id': conf['sample_id'],
                            'session_id': SESSION_ID,
                            'cmd_id': CMD_ID,
                            'timeout': conf['timeout']['load'],
                        }),
                        *BASE_ARGS,
                    ]
                    CMD_ID += 1
                    cnt = client.containers.run(raw_data_image, args, environment=BASE_ENV, volumes=vols, **BASE_CNT)
                    load_timeout = conf['timeout']['load'] + conf['timeout']['extra_container']
                    if log_res(results,
                               *cexec_query(cnt,
                                            timeout=load_timeout,
                                            rm=True,
                                            session_id=SESSION_ID,
                                            run_conf={'sampling': data_image})):
                        fatal(f"Timeout loading creating {data_image}")
                    log.info(f'Sample created at {sample_path}')

                if sample_only:
                    continue

                # Create mappings and patching data
                log.info(f'Creating mappings and patching {database} for {ds_name} and {conf["sample_id"]}')
                env = BASE_ENV + ['GDB_SAFE_SHUTDOWN=true']
                args = [
                    '-p', json.dumps([{}]),
                    '-x', json.dumps({
                        'query': conf['mapsample'],
                        'dataset': ds,
                        'sample_id': conf['sample_id'],
                        'session_id': SESSION_ID,
                        'cmd_id': CMD_ID,
                        'timeout': conf['timeout']['load'],
                    }),
                    *BASE_ARGS,
                ]
                CMD_ID += 1
                cnt = client.containers.run(raw_data_image, args, environment=env, volumes=vols, **BASE_CNT)
                load_timeout = conf['timeout']['load'] + conf['timeout']['extra_container']
                if log_res(results,
                           *cexec_query(cnt,
                                        timeout=load_timeout,
                                        rm=False,
                                        session_id=SESSION_ID,
                                        run_conf={'mapsample': raw_data_image})):
                    fatal(f"Timeout mapping {data_image}")
                log.info(f'Commiting images for {database} with {ds_name} and {conf["sample_id"]} as {data_image}')
                cnt.commit(data_image)
                cnt.remove()

            if load_only or sample_only:
                continue

            if not isfile(sample_path):
                fatal("Sample file is missing after loading [WAT]")
            sample_conf = validate_sample(json.load(open(sample_path, 'r')), conf, sample_fallback)


            # ------------------------------------------------------------------
            # Now that we have the correct sample data,
            # we ask for execution information (only once).
            if not qconf:
                log.debug(f"Get queries execution configuration list")
                args = ['-g', json.dumps({**sample_conf, **{'threads': conf['threads']}}), *BASE_ARGS]
                cnt = client.containers.run(db_image, args, environment=BASE_ENV, volumes=vols, **BASE_CNT)
                qconf = json.loads(grep_last_json(cexec_must(cnt, timeout=conf['timeout']['extra_container'] * 2)))
                validate_query_conf(qconf, conf, commit_suffix)

            # ------------------------------------------------------------------
            # Ensure proper image creation
            if commit_suffix:
                BASE_ENV = BASE_ENV + ['GDB_SAFE_SHUTDOWN=true']

            # ------------------------------------------------------------------
            # Actually execute the experiments for this database/dataset/sample
            for query in conf['queries']:
                log.info(f"Current query is {query}")

                for iteration in range(0, conf['iterations']):
                    if qconf[query]['only_once'] and iteration > 0:
                        log.info(f"Query {query} can be executed only once; skipping next iterations")
                        break

                    for mode in conf['modes']:
                        run_conf = {
                            'query': query,
                            'dataset': ds,
                            'sample_id': conf['sample_id'],
                            'mode': mode,
                            'session_id': SESSION_ID,
                            'threads': conf['threads'],
                            'timeout': conf['timeout'][mode.lower()],
                            'warmup': conf.get('warmup', []),
                            'cmd_id': CMD_ID,
                            'data_suffix': data_suffix,
                        }
                        CNT_TIMEOUT = conf['timeout'][mode.lower()] + conf['timeout']['extra_container']

                        log.info(f"Current mode is {mode}")
                        if not qconf[query].get(mode.lower() + '_ok', False):
                            log.info(f'Skipping query {query}. It cannot be executed in {mode} mode.')
                            continue

                        if mode == 'SINGLE_SHOT':
                            log.debug(qconf[query]['configurations'])
                            tos = 0
                            for i, c in enumerate(qconf[query]['configurations']):
                                args = [
                                    '-p', json.dumps([c]),
                                    '-x', json.dumps(run_conf),
                                    *BASE_ARGS,
                                ]
                                CMD_ID += 1
                                run_conf['cmd_id'] = CMD_ID,

                                cnt = client.containers.run(data_image,
                                                            args,
                                                            environment=BASE_ENV,
                                                            volumes=vols,
                                                            **BASE_CNT)
                                if log_res(results,
                                           *cexec_query(cnt,
                                                        timeout=CNT_TIMEOUT,
                                                        rm=not bool(commit_suffix),
                                                        session_id=SESSION_ID,
                                                        run_conf=run_conf)):
                                    if commit_suffix:
                                        fatal(f'Cannot commit, query timed-out.')
                                    tos += 1
                                    if tos > conf['timeout']['consecutive'] and conf['timeout']['consecutive'] != -1:
                                        log.warn(f'Query {query} timedout too many times, skipping next configs')
                                        break
                                else:
                                    tos = 0
                                    if commit_suffix:
                                        commit_image = f'{data_image}{commit_suffix}'
                                        log.debug(f"Commiting {commit_image}")
                                        cnt.commit(commit_image)
                                        cnt.remove(force=True)

                        else:  # 'BATCH', 'CONCURRENT'
                            cs = qconf[query]['configurations']
                            if mode == 'CONCURRENT' and qconf[query].get('alt_concurrent_conf'):
                                cs = qconf[query]['alt_concurrent_conf']

                            args = [
                                '-p', json.dumps(cs),
                                '-x', json.dumps(run_conf),
                                *BASE_ARGS
                            ]
                            CMD_ID += 1

                            cnt = client.containers.run(data_image,
                                                        args,
                                                        environment=BASE_ENV,
                                                        volumes=vols,
                                                        **BASE_CNT)
                            log_res(results,
                                    *cexec_query(cnt, timeout=CNT_TIMEOUT, session_id=SESSION_ID, run_conf=run_conf))


if __name__ == '__main__':
    main()
