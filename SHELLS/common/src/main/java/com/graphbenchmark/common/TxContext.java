package com.graphbenchmark.common;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.lang.reflect.InvocationTargetException;

public class TxContext {
	final GraphTraversalSource gts;
	Boolean supportsTransaction;

	public TxContext(final GraphTraversalSource gts, boolean commitAndCloseDefault) {
		this.gts = gts;
		try {
			this.supportsTransaction = gts.getGraph().features().supports(Graph.Features.GraphFeatures.class, "Transactions");
			GdbLogger.getLogger().debug("Supports transactions? %s", this.supportsTransaction);
		} catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException e) {
			this.supportsTransaction = false;
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Error checking support for transaction");
		}

		if (commitAndCloseDefault)
			this.close();
	}

	public TxContext(final GraphTraversalSource gts) {
		this(gts, true);
	}

	public void close() {
		if (!this.supportsTransaction) return;
		if (!this.gts.tx().isOpen()) return;
		commit();
		this.gts.tx().close();
	}

	public void open() {
		/* source: https://docs.janusgraph.org/basics/transactions/#common-transaction-handling-problems
			Transactions are started automatically with the first operation executed against the graph.
		 	One does NOT have to start a transaction manually.

			Transactions are automatically started under the TinkerPop semantics but not automatically terminated.
			Transactions must be terminated manually with commit() or rollback(). If a commit() transactions fails,
			it should be terminated manually with rollback() after catching the failure.
		*/
		throw new UnsupportedOperationException("Transaction are opened automatically!!!");
		// if (!this.supportsTransaction) return;
		// if (this.gts.tx().isOpen()) close();
		// this.gts.tx().open();
	}

	public void commit() {
		if (!this.supportsTransaction) return;
		if (!this.gts.tx().isOpen())
		GdbLogger.getLogger().debug("[T%d] Invoke gts level commit", Thread.currentThread().getId());
		this.gts.tx().commit();
	}

	public void rollback() {
		if (!this.supportsTransaction) return;
		if (!this.gts.tx().isOpen()) return;
		this.gts.tx().rollback();
	}
}
