query="MATCH (n) WHERE id(n)=-1 RETURN count(n) as cnt";
graph.cypher(query).select('cnt').next();
