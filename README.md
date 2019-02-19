# Graph Databases Benchmarking Suite

**This repository contains the code for the following publication:**

> **Beyond Macrobenchmarks: Microbenchmark-based Graph Database Evaluation**
> 
> *by* **Matteo Lissandrini, Martin Brugnara, Yannis Velegrakis**
> 
> *published in the _Proceedings of the Conference on Very Large Databases (PVLDB), 2018_*

A guide to reproduce the experiments in the paper is provided in the [REPRODUCE.md](REPRODUCE.md) file.


---

**Comparison of Graph Databases**  
Graph databases (GDBs) are grounded on the concepts of graph theory: abstracting data in the form of nodes, edges and properties.

A variety of GDB implementations has been appearing, and most of the systems are usually accompanied with proprietary tests showcasing their performance in specific use-cases.

There are also some experimental comparisons that focus on particular aspects and some benchmarking suites have been proposed. Most of them are outdated, others are just extremely partial in their scope. A broad study of scalability and applicability of existing solutions is required in order to highlight in which context (type of data, management of metadata, and type of queries) each existing implementation performs better and at which scale. In particular we set the goal to identify the actual advantages or limitations of specific design choices, operator implementations, and processing techniques in different GDB systems.

**This repository**  
Thi repository contains the framework we developed to carry out such study. We made it open-source such that 1) it can be used  at the current state to verify our findings and 2) be extended via the addition of queries or database images to carry out new tests.

In particular, we provide docker images for the following databases: 

- ArangoDB
- Blazegraph
- Neo4j (Versions 1.9, and 3.0 )
- OrientDB
- PostgreSQL (via sqlg)
- Sparksee
- Titan (Versions 0.5.4, and 1.0.0)

Other may be added as explained in [SETUP.md](SETUP.md). 


## Quick Setup

1. Clone this repository.

2. Install the dependencies (Python 2.7):  

   ```
   virtualenv .venv && source .venv
   pip install -r requirements.txt
   ```

3. Prepare the database image ( in this example we will use `neo4j` v1.9 for Tinkerpop 2):
   `cd images/ && make gremlin-neo4j.dockerfile`

4. Provide the test dataset[s]. Let's use the provided toy graph:

   ```bash
   # Make sure the directory exists.
     mkdir -p runtime/data
   # Provided the data.
     cp ~/freebase_small.json{,2} runtime/data/
   ```

5. Configure the experiment with the select query-set.  
   Queries  are stored in `runtime/tp[2,3]/queries` and their meta-parmeters can be found in `runtime/meta`.

   ```bash
   # Configuration example
   cat<<EOF > test.json
   {
     "datasets": [
        "freebase_0000.json2"
     ],
      "queries": [
         "count-edges.groovy",
         "count-nodes.groovy"
     ]
   }
   EOF
   ```

6. Run the tests:  

   ```bash
   # Execute three times (-r) the tests defined in 'test.json' (-s) 
   # on neo4j (-i) providing JAVA_OPTS as extra environment variable (-e).
   python test.py -r 3 \
   		-i dbtrento/gremlin-neo4j \
   		-e JAVA_OPTS="-Xms1G -Xmn128M -Xmx8G" \
   		-s test.json
   ```

## Personalize your experiments


**How to use** the test suite: [RUN.md](RUN.md)

Insights on the **folder structure** and scripts role: [FILES.md](FILES.md)

Guide to include a **new database system** in the framework: [SETUP.md](SETUP.md)

## Authors
Matteo Lissandrini  <ml@disi.unitn.eu> (@Kuzeko).  
Martin Brugnara <mb@disi.unitn.eu> (@MartinBrugnara).


***Contributors***   
Liviu Bogdan (UniTN), Allan Nielsen (UniTN), Denis Gallo (UniTN).
