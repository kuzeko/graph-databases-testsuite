#META:

query="MATCH (n) WHERE NOT (n)<--() RETURN count(n) as cnt";
t = System.nanoTime();
count=graph.cypher(query).select("cnt").next();
//count = g.V.where(inE().count().is(0)).count().next();
exec_time = System.nanoTime() - t;

//DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(K_VALUE)
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(count), "0"];
println result_row.join(',');

//g.shutdown();
