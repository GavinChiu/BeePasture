/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.shenjitang.beepasture.function.ScriptTemplateExecuter;

/**
 *
 * @author xiaolie
 */
public class JdbcResource extends BeeResource{
    private static final Log LOGGER = LogFactory.getLog(JdbcResource.class);
    private DataSource ds;
    private Integer batchExecuteCount = 100;
            
    public JdbcResource() {
    }

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param);
        if (param.containsKey("batchExecuteCount")) {
            batchExecuteCount = Integer.valueOf(param.get("batchExecuteCount").toString());
        }
        Properties props = new Properties();
        props.put("driverClassName", getDriverClassName(url));
        for (Object k : param.keySet()) {
            props.put(k, param.get(k));
        }
        ds = BasicDataSourceFactory.createDataSource(props);
    }

    @Override
    public void persist(String varName, Object obj, Map persistParams) throws Exception {
        String topVarName = varName.split("[.]")[0];
        String tailVarName = null;
        if (topVarName.length() < varName.length()) {
            tailVarName = varName.substring(topVarName.length() + 1);
        }
        List<String> sqlList = new ArrayList();
        ScriptTemplateExecuter template = new ScriptTemplateExecuter();
        String sql = (String)persistParams.get("sql");
        if (obj instanceof List) {
            int i = 0;
            for (Object item : (List)obj) {
                try {
                    Map map = new HashMap();
                    map.put(topVarName, item);
                    if (tailVarName != null) {
                        Map mm = (Map)item;
                        List iitemList = (List)mm.get(tailVarName);
                        for(Object iitem : iitemList) {
                            map.put(tailVarName, iitem);
                            sqlList.add(template.expressCalcu(sql, map));
                            if (++i >= batchExecuteCount) {
                                executeSql(sqlList);
                                sqlList.clear();
                            }
                        }
                    } else {
                        sqlList.add(template.expressCalcu(sql, map));
                        if (++i >= batchExecuteCount) {
                            executeSql(sqlList);
                            sqlList.clear();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (sqlList.size() > 0) {
                executeSql(sqlList);
            }
        } else {
            try {
                    Map map = new HashMap();
                    map.put(topVarName, obj);
                    if (tailVarName != null) {
                        Map mm = (Map)obj;
                        List iitemList = (List)mm.get(tailVarName);
                        for(Object iitem : iitemList) {
                            map.put(tailVarName, iitem);
                            String rsql = template.expressCalcu(sql, map);
                            executeSql(rsql);
                        }
                    } else {
                        String rsql = template.expressCalcu(sql, map);
                        executeSql(rsql);
                    }
            } catch (Exception ex) {
                    ex.printStackTrace();
            }
            throw new RuntimeException("目前只支持List入库，obj:" + obj);
        }
    }
    
    @Override
    public Object loadResource(Map loadParam) throws Exception {
        List list = new ArrayList();
        String sql = (String)loadParam.get("sql");
        try (Connection conn = ds.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                LOGGER.debug("sql:" + sql);
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData meta = rs.getMetaData();
                while(rs.next()) {
                    Map record = new HashMap();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String key = meta.getColumnName(i);
                        int type = meta.getColumnType(i);
                        record.put(key, rs.getObject(i));
                    }
                    list.add(record);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }   
     
    private void executeSql( List<String> sqlList) throws Exception {
        Connection conn = ds.getConnection();
        try {
            Statement stmt = conn.createStatement();
            try {
                LOGGER.debug("batch to db begin ......");
                for (String sql : sqlList) {
                    if (StringUtils.isNotBlank(sql)) {
                        LOGGER.info(sql);
                        stmt.addBatch(sql);
                    }
                }
                stmt.executeBatch();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stmt.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            conn.close();
        }
    }    
    
    private void executeSql(String sql) throws Exception {
        Connection conn = ds.getConnection();
        try {
            Statement stmt = conn.createStatement();
            try {
                if (StringUtils.isNotBlank(sql)) {
                    LOGGER.info(sql);
                    stmt.execute(sql);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stmt.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            conn.close();
        }
    }    
    
    public String getDriverClassName(String url) {
        if (url.contains("jdbc:jtds:sqlserver")) {
            return "net.sourceforge.jtds.jdbc.Driver";
        } else if (url.contains("jdbc:sqlserver")) {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        } else if (url.contains("jdbc:oracle:thin")) {
            return "oracle.jdbc.OracleDriver";
        } else if (url.contains("jdbc:mysql:")) {
            return "com.mysql.jdbc.Driver";
        } else {
            return null;
        }
    }    
}