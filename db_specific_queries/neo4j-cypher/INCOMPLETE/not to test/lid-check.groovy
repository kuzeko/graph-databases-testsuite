SID = "0"


def execute_query_nodes(graph,g,prop_name,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,f){
    query="MATCH (a) WHERE id(a)="+id+" RETURN a."+prop_name+" as value";
    t = System.nanoTime();
    found = false
    v = 0
    search=graph.cypher(query).select('value');
    //search = g.V(id)
    if(search.hasNext()){
         v = f.infer_type(search.next())
         found= (v in  OBJECT_ARRAY)
    }
    if(!found){
        System.err.println("NODE LID " + id + " FOR " + v + "  MISSING!! ");
        System.exit(2);
    }

    exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE)
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), id, v, found];
    println result_row.join(',');
}


def execute_query_edges(graph, g,prop_name,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,f){
    t = System.nanoTime();
    found = false
    search = graph.cypher("MATCH ()-[n]-() WHERE id(n)="+id+" RETURN n").select("n");
    //search = g.E(id)
    if(search.hasNext()){
         e = search.next()
         e_in=graph.cypher("MATCH (a)-[n]-(b) WHERE id(n)="+id+" RETURN b."+prop_name+" as val").select('val').next();
         e_out=graph.cypher("MATCH (a)-[n]-(b) WHERE id(n)="+id+" RETURN a."+prop_name+" as val").select('val').next();
         e_lab=graph.cypher("MATCH (a)-[n]-(b) WHERE id(n)="+id+" RETURN TYPE(n) as val").select('val').next();

         for( es in OBJECT_ARRAY){
             /*println es.source;
             println es.target;
             println e_out;
             println e_in;
             println "-------";*/
             found = found || ( e_out.equals(f.infer_type(es.source))
                            && e_in.equals(f.infer_type(es.target))
                            && e_lab.equals(es.label) );
            /*found = found || ( f.infer_type(e.outVertex().value(prop_name)).equals(f.infer_type(es.source))
                            && f.infer_type(e.inVertex().value(prop_name)).equals(f.infer_type(es.target))
                            && e.label().equals(es.label) )*/
            //System.err.println( [es.source, es.target, es.label].join(' ')   );
         }
    }

    if(!found){
        logM = e == null ? "NULL" : [e.outVertex().value(prop_name), e.inVertex().value(prop_name), e.label()].join(' ')
        System.err.println("EDGE LID " + id + " FOR " + logM +  "   MISSING!! ");
        System.exit(2);
    }

    exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(EDGE)
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),id, String.valueOf(e), found];
    println result_row.join(',');

}



order_j = 1;
for (i in RAND_ARRAY) {
   execute_query_nodes(graph,g,f.uid_field,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,f);
   order_j++;
}


order_j = 1;
for (i in RAND_ARRAY) {
  execute_query_edges(graph,g,f.uid_field,EDGE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID,f);
  order_j++;
}


/*
4,6878805548,true
8,6880808548,true
9,6878808548,true
6,6815548,true
3,688080815548,true
5,8080815548,true
2,68788080815548,true
1,78844995536848,true
7,687880808,true
0,50879087846848,true
*/

/*7,e[7][8-/type/object/type->9],true
gremlin-neo4j-tp3,/runtime/data/freebase_0000.json,lid-check.groovy,0,0,2,182810057,9,e[9][1-/type/object/type->6],true
gremlin-neo4j-tp3,/runtime/data/freebase_0000.json,lid-check.groovy,0,0,3,124271283,8,e[8][9-/type/object/type->0],true
gremlin-neo4j-tp3,/runtime/data/freebase_0000.json,lid-check.groovy,0,0,4,111036107,5,e[5][5-/type/object/type->6],true
gremlin-neo4j-tp3,/runtime/data/freebase_0000.json,lid-check.groovy,0,0,5,106910612,2,e[2][2-/type/object/type->3],true
gremlin-neo4j-tp3,/runtime/data/freebase_0000.json,lid-check.groovy,0,0,6,96706445,4,e[4][4-/type/object/type->5],true
gremlin-neo4j-tp3,/runtime/data/freebase_0000.json,lid-check.groovy,0,0,7,120803890,0,e[0][0-/type/object/type->1],true
gremlin-neo4j-tp3,/runtime/data/freebase_0000.json,lid-check.groovy,0,0,8,107592070,1,e[1][1-/type/object/type->2],true
gremlin-neo4j-tp3,/runtime/data/freebase_0000.json,lid-check.groovy,0,0,9,93504610,3,e[3][3-/type/object/type->4],true
gremlin-neo4j-tp3,/runtime/data/freebase_0000.json,lid-check.groovy,0,0,10,91852748,6,e[6][7-/type/object/type->8],true*/



