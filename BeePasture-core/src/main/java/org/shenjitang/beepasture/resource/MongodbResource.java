/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;
import com.google.common.collect.Sets;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.shenjitang.beepasture.core.GatherStep;
import org.shenjitang.mongodbutils.MongoDbOperater;

/**
 *
 * @author xiaolie
 */
public class MongodbResource extends BeeResource {
    private MongoClient mongoClient;
    private MongoDbOperater mongoDbOperater;
    private String databaseName;
    private int batchExecuteCount = 100;

    public MongodbResource() throws Exception {
    }

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param); //To change body of generated methods, choose Tools | Templates.
        if (param.containsKey("batchExecuteCount")) {
            batchExecuteCount = Integer.valueOf(param.get("batchExecuteCount").toString());
        }
        String ip = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        if (StringUtils.isNoneBlank(path) && path.length() > 1) {
            databaseName = path.substring(1);
        }
        MongoClientURI clientUri = new MongoClientURI(url);
        mongoClient = new MongoClient(clientUri);
        mongoDbOperater = new MongoDbOperater();
        mongoDbOperater.setMongoClient(mongoClient);
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public void setMongoClient(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public MongoDbOperater getMongoDbOperater() {
        return mongoDbOperater;
    }

    public void setMongoDbOperater(MongoDbOperater mongoDbOperater) {
        this.mongoDbOperater = mongoDbOperater;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }


    @Override
    public void persist(GatherStep gatherStep, String varName, Object obj, Map persistParams) {
        Map allParam = new HashMap();
        allParam.putAll(this.params);
        allParam.putAll(persistParams);
        
        String dbName = (String) allParam.get("database");
        if (StringUtils.isBlank(dbName)) {
            dbName = databaseName;
        }
        String colName = (String) allParam.get("collection");
        if (StringUtils.isBlank(colName)) {
            colName = varName;
        }
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                mongoDbOperater.insert(dbName, colName, convertMap((Map)item));
            }
        } else {
            mongoDbOperater.insert(dbName, colName, convertMap((Map)obj));
        }
    }
    
    private Map convertMap(Map item) {
        Map map = new HashMap();
        for (Object key : item.keySet()) {
            Object value = item.get(key);
            if (value instanceof BigDecimal) {
                map.put(key, ((BigDecimal) value).doubleValue());
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    @Override
    public Object loadResource(GatherStep gatherStep, Map loadParam) throws Exception {
        String dbName = (String) params.get("database");
        if (StringUtils.isBlank(dbName)) {
            dbName = databaseName;
        }
        String sql = (String) params.get("sql");
        return mongoDbOperater.find(dbName, sql);
    }

    @Override
    public Iterator<Object> iterate(GatherStep gatherStep, Map param) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<String> getParamKeys() {
        return Sets.newHashSet("database", "collection", "sql");
    }

}
