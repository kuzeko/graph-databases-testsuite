#META:

t = System.nanoTime();
count = g.V.where(bothE().count().is(0)).count().next();
exec_time = System.nanoTime() - t;

//DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(K_VALUE)
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(count),"0"];
println result_row.join(',');

//g.shutdown();
