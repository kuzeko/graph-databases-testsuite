package com.graphbenchmark.shell.orientdb;

import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.HashMap;
import java.util.Map;

public class OrientdbShell extends GenericShell {
	static {
		shellClass = OrientdbShell.class;
	}

	final static String DB_URI = "plocal:/data/thedb";

	private static final Boolean lck = false;
	private static OrientGraphFactory og = null;

	private void initFactory() {
		if (og == null) {
			synchronized (lck) {
				if (og == null) {
					// og = new OrientGraphFactory(DB_URI).setupPool(1, Math.min(this.runConf.threads,2));
					og = new OrientGraphFactory(DB_URI);
				}
			}
		}
	}

	@Override
	public GraphTraversalSource getConcurrentGTS() {
		initFactory();
		try {
			// http://orientdb.com/docs/3.0.x/java/Java-Multi-Threading-Usage.html
			GdbLogger.getLogger().debug("Orient graph requested from %d", Thread.currentThread().getId());
			OrientGraph g = og.getTx();
			// NOTE: This assumes we are already in the right thread (Otherwise we shall call activate from there)
			//	Without this it does not work.
			//  See implementation of OrientGraph.commit()
			//  See http://orientdb.com/docs/3.0.x/general/Concurrency.html
			g.makeActive();
			return g.traversal();
		} catch (Exception e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Error creating graph object");
		}
		return null;
	}

	public OrientGraph getNonConcurrentDB() {
		initFactory();
		OrientGraph g = og.getNoTx();
		g.makeActive();
		return g;
	}

	@Override
	public GenericIndexMgm getIndexManager(Graph g) { return new IndexMgm(g, this); }

	@Override
	public Map<String, String> getQueryOverrideMap() {
		HashMap<String, String> h = new HashMap<>();
		h.put(com.graphbenchmark.queries.mgm.Load.class.getName(), Load.class.getName());
		h.put(com.graphbenchmark.queries.mgm.MapSample.class.getName(), MapSample.class.getName());
		h.put(com.graphbenchmark.queries.mgm.IndexUID.class.getName(), IndexUID.class.getName());
		return h;
	}


	private static final Boolean noTxLck = true;
	private OrientGraph noTxOg = null;

	public OrientGraph mustNotTX(Graph g) {
		if (this.noTxOg != null)
			return this.noTxOg;

		synchronized (noTxLck) {
			if (this.noTxOg != null)
				return this.noTxOg;

			try {
				GdbLogger.getLogger().debug("Killing main GraphTx object");
				if (g != null) {
					g.tx().rollback();
					g.tx().close();
					g.close();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				GdbLogger.getLogger().fatal("Hack for NO-transaction failed");
			}
			this.noTxOg = getNonConcurrentDB();
		}
		return this.noTxOg;
	}

	public OrientGraph mustNotTX(GraphTraversalSource gts) {
		if (this.noTxOg != null)
			return this.noTxOg;

		synchronized (lck) {
			if (this.noTxOg != null)
				return this.noTxOg;

			try {
				GdbLogger.getLogger().debug("Killing main GraphTx object");
				if (gts != null) {
					gts.tx().rollback();
					gts.tx().close();
					gts.getGraph().close();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				GdbLogger.getLogger().fatal("Hack for NO-transaction failed");
			}
			this.noTxOg = getNonConcurrentDB();
		}
		return this.noTxOg;
	}
}
