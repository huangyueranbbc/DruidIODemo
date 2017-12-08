package com.hyr.druid.io.query;

/*******************************************************************************
 * @date 2017-12-06 下午 4:33
 * @author: <a href=mailto:huangyr@bonree.com>黄跃然</a>
 * @Description:
 ******************************************************************************/
public class INFO {

    // Druid 查询配置源文件
    // 1.timeseries-pages.json          时间维度(Time dimension)
    // 2.top-pages.json                 TopN(TopN)
    // 3.groupby-pages.json             GroupBy(GroupBy)
    // 4.dataSourceMetadata-pages.json  数据源的元文件(Meta file of data source)
    // 5.boundary-pages.json            时间范围(time frame)
    // 6.search-pages.json              搜索(search)
    // 7.select-pages.json              select选择查询(Select selection query)
    // 8.segmentMetadata-pages.json     segment元数据(segmentmetadata)

    // 查询命令:
    // curl -X POST 'http://localhost:8083/druid/v2/?pretty' -H 'Content-Type:application/json' -d @quickstart/wikiticker-timeseries-pages.json

    // src\main\java\com\hyr\druid\io\query\java\api Support for JAVA 对JAVA的支持

}
