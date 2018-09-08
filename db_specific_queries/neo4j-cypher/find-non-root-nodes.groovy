#META:


query="MATCH ()-[]->(a) RETURN count(distinct a) as cnt";
t = System.nanoTime();	
c=graph.cypher(query).select("cnt").next();
//c = g.V.out.dedup().count().next();	
exec_time = System.nanoTime() - t;

//DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(c)];
println result_row.join(',');

//g.shutdown();
