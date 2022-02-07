Version 2.0.1

# Website
https://graphbenchmark.com

# Quick Start
```bash
git clone https://github.com/kuzeko/graph-databases-testsuite.git graphbenchmark
cd graphbenchmark

# Build everything and create sample config
make init

# Run experiments
cd CONTROL
python control.py run-benchmark --config conf.toml -r ../runtime
```

# Step by step targeted installation

*Assuming we wanto to run the benchmark only for `neo4j`*

```bash
git clone https://github.com/kuzeko/graph-databases-testsuite.git graphbenchmark

cd graphbenchmark
cd IMAGES
make neo4j.dockerfile

cd ..
cd SHELLS
make all

cd ..

cd CONTROL
make .venv
source .venv/bin/activate 

```


# Datasets
Download: [Mirror 1](http://disi.unitn.it/~brugnara/data/GraphDatabaseComparison_BrugnaraLV_VLDBJ.tgz)
MD5:  3be624ed569b8b55316a80e6bd7600f1 
SHA1: 70b551d5d7ef6aed49273c3382c82a09334004b7 

`#V` | `#E` | `#Lv` | `#Le` | `#Pv` | `#Pe`
--- | --- | --- | --- | --- | --- |
ldbc.10    | 30M  | 178M | 11 | 15  | 18  | 4 
DBpedia    | 16M  | 27M  | 1  | 12K | 47K | 0 
air-routes | 3742 | 57K  | 4  | 2   | 15  | 1 

# In details
## Bootstrap
This project requires `python >= 3.7`, `virtualenv`, and `docker`, `jdk`, `maven`, `make`.  
To bootstrap the project execute `make init`,
this will perform the following actions:

1) Create docker images for all supported systems.
2) Compile the test-suit for each system.
3) Create a basic runtime directory with a demo dataset.
4) Create a base experiment configuration.

## Run experiments
To execute the experiments with the automatically generated configuration, issue the following commands from a user that has permission to interact with the docker daemon.
```
$ cd CONTROL            
$ source .venv/bin/activate
$ python control.py run-benchmark --config conf.toml -r ../runtime
```


### Advanced
#### Partial execution
To exclude some queries/datasets/systems from the next tests simply comment them out from the configuration file by prefixing them with the `#`

#### Indexes & Persistence
By default, each test is performed in total isolation to guarantee fairness.
But it may come the time when persistence is necessary, like for tasting the effect of indexes.

To have the state of a database saved after the execution of a query _e.g._ CreateIndex), invoke `control.py` with the flag `--commit_suffix <descriptive_tag>`.
You will be able to use such version as source by using the `--data_suffix <descriptive_tag>` flag.
While there is no limit to the number of snapshot that can be taken, they must be generated one at a time; this means 1 query, which takes only one parameters configuration, executed only in `SINGLE_SHOT` mode.

#### Limit resource usage
The java virtual machine resources are controlled by the `jvm_opts` configuration option, that will then be used as `JAVA_OPTIONS`.
This variable can also be overwritten by each shell (`shell/java_opts`).

The container resources (and capabilities) can be manipulated by the `cnt_opts` configuration options.
These options will be passed directly to the docker-py API ([doc](https://docker-py.readthedocs.io/en/stable/containers.html)).
The most relevant is perhaps `cnt_opts/mem_limit`, by default is set to 90% of the host memory.

## Adding a new datasets
Create a new entry in the configuration file follows.
```toml
[datasets."my_dataset.json"]
path = "/runtime/data/my_dataset.json"
uid_field = "uid"
```

The `path` can either be absolute to anywhere on the system,
or relative to the `runtime` directory.
In the latter case you may use a path starting with `/runtime/`.

The dataset must be encoded in the [GraphSON 3.0](https://tinkerpop.apache.org/docs/3.4.1/dev/io/#graphson-3d0) format.

All node must have a common __property__ of type `long` with a unique value for each; the `uid_field` is the name of said property.

All `labels` must be valid PostgreSQL table names; any ASCII sequence `[a-Z][a-Z0-1_]+` should be fine.


## Adding a new query
To add a new query start by creating a class that extends `GenericQuery`.  
Most of the fields/method are self-explicative, but please look out for:
- `getMetaType()` should return a `Type` that can be used by the `GSON` library to parse the query invocation parameter. Example:
```
TypeToken<ArrayList<SimpleQParam>>(){}.getType()
```
- The configuration returned by `getConf()` can specify two different sets of invocation parameters for when the query is invoked in sequential mode (SINGLE SHOT or BATCH) and in CONCURRENT mode.
- Your query class must be thread safe.

### TIPS
* Query blueprints  
    Before implementing a query from scratch, you may consider to extend one of the template we provide in `queries.blueprint`.

* Transactions  
    Transactions are not supported by all systems.
    To simplify development, we provide `TransactionCtx`.
    Since some systems implement opportunistic transactions, _i.e._ you transaction may fail instead of being serialized, we provide a `RetryTransactionCtx`. Resources should be fetched inside this context.

    All data updates must be performed inside a transaction.

* Adding new nodes  
    Some systems does not supports adding nodes via `GraphTraversalSource`, please use the `Graph` object like `gts.getGraph().addVertex(label)`.  
    Furthermore, some does not support label-less nodes, thus always use labels. 
    Finally, keep in mind that some database does not allow for implicit or concurrent schema modification.


## Adding a new system
### Shell
Crate a maven modules named `shell-<database>`, where `<database>` is the name used in the configuration, with (at least) the following dependencies.

```xml
<dependencies>
    <dependency>
        <artifactId>common</artifactId>
        <groupId>com.graphbenchmark</groupId>
        <version>2.0-alpha</version>
    </dependency>

    <dependency>
        <groupId>org.apache.tinkerpop</groupId>
        <artifactId>gremlin-core</artifactId>
        <!-- DO NOT TOUCH IT!! 3.4.5 breaks neo and arango -->
        <!-- <version>3.4.2</version> -->
        <version>3.4.2</version>
    </dependency>
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.8.6</version>
    </dependency>
    <dependency>
        <groupId>commons-cli</groupId>
        <artifactId>commons-cli</artifactId>
        <version>1.4</version>
    </dependency>
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <version>28.1-jre</version>
    </dependency>
</dependencies>
```

Then create a class that extends `GenericShell`.
Things to look out for:
- the `getConcurrentGTS` method shall return a thread-safe `GraphTraversalSource` object connected to the database in question.
- the static attribute `shellClass` must be set to `YourClassName.class`.
- since there is no common way to create indexes, `getIndexManager` is your opportunity to supply a custom manager: a class that implements `GenericIndexMgm`.

If your database requires a __schema__, you may create a class that extends `queries.mgm.Load` and overrides `createSchema` and `qSupportPatch`; do not forget to call `super()` at the end of the latter. 
 
 
### Docker image
If you intend to configure you database as a java library and thus shares the `jvm` with the framework then you may inherit from our `graphbenchmark.com/base` image and be ready to go.

_Example_:

```
FROM graphbenchmark.com/base:latest

LABEL com.graphbenchmark.dbname="my_new_database"
```

If you system lives as a standalone process than you shall:
- Write a `dockerfile` that installs all your dependencies, `numactl`, and `java 8` or later (best 11).
- If possible use Debian buster slim as base image: `openjdk:13-slim-buster`.
- Write an init script that starts your database, waits until it is online, and then invokes the shell -- that will be mounted at `/shell.jar` -- with all the script arguments and the proper `numactl` flags sets. The init script must also take care of the safe shutdown of the database process when the `$GDB_SAFE_SHUTDOWN` env variable is set.
- Set the init script as the `entrypoint` for the docker image`.
- The database data shall live inside the docker image and __not__ in volumes.

Please refer to `IMAGES/base.dockerfile`, `IMAGES/extra/common-init.sh`, and `IMAGES/extra/janus/janus-init.sh` for examples.

#### A note on `JAVA`.
* OpenJDK vs Oracle JDK   
    From [https://openjdk.java.net/]().
    > Download and install the open-source JDK for most popular Linux distributions.
    > Oracle's free, GPL-licensed, production-ready OpenJDK JDK 13 binaries are at [jdk.java.net/13]();
    > Oracle's commercially-licensed JDK 13 binaries for Linux, macOS, and Windows, based on the same code, are [http://www.oracle.com/technetwork/java/javase/downloads/index.html](here).

    ... <span style="text-decoration:underline">based on the same code</span> ...

* Respecting CPU and memory constraints  
    From [https://hub.docker.com/_/openjdk]()
    > Inside Linux containers, OpenJDK versions 8 and later can correctly detect container-limited number of CPU cores and available RAM.
    > In OpenJDK 11 this is turned on by default.
    > In versions 8, 9, and 10 you have to enable the detection of container-limited amount of RAM using the following options:
    >
    > ```$ java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap ..```


## Known quirks

* Complete schema must be known a-priory.  
    The schema is automatically inferred the first time a new dataset is used and then saved into `runtime/schemas/`. 
    Additional labels and properties, defined in `QSchemaSupport` and used by the provided queries, are injected at loading time.

    If you are developing new queries that requires custom labels/properties you shall inject them in the schema.
    This is due to the limitations of some systems.

* Sqlg and PostgreSQL.  
    While PostgreSQL default configuration is good enough for most application,
    we suggest to provide a specific one for the host the system will run on.
    A good starting point seams [pgtune](https://pgtune.leopard.in.ua/).
    
    The configuration file shall be dropped in `IMAGES/extra/postgresql.conf`,
    the one we provide is for a {24 threads, 128GB, HHD} system.
    If you prefer instead to use the default configuration, just remove the file.


## 3rd parties issues
### Janus graph
- [Deadlock when concurrent node modification in berkeley · Issue #1623 · JanusGraph/janusgraph](https://github.com/JanusGraph/janusgraph/issues/1623)

### Neo4j
- [Multiple threaded access to graph? · Issue #19 · neo4j-contrib/neo4j-tinkerpop-api-impl](https://github.com/neo4j-contrib/neo4j-tinkerpop-api-impl/issues/19) 
- [Cannot retrieve node with `::` in label name? · Issue #20 · neo4j-contrib/neo4j-tinkerpop-api-impl](https://github.com/neo4j-contrib/neo4j-tinkerpop-api-impl/issues/20#issuecomment-670491586) 

### OrientDB
- [Cannot create property-index on graph loaded via gremlin API · Issue #170 · orientechnologies/orientdb-gremlin](https://github.com/orientechnologies/orientdb-gremlin/issues/170)
- [Inconsistent transaction beheviour w/remote: · Issue #169 · orientechnologies/orientdb-gremlin](https://github.com/orientechnologies/orientdb-gremlin/issues/169)
- [Maven Group ID in pom.xml is not the official one? · Issue #172 · orientechnologies/orientdb-gremlin](https://github.com/orientechnologies/orientdb-gremlin/issues/172)

### PgSql
- [No support for `addV` only `addVertex` · Issue #15 · twilmes/sql-gremlin](https://github.com/twilmes/sql-gremlin/issues/15)
- [Implementing shortest path, does sqlg supports repeat().times().cap() · Issue #380 · pietermartin/sqlg](https://github.com/pietermartin/sqlg/issues/380)


### ArangoDB
- [[Questions] On loading GraphSON encoded graph. · Issue #58 · ArangoDB-Community/arangodb-tinkerpop-provider](https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider/issues/58)
- [Add new "kind" of edges · Issue #59 · ArangoDB-Community/arangodb-tinkerpop-provider](https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider/issues/59)
- [[feature request] Support schema evolution · Issue #60 · ArangoDB-Community/arangodb-tinkerpop-provider](https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider/issues/60)
- [Provide schma to use with arangoimp · Issue #62 · ArangoDB-Community/arangodb-tinkerpop-provider](https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider/issues/62)
- [AQL: too many collections/shards · Issue #63 · ArangoDB-Community/arangodb-tinkerpop-provider](https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider/issues/63)
- [https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider/issues/39](https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider/issues/39)
- [https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider/issues/38](https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider/issues/38)

### Stardog
- [No Support for Gremlin 3.4.X](https://community.stardog.com/t/support-for-gremlin-3-4-x/2287)
