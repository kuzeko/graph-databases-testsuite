# RUN EXPERIMENTS

## Example

```bash
make clean && python test.py -d -i dbtrento/gremlin-neo4j -v /dfs/vol3/ -e JAVA_OPTS="-Xms1G -Xmn128M -Xmx120G"
```

## The Commands

The `Makefile` allows to clean the directories and to collect results, 


**Results**

  - `follow`:     follows all logs stream
  - `follow_short`:   follows result and error logs stream
  - `collect`:    move results and log into `COLLECTED` folder
  - `rm_log`:     remove logs, but do not touch containers
  - `clean`:      clean up environement for next experiment
  - `purge`:      like clean but remove (not collect) the results

**Containers management command**

  - `rm_dead`:    Remove dead containers
  - `stop`:       Stop all running containers
  - `kill`:       Kill all running containers

**Image management command**

  - `rm_notag`:   Remove images without a tag
  - `rm_noname`:  Remove images without a name

**Dangerous management command**

   - `destroy`:    Reset the docker installation.
                     `rm -rf` images and containers



The `test.py` is in charge of spawning the docker enviroment, mounting the directories and sending commands to execute the queries. 
The most common options are

- `'-d'` enables the debug messages
- `'-v'` makes host filesystem resources available to the docker image
- `'-s'` points to a settings file, which lists datasets and queries, if no settings is provided it will try with all that exists in the runtime folder



## Environment Variables

Use `-e` to specify environment variables, e.g., `JAVA_OPTIONS` 

~~~bash
-e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx120G -XX:+UseG1GC'`
~~~

Different GDBs need different options, those are the known ones


* For Neo4j  & Sparksee: set `JAVA_OPTIONS`
 
    ~~~bash
    -e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx120G  -XX:+UseG1GC'
    ~~~


* OrientDB: we normalized java options variables in scripts, so it only needs `JAVA_OPTIONS`

	```bash
    -e JAVA_OPTIONS='-Xms4g -Xmx20g -XX:+UseG1GC -Dstorage.diskCache.bufferSize=102400'
    ```
    
   If you've got more than `32676/#{CPUs}` labels, e.g., with 4 CPU it is `8169`,  set: 
   
    ```bash
    -e MINIMUMCLUSTERS=true
	```

* Sparksee: set `JAVA_OPTIONS`
 
	```bash
	-e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx120g -XX:+UseG1GC '
	```

* Titan (both versions) never set `JAVA_OPTIONS`, use `TITAN_JAVA_OPTS` as follows:

	```bash
    -e USE_INDEX=true     # activate indexes in loading phase

    -e ALT_GET_LID=true   # use client side iteration to filter over nodes label
    -e TITAN_JAVA_OPTS='-Xms4G -Xmx120G -XX:+UseG1GC \
                        -Dcassandra.jmx.local.port=9999 -Dcom.sun.management.jmxremote.port=9999\
                        -Dcom.sun.management.jmxremote.authenticate=false'
    ```

* Blazegraph:  set `JAVA_OPTIONS`

	```bash
    -e  JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx120G -XX:MaxDirectMemorySize=60000m -XX:+UseG1GC'
    -e ALT_GET_LID=true   # use client side iteration to filter over nodes label
	```

**Also consider for all the above to provide these additional parameters**

```bash        
-XX:-UseGCOverheadLimit -XX:+UseConcMarkSweepGC -XX:+UseParNewGC  \
-XX:NewSize=6g  -XX:+CMSParallelRemarkEnabled -XX:+ParallelRefProcEnabled \
-XX:+CMSClassUnloadingEnabled
```

## Symlinks and Folders
When loading you can use symlinks to avoid copy the dataset around.
The typical use case is to create a symlink to the dataset file inside the `runtime/data` folder.

In this case you need to mount the parent folder of the original dataset file (or one of its ancestor) as a host volume inside docker. 
You can do that using the `-v` options of the `test.py` as `-v /absolute/file/path/directory` . 

As an example suppose to have a huge dataset at `/datastore/A.json`, instead of coping it to the disk where the code runs just create a symlink as following. 

```bash
ln -s /datastore/A.json runtime/data/
```

This will create a file called `A.json` inside `runtime/data` which will link to the real dataset file.

Then start the script with

```bash
python test.py -d -i dbtrento/gremlin-neo4j  -v /datastore [...] 
```

**The dataset files are needed only the first the image is created so the data is loaded in the database**.
When running queries - i.e., not loading - it’s enough to have an empty file as placeholder for the dataset or not file at all when `settings.json` is used.


##  Loading Phase

If **only** the loading phase is needed one could provide the `-l` flag to the test script, i.e., 

```bash
python test.py -d -l -i dbtrento/gremlin-neo4j  -v /datastore [...] 
```

Otherwise the system will automatically take action.


When an image is executed for the first time it has to read the data from a file and import it into the database system, and this image is then saved.
This means that for each database and dataset combination we will create a docker image.
Queries will run inside such docker image, but the status after the query execution will be erased, so that each query is run on a clean slate.

Some graph databases are not able to support loading through gremlin, for those different technique of loading are in place, called *Native Loading* as they usually use built-in tools.
Those are ArangoDB and OrientDB.
Blazegraph instead uses Tinkerpop3 code, also for the loading.

The load phase in `test.py` will start the database base image and run the /`loader.groovy` query or the native process, once the loading process has been completed, the `sampler.groovy` will retrieve `LID` for that database, if no sample exists for that dataset then also sampling is performed.
Then it will stop the container and commit it's state. 



##  Testing Phase

For each database and dataset combination create, in succession, a container for each combination of query and query-parameters.

Queries are executed in isolation, so a docker image is spawn, the query executed and then the image state deleted.

Some queries take parameters, e.g., `BFS` needs the starting node and the maximum number of hops.
Those are stored in the `meta/` folder  inside a file with the same name of the query with a `META` header like the following

    #META:BFS_DEPTH=[2-5];SID=[0-10]

For more detail and quirks look at the documentation at the beginning of the `read_meta` function in `test.py`.
Such parameters are made available at query time via environment variables having as name the name of the parameter and as value one in the defined domain.
In the example above, the environment variable `BFS_DEPTH` will have one of the values between `2` and `5` inclusive.

The container starts running and the name of the query to run is passed as environment variable as well.
The docker image will run either a init script from the `images/init` folder for any bootstrap step needed by the database, or will call the   `execute.sh` script in either `runtime/tp2` or `runtime/tp3` folders, depending on wether it uses tinkerpop 2 or 3.

The role of the  `execute.sh` script is to compose the code of the query to run. The script creates a file at `/tmp/query` by first selecting the current database driver and configuration contained in the `header.groovy.sh` script.
Then the code to load the sampled nodes arrays with the  `sampler.groovy` code. Finally the actual query code is appended. 
The `gremlin.sh` console script is invoked to execute the file created in this way.

For range parameters queries are executed twice.
Once, in isolation for every single parameter combination, i.e., once with `BFS_DEPTH=2` and `SID=0`, once with `BFS_DEPTH=2` and `SID=1` and so on.
Then again in `BATCH_MODE`, when the `BFS_DEPTH` is kept and all the parameters for `SID` are tested one after the other in random order.
In this second case queries are not run in isolation, but in a single batch.

The `BATCH_VAR` variable inside `test.py` lists the variables that are eligible to be run inside a batch.


## Logging

Every execution poduces different log files:
By default the `test.py` script creates 3 files:


  - `docker.log` contains the output of the docker image.

  - `test.log`  contains the content of the groovy scripts that are run.
    By default contains subprocess command stdout and stderr,
    when in debug mode contains also a copy of each `/tmp/query` that is executed.


  - `timeout.log`  contains informations about executions timing out.

  - `runtime/errors`
	Contains the stderr stream from gremlin.sh and also the info log from our scripts (we do not want to pollute the results file)

  - `runtime/results`
	A simil-CSV, containing the timing of the query. A first set of columns is common to all the queries, for some, follows additional fields useful for stats.
	It is not technically a valid CSV since not all the rows have the same number of columns.
		In debug mode it contains also all the stack trace and errors messages from java, sometime more useful than the one in errors to `docker.log`.


## ALL CONFIGS SHOULD BE IN /runtime/confs

- Gremlin Tp2 & Tp3 compatibility Docs
  * http://tinkerpop.apache.org/docs/current/reference/#_tinkerpop2_data_migration
  * http://tinkerpop.apache.org/docs/current/reference/#sugar-plugin
  * http://tinkerpop.apache.org/javadocs/current/full/org/apache/tinkerpop/gremlin/structure/Graph.html
  * http://tinkerpop.apache.org/javadocs/current/full/org/apache/tinkerpop/gremlin/process/traversal/dsl/graph/GraphTraversalSource.html
  * http://tinkerpop.apache.org/docs/current/dev/io/#graphson 

- Permissions:
User running the commands needs to have permissions to interact with the docker daemon.
Typically it's sufficient for it to be be member of the 'docker' group.

- Caveats:
  * In order to kill not respondiq queries with python2 we need 'subprocess32'.

    ~~~bash
    $ pip install -r requirements.txt
    ~~~

  * Sparksee requires a valid license to use all the resources available.
    Please provide a valid license editing `runtime/confs/sparksee.cfg`.
