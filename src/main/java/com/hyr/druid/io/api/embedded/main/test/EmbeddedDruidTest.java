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

package com.hyr.druid.io.api.embedded.main.test;

import com.google.common.collect.Lists;
import com.hyr.druid.io.api.embedded.IndexHelper;
import com.hyr.druid.io.api.embedded.QueryHelper;
import com.hyr.druid.io.api.embedded.load.Loader;
import com.hyr.druid.io.api.embedded.load.impl.CSVLoader;
import io.druid.data.input.Row;
import io.druid.data.input.impl.DimensionSchema;
import io.druid.data.input.impl.DimensionsSpec;
import io.druid.data.input.impl.StringDimensionSchema;
import io.druid.java.util.common.granularity.Granularity;
import io.druid.java.util.common.guava.Sequence;
import io.druid.java.util.common.guava.Sequences;
import io.druid.query.Result;
import io.druid.query.aggregation.*;
import io.druid.query.filter.DimFilter;
import io.druid.query.filter.DimFilters;
import io.druid.query.groupby.GroupByQuery;
import io.druid.query.spec.QuerySegmentSpecs;
import io.druid.query.topn.TopNQuery;
import io.druid.query.topn.TopNQueryBuilder;
import io.druid.segment.QueryableIndex;
import io.druid.segment.incremental.IncrementalIndexSchema;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmbeddedDruidTest {

    public static QueryableIndex createDruidSegments() throws IOException {
        System.setProperty("druid.segment.dir", "E:/druid/segment/"); // 设置segment 目录

        //  Create druid segments from raw data
        Reader reader = new BufferedReader(new FileReader(new File("./src/main/resources/report.csv")));

        List<String> columns = Arrays.asList("colo", "pool", "report", "URL", "TS", "metric", "value", "count", "min", "max", "sum");
        List<String> exclusions = Arrays.asList("_Timestamp", "_Machine", "_ThreadId", "_Query");
        List<String> metrics = Arrays.asList("value", "count", "min", "max", "sum");
        List<DimensionSchema> dimensions = new ArrayList<DimensionSchema>();
        for (String dim : columns) {
            dimensions.add(new StringDimensionSchema(dim));
        }
        dimensions.removeAll(exclusions);
        dimensions.removeAll(metrics);
        Loader loader = new CSVLoader(reader, columns, columns, "TS");

        DimensionsSpec dimensionsSpec = new DimensionsSpec(dimensions, null, null);
        AggregatorFactory[] metricsAgg = new AggregatorFactory[]{
                new LongSumAggregatorFactory("agg_count", "count"),
                new LongMaxAggregatorFactory("agg_max", "max"),
                new LongMinAggregatorFactory("agg_min", "min"),
                new DoubleSumAggregatorFactory("agg_sum", "sum"),
        };
        //IncrementalIndexSchema indexSchema = new IncrementalIndexSchema(0, QueryGranularity.fromString("ALL"), dimensionsSpec, metricsAgg);

        IncrementalIndexSchema.Builder builder = new IncrementalIndexSchema.Builder();
        Granularity queryGranularity = Granularity.fromString("ALL");
        builder.withQueryGranularity(queryGranularity);
        builder.withMinTimestamp(0);
        builder.withDimensionsSpec(dimensionsSpec);
        builder.withMetrics(metricsAgg);
        IncrementalIndexSchema indexSchema = builder.build();

        QueryableIndex index = IndexHelper.getQueryableIndex(loader, indexSchema);
        return index;
    }

    @Test
    public void groupByQuery() throws IOException {
        QueryableIndex index = createDruidSegments();
        List<DimFilter> filters = new ArrayList<DimFilter>();
        filters.add(DimFilters.dimEquals("report", "URLTransaction"));
        filters.add(DimFilters.dimEquals("pool", "r1cart"));
        filters.add(DimFilters.dimEquals("metric", "Duration"));
        GroupByQuery query = GroupByQuery.builder()
                .setDataSource("test")
                .setQuerySegmentSpec(QuerySegmentSpecs.create(new Interval(0, new DateTime().getMillis())))
                .setGranularity(Granularity.fromString("NONE"))
                .addDimension("URL")
                .addAggregator(new LongSumAggregatorFactory("agg_count", "agg_count"))
                .addAggregator(new LongMaxAggregatorFactory("agg_max", "agg_max"))
                .addAggregator(new LongMinAggregatorFactory("agg_min", "agg_min"))
                .addAggregator(new DoubleSumAggregatorFactory("agg_sum", "agg_sum"))

                .setDimFilter(DimFilters.and(filters))
                .build();

        @SuppressWarnings("unchecked")
        Sequence<Row> sequence = QueryHelper.run(query, index);
        QueryHelper.run(query, index);
        ArrayList<Row> results = Sequences.toList(sequence, Lists.<Row>newArrayList());
        Assert.assertEquals(results.size(), 2);

        for(Row r:results){
            System.out.println(r.toString());
        }

        if (results.get(0).getDimension("URL").get(0).equals("abc")) {
            Assert.assertEquals(results.get(0).getLongMetric("agg_sum"), 247);
            Assert.assertEquals(results.get(0).getLongMetric("agg_min"), 0);
            Assert.assertEquals(results.get(0).getLongMetric("agg_max"), 124);
            Assert.assertEquals(results.get(0).getLongMetric("agg_count"), 12);
            Assert.assertEquals(results.get(1).getLongMetric("agg_sum"), 123);
            Assert.assertEquals(results.get(1).getLongMetric("agg_min"), 0);
            Assert.assertEquals(results.get(1).getLongMetric("agg_max"), 123);
            Assert.assertEquals(results.get(1).getLongMetric("agg_count"), 3);

        } else {
            Assert.assertEquals(results.get(0).getLongMetric("agg_sum"), 123);
            Assert.assertEquals(results.get(0).getLongMetric("agg_min"), 0);
            Assert.assertEquals(results.get(0).getLongMetric("agg_max"), 123);
            Assert.assertEquals(results.get(0).getLongMetric("agg_count"), 3);
            Assert.assertEquals(results.get(1).getLongMetric("agg_sum"), 247);
            Assert.assertEquals(results.get(1).getLongMetric("agg_min"), 0);
            Assert.assertEquals(results.get(1).getLongMetric("agg_max"), 124);
            Assert.assertEquals(results.get(1).getLongMetric("agg_count"), 12);
        }

    }

    @Test
    public void topNQuery() throws IOException {
        QueryableIndex index = createDruidSegments();
        List<DimFilter> filters = new ArrayList<DimFilter>();
        filters.add(DimFilters.dimEquals("report", "URLTransaction"));
        filters.add(DimFilters.dimEquals("pool", "r1cart"));
        filters.add(DimFilters.dimEquals("metric", "Duration"));
        TopNQuery query =
                new TopNQueryBuilder()
                        .threshold(5)
                        .metric("agg_count")
                        .dataSource("test")
                        .intervals(QuerySegmentSpecs.create(new Interval(0, new DateTime().getMillis())))
                        .granularity(Granularity.fromString("NONE"))
                        .dimension("colo")
                        .aggregators(
                                Arrays.<AggregatorFactory>asList(
                                        new LongSumAggregatorFactory("agg_count", "agg_count"),
                                        new LongMaxAggregatorFactory("agg_max", "agg_max"),
                                        new LongMinAggregatorFactory("agg_min", "agg_min"),
                                        new DoubleSumAggregatorFactory("agg_sum", "agg_sum"))

                        )
                        .filters(DimFilters.and(filters)).build();
        @SuppressWarnings("unchecked")
        Sequence<Result> sequence = QueryHelper.run(query, index);
        ArrayList<Result> results = Sequences.toList(sequence, Lists.<Result>newArrayList());

        // TODO 打印
        for (Result r : results) {
            System.out.println(r.toString());
        }

        Assert.assertEquals(results.size(), 1);
    }

}
