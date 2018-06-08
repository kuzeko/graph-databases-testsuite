#META:

// Shared loader:
//  Loads a database, in .GraphSON or .xml format, into a graph server
//   via graph object _g_ provided by header.groovy
//  Maintainer: Lissandrini

DEBUG = System.env.get("DEBUG") != null

System.err.println("Start loading");
stime = System.currentTimeMillis();
try{

#BLAZEgraph.setBulkLoad(true);

    if (DATASET.endsWith('.xml')) {
        g.loadGraphML(DATASET);
    } else if (DATASET.endsWith('.json3')) {
        final InputStream is = new FileInputStream(DATASET)
        final GraphSONMapper mapper = graph.io(IoCore.graphson()).mapper().create()
        //graph.io(IoCore.graphson()).writer().mapper(mapper).create().writeGraph(os, graph);
        graph.io(IoCore.graphson()).reader().create().readGraph(is, graph)
        is.close();
    } else {
#SQLG   graph.tx().normalBatchModeOn();
        LegacyGraphSONReader r = LegacyGraphSONReader.build().batchSize(2_000_000).create();
                          // r = LegacyGraphSONReader.build().create()
        InputStream stream ;
        try{
            stream = new FileInputStream(DATASET);
            // r.readGraph(new FileInputStream(DATASET), graph)
            r.readGraph(stream, graph);
        } finally {
            stream.close();
        }
    }


    try {
        graph.tx().commit();
    } catch (MissingMethodException e) {
        System.err.println("Does not support g.tx().commit(). Ignoring.");
    }

#BLAZEgraph.setBulkLoad(false);


}catch( Exception e){

    traceFile = "/runtime/logs/loader.exception."+System.currentTimeMillis()+".trace"

    fos = new FileOutputStream(new File(traceFile), true);
    ps = new PrintStream(fos);
    e.printStackTrace(ps);
    fos.close()
    System.err.println("Error " + e.getMessage());
    System.exit(2)

}

exec_time = System.currentTimeMillis() - stime

System.err.println("End loading");

result_row = [DATABASE, DATASET, QUERY,'','','',String.valueOf(exec_time)]
println result_row.join(',')


if (DEBUG) {
    System.err.println(" ########################################## DEBUG ");
    g = graph.traversal()
    vid = g.V().next().id();
    System.err.println(" First node is " + vid);
    System.err.print("Stats: Nodes... ");
    v = g.V().count().next();
    System.err.println("& Edges Nodes");
    e = g.E().count().next();
    System.err.println(DATABASE + " loaded V: " + v + " E: " + e);
    System.err.println(" ########################################## DEBUG ");

}
