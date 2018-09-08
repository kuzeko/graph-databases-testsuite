#META:
/*	query="MATCH (n)<-[r]-() WHERE id(n)=";
	query+=id;
	query+=" RETURN count(r) as cnt";
graph.cypher(query).next();*/
query="START n=node(*) MATCH n return count(n) as cnt";
t = System.nanoTime();
count=graph.cypher(query).select('cnt').next();
//count = g.V.count().next();
exec_time = System.nanoTime() - t;

//DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(count)];
println result_row.join(',');

//g.shutdown();



//occhio che c'ho fatto modifica
