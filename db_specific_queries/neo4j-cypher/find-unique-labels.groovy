#META:

query="match ()-[r]-() return count(distinct type(r)) as cnt";
t = System.nanoTime();	
count=graph.cypher(query).select('cnt').next();
//count = g.E.label.dedup().count().next();	
exec_time = System.nanoTime() - t;

//DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(count)];
println result_row.join(',');

//g.shutdown();
