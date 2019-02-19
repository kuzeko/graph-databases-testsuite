# Reproducibility Instructions

The following instructions are for reproducing the experiments we presented in our recent [VLDB paper](http://www.vldb.org/pvldb/vol12/p390-lissandrini.pdf):

> Beyond Macrobenchmarks: Microbenchmark-based Graph Database Evaluation.
  by Lissandrini, Matteo; Brugnara, Martin; and Velegrakis, Yannis.
  In PVLDB, 12(4):390-403, 2018. 

To reproduce and extend the experiments you should clone the branch `master` on the Github repository at the `vldb19` tag as follows

```
git clone --branch vldb19 --depth 1 https://github.com/kuzeko/graph-databases-testsuite.git
```

The repository contains all code, instructions, and queries. 
Data should be downloaded separately as explained below.

## Requirements

The experiments run on a _single large machine_.
To reproduce the experiments the suggested setup is:

- **Software:**
    - A GNU/Linux distribution (with git, bash, make, and wget)
    - Python 2, pip, and virtualenv
    - Docker
    - A valid licence for Sparksee (Academic licences are available from the vendor)

- **Hardware used in the experiments (and minimum required):**
    - RAM: 128 GB (minimum 64GB)
    - CPU: 24 cores (minimum 8)
    - Disk space: ~500GB free



## Getting the data

We provide a copy of all our datasets in a [single archive](https://disi.unitn.it/~brugnara/data/) and on a [mirror on Drive](https://drive.google.com/drive/folders/0BwX66B9ISrt4UXZrXzhIRGV2V3M?usp=sharing)

You can download, unpack it in the folder `runtime/data`, and check the integrity with the following commands:
 
```
mkdir -p runtime/data
cd runtime/data
wget https://disi.unitn.it/~brugnara/data/GraphDatabaseComparison_LissandriniBV_VLDB19.tar.gz

wget https://disi.unitn.it/~brugnara/data/GraphDatabaseComparison_LissandriniBV_VLDB19.tar.gz.md5
wget https://disi.unitn.it/~brugnara/data/GraphDatabaseComparison_LissandriniBV_VLDB19.tar.gz.sha1

md5sum -c GraphDatabaseComparison_LissandriniBV_VLDB19.tar.gz.md5
sha1sum -c GraphDatabaseComparison_LissandriniBV_VLDB19.tar.gz.sha1

tar -xzf GraphDatabaseComparison_LissandriniBV_VLDB19.tar.gz
md5sum -c md5.txt
cd ../../
```

## Software Setup

1. Build the databases images
    
    ```
    cd images && make vldb19
    ```

2. Install the python dependencies

    ```
    virtualenv -p python2 .venv
    source .venv/bin/activate
    pip install -r requirements.txt
    ```

3. Sparksee would require a valid license, as our graphs exceeds the limits of the free license.
  You can get it for free for research purposes on the [product website](http://www.sparsity-technologies.com/#licenses). Then run the following: 
  
    ```
    LICENSE='your_license'
    echo "sparksee.license=$LICENSE" >> runtime/confs/sparksee.cfg
    ```

4. If you want to disable/enable systems, change JVM options, or other configurations, you can edit the main benchmark invocation script.
Most options have been adjusted following the vendors' suggestions

    ```
    vim vldb19.sh
    ```


## Experimentation

All the experiments described in the paper will be executed by the provided script

```
./vldb19.sh
```

The script will run all queries for each system in all the version supported on all the datasets.
Queries that accept different seeds (e.g., find node by ID) will be instantiated automatically multiple times (e.g., with different node IDs).
Each query instance will be run multiple times (the parameter is configurable as well), e.g., for a query to retrieve nodes by ID, the framework will select 10 different IDs and each instance will be tested 3 times for a total of 30 separate runs.
This will result in some hundreds of queries run for each system.
**This entire process can take days or weeks!**

You may want to exclude some datasets or some queries (or some systems).
Specific configuration of queries and datasets are listed in the `settings` directory.
You can comment out datasets/queries to be skipped in the respective JSON files.
Yet, do not comment out queries for index creation unless you plan to not run indexed queries at all (in which case it is possible to comment out the respective steps in the `vldb19.sh` script).
If an entire system should be skipped, then the corresponding fragment should be commented out in the preamble of the `vldb19.sh` script.

With default configuration, for each system, the experiments will run (i) micro-benchmark queries without indexes,(ii) will create images with indexes for the macro benchmark and (iv) run the macro benchmark test on those, (v) create indexes for the micro-benchmark queries and (vi) run the micro-benchmark queries on those images.

**Note 1**: the commands to create images with indexes (steps ii. and v. ) have no timeout set and can take many hours.

**Note 2**: Not all systems support indexes


### Queries

The queries tested in the paper and corresponding files are listed below.

#### Load and Insert

|    #Q        |   Query        | File name |
------------- |  ------------- | -------------
|     1        | Load Dataset   | `loader.groovy` |
|     2        | New Node       | `insert-node.groovy` |
|     3        | New Edge       | `insert-edge.groovy` |
|     4        | New Edge with properties  | `insert-edge-with-property.groovy` |
|     5        | New Node property  | `insert-node-property.groovy` |
|     6        | New Edge property  | `insert-edge-property.groovy` |               
|     7        | New Node+Edges | `insert-node-with-edges.groovy` |


#### Read

|    #Q        |   Query        | File name |
------------- |  ------------- | -------------                    
|     8         | Count Nodes  |  `count-nodes.groovy` |
|     9         | Count Edges  | `count-edges.groovy` |
|    10         | Count distinct Edge Labels  |  `find-unique-labels.groovy` |
|    11         | Search nodes by property  | `node-property-search.groovy` |
|    12         | Search edges by property  |   `edge-specific-property-search.groovy` |
|    13         | Search edges by label  |  `label-search.groovy` |
|    14         | Search node by ID | `id-search-node.groovy` |
|    15         | Search edge by ID  | `id-search-edge.groovy` |

#### Update

|    #Q        |   Query        | File name |
------------- |  ------------- | -------------                     
|    16        | Update Node property  | `update-node-property.groovy` |
|    17        | Update Edge property  | `update-edge-property.groovy` |


#### Delete

|    #Q        |   Query        | File name |
------------- |  ------------- | -------------                    
|    18         | Delete Node  | `delete-nodes.groovy` |
|    19         | Delete Edge  | `delete-edges.groovy` | 
|    20         | Remove property from Node  | `delete-node-property.groovy` |
|    21         | Remove property from Edge  | `delete-edge-property.groovy` |


#### Graph Traversals

|     #Q       |   Query        | File name |
------------- |  ------------- | -------------                    
|   22         | Get incoming edges of given Node | `NN-incoming.groovy` |
|    23         | Get outgoing edges of given Node | `NN-outgoing.groovy` |
|    24         | Get all edges (in+out) filtered by label of given Node | `NN-both-filtered.groovy` |
|    25         | Get edge labels of incoming edges for given Node | `NN-incoming-unique-label.groovy` |
|    26         | Get edge labels of outgoing edges for given Node | `NN-outgoing-unique-label.groovy` |
|    27         | Get edge labels of all edges (in+out) for given Node | `NN-both-unique-label.groovy` |
|    28         | Get nodes with out-degree >=k  | `k-degree-in.groovy` |
|    29         | Get nodes with in-degree >=k  | `k-degree-out.groovy` |
|    30         | Get nodes with degree >=k (in+out) | `k-degree-both.groovy` |
|    31         | All nodes with at least 1 incoming edge  | `find-non-root-nodes.groovy` |
|    32         | BFS from given Node  | `BFS.groovy` |
|    33         | BFS from given Node restricted to set of labels | `BFS-labelled.groovy` |
|    34         | Unweighted shorted path  | `shortest-path.groovy` |
|    35         | Unweighted shorted path restricted to set of labels  | `shortest-path-labelled.groovy` |

### Outputs and statistics

All tests will produce `.csv` files plus some log files.

To extract statistical information regarding dataset and images size data, use the following command

```
# Dataset size on disk
du -s runtime/data/*.json2 | sed 's@runtime/data/@@' | sed 's@\.json2$@@' | sort -n > notebooks/datasets.tsv
docker images --format "{{.Repository}},{{.Size}}" | grep 'gremlin-' | grep 'json2,' | sed 's@dbtrento/@@' > notebooks/images.csv
```

The resulting files will be then processed by the provided python notebook used to produce charts.

## Process Data

The provided jupyter notebook will run inside a Docker container.
Start and play the notebook to process the result and produce the charts as described below.

```
docker run --rm -d -v "$(pwd)/notebooks":/home/jovyan/work -v "$(pwd)/collected/RESULTS":/results -p8888:8888 jupyter/scipy-notebook start-notebook.sh --NotebookApp.token=""

PAGE=$(ip route | grep default | awk -F' ' '{print "http://" $3 ":8888/tree/work/"}')
LAUNCHER=$(which xdg-open 2>/dev/null || which open)
$LAUNCHER $PAGE || echo "Please visit: $PAGE"
```

The notebook contains code to normalize, label, and aggregate the data.
The main data-points processed represent running-times for each query or group of queries.
