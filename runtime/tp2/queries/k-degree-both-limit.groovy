K=50
res = [] 
t = System.nanoTime();
count = g.V.filter{it.bothE.count() >= K}[0..9].store(res).hasNext();
exec_time = System.nanoTime() - t;
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(res),String.valueOf(K)];
println result_row.join(',');
