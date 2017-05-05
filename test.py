#!/usr/bin/env python

"""
    Assumptions:
    - The entry script (CMD) of every docker-image will eventually execute
      `/runtime/{tp2,tp3}/execute.sh`, after database bootstrap operation have been completed.
    - `runtime` directory structure

    Provided guarantees:
    - Query are executed in alphabetical order
    - Files whose name start by '.' are ignored.

    Maintainer: Brugnara <mb@disi.unitn.eu>
"""

import os
import re
import sys
import json
import hashlib
import logging
import argparse
import datetime
import tempfile
import itertools
import subprocess32 as subprocess


from os import listdir
from base64 import b64encode
from itertools import product
from os.path import join, exists, isdir, normpath, abspath, basename

# -----------------------------------------------------------------------------
# BASE EXPERIMENT SETTINGS:
ITERATIONS=5
DEBUG=False
PRINT_ONLY=False

# Per query timeout (in seconds) [2 hours]
# 2602253683670
#             00
TIMEOUT=2 * 60 * 60

# When a query timeout, skip testing with other meta values?
TIMEOUT_MAX_RETRY = 7 
# SKIP = False --> 0
# SKIP = True --> 1


# -----------------------------------------------------------------------------
# LOGGING
logger, to_log = None, None
SUBPROCESS_LOGF_NAME='docker.log'
SUBPROCESS_LOGF=None


# -----------------------------------------------------------------------------
# Docker images
DATABASES = [
    'neo4j',
    'orientdb',
    'sparksee',
    'arangodb',     # NOTE: it uses its own loader (@see README).
    'titan',
    'blazegraph',   #  Uses Tp3
    'neo4j-tp3',    #  Uses Tp3
    'titan-tp3',    #  USes Tp3
    # leas as last
    '2to3',         #  Only for conversion
]
IMAGES = [
    'dbtrento/gremlin-neo4j',
    'dbtrento/gremlin-orientdb',
    'dbtrento/gremlin-sparksee',
    'dbtrento/gremlin-arangodb',
    'dbtrento/gremlin-titan',
    'dbtrento/gremlin-blazegraph',
    'dbtrento/gremlin-neo4j-tp3',
    'dbtrento/gremlin-titan-tp3',
    'dbtrento/gremlin-2to3',
]
assert len(DATABASES) == len(IMAGES)

# Changing this requires rebuilding all images with updated paths
RUNTIME_DIR='/runtime'

# -----------------------------------------------------------------------------
# Global variables
ENV_EXT = [
    # Extra env variables,
    # @see -e option.
    "-e", "RUNTIME_DIR=" + RUNTIME_DIR,
]

CMD_EXT = [
    # Command extra arguments,
    # @see -v option for volumes
    "--cap-add=IPC_LOCK",
    "--ulimit",
    "memlock=-1:-1",
]

SETTINGS_FNAME=None
DEFAULT_SETTINGS_FILE = "./settings.json"

# Support for 'batch only mode'
BATCH_VAR = ('SID', 'INDEX')
BATCH_ONLY = False

# Flags
LOAD_ONLY=False
FORCE_LOAD=False

# -----------------------------------------------------------------------------
# Core

def main(root):
    """ Main function
            root: absolute path to 'runtime/'

        + for each database
        | + for each dataset
        | | + if not loaded
        | | | load $dataset in $database
        | | | commit $database as $data_iamge
        | | -
        | |
        | | + for each query
        | | | execute $query on $data_image
        - - -
    """

    logger.debug('Root {}'.format(root))
    datasets, queries, meta_dir = get_test_settings(root, SETTINGS_FNAME)

    logger.info("Main testing loop (for each database)")
    for db_name, image in zip(DATABASES, IMAGES):

        logger.info("[CURRENT] DB: {} w/docker image {}".format(db_name, image))

        for dataset in datasets:
            logger.info("[CURRENT] DATASET: " + dataset)

            # Setting up common environment
            dataset_path = join(RUNTIME_DIR, 'data', dataset)
            data_image = '{}_{}'.format(basename(image), dataset)\
                    .replace(' ', '-')

            samples_file = join(RUNTIME_DIR, 'presampled/', 'samples_' + dataset)
            lids_file = join(RUNTIME_DIR, 'presampled/',
                    'lids_' + '_'.join([dataset, db_name, b64encode(image)]))

            base_environ = ENV_EXT + [
                "-e", "DATABASE_NAME=" + db_name,       # neo4j
                "-e", "DATABASE=" + basename(image),    # gremlin-neo4j
                "-e", "DATASET=" + dataset_path,
                "-e", "SAMPLES_FILE=" + samples_file,
                "-e", "LIDS_FILE=" + lids_file,
            ]
            logger.debug('base_environ: ' + ' '.join(base_environ))

            try:
                load_env = base_environ + [
                    "-e", 'QUERY=loader.groovy',
                    "-e", 'ITERATION=0',
                ]
                exec_query(root, image, load_env, commit_name=data_image)
            except subprocess.TimeoutExpired:
                # should never happen
                logger.error('Timeout while loading. How (no timeout set)?.')
                sys.exit(42)
            except subprocess.CalledProcessError as e:
                # error loading this database stop all
                logger.fatal('Failed loading {} into {}'.format(db_name, image))
                sys.exit(3)

            if LOAD_ONLY:
                logger.info("Load only flag is set, skipping tests.")
                continue

            logger.info("Starting benchmark")
            for query in queries:
                TIMEOUT_COUNTER = 0
                logger.info("[CURRENT] query: " + query)

                # Query meta variables parsing and range generation
                meta_names, meta_values, contains_batch =\
                        read_meta(meta_dir, query)
                if BATCH_ONLY and not contains_batch:
                    logger.info("BATCH ONLY mode: skipping " + query)
                    continue

                logger.info("Query {} on {} using {} (image: {})"\
                        .format(query, dataset, db_name, image))
                query_env = base_environ + ["-e", "QUERY=" + query]


                logger.info("({}) meta parameters: {}."\
                        .format(len(meta_names), meta_names))
                for values in meta_values:
                    # Express meta parameters as ENV variables
                    meta_env = list(itertools.chain.from_iterable(
                        ("-e", "{}={}".format(n, v)) for n, v in\
                            zip(meta_names, values)))

                    try:
                        test_env = ['--rm'] + query_env + meta_env
                        exec_query(root, data_image, test_env, timeout=TIMEOUT)
                        TIMEOUT_COUNTER = 0
                    except subprocess.TimeoutExpired:
                        to_log.error(','.join([basename(image), dataset, query,
                            str(TIMEOUT), str(zip(meta_names, values))]))
                        TIMEOUT_COUNTER += 1

                        if TIMEOUT_MAX_RETRY != 0 and TIMEOUT_COUNTER >= TIMEOUT_MAX_RETRY:
                            logger.warn('SKIP_ON_TIMEOUT giving up on {}'\
                                    .format(query))
                            break

                    except subprocess.CalledProcessError, e:
                        logger.error('Executing query {}'.format(query))
                        logger.error(e)
    logger.info("Done for now")


def exec_query(root, docker_image, env, timeout=None, commit_name=None):
    """ Create a container from the $docker_image,
        append the $env list (of `-e` option) to the docker command.
        Waits for the container to exit.
        - timeout: enforce maximum running time.
        - commit_name: if specified and the container returns successfully (0),
                       the container will be committed as $commit_name

        Globals:
            DEBUG, PRINT_ONLY, FORCE_LOAD, CMD_EXT, SUBPROCESS_LOGF, logger

        Throws:
            subprocess.TimeoutExpired: the timeout fired
            subprocess.CalledProcessError: there was an error in the container
    """

    # Check if what we are going to commit (if any) already exists
    if commit_name:
        command = ["docker", "images", "-q", commit_name]
        if len(subprocess.check_output(command).strip('\n ')):
            if not FORCE_LOAD:
                logger.info("Loading: use existing {}".format(commit_name))
                return
            logger.info("Loading: overriding {}".format(commit_name))

    # Build command
    command = ["docker", "run", "-v", root + ':' + RUNTIME_DIR] + CMD_EXT + env
    container_name = 'CNT_' + hashlib.sha256(
            ''.join(command + [docker_image]).encode()).hexdigest()
    command +=  ['--name', container_name, docker_image]

    try:
        # Execute the query
        logger.debug('Command: ' + ' '.join(command))
        if PRINT_ONLY:
            print(' '.join(command))
            return

        subprocess.check_call(command, stdout=SUBPROCESS_LOGF,
                stderr=SUBPROCESS_LOGF, timeout=timeout)

        # Commit if we should
        if commit_name:
            logger.info('Committing {} as {}'.format(container_name, commit_name))
            command = ["docker", "commit", container_name, commit_name]
            subprocess.check_call(command, stdout=SUBPROCESS_LOGF,
                    stderr=SUBPROCESS_LOGF)
    finally:
        rmf(container_name)


def rmf(container_name):
    """ Force the removal of a container
    """
    command = ["docker", "rm", "-f", container_name]
    try:
        error=None
        proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        outs, errs = proc.communicate(timeout=TIMEOUT)
        # TODO: clean up this 'except' section, looks crappy.
    except subprocess.TimeoutExpired, e:
        proc.kill()
        error = e
        outs, errs = proc.communicate()
    except Exception, e:
        error = e
        outs, errs = proc.communicate()
    finally:
        proc.kill()
        if error and (not ("No such container" in  str(errs))):
            logger.error(error)


def read_meta(meta_dir, query_name, iterations=ITERATIONS):
    """
        Parse metadata.
        Format:
            *first line of the query file only*
            - Starts with '#META:'
            - variables are encoded as 'name=value'
                - value can be range as '[start-end]'
                    [0-3] == {0,1,2,3}
                - value can be set as {1,2,3,stella}
                    - set values can not have space in front or tail;
                      if needed use ' eg: {1,' ciao mondo '}
                    - space between value will be stripped:
                      {1,2,3} == {1   , 2, 3}
            - different variables should be separated by ;
            * this chars are not allowed as parts of set values: []-{},;=

            #META:first=[1-4];second={1,2,3,stella}

            -> 'first' in (1,2,3,4)
            -> 'second' in set{1,2,3,stella}
    """
    with open(join(meta_dir, query_name), 'r') as query:
        first_line = query.readline()

    signature='#META:'
    meta_names=['ITERATION']
    meta_values=[xrange(0,iterations)]
    contains_batch=False

    first_line=first_line.strip(' ,;')
    if first_line.startswith(signature):
        line=first_line[len(signature):]
        if len(line.strip()):
            for (name, value) in (x.split('=') for x in line.split(';')):
                name, value = name.strip(), value.strip()
                meta_names.append(name)
                if value[0] == '[':
                    start, end = value.strip('[]').split('-')
                    if BATCH_ONLY and name in BATCH_VAR:
                        meta_values.append([int(end)])
                        contains_batch=True
                    else:
                        meta_values.append(xrange(int(start), int(end) + 1))
                else:
                    meta_values.append(map(lambda v: v.strip(), value.strip('{}').split(',')))
    return meta_names, product(*meta_values), contains_batch


# -----------------------------------------------------------------------------
# Support functions

def dir_iter(directory):
    return (f for f in sorted(listdir(directory)) if not isdir(f) and not f.startswith('.'))

# TODO: rename
def check_paths(root):
    dataset_dir = join(root, 'data')
    query_tp2 = join(root, 'tp2/queries')
    query_tp3 = join(root, 'tp3/queries')
    meta = join(root, 'meta')

    assert exists(dataset_dir), "dataset directory should exists inside root"
    assert exists(query_tp2), "tp2 query directory should exists inside root"
    assert exists(query_tp3), "tp3 query directory should exists inside root"
    assert exists(meta), "meta directory should exists inside root"

    return dataset_dir, query_tp2, query_tp3, meta

# -----------------------------------------------------------------------------
# Logging

def init_loggers():
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')

    # Script logger
    logger = logging.getLogger(__name__)
    logger.setLevel(logging.INFO)

    handler = logging.FileHandler('test.log')
    handler.setLevel(logging.DEBUG)
    handler.setFormatter(formatter)
    logger.addHandler(handler)

    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.DEBUG if DEBUG else logging.INFO)
    handler.setFormatter(formatter)
    logger.addHandler(handler)

    logger.info('Logger configuration completed')

    # Timeout tracker
    to_log = logging.getLogger('timeout')
    to_log.setLevel(logging.INFO)

    to_handler = logging.FileHandler('timeout.log')
    to_handler.setLevel(logging.INFO)
    to_handler.setFormatter(formatter)
    to_log.addHandler(to_handler)

    logger.info('Timeout logger configuration completed')

    return logger, to_log

# -----------------------------------------------------------------------------
# Command line arguments parsing

def parse_arguments():
    parser = argparse.ArgumentParser(
            description='dbTrento "graph databases comparison" script.')

    parser.add_argument('-d', '--debug', action='store_true', default=False,
            help='enable debug information')

    parser.add_argument('-e', '--env', action='append', metavar='ENV',
            help='Set this ENV variables in the executed docker')
    parser.add_argument('-v', '--volume', action='append', metavar='VOLUME',
            help='Mount additional volumes (use for resources under ln -s)')

    parser.add_argument('-l', '--load_only', action='store_true',
            default=False, help='prepare the images but do not run tests')
    parser.add_argument('-f', '--force_load', action='store_true',
            default=False, help='recreate data image even if it exists')
    parser.add_argument('-b', '--batch_only', action='store_true',
            default=False, help='run only "batch" tests')

    parser.add_argument('-s', '--settings',
            default=None, help='JSON file with dataset and queries fnames')

    parser.add_argument('-i', '--image', action='append', metavar='IMAGE_TAG',
            help='run only on those images/databases')

    parser.add_argument('-p', '--print-only', dest='print_only',  action='store_true',
            default=False, help='only print the docker command')


    args = parser.parse_args(sys.argv[1:])


    global DEBUG
    DEBUG = DEBUG or args.debug

    global PRINT_ONLY
    PRINT_ONLY = PRINT_ONLY or args.print_only


    global LOAD_ONLY, FORCE_LOAD, BATCH_ONLY, S
    LOAD_ONLY=args.load_only
    FORCE_LOAD=args.force_load
    BATCH_ONLY=args.batch_only

    global SETTINGS_FNAME
    SETTINGS_FNAME=args.settings

    parse_images(args.image)
    parse_volumes(args.volume)
    parse_env(args.env)

    if DEBUG:
        ENV_EXT.extend(['-e', 'DEBUG=true'])


def parse_images(i):
    global DATABASES, IMAGES
    if not(i and len(i)):
        DATABASES = DATABASES[:-1]
        IMAGES = IMAGES[:-1]
        return

    new_db, new_img = [], []
    for index, name in enumerate(IMAGES):
        if name in i:
            new_db.append(DATABASES[index])
            new_img.append(IMAGES[index])
    DATABASES, IMAGES = new_db, new_img

def parse_volumes(vols):
    if not(vols and len(vols)):
        return
    CMD_EXT.extend(itertools.chain(*map(lambda x:
        ['-v', '{0}:{0}'.format(abspath(x))], vols)))

def parse_env(env):
    if env and len(env):
        ENV_EXT.extend(itertools.chain.from_iterable(map(lambda v: ['-e', v], env)))
        # Consider if enforce add of ' only if not '
        # v.split('=',1)[0]+ "='"+v.split('=',1)[1].strip("'")+"'"], env)))
        logger.info('Additional ENV from prompt:')
        logger.info(ENV_EXT)
    else:
        logger.info('You may want to specify, via -e,' + \
                '(JAVA_TOOL_OPTIONS, JAVA_OPTIONS, JAVA_OPTS)')
        logger.info("Example: -e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx120G'")


# -----------------------------------------------------------------------------
def comment_remover(text):
    def replacer(match):
        s = match.group(0)
        if s.startswith('/'):
            return " " # note: a space and not an empty string
        else:
            return s
    pattern = re.compile(
        r'//.*?$|/\*.*?\*/|\'(?:\\.|[^\\\'])*\'|"(?:\\.|[^\\"])*"',
        re.DOTALL | re.MULTILINE
    )
    return re.sub(pattern, replacer, text)

# Test settings
def get_test_settings(root, fname=None):
    """ Return the list of datasets and queries onto run the tests.
        fname: name of the JSON file from which read the settings.
            {
                'datasets': [],
                'queries': []
            }
        If no file is provided the settings are inferred from the directories
        content; i.e. ls runtime/dataset ls runtime/tp2/query

        NOTE: All queries (begin from settings.queries or ls)
            must exists in both runtime/tp2/queries, runtime/tp3/queries
    """

    if fname is None and exists(DEFAULT_SETTINGS_FILE):
        fname = DEFAULT_SETTINGS_FILE
    datasets = queries = None
    if fname:
        if not exists(fname):
            logger.fatal('Settings file {} does not exists'.format(fname))
            sys.exit(4)

        try:
            with open(fname) as f:
                settings = json.loads(comment_remover(f.read()))
        except ValueError, e:
            logger.fatal('Settings file {} is not valid JSON'.format(fname))
            sys.exit(5)

        datasets, queries = settings['datasets'], settings['queries']
        logger.info("From settings: {} Datasets and {} Queries".format(
            len(datasets), len(queries)))

    dataset_dir, query_tp2, query_tp3, meta_dir = check_paths(root)
    tp2_queries = set(dir_iter(query_tp2))
    tp3_queries = set(dir_iter(query_tp3))
    metas = set(dir_iter(meta_dir))

    if datasets is None:
        datasets = sorted(list(dir_iter(dataset_dir)))
    datasets = [x for x in datasets if x]

    if queries is None:
        queries = sorted(list(tp2_queries | tp3_queries))
    queries = [x for x in queries if x]

    common_queries = tp2_queries & tp3_queries & metas
    missing_queries = set(queries) - common_queries
    assert not len(missing_queries),\
            'Missing implementation of {} in tp2, {} in tp3, {} in meta'.format(
            missing_queries - tp2_queries, missing_queries - tp3_queries,
            missing_queries - metas)

    return datasets, queries, meta_dir


# =============================================================================

if __name__ == '__main__':
    logger, to_log = init_loggers()
    parse_arguments()

    with open(SUBPROCESS_LOGF_NAME, 'a') as SUBPROCESS_LOGF:
        main(abspath("./runtime"))
