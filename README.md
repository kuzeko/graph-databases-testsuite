# Graph databases Test Suite
Docker Images, installation scripts, and testing &amp; benchmarking suite for Graph Databases

## Quick Setup

1. Clone this repo 

2. Install the dependencies:
   `pip install -r requirements.txt`

3. Prepare your image, we will go with `neo4j` **old version**:
   `cd images/ && make gremlin-neo4j.dockerfile`

4. Prepare a directory for your data
   `mkdir runtime/data`

5. You can use your own data, here we provide a sample:
    `cp sample_data/* runtime/data/`

6. Decide which query to run (they are in `runtime/tp[2,3]/queries`, their meta-parmeters are in `runtime/meta`), try the following for a test

   ```bash
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

7. run the `test.json` queries, with debug infos (`-d`), once (`-r 1`):
   `python test.py -d -i dbtrento/gremlin-neo4j  -e JAVA_OPTS="-Xms1G -Xmn128M -Xmx8G" -s test.json -r 1`



* More details on how to use the test suite: [RUN.md](RUN.md)

* Insights on the folder structure and scripts role: [FILES.md](FILES.md)
