package com.graphbenchmark.common;

import com.google.gson.Gson;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.samples.SampleManger;
import com.graphbenchmark.queries.mgm.QueryManager;
import org.apache.commons.cli.*;
import org.apache.commons.collections.map.HashedMap;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public abstract class GenericShell {
    // https://stackoverflow.com/questions/2988334/inheriting-the-main-method#answer-25677008
    protected static Class<? extends GenericShell> shellClass = null;

    public RunConf runConf;
    public String params;
    public Sample sample;

    public void init(RunConf runConf, String params) {
    	this.runConf = runConf;
        this.params = params;
    }

    // Returns a self-contained graph instance.
    public abstract GraphTraversalSource getConcurrentGTS();

    public GenericQuery createQuery(String queryCls) {
        Map<String, String> override = getQueryOverrideMap();
        try {
        	// Check if we shall override
			String queryClsO = override.getOrDefault(queryCls, queryCls);
			if (!queryCls.equals(queryClsO)) {
			    GdbLogger.getLogger().info("Overriding %s with %s", queryCls, queryClsO);
			}

            // NOTE: queryCls.class must be in the ClassPath (and we are compiling with it).
            Class<? extends GenericQuery> cls = (Class<? extends GenericQuery>) Class.forName(queryClsO);
            return cls.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public Map<String, String> getQueryOverrideMap() {
    	return new HashedMap();
    }

    /*
     * @return execution information string (database, dataset)
     */
    public String getExecEnv() {
        String[] shellClassName = shellClass.getName().split("[.]");
        String queryName = this.runConf.query.substring("com.graphbenchmark.".length());
        return String.join(";", new String[]{
                this.runConf.session_id,
                this.runConf.cmd_id,
                shellClassName[shellClassName.length - 1],
                (new File(this.runConf.dataset.path)).getName(),
				this.runConf.sample_id.toString(),
                this.runConf.data_suffix,
                queryName,
                VersionMGM.getVersion(GenericShell.class),
                this.runConf.timeout.toString(),
                this.runConf.mode.toString(),
                this.params,
        });
    }

    // Wrap a checkpoint with execution context.
    public String fmtResult(TimingCheckpoint checkpoint) {
        return String.format("%s;OK;%s", this.getExecEnv(), checkpoint);
    }

    public String fmtTimeout() {
        return String.format("%s;TIMEOUT", this.getExecEnv());
    }

    public String fmtOOM() {
        return String.format("%s;OOM", this.getExecEnv());
    }

    public final void exec() {
        GenericQuery q = this.createQuery(this.runConf.query);
        List<? extends GenericQuery> warmups = List.of();
        if (q.allowsWarmup())
            warmups = this.runConf.warmup.stream().map(this::createQuery).collect(Collectors.toList());

        // Load real samples only if needed.
        this.sample = null;
        if (q.requiresSamples() || warmups.parallelStream().anyMatch(GenericQuery::requiresSamples)) {
        	sample = SampleManger.load(this.runConf.dataset.path, this.runConf.sample_id);
        } else {
            sample = new Sample();
            sample.sample_id = this.runConf.sample_id;
        }

        // Execute warm-up queries
        Gson gson = new Gson();
        ExperimentSettings es = ExperimentSettings.infer(sample); // NOTE: this breaks the assumption with current conf.
        Sample finalSample = sample;
        // GdbLogger.getLogger().debug("Start warmup with %d queries", warmups.size());
        warmups.forEach(w -> {
            QueryConf qc = w.getConf(es);
            List<? extends BaseQParam> wparams =
                    gson.fromJson(gson.toJson(qc.configurations), w.getMetaType());

            if (qc.batch_ok) {
                // Execute as batch
				this.exec_batch(w, wparams, finalSample);
            } else if (qc.single_shot_ok) {
                // Execute as single with only first parameter.
                this.exec_one_shot(w, wparams, finalSample);
            } else {
                GdbLogger.getLogger().fatal("Warmup query must support either batch or single_shot: %s does not",
                        qc.getClass().getSimpleName());
            }
        });
        // GdbLogger.getLogger().debug("Warmup completed");

        // Parse invocation parameters
        List<BaseQParam> params = gson.fromJson(this.params, q.getMetaType());

        // Execute query
        Collection<String> timings = null;
        switch (this.runConf.mode) {
            case BATCH:
                timings = this.exec_batch(q, params, sample);
                break;
            case CONCURRENT:
                timings = this.exec_concurrent(q, params, sample);
                break;
            case SINGLE_SHOT:
                timings = this.exec_one_shot(q, params, sample);
                break;
            default:
            	GdbLogger.getLogger().fatal("Invalid execution mode");
        }

        // Returning via stdout
		timings.forEach(System.out::println);
    }

    private Collection<String> exec_to(int threads, List<Callable<Collection<TimingCheckpoint>>> calls) {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Collection<TimingCheckpoint>>> features;

        long start = System.currentTimeMillis();
        try {
            // Please (re-)read the doc before touching this code.
            features = pool.invokeAll(calls, this.runConf.timeout, TimeUnit.SECONDS);
            pool.shutdown();
        } catch (InterruptedException e) {
            GdbLogger.getLogger().fatal("Shell interrupted, killed by external timeout or SIGTERM?");
            return null; // Will never be invoked
        } catch (OutOfMemoryError e) {
            return List.of(fmtOOM());
        }

        // Check if we timed-out
		// TODO: we may instead return how many succeeded vs failed.
        if (features.stream().anyMatch(f -> (!f.isDone()) || f.isCancelled()))
            return List.of(fmtTimeout());

        List<String> res = new ArrayList<>();
        try {
        	// We cannot use functional becouse we want early termination on OOM errors
            for (Future<Collection<TimingCheckpoint>> f : features) {
                // Collect partial results from different threads/runs
                res.addAll(f.get().stream().map(this::fmtResult).collect(Collectors.toList()));
            }
        } catch (CancellationException | InterruptedException e) {
            e.printStackTrace();
            GdbLogger.getLogger().fatal("Query interrupted: %s", e.getMessage());
        } catch (OutOfMemoryError e) {
            return List.of(fmtOOM());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof OutOfMemoryError)
                return List.of(fmtOOM());

            e.printStackTrace();
            GdbLogger.getLogger().fatal("Error in query execution: %s", e.getMessage());
        }

        // The following checkpoint will be the "TOTAL" query execution time.
        // Basic stats should be build around this.
        long end = System.currentTimeMillis();
        res.add(fmtResult(new TimingCheckpoint("TOTAL", end-start, this.params)));
        return res;
    }

    public final Collection<String> exec_one_shot(GenericQuery q, List<? extends BaseQParam> ps, Sample s) {
        if (ps.size() != 1)
            GdbLogger.getLogger().fatal("In SINGLE_SHOT mode only one configuration a time is allowed, %d were provided", ps.size());
        return exec_to(1, List.of(() -> q.execute(this.getConcurrentGTS(), ps.get(0), s, this.runConf.dataset, this)));
    }

    public final Collection<String> exec_concurrent(GenericQuery q, List<? extends BaseQParam> ps, Sample s) {
        return exec_to(runConf.threads, (List<Callable<Collection<TimingCheckpoint>>>)(ps.stream()
                .map(p -> (Callable<Collection<TimingCheckpoint>>)
                        (() -> q.execute_concurrent(this.getConcurrentGTS(), p, s, runConf.dataset)))
                .collect(Collectors.toList())));
    }

    public final Collection<String> exec_batch(GenericQuery q, List<? extends BaseQParam> ps, Sample s) {
        GraphTraversalSource gts = this.getConcurrentGTS();
        return exec_to(1, (List<Callable<Collection<TimingCheckpoint>>>)(ps.stream()
                .map(p -> (Callable<Collection<TimingCheckpoint>>)
                        // TODO: check if this is ok, working code had (this.getConcurrentGTS())
                        (() -> q.execute_concurrent(gts, p, s, runConf.dataset)))
                .collect(Collectors.toList())));
    }

    public GenericIndexMgm getIndexManager(Graph g) {
    	return new GenericIndexMgm(g);
    }

    public static void main(String ... args) throws ParseException, IOException {
        Options opts = new Options();
        opts.addOption("g", "generate-config", true, "Generate all possible invocation configurations");
        opts.addOption("x", "execute", true, "Execute");
        opts.addOption("p", "parameters", true, "List of query parameters");
        opts.addOption("d", "debug", false, "Enable verbose debugging");
        opts.addOption("h", "help", false, "Display this message");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(opts, args);

        GdbLogger.setup(opts.hasOption("d"), shellClass.getSimpleName());
        GdbLogger log = GdbLogger.getLogger();

        if (shellClass == null)
        	log.fatal("The static shellClass variable was not set");

        if (cmd.hasOption('h')) {
            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( shellClass.getName(), opts );

            log.close();
            System.exit(0);
        }

        Gson gson = new Gson();
        if (cmd.hasOption("g")) {
            ExperimentSettings exp = gson.fromJson(cmd.getOptionValue("g"), (Type) ExperimentSettings.class);
            if (exp == null)
                log.fatal("Error parsing experiment setup (-g): %s", cmd.getOptionValue("g"));
            System.out.print(gson.toJson(QueryManager.generateConfs(exp)));

            log.close();
            System.exit(0);
        }

        if (!cmd.hasOption("x"))
        	log.fatal("Specify at least -x or -g.");

        if (!cmd.hasOption("p"))
            log.fatal("Please specify also the query parameters with -p.");

        RunConf runConf = gson.fromJson(cmd.getOptionValue("x"), RunConf.class);
        log.delayed("CONTEXT", "[session_id] %s", runConf.session_id);
        log.delayed("CONTEXT", "[dataset] %s", runConf.dataset.path);
        log.delayed("CONTEXT", "[query] %s", runConf.query);
        log.delayed("CONTEXT", "[-x] %s", cmd.getOptionValue("x"));
        log.delayed("CONTEXT", "[-p] %s", cmd.getOptionValue("p"));

        GenericShell sh = null;
        try {
            sh = (GenericShell) shellClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            log.fatal("Error init the shell class");
        }

        // Init and invoke
        try {
            sh.init(runConf, cmd.getOptionValue('p'));
            sh.exec();
        } catch (Exception e) {
            e.printStackTrace();
            log.fatal("Fatal error executing the query");
        }

        log.close();
        System.exit(0);
    }
}
