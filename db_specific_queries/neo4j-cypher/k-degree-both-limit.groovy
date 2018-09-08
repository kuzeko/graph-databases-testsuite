#META:
K=50
res =[]
query="START n=node(*) MATCH n--() WITH n,count(*) as degree WHERE degree>";
query+=K-1;
query+=" WITH n LIMIT 10 RETURN count(n) as cnt";
t = System.nanoTime();
count=graph.cypher(query).select("cnt").next();
//count = g.V.where(bothE().count().is(gte(K))).limit(10).fill(res).hasNext();
exec_time = System.nanoTime() - t;

//DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(K_VALUE)
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(count),String.valueOf(K)];
println result_row.join(',');

//g.shutdown();
