K=50
t = System.nanoTime();
count = g.V.where(inE().count().is(gte(K))).count().next();
exec_time = System.nanoTime() - t;
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(count), String.valueOf(K)];
println result_row.join(',');
