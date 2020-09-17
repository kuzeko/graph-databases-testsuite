package com.graphbenchmark.queries.mgm;

import com.graphbenchmark.common.*;
import com.graphbenchmark.queries.*;
import com.graphbenchmark.queries.vldb.*;
import com.graphbenchmark.queries.vldbj.*;

import java.util.HashMap;
import java.util.List;

public class QueryManager {

	public final static List<? extends GenericQuery> queries = List.of(
			new Load(),
			new Sampler(),
			new MapSample(),
			new CreateVertexIndexes(),
			new IndexUID(),
			new Noop(),

			// VLDB 19 [ok]
			new InsertNode(),
			new InsertEdge(),
			new InsertEdgeWithProperty(),
			new InsertNodeProperty(),
			new InsertEdgeProperty(),
			new InsertNodeWithEdges(),

			new CountNodes(),
			new CountEdges(),
			new CountUniqueEdgeLabels(),
			new NodePropertySearch(),
			new NodeUIDSearch(),
			new EdgePropertySearch(),
			new EdgeLabelSearch(),
			new IdSearchNode(),
			new IdSearchEdge(),

			new UpdateNodeProperty(),
			new UpdateEdgeProperty(),

			new DeleteNode(),
			new DeleteEdge(),
			new DeleteNodeProperty(),
			new DeleteEdgeProperty(),

			new NNincoming(),
			new NNoutgoing(),
			new NNbothFiltered(),
			new NNincomingUniqueLabel(),
			new NNoutgoingUniqueLabel(),
			new NNbothUniqueLabel(),
			new KDegreeIn(),
			new KDegreeOut(),
			new KDegreeBoth(),
			new CountNonRoot(),
			new BFS(),
			new BFSfiltered(),
			new ShortestPath(),
			new ShortestPathFiltered(),

			new InsertNodeWithProperty(),//new

			// VLDBJ 20


			new NodePathLabelSearch(),
			new NodePathLabelSearchOut(),

			new NodeKReachability(),
			new NodeKReachabilityOut(),

			new NodeLabelledKReachability(),
			new NodeLabelledKReachabilityOut(),
			//new AllPathLabelSearch(), --> too complex, removed
			//new AllPathLabelSearchOut(), --> too complex, removed

			//new AllPatternTriangle(), --> too complex, removed
			new AllEdgeLabelPatternTriangle(),
			new AllNodeLabelPatternTriangle(),
			new NodePatternTriangle(),


			//new AllPatternSquare(), --> too complex, removed
			new AllEdgeLabelPatternSquare(),
			new AllNodeLabelPatternSquare(),
			new NodePatternSquare(),



			new CountUniqueNodeLabels(),
			new NodeLabelSearch(),
			new NodeLabelledPropertySearch(),

			// Testing
			new MixCountDelete(),
			new MixCountInsertSchema(),
			new MixCountInsertSimple(),

			new TestLoader()
	);

	public static HashMap<String, Object> generateConfs(ExperimentSettings exp) {
		String version = VersionMGM.getVersion(QueryManager.class);
		HashMap<String, Object> tmp = new HashMap<>();

		tmp.put("_queries_version", version); 				// Set version
		tmp.put("_loader", Load.class.getName());
		tmp.put("_sampler", Sampler.class.getName());
		tmp.put("_mapsample", MapSample.class.getName());

		for (GenericQuery q : queries) {				// Set queries
			QueryConf qc = q.getConf(exp);
			tmp.put(q.getClass().getName(), qc);
		}
		return tmp;
	}
}

