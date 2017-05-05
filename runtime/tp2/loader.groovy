#META:

// Shared loader:
//  Loads a database, in .GraphSON or .xml format, into a graph server
//   via graph object _g_ provided by header.groovy
//  Maintainer: Brugnara

def DATASET_FILE = System.env.get("DATASET")
def DEBUG = System.env.get("DEBUG") != null

def stime = System.currentTimeMillis()
if (DATASET_FILE.endsWith('.xml'))
    g.loadGraphML(DATASET_FILE)
else {
    System.err.println("Start loading")
    g.loadGraphSON(DATASET_FILE)
    System.err.println("End loading")
}
def exec_time = System.currentTimeMillis() - stime

try {
    g.commit();
} catch (MissingMethodException e) {
    System.err.println("Does not support g.commit(). Ignoring.");
}

if (DEBUG) {
    v = g.V.count();
    e = g.E.count();
    System.err.println(DATABASE + " loaded V: " + v + " E: " + e)
}

result_row = [DATABASE, DATASET, QUERY,'','','',String.valueOf(exec_time)]
System.out.println(result_row.join(','));


