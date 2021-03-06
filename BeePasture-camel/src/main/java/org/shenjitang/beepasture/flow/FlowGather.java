/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.flow;

import com.beust.jcommander.internal.Lists;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ho.yaml.Yaml;
import org.shenjitang.beepasture.core.BeeGather;
import org.shenjitang.beepasture.core.GatherStep;
import org.shenjitang.beepasture.resource.CamelContextResource;
import org.shenjitang.beepasture.util.ParseUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 *
 * @author xiaolie
 */
public class FlowGather extends BeeGather {
    protected static final Log LOGGER = LogFactory.getLog(FlowGather.class);
    
    private CamelContext camelContext;
    private ApplicationContext springContext;
    private List<Map> flowList;
    private static int threadNo = 1;
    private CamelContextResource camelResource;
    private Long waitBeforeExit;
    private static int id = 1;

    public void setWaitBeforeExit(Long waitBeforeExit) {
        this.waitBeforeExit = waitBeforeExit;
    }

    public FlowGather(String yamlString) {
        super(yamlString);
    }

    @Override
    public void init() throws Exception {
        String oWaitTime = (String)program.get("exit");
        if (StringUtils.isNotBlank(oWaitTime)) {
            waitBeforeExit = ParseUtils.getTimeLong(oWaitTime);
        }
        String springContextUrl = (String)program.get("camel");
        if (StringUtils.isNotBlank(springContextUrl)) {
            URI uri = URI.create(springContextUrl);
            String schema = uri.getScheme();
            String path = uri.getSchemeSpecificPart();
            if ("file".equalsIgnoreCase(schema)) {
                springContext = new FileSystemXmlApplicationContext(path);
            } else if ("classpath".equalsIgnoreCase(schema)) {
                springContext = new ClassPathXmlApplicationContext(path);
            }
            camelContext = springContext.getBean(CamelContext.class);
        } else {
            camelContext = new DefaultCamelContext();
        }
        camelResource = new CamelContextResource();
        camelResource.init(springContextUrl, null);
        camelResource.setCamelContext(camelContext);
        resourceMng.addResource("camel", camelResource);
        
        flowList = (List)getProgram().get("flow");//(List<Map>)param.get("gather");
        Set flowFromResourceSet = (flowList == null ? new HashSet() : getFlowFromResourceSet(flowList));
        
        //resources = (Map)program.get("resource");
        loadResources();
        if (resources != null) {
            if (resourcesList == null) {
                resourceMng.init(resources);
            } else {
                resourceMng.init(resourcesList);
            }
            for (String key :resourceMng.getResourceMap().keySet()) {
                if (flowFromResourceSet.contains(key)) {
                    resourceMng.getResource(key).setFlowOutEndpoint("seda:__inner." + key);
                }
            }
        } else {
            resources = new HashMap();
        }
        vars = (Map)program.get("var");
        if (vars == null) {
            vars = new HashMap();
            program.put("var", vars);
        }
        initVars(vars, resources);
        gatherStepList = (List)program.get("gather");
        persistStep = (Map)program.get("persist");  
        if (flowList != null) {
            for (Map processMap : flowList) {
                startProcess(processMap);
            }
        }
    }
    
    protected String getEndpoint( Map processMap) {
        String flowUrl = (String)processMap.get("endpoint");
        if (StringUtils.isBlank(flowUrl)) {
            flowUrl = (String)processMap.get("url");
        }
        if (StringUtils.isBlank(flowUrl)) {
            String resourceName = (String)processMap.get("resource");
            if (StringUtils.isNotBlank(resourceName)) {
                flowUrl = "seda:__inner." + resourceName;
            } else {
                throw new RuntimeException("flow 下边必须有：url,endpoint,resource 之一");
            }
        }
        return flowUrl;
    }
    
    protected void startProcess(final Map processMap) throws ClassNotFoundException {
        final String flowUrl = getEndpoint(processMap);
        final String bodyType = (String)processMap.get("type");
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Exchange exchange = camelResource.receive(flowUrl);
                        Object body = bodyType == null ? exchange.getIn().getBody() : exchange.getIn().getBody(Class.forName(bodyType));
                        if(body != null) {
                            if (body instanceof org.fusesource.hawtbuf.Buffer) {
                                try {
                                    body = new String(((org.fusesource.hawtbuf.Buffer)body).getData(), "utf8");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            Map<String, Object> headers = exchange.getIn().getHeaders();
                            doProcess(processMap, body, headers);
                            //exchange.getOut().
                        } else {
                            LOGGER.warn(flowUrl + " body is null!");
                        }
                    } catch (Throwable th) {
                        LOGGER.warn(flowUrl, th);
                        th.printStackTrace();
                    }
                }
            }

        }, "bee-camel-process-" + threadNo++);
        th.start();
        LOGGER.debug("Thread: " + th.getName() + " started.");
    }

    private void doProcess(final Map processMap, final Object body, final Map<String, Object> headers) throws Exception{
        processMap.put("_with", body);
        GatherStep gatherStep = new GatherStep(processMap, id++);
        gatherStep.execute();
        //gatherStep.onceGather(processMap.get("url"));
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public ApplicationContext getSpringContext() {
        return springContext;
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    static Boolean debug = false;
    public static void main( String[] args ) throws Exception {
        String fileEncoding = System.getProperty("fileEncoding", "utf8");
        Long waitBeforeExit = Long.valueOf(System.getProperty("waitBeforeExit", "1000"));
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\filter_sample.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\dfa_sample_obj_js.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\esper_sample.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\esper_accesslog_new.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\nagao_sample.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\dce_03.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\ry\\100ppi_info_es.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\ry\\24h_gl.yaml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\ry\\es2file.yml";
        //String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\es2tagservice.yml";
        String fileName = "D:\\workspace\\神机堂\\GitHub\\BeePasture\\ry\\wechat\\weichat2es_flow_2.yaml";
        if (args.length > 0) {
            fileName = args[0];
        }
        MAIN_LOGGER.info("start fileEncoding=" + fileEncoding + " script=" + fileName);
        try {
            File file = new File(fileName);
            String yaml = FileUtils.readFileToString(file, fileEncoding);
            FlowGather flowGather = new FlowGather(yaml);
            flowGather.setWaitBeforeExit(waitBeforeExit);
            flowGather.init();
            flowGather.doGather();
            flowGather.saveTo();
            MAIN_LOGGER.info("finish fileEncoding=" + fileEncoding + " script=" + fileName);
            if ((args.length > 1 && "-d".equalsIgnoreCase(args[1])) || debug) {
                while(Boolean.TRUE) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {}
                }
            } else {
                flowGather.exit();
            }
        } catch (FileNotFoundException e) {
            MAIN_LOGGER.warn(fileName + " not exist 文件不存在！");
        }
    }
    
    private Set getFlowFromResourceSet(List<Map> flowList) {
        Set set = new HashSet();
        for (Map processMap : flowList) {
            String endpoint = (String)processMap.get("resource");
            if (StringUtils.isNotBlank(endpoint)) {
                set.add(endpoint);
            }
        }
        return set;
    }

    private void exit() {
        while (System.currentTimeMillis() - GatherStep.activeTime < waitBeforeExit) {
            System.out.println("******************* waitBeforeExit=" + waitBeforeExit + "    now to activeTime=" + (System.currentTimeMillis() - GatherStep.activeTime));
            try {
                Thread.sleep(waitBeforeExit);
            } catch (Exception e) {}
        }
        System.out.println("system exit.");
        System.exit(0);
    }

  
}
