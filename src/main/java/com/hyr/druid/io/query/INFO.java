package com.hyr.druid.io.query;

/*******************************************************************************
 * 版权信息：博睿宏远科技发展有限公司
 * Copyright: Copyright (c) 2007博睿宏远科技发展有限公司,Inc.All Rights Reserved.
 *
 * @date 2017-12-06 下午 4:33 
 * @author: <a href=mailto:huangyr@bonree.com>黄跃然</a>
 * @Description:
 ******************************************************************************/
public class INFO {

    // Druid 查询配置源文件
    // 1.timeseries-pages.json          时间维度
    // 2.top-pages.json                 TopN
    // 3.groupby-pages.json             GroupBy
    // 4.dataSourceMetadata-pages.json  数据源的元文件
    // 5.boundary-pages.json            时间范围
    // 6.search-pages.json              搜索
    // 7.select-pages.json              select选择查询
    // 8.segmentMetadata-pages.json     segment元数据

    // 查询命令:
    // curl -X POST 'http://localhost:8083/druid/v2/?pretty' -H 'Content-Type:application/json' -d @quickstart/wikiticker-timeseries-pages.json

}
