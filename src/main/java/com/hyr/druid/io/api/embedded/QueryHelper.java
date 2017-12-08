/*
 * Copyright 2015 eBay Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hyr.druid.io.api.embedded;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import io.druid.collections.BlockingPool;
import io.druid.collections.DefaultBlockingPool;
import io.druid.jackson.DefaultObjectMapper;
import io.druid.java.util.common.guava.Sequence;
import io.druid.query.*;
import io.druid.query.groupby.*;
import io.druid.query.groupby.strategy.GroupByStrategySelector;
import io.druid.query.groupby.strategy.GroupByStrategyV1;
import io.druid.query.groupby.strategy.GroupByStrategyV2;
import io.druid.query.metadata.SegmentMetadataQueryConfig;
import io.druid.query.metadata.SegmentMetadataQueryQueryToolChest;
import io.druid.query.metadata.SegmentMetadataQueryRunnerFactory;
import io.druid.query.metadata.metadata.SegmentMetadataQuery;
import io.druid.query.search.SearchQueryQueryToolChest;
import io.druid.query.search.SearchQueryRunnerFactory;
import io.druid.query.search.SearchStrategySelector;
import io.druid.query.search.search.SearchQuery;
import io.druid.query.search.search.SearchQueryConfig;
import io.druid.query.select.*;
import io.druid.query.timeboundary.TimeBoundaryQuery;
import io.druid.query.timeboundary.TimeBoundaryQueryRunnerFactory;
import io.druid.query.timeseries.TimeseriesQuery;
import io.druid.query.timeseries.TimeseriesQueryEngine;
import io.druid.query.timeseries.TimeseriesQueryQueryToolChest;
import io.druid.query.timeseries.TimeseriesQueryRunnerFactory;
import io.druid.query.topn.TopNQuery;
import io.druid.query.topn.TopNQueryConfig;
import io.druid.query.topn.TopNQueryQueryToolChest;
import io.druid.query.topn.TopNQueryRunnerFactory;
import io.druid.segment.QueryableIndex;
import io.druid.segment.QueryableIndexSegment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * This Helper class maintains all required query interface related classes and invokes it based
 * on requested query.
 */
public class QueryHelper {
    private static final QueryRunnerFactoryConglomerate conglomerate;

    /*
     * Initialize QueryRunnerFactoryConglomerate.
     */
    static {
        HashMap<Class<? extends Query>, QueryRunnerFactory> map =
                Maps.<Class<? extends Query>, QueryRunnerFactory>newHashMap();

        // Register all query runner factories.
        map.put(GroupByQuery.class, getGroupByQueryRunnerFactory());
        map.put(TopNQuery.class, getTopNQueryRunnerFactory());
        map.put(SearchQuery.class, getSearchQueryRunnerFactory());
        map.put(SelectQuery.class, getSelectQueryRunnerFactory());
        map.put(SegmentMetadataQuery.class, getSegmentMetadataQueryRunnerFactory());
        map.put(TimeseriesQuery.class, getTimeseriesQueryRunnerFactory());
        map.put(TimeBoundaryQuery.class, getTimeBoundaryQueryRunnerFactory());


        DefaultQueryRunnerFactoryConglomerate _conglomerate =
                new DefaultQueryRunnerFactoryConglomerate(map);
        conglomerate = _conglomerate;
    }

    @SuppressWarnings("unchecked")
    public static Sequence run(Query query, QueryableIndex index) {

        return findFactory(query).createRunner(new QueryableIndexSegment("", index)).run(query, null);
    }

    @SuppressWarnings("unchecked")
    public static QueryRunnerFactory findFactory(Query query) {
        return conglomerate.findFactory(query);
    }

	/*
     * All subclasses of Query with default configuration.
	 */

    private static TimeseriesQueryRunnerFactory getTimeseriesQueryRunnerFactory() {
        TimeseriesQueryQueryToolChest toolChest =
                new TimeseriesQueryQueryToolChest(Utils.NoopIntervalChunkingQueryRunnerDecorator());
        TimeseriesQueryEngine engine = new TimeseriesQueryEngine();
        final TimeseriesQueryRunnerFactory factory =
                new TimeseriesQueryRunnerFactory(toolChest, engine, Utils.NOOP_QUERYWATCHER);
        return factory;
    }

    private static TimeBoundaryQueryRunnerFactory getTimeBoundaryQueryRunnerFactory() {
        final TimeBoundaryQueryRunnerFactory factory =
                new TimeBoundaryQueryRunnerFactory(Utils.NOOP_QUERYWATCHER);
        return factory;
    }

    private static SegmentMetadataQueryRunnerFactory getSegmentMetadataQueryRunnerFactory() {
        SegmentMetadataQueryConfig smqc = new SegmentMetadataQueryConfig();
        SegmentMetadataQueryQueryToolChest toolChest = new SegmentMetadataQueryQueryToolChest(smqc);
        final SegmentMetadataQueryRunnerFactory factory =
                new SegmentMetadataQueryRunnerFactory(toolChest, Utils.NOOP_QUERYWATCHER);
        return factory;
    }

    private static SelectQueryRunnerFactory getSelectQueryRunnerFactory() {
        Supplier<SelectQueryConfig> selectQueryConfig = new Supplier<SelectQueryConfig>() {
            @Override
            public SelectQueryConfig get() {
                return new SelectQueryConfig(true);
            }
        };

        SelectQueryQueryToolChest toolChest =
                new SelectQueryQueryToolChest(new ObjectMapper(), Utils.NoopIntervalChunkingQueryRunnerDecorator(), selectQueryConfig);


        SelectQueryEngine engine = new SelectQueryEngine(selectQueryConfig);
        final SelectQueryRunnerFactory factory =
                new SelectQueryRunnerFactory(toolChest, engine, Utils.NOOP_QUERYWATCHER);
        return factory;
    }

    private static SearchQueryRunnerFactory getSearchQueryRunnerFactory() {
        SearchQueryQueryToolChest toolChest =
                new SearchQueryQueryToolChest(new SearchQueryConfig(),
                        Utils.NoopIntervalChunkingQueryRunnerDecorator());

        Supplier<SearchQueryConfig> searchQueryConfig = new Supplier<SearchQueryConfig>() {
            @Override
            public SearchQueryConfig get() {
                return new SearchQueryConfig();
            }
        };
        SearchStrategySelector selector = new SearchStrategySelector(searchQueryConfig);

        SearchQueryRunnerFactory factory =
                new SearchQueryRunnerFactory(selector, toolChest, Utils.NOOP_QUERYWATCHER);
        return factory;
    }

    private static TopNQueryRunnerFactory getTopNQueryRunnerFactory() {
        TopNQueryQueryToolChest toolchest =
                new TopNQueryQueryToolChest(new TopNQueryConfig(),
                        Utils.NoopIntervalChunkingQueryRunnerDecorator());
        TopNQueryRunnerFactory factory =
                new TopNQueryRunnerFactory(Utils.getBufferPool(), toolchest, Utils.NOOP_QUERYWATCHER);
        return factory;
    }

    private static GroupByQueryRunnerFactory getGroupByQueryRunnerFactory() {
        ObjectMapper mapper = new DefaultObjectMapper();
        GroupByQueryConfig config = new GroupByQueryConfig();
        config.setMaxIntermediateRows(10000);

        Supplier<GroupByQueryConfig> configSupplier = Suppliers.ofInstance(config);
        GroupByQueryEngine engine = new GroupByQueryEngine(configSupplier, Utils.getBufferPool());

        GroupByStrategyV1 groupByStrategyV1 = new GroupByStrategyV1(configSupplier, engine, Utils.NOOP_QUERYWATCHER, Utils.getBufferPool());
        DruidProcessingConfig druidProcessingConfig = new DruidProcessingConfig() {
            @Override
            public String getFormatString() {
                return ",";
            }
        };

        Supplier<ByteBuffer> byteBuffer = new Supplier<ByteBuffer>() {
            @Override
            public ByteBuffer get() {
                return ByteBuffer.allocate(Utils.MAX_TOTAL_BUFFER_SIZE / 200);
            }
        };

        BlockingPool<ByteBuffer> blockingPool = new DefaultBlockingPool<ByteBuffer>(byteBuffer, 10);

        GroupByStrategyV2 groupByStrategyV2 = new GroupByStrategyV2(druidProcessingConfig, configSupplier, Utils.getBufferPool(), blockingPool, mapper, Utils.NOOP_QUERYWATCHER);

        GroupByStrategySelector groupByStrategySelector = new GroupByStrategySelector(configSupplier, groupByStrategyV1, groupByStrategyV2);


        IntervalChunkingQueryRunnerDecorator intervalChunkingQueryRunnerDecorator = Utils.NoopIntervalChunkingQueryRunnerDecorator();

        GroupByQueryQueryToolChest groupByQueryQueryToolChest = new GroupByQueryQueryToolChest(groupByStrategySelector, intervalChunkingQueryRunnerDecorator);
        GroupByQueryRunnerFactory factory = new GroupByQueryRunnerFactory(groupByStrategySelector, groupByQueryQueryToolChest);

        return factory;
    }

    public static Query getQuery(InputStream queryInputStream) throws JsonParseException,
            JsonMappingException, IOException {
        ObjectMapper jsonMapper = new DefaultObjectMapper();
        return jsonMapper.readValue(queryInputStream, Query.class);
    }
}
