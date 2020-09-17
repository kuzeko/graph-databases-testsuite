package com.graphbenchmark.shell.orientdb;

import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.schema.v2.Schema;
import com.graphbenchmark.settings.Dataset;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.javatuples.Pair;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

// Build schema before loading (otherwise will not be able to index)
// https://github.com/orientechnologies/orientdb-gremlin/issues/170
@SuppressWarnings("unused")
public class Load extends com.graphbenchmark.queries.mgm.Load {
	private GdbLogger log = GdbLogger.getLogger();

	public static void createProperty(OrientGraph og, String label, String property, Class type, boolean isEdge){
		ODatabaseDocument db = og.getRawDatabase();
		OSchema schema = db.getMetadata().getSchema();
		OClass cls = schema.getClass(og.labelToClassName(label, isEdge ? OClass.EDGE_CLASS_NAME : OClass.VERTEX_CLASS_NAME));
		cls.createProperty(property, dbTypeMap(type));
	}

	@Override
	public Collection<TimingCheckpoint> query(GraphTraversalSource gts, BaseQParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		// OrientDB requires a non transactional instance for schema update.
		// Re-invoke with correct GTS
		OrientGraph og = ((OrientdbShell) shell).mustNotTX(gts);
		return super.query(og.traversal(), params, sample, ds, thread_id, imgm, shell);
	}

	@Override
	public Collection<TimingCheckpoint> createSchema(GraphTraversalSource gts, Dataset ds, GenericShell shell) {
		OrientGraph og = (OrientGraph) gts.getGraph();
		List<TimingCheckpoint> tc = new ArrayList<>();

		// Disable minicluster
		og.getRawDatabase().command("ALTER DATABASE minimumclusters 1");

		// Infer/Load schema
		long start = System.currentTimeMillis();
		Pair<Stream<String>, Stream<String>> sl = (new Schema(ds.path)).getLabelStreams();
		tc.add(new TimingCheckpoint("Schema", System.currentTimeMillis()-start, "infer", (Object)ds.path));

		// Create schema
		start = System.currentTimeMillis();
		sl.getValue0().forEach(og::createVertexClass);
		sl.getValue0().close();
		tc.add(new TimingCheckpoint("Schema", System.currentTimeMillis()-start, "-vertex labels", (Object)ds.path));

		sl.getValue1().forEach(og::createEdgeClass);
		sl.getValue1().close();
		tc.add(new TimingCheckpoint("Schema", System.currentTimeMillis()-start, "-edges labels", (Object)ds.path));

		// Property declaration happens only for sampled nodes/edges in MapSample

		// Will this work since we are in non TX?
		og.commit();
		tc.add(new TimingCheckpoint("Schema", System.currentTimeMillis()-start, "-commit", (Object)ds.path));

		return tc;
	}

	private static OType dbTypeMap(Class c) {
		// See: SchemaInference.inferNodeProperty
		if (Date.class.equals(c))
			return OType.DATE;
		else if (Double.class.equals(c))
			return OType.DOUBLE;
		else if (Float.class.equals(c))
			return OType.FLOAT;
		else if (Integer.class.equals(c))
			return OType.INTEGER;
		else if (Long.class.equals(c))
			return OType.LONG;
		else if (Timestamp.class.equals(c))
			GdbLogger.getLogger().warning("Unsupported type g:Timestamp, treating as STRING");
		else if (UUID.class.equals(c))
			GdbLogger.getLogger().warning("Unsupported type g:UUID, treating as STRING");
		else if (Object.class.equals(c))
			return OType.ANY;
		return OType.STRING;
	}
}
