package com.hyr.druid.io.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/*******************************************************************************
 * @date 2017-12-06 下午 3:40
 * @author: <a href=mailto:huangyr@bonree.com>黄跃然</a>
 * @Description:JDBC连接Druid
 ******************************************************************************/
public class JDBCDruidDEMO {

    /*
    使用该功能前,需要开启SQL-SERVER服务
    修改Broker配置,添加新的配置项:
    druid.sql.enable=true
    druid.sql.avatica.enable=true
    druid.sql.http.enable=true
     */

    public static void main(String[] args) throws ClassNotFoundException, SQLException {

        // Connect to /druid/v2/sql/avatica/ on your broker.
        String url = "jdbc:avatica:remote:url=http://druidmaster:8082/druid/v2/sql/avatica/";

        // Set any connection context parameters you need here (see "Connection context" below).
        // Or leave empty for default behavior.
        Properties connectionProperties = new Properties();

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, connectionProperties);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 使用JDBC查询Druid数据 Count(*)
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT COUNT(*) AS cnt FROM wikiticker");
        while (resultSet.next()) {
            // Do something
            System.out.println(resultSet.getString(1));
        }

        // 使用JDBC查询Druid数据
        ResultSet resultSet1 = connection.createStatement().executeQuery("SELECT * FROM wikiticker");
        while (resultSet1.next()) {
            // Do something
            for (int i = 1; i < 10; i++) {
                System.out.print(resultSet1.getString(i) + "\t");
            }
            System.out.println();
        }
    }

}
