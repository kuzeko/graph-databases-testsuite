#META:
K=50

t = System.nanoTime();
count = g.V.where(outE().count().is(gte(K))).count().next();
exec_time = System.nanoTime() - t;

//DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(K_VALUE)
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(count), String.valueOf(K)];
println result_row.join(',');

//g.shutdown();