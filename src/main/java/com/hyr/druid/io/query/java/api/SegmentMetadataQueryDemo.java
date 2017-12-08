package com.hyr.druid.io.query.java.api;

import com.google.common.collect.Lists;
import com.hyr.druid.io.api.embedded.IndexHelper;
import com.hyr.druid.io.api.embedded.QueryHelper;
import com.hyr.druid.io.api.embedded.load.Loader;
import com.hyr.druid.io.api.embedded.load.impl.CSVLoader;
import io.druid.data.input.impl.DimensionSchema;
import io.druid.data.input.impl.DimensionsSpec;
import io.druid.data.input.impl.StringDimensionSchema;
import io.druid.java.util.common.granularity.Granularity;
import io.druid.java.util.common.guava.Sequence;
import io.druid.java.util.common.guava.Sequences;
import io.druid.query.Druids;
import io.druid.query.aggregation.*;
import io.druid.query.metadata.metadata.SegmentAnalysis;
import io.druid.query.metadata.metadata.SegmentMetadataQuery;
import io.druid.query.spec.QuerySegmentSpecs;
import io.druid.segment.QueryableIndex;
import io.druid.segment.incremental.IncrementalIndexSchema;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.testng.annotations.Test;

import java.io.*;
import java.util.*;

/*******************************************************************************
 * @date 2017-12-08 下午 18:10:03
 * @author: <a href=mailto:huangyr@bonree.com>黄跃然</a>
 * @Description: 查询segment元数据
 ******************************************************************************/
public class SegmentMetadataQueryDemo {

    @Test
    public static void main() throws IOException {
        QueryableIndex index = createDruidSegments();

        Map<String, Object> contextConfig = new HashMap<String, Object>(); // 优化配置

        SegmentMetadataQuery query = new Druids.SegmentMetadataQueryBuilder()
                .dataSource("test")
                .intervals(QuerySegmentSpecs.create(new Interval(0, new DateTime().getMillis())))
                .context(contextConfig)
                .build();

        Sequence<SegmentAnalysis> sequence = QueryHelper.run(query, index);
        ArrayList<SegmentAnalysis> results = Sequences.toList(sequence, Lists.<SegmentAnalysis>newArrayList());

        // 打印 元数据信息
        for (SegmentAnalysis r : results) {
            System.out.println(r);
        }


    }

    /**
     * 加载数据源文件 并创建segement
     *
     * @return
     * @throws IOException
     */
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

}
