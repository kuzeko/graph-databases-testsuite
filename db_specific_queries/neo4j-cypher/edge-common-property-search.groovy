#META:
PROPERTY_NAME= "test_common_property";
PROPERTY_VALUE = "test_value";

query="MATCH ()-[r]->() WHERE r.";
query+=PROPERTY_NAME;
query+=" = \"";
query+=PROPERTY_VALUE;
query+="\" RETURN count(*) AS cnt";

t = System.nanoTime();	

count=graph.cypher(query).select('cnt').next();
//count = g.E.has(PROPERTY_NAME,PROPERTY_VALUE).count().next();	
exec_time = System.nanoTime() - t;

               //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETERS
result_row = [ DATABASE, DATASET, QUERY,"0", ITERATION, "0", String.valueOf(exec_time), String.valueOf(count), String.valueOf(PROPERTY_NAME), String.valueOf(PROPERTY_VALUE)];
println result_row.join(',');

//g.shutdown();
