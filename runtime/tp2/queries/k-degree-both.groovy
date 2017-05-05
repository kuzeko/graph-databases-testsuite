K=50
t = System.nanoTime();
count = g.V.filter{it.bothE.count() >= K}.count();
exec_time = System.nanoTime() - t;
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(count),String.valueOf(K)];
println result_row.join(',');
