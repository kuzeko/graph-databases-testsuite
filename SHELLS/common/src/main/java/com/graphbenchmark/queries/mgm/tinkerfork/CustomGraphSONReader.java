/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.graphbenchmark.queries.mgm.tinkerfork;

import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.samples.SampleManger;
import org.apache.commons.lang.NotImplementedException;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.io.GraphReader;
import org.apache.tinkerpop.gremlin.structure.io.Mapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONTokens;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.structure.util.Attachable;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedEdge;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedProperty;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.star.StarGraph;
import org.apache.tinkerpop.gremlin.structure.util.star.StarGraphGraphSONDeserializer;
import org.apache.tinkerpop.gremlin.util.function.FunctionUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.apache.tinkerpop.shaded.jackson.core.type.TypeReference;
import org.apache.tinkerpop.shaded.jackson.databind.JsonNode;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.apache.tinkerpop.shaded.jackson.databind.node.JsonNodeType;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A @{link GraphReader} that constructs a graph from a JSON-based representation of a graph and its elements.
 * This implementation only supports JSON data types and is therefore lossy with respect to data types (e.g. a
 * float will become a double, element IDs may not be retrieved in the format they were serialized, etc.).
 * {@link Edge} and {@link Vertex} objects are serialized to {@code Map} instances.  If an
 * {@link org.apache.tinkerpop.gremlin.structure.Element} is used as a key, it is coerced to its identifier.  Other complex
 * objects are converted via {@link Object#toString()} unless there is a mapper serializer supplied.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 *
 *
 * The original version parses the whole dataset and loads it in memory, with any real dataset this thing explodes.
 * This modified version parses the file 2 times but minimze the memory. In the first pass it created the nodes and
 * a reverse index for json.id -> system.id. In the second pass it creates the edges usign the aformentinoed index.
 */
public final class CustomGraphSONReader implements GraphReader {
    private final ObjectMapper mapper;
    private final long batchSize;
    private final GraphSONVersion version;
    private boolean unwrapAdjacencyList = false;

    final TypeReference<Map<String, Object>> mapTypeReference = new TypeReference<Map<String, Object>>() {};
    final TypeReference<LinkedHashMap<String, Object>> linkedHashMapTypeReference = new TypeReference<LinkedHashMap<String, Object>>() {};

    private CustomGraphSONReader(final Builder builder) {
        mapper = builder.mapper.createMapper();
        batchSize = builder.batchSize;
        unwrapAdjacencyList = builder.unwrapAdjacencyList;
        version = ((GraphSONMapper)builder.mapper).getVersion();
    }

    @Override
    public void readGraph(final InputStream inputStream, final Graph graphToWriteTo) throws IOException {
    	throw new NotImplementedException();
	}

	// TODO: get flag for serial or concurrent!
    public void readGraph(final File inputFile, final Graph graphToWriteTo, String uid_field) throws IOException {
    	// Note:
        // Real ids are not exposed by id() after creation. Either cache the nodes (will cause OOM), or refresh the object.
        // Real vertex object are needed to create edges.

		// NOTE: Optimization opportunity. If we can ensure that the ids are sequential than we can use an array instead.
        final Map<Long, Object> rev_index = new HashMap<>();

        final AtomicLong counter = new AtomicLong(0);

        final boolean supportsTx = graphToWriteTo.features().graph().supportsTransactions();
        final Graph.Features.EdgeFeatures edgeFeatures = graphToWriteTo.features().edge();

        // First pass: create vertex and rev_index
        readVertexStrings(new FileInputStream(inputFile)).<Vertex>map(FunctionUtils.wrapFunction(line -> readVertex(new ByteArrayInputStream(line.getBytes()), null, null, null)))
        .forEach(v -> {
            final Long id = SampleManger.forceLong(v.id()); // At this point it either is int32 or int64

			// NOTE: Always add without ID, so that the ID field is populated with the correct value from the DB.
            //  If T.ID is specified, then the value is not updated.
			// TODO: consider using addV() to chain vertex property.
            final Vertex vertex = graphToWriteTo.addVertex(T.label, v.label());
            v.properties().forEachRemaining(vp -> {
                final VertexProperty vertexProperty = vertex.property(graphToWriteTo.features().vertex().getCardinality(vp.key()), vp.key(), vp.value());
                vp.properties().forEachRemaining(p -> vertexProperty.property(p.key(), p.value()));
            });

            // Assert: note in previous statement must be true.
			// final Object db_id = gts1.V().has(uid_field, SampleManger.forceLong(vertex.value(uid_field))).next().id();
			// GdbLogger.getLogger().debug("UID: %d, RAW: %d, vertex: %s, db: %s", vertex.value(uid_field), id, vertex.id(), db_id);

            rev_index.put(id, vertex.id());   // Populate rev index

            if (supportsTx && counter.incrementAndGet() % batchSize == 0)
                graphToWriteTo.tx().commit();
        });

        GdbLogger.getLogger().debug("Vertexes creation step completed");

        // TODO: consider adding LRU cache
        counter.set(0);
        final GraphTraversalSource gts = graphToWriteTo.traversal();
        readVertexStrings(new FileInputStream(inputFile)).<Vertex>map(FunctionUtils.wrapFunction(line -> readVertex(new ByteArrayInputStream(line.getBytes()), null, null, Direction.IN)))
        .forEach(v -> {
            v.edges(Direction.IN).forEachRemaining(e -> {       // NOTE: that's why only IN are read!!
                GraphTraversal<Edge, Edge> ge = gts.addE(e.label())
                        .from(__.V(rev_index.get(SampleManger.forceLong(e.outVertex().id()))))
                        .to(__.V(rev_index.get(SampleManger.forceLong(e.inVertex().id()))));
                e.properties().forEachRemaining(p -> ge.property(p.key(), p.value()));
                ge.count().next();
                if (supportsTx && counter.getAndIncrement() % 1000 == 0)
                    graphToWriteTo.tx().commit();
            });
        });
        if (supportsTx) graphToWriteTo.tx().commit();

        GdbLogger.getLogger().debug("Edge creation step completed");
    }



    @Override
    public Iterator<Vertex> readVertices(final InputStream inputStream,
                                         final Function<Attachable<Vertex>, Vertex> vertexAttachMethod,
                                         final Function<Attachable<Edge>, Edge> edgeAttachMethod,
                                         final Direction attachEdgesOfThisDirection) throws IOException {
        return readVertexStrings(inputStream).<Vertex>map(FunctionUtils.wrapFunction(line -> readVertex(new ByteArrayInputStream(line.getBytes()), vertexAttachMethod, edgeAttachMethod, attachEdgesOfThisDirection))).iterator();
    }


    @Override
    public Vertex readVertex(final InputStream inputStream, final Function<Attachable<Vertex>, Vertex> vertexAttachMethod) throws IOException {
        return readVertex(inputStream, vertexAttachMethod, null, null);
    }


    @Override
    public Vertex readVertex(final InputStream inputStream,
                             final Function<Attachable<Vertex>, Vertex> vertexAttachMethod,
                             final Function<Attachable<Edge>, Edge> edgeAttachMethod,
                             final Direction attachEdgesOfThisDirection) throws IOException {
        // graphson v3 has special handling for generic Map instances, by forcing to linkedhashmap (which is probably
        // what it should have been anyway) stargraph format can remain unchanged across all versions
        final Map<String, Object> vertexData = mapper.readValue(inputStream, version == GraphSONVersion.V3_0 ? linkedHashMapTypeReference : mapTypeReference);
        final StarGraph starGraph = StarGraphGraphSONDeserializer.readStarGraphVertex(vertexData);
        if (vertexAttachMethod != null) vertexAttachMethod.apply(starGraph.getStarVertex());

        if (attachEdgesOfThisDirection != null) {
            if (vertexData.containsKey(GraphSONTokens.OUT_E) && (attachEdgesOfThisDirection == Direction.BOTH || attachEdgesOfThisDirection == Direction.OUT))
                StarGraphGraphSONDeserializer.readStarGraphEdges(edgeAttachMethod, starGraph, vertexData, GraphSONTokens.OUT_E);

            if (vertexData.containsKey(GraphSONTokens.IN_E) && (attachEdgesOfThisDirection == Direction.BOTH || attachEdgesOfThisDirection == Direction.IN))
                StarGraphGraphSONDeserializer.readStarGraphEdges(edgeAttachMethod, starGraph, vertexData, GraphSONTokens.IN_E);
        }

        return starGraph.getStarVertex();
    }

    @Override
    public Edge readEdge(final InputStream inputStream, final Function<Attachable<Edge>, Edge> edgeAttachMethod) throws IOException {
        if (version == GraphSONVersion.V1_0) {
            final Map<String, Object> edgeData = mapper.readValue(inputStream, mapTypeReference);

            final Map<String, Object> edgeProperties = edgeData.containsKey(GraphSONTokens.PROPERTIES) ?
                    (Map<String, Object>) edgeData.get(GraphSONTokens.PROPERTIES) : Collections.EMPTY_MAP;
            final DetachedEdge edge = new DetachedEdge(edgeData.get(GraphSONTokens.ID),
                    edgeData.get(GraphSONTokens.LABEL).toString(),
                    edgeProperties,
                    edgeData.get(GraphSONTokens.OUT), edgeData.get(GraphSONTokens.OUT_LABEL).toString(),
                    edgeData.get(GraphSONTokens.IN), edgeData.get(GraphSONTokens.IN_LABEL).toString());

            return edgeAttachMethod.apply(edge);
        } else {
            return edgeAttachMethod.apply((DetachedEdge) mapper.readValue(inputStream, Edge.class));
        }
    }

    @Override
    public VertexProperty readVertexProperty(final InputStream inputStream,
                                             final Function<Attachable<VertexProperty>, VertexProperty> vertexPropertyAttachMethod) throws IOException {
        if (version == GraphSONVersion.V1_0) {
            final Map<String, Object> vpData = mapper.readValue(inputStream, mapTypeReference);
            final Map<String, Object> metaProperties = (Map<String, Object>) vpData.get(GraphSONTokens.PROPERTIES);
            final DetachedVertexProperty vp = new DetachedVertexProperty(vpData.get(GraphSONTokens.ID),
                    vpData.get(GraphSONTokens.LABEL).toString(),
                    vpData.get(GraphSONTokens.VALUE), metaProperties);
            return vertexPropertyAttachMethod.apply(vp);
        } else {
            return vertexPropertyAttachMethod.apply((DetachedVertexProperty) mapper.readValue(inputStream, VertexProperty.class));
        }
    }

    @Override
    public Property readProperty(final InputStream inputStream,
                                 final Function<Attachable<Property>, Property> propertyAttachMethod) throws IOException {
        if (version == GraphSONVersion.V1_0) {
            final Map<String, Object> propertyData = mapper.readValue(inputStream, mapTypeReference);
            final DetachedProperty p = new DetachedProperty(propertyData.get(GraphSONTokens.KEY).toString(), propertyData.get(GraphSONTokens.VALUE));
            return propertyAttachMethod.apply(p);
        } else {
            return propertyAttachMethod.apply((DetachedProperty) mapper.readValue(inputStream, Property.class));
        }
    }

    @Override
    public <C> C readObject(final InputStream inputStream, final Class<? extends C> clazz) throws IOException {
        return mapper.readValue(inputStream, clazz);
    }

    private Stream<String> readVertexStrings(final InputStream inputStream) throws IOException {
        if (unwrapAdjacencyList) {
            final JsonNode root = mapper.readTree(inputStream);
            final JsonNode vertices = root.get(GraphSONTokens.VERTICES);
            if (!vertices.getNodeType().equals(JsonNodeType.ARRAY)) throw new IOException(String.format("The '%s' key must be an array", GraphSONTokens.VERTICES));
            return IteratorUtils.stream(vertices.elements()).map(Object::toString);
        } else {
            final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            return br.lines();
        }
    }

    public static Builder build() {
        return new Builder();
    }

    public final static class Builder implements ReaderBuilder<CustomGraphSONReader> {
        private long batchSize = 10000;

        private Mapper<ObjectMapper> mapper = GraphSONMapper.build().create();
        private boolean unwrapAdjacencyList = false;

        private Builder() {}

        /**
         * Number of mutations to perform before a commit is executed when using
         * {@link CustomGraphSONReader#readGraph(InputStream, Graph)}.
         */
        public Builder batchSize(final long batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        /**
         * Override all of the {@link GraphSONMapper} builder
         * options with this mapper.  If this value is set to something other than null then that value will be
         * used to construct the writer.
         */
        public Builder mapper(final Mapper<ObjectMapper> mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder unwrapAdjacencyList(final boolean unwrapAdjacencyList) {
            this.unwrapAdjacencyList = unwrapAdjacencyList;
            return this;
        }

        public CustomGraphSONReader create() {
            return new CustomGraphSONReader(this);
        }
    }
}
