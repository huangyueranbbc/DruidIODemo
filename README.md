**Druid-Io Demo**  
整理学习Druid过程中的代码和文档  
2017年12月6日17:39:47   

查询json数据格式  
查询javaDEMO  
JDBC操作Druid-io  
 
更新中......    
  
**Druid-Io Demo**  
Collate the code and document in the process of learning Druid  
December 6, 2017 17:39:47  
  
Query JSON data format  
Query javaDEMO  
JDBC operation Druid-io  
  
In the update...  
  
  
  
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
   
 TimeseriesQuery          时间维度  
 TopNQuery                TopN  
 GroupByQuery             GroupBy  
 TimeBoundaryQuery        时间范围  
 SearchQuery              搜索  
 SelectQuery              select选择查询  
 SegmentMetadataQuery     segment元数据    
   