((GroovyClassLoader) this.class.classLoader).parseClass(new File("/tmp/BlazegraphRepositoryProvider.groovy"))

// instantiate a sail and a Sesame repository
repo = BlazegraphRepositoryProvider.open("/srv/db.jnl", true)
repo.shutDown()

System.err.println("Repo initialized")

repo = BlazegraphRepositoryProvider.open("/srv/db.jnl", true)
System.err.println("Repo Reopened")

graph = BlazeGraphEmbedded.open(repo)
System.err.println("Graph Obtained ")

g = graph.traversal()
System.err.println("Traversal Obtained")


System.exit(0)

