package com.hyr.druid.io.query.java.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hyr.druid.io.api.embedded.IndexHelper;
import com.hyr.druid.io.api.embedded.Utils;
import com.hyr.druid.io.api.embedded.load.Loader;
import com.hyr.druid.io.api.embedded.load.impl.CSVLoader;
import io.druid.data.input.impl.DimensionSchema;
import io.druid.data.input.impl.DimensionsSpec;
import io.druid.data.input.impl.StringDimensionSchema;
import io.druid.jackson.DefaultObjectMapper;
import io.druid.java.util.common.granularity.Granularity;
import io.druid.java.util.common.guava.Sequence;
import io.druid.java.util.common.guava.Sequences;
import io.druid.query.DefaultGenericQueryMetricsFactory;
import io.druid.query.Druids;
import io.druid.query.GenericQueryMetricsFactory;
import io.druid.query.Result;
import io.druid.query.aggregation.*;
import io.druid.query.datasourcemetadata.DataSourceMetadataQuery;
import io.druid.query.datasourcemetadata.DataSourceMetadataQueryRunnerFactory;
import io.druid.query.datasourcemetadata.DataSourceMetadataResultValue;
import io.druid.query.datasourcemetadata.DataSourceQueryQueryToolChest;
import io.druid.segment.QueryableIndex;
import io.druid.segment.QueryableIndexSegment;
import io.druid.segment.incremental.IncrementalIndexSchema;
import org.testng.annotations.Test;

import java.io.*;
import java.util.*;

/*******************************************************************************
 * @date 2017-12-08 下午 4:03
 * @author: <a href=mailto:huangyr@bonree.com>黄跃然</a>
 * @Description: 数据源的元文件(Meta file of data source)
 ******************************************************************************/
public class DataSourceMetadataQueryDemo {

    @Test
    public static void main() throws IOException {
        QueryableIndex index = createDruidSegments();

        Map<String, Object> contextConfig = new HashMap<String, Object>(); // 优化配置

        DataSourceMetadataQuery query = new Druids.DataSourceMetadataQueryBuilder()
                .dataSource("test")
                .context(contextConfig)
                .build();


        ObjectMapper objectMapper= new DefaultObjectMapper();

        GenericQueryMetricsFactory genericQueryMetricsFactory = new DefaultGenericQueryMetricsFactory(objectMapper);
        DataSourceQueryQueryToolChest dataSourceQueryQueryToolChest= new DataSourceQueryQueryToolChest(genericQueryMetricsFactory);

        DataSourceMetadataQueryRunnerFactory factory=new DataSourceMetadataQueryRunnerFactory(dataSourceQueryQueryToolChest, Utils.NOOP_QUERYWATCHER);

        Sequence<Result<DataSourceMetadataResultValue>> sequence = factory.createRunner(new QueryableIndexSegment("", index)).run(query, null);

        ArrayList<Result<DataSourceMetadataResultValue>> results = Sequences.toList(sequence, Lists.<Result<DataSourceMetadataResultValue>>newArrayList());

        for (Result r : results) {
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
