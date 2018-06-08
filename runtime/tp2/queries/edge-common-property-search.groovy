#META:
PROPERTY_NAME= "test_common_property";
PROPERTY_VALUE = "test_value";

t = System.nanoTime();	
count = g.E.has(PROPERTY_NAME,PROPERTY_VALUE).count();	
exec_time = System.nanoTime() - t;

               //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETERS
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(count), String.valueOf(PROPERTY_NAME), String.valueOf(PROPERTY_VALUE)];
println result_row.join(',');

//g.shutdown();
