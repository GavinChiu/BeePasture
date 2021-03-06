/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import org.shenjitang.beepasture.resource.util.ResourceUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SimpleDateFormatSerializer;
import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.codehaus.plexus.util.StringUtils;
import org.ho.yaml.Yaml;
import org.shenjitang.beepasture.core.GatherStep;
import org.shenjitang.beepasture.resource.util.ExcelParser;
import org.shenjitang.beepasture.util.ZipUtils;
import org.shenjitang.commons.csv.CSVUtils;

/**
 *
 * @author xiaolie
 */
public class FileResource extends BeeResource {
    protected File file;
    protected String fileName;
    static SerializeConfig mapping = new SerializeConfig();

    public FileResource() {
    }

    @Override
    public void init(String url, Map param) throws Exception {
        super.init(url, param); //To change body of generated methods, choose Tools | Templates.
        if (uri == null) {
            if (url.startsWith("file://")) {
                this.fileName = url.substring(7);
            } else if (url.startsWith("file:")) {
                this.fileName = url.substring(5);
            } else {
                this.fileName = url;
            }
            int idx = this.fileName.indexOf("?");
            if (idx > 0) {
                this.fileName = this.fileName.substring(0, idx);
            }
        } else {
            if (uri.getAuthority() != null && uri.getPath() != null) {
                this.fileName = uri.getAuthority() + uri.getPath();
            } else {
                this.fileName = uri.getSchemeSpecificPart();
            }
        }
        file = new File(this.fileName);
    }

    @Override
    public void persist(GatherStep gatherStep, String varName, Object obj, Map persistParams) {
        Map allParam = new HashMap();
        allParam.putAll(this.params);
        allParam.putAll(persistParams);
        obj = getValue(varName, obj);
        try {
            String encoding = ResourceUtils.get(allParam, "encoding", "GBK");
            String format = ResourceUtils.get(allParam, "format", "plant");
            String dataFormat = ResourceUtils.get(allParam, "dataFormat", "yyyy-MM-dd HH:mm:ss");
            LOGGER.info("save var:" + varName + " to file:" + file.getAbsolutePath());
            String path =  FilenameUtils.getPathNoEndSeparator(this.fileName);
            if (StringUtils.isNotBlank(path)) {
                File dir = new File(path);
                if (!dir.exists()) {
                    FileUtils.forceMkdir(dir);
                }
            }
            if ("yaml".equalsIgnoreCase(format)) {
                String content = Yaml.dump(obj);
                FileUtils.write(file, content, encoding);
            } else if ("json".equalsIgnoreCase(format)) {          
                mapping.put(Date.class, new SimpleDateFormatSerializer(dataFormat));
                StringBuilder sb = new StringBuilder();
                String jsonStr = JSON.toJSONString(obj, mapping);
                FileUtils.write(file, jsonStr, encoding);
            } else if ("csv".equalsIgnoreCase(format)) {
                if (obj instanceof List) {
                    CSVUtils csvUtils = new CSVUtils();
                    String csvStr = csvUtils.getCSVWithHeads((List)obj);
                    FileUtils.write(file, csvStr, encoding);
                } else {
                    throw new RuntimeException("Object:" + obj.getClass().getName() + " can not trans to csv");
                }
            } else { //default = plant
                if (obj instanceof List) {
                    StringBuilder sb = new StringBuilder();
                    for (Object o : (List) obj) {
                        sb.append(o.toString()).append("\n");
                    }
                    FileUtils.write(file, sb, encoding);
                } else {
                    FileUtils.write(file, obj.toString(), encoding);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("file:" + file.getAbsolutePath(), e);
        }
    }

    @Override
    public Object loadResource(GatherStep gatherStep, Map loadParam) throws Exception {
        Map iparams = new HashMap();
        iparams.putAll(uriParams);
//        String query  = uri.getQuery();
//        if (query != null) {
//            List<NameValuePair> queryPair = URLEncodedUtils.parse(query, Charset.forName("UTF-8"));
//            if (queryPair != null) {
//                for (NameValuePair nvp : queryPair) {
//                    iparams.put(nvp.getName(), nvp.getValue());
//                }
//            }
//        }
        iparams.putAll(loadParam);
        String encoding = ResourceUtils.get(iparams, "encoding", "GBK");
        String format = ResourceUtils.get(iparams, "format", "plant");
        if (format.equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))) {
            String unzipFilepath = ResourceUtils.get(loadParam, "to", ".");//System.getProperty("java.io.tmpdir")
            return ZipUtils.unzip(file, unzipFilepath, true, encoding);
        } else {
            return readFile(file, encoding, format);
        }
    }
    
    public static Object readFile(File file, String encoding, String format) throws Exception {
        if ("yaml".equalsIgnoreCase(format)) {
            return Yaml.load(FileUtils.readFileToString(file, encoding));
        } else if ("line".equalsIgnoreCase(format)) {
            return FileUtils.readLines(file, encoding);
        } else if ("json".equalsIgnoreCase(format)) {
            String str = FileUtils.readFileToString(file, encoding);
            return JSON.parse(str);
        } else if ("excel".equalsIgnoreCase(format)) {
            return ExcelParser.parseExcel(file, null);
        } else if ("pdf".equalsIgnoreCase(format)) {
            return parse(file);
        } else if ("auto".equalsIgnoreCase(format)) {
            return parse(file);
//            String ext = FilenameUtils.getExtension(file.getName());
//            if ("xls".equalsIgnoreCase(ext) || "xlsx".equalsIgnoreCase(ext)) {
//                return ExcelParser.parseExcel(file, null);
//            } else if ("pdf".equalsIgnoreCase(ext)) {
//                throw new RuntimeException("尚未实现");
//            } else {
//                return FileUtils.readFileToString(file, encoding);
//            }
        } else { //default = plant
            return FileUtils.readFileToString(file, encoding);
        }    
    }
    
    public static String parse(File file) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        try (InputStream stream = new FileInputStream(file)) {
            parser.parse(stream, handler, metadata);
            return handler.toString();
        }
    }    

    @Override
    public Iterator<Object> iterate(GatherStep gatherStep, Map loadParam) throws Exception {
        Map iparams = new HashMap();
        
        String query  = uri.getQuery();
        if (query != null) {
            List<NameValuePair> queryPair = URLEncodedUtils.parse(query, Charset.forName("UTF-8"));
            if (queryPair != null) {
                for (NameValuePair nvp : queryPair) {
                    iparams.put(nvp.getName(), nvp.getValue());
                }
            }
        }
        iparams.putAll(this.params);
        iparams.putAll(loadParam);
        return new FileLIneIterator(iparams);
    }

    @Override
    public Set<String> getParamKeys() {
            return Sets.newHashSet("encoding", "format", "dataFormat");
    }
    
    public class FileLIneIterator implements Iterator<Object> {
        String encoding;
        String format;
        BufferedReader reader;
        String line = null;
        public FileLIneIterator(Map loadParam) throws FileNotFoundException, UnsupportedEncodingException {
            String encoding = ResourceUtils.get(loadParam, "encoding", "GBK");
            String format = ResourceUtils.get(loadParam, "format", "line");
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
        }

        @Override
        public boolean hasNext() {
            try {
                line = reader.readLine();
                return line != null;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public Object next() {
            return line;
        }

        @Override
        public void remove() {
        }
        
    }
    
}
