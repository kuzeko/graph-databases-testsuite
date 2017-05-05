 DATASET = System.env.get("DATASET");              // path

g = new TinkerGraph() 
g.loadGraphSON(DATASET)
GraphSONWriter.outputGraph(g,DATASET+"2",GraphSONMode.EXTENDED)
