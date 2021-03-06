/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.debug;

import com.alibaba.fastjson.JSON;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.shenjitang.beepasture.core.BeeGather;
import org.shenjitang.beepasture.core.GatherStep;
import org.shenjitang.beepasture.resource.util.ResourceUtils;

/**
 *
 * @author xiaolie
 */
public class GatherDebug {
    static Boolean debug = Boolean.valueOf(System.getProperty("debug", "false"));
    private static String msg = "";
    private static DebugLevel level = DebugLevel.STATEMENT;
    private static GatherStep gatherStep;

    public GatherDebug() {
    }
    
    public static void setGatherStep(GatherStep gatherStep) {
        GatherDebug.gatherStep = gatherStep;
    }
    
    public static void help() {
        System.out.println("单点调试只支持extract键下的语句，extract键外的处理不建议使用，将来也不会支持。");
        System.out.println("?             说明：help 显示帮助");
        System.out.println("n             说明：next 下一步");
        System.out.println("ns            说明：next step 跳到下一个step");
        System.out.println("ng            说明：next gather 跳到下一个gather");
        System.out.println("l             说明：list 列出变量名");
        System.out.println("p [varname]   说明：print 打印变量内容");
        System.out.println("e             说明：exit 退出debug模式，执行到底");
        System.out.println("q             说明：quit 退出程序");
        System.out.println("s [varname] [charset] [filename]   说明：save 保存变量到文件");
        System.out.println("t [varname] [key]=[value]   说明：test 针对变量做测试，变量是it可以省略，key有：xpath,jsonpath,regex,script");

    }
    
    public static void debug(DebugLevel currentLevel, String desc) {
        if (!debug) {
            return;
        }
        if (level == DebugLevel.GATHER) {
            if (currentLevel != DebugLevel.GATHER) {
                return;
            }
        }
        if (level == DebugLevel.STEP) {
            if (currentLevel == DebugLevel.STATEMENT) {
                return;
            }
        }
        try {
            System.out.println("prev: " + msg);
            msg = desc;
            System.out.println("next: " + msg);
            System.out.println("cmd: ? n l p e s t");
            while(debug) {
                System.out.print("Gather:" + gatherStep.getId() + "> ");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String cmd = reader.readLine().trim();
                if (cmd.toLowerCase().startsWith("n")) { //下一步
                    level = DebugLevel.STATEMENT;
                    return;
                } else if (cmd.toLowerCase().startsWith("ng")) { //下一个gather
                    level = DebugLevel.GATHER;
                    return;
                } else if (cmd.toLowerCase().startsWith("ns")) { //下一个step
                    level = DebugLevel.STEP;
                    return;
                } else if (cmd.toLowerCase().startsWith("?")) {
                    help();
                } else if (cmd.toLowerCase().startsWith("t")) {
                    String[] pres = ResourceUtils.substringHead(cmd, "=").split(" ");
                    String varname = "it";
                    String key = pres[1];
                    if (pres.length == 3) {
                        varname = pres[1];
                        key = pres[2];
                    }
                    String express = ResourceUtils.substringTail(cmd, "=").trim();
                    Object content = getContent(varname);
                    if ("xpath".equalsIgnoreCase(key)) {
                        List res = gatherStep.doXpath(content.toString(), express);
                        System.out.println(JSON.toJSONString(res));
                    } else if ("jsonpath".equalsIgnoreCase(key)) {
                        List res = gatherStep.doJsonpath(content.toString(), express);
                        System.out.println(JSON.toJSONString(res));
                    } else if ("regex".equalsIgnoreCase(key)) {
                        String res = gatherStep.doRegex(content.toString(), express);
                        System.out.println(JSON.toJSONString(res));
                    } else if ("script".equalsIgnoreCase(key)) { //t script=[value]
                        Object res = gatherStep.doScript(express);
                        System.out.println(JSON.toJSONString(res));
                    }
                } else if (cmd.toLowerCase().startsWith("l")) {
                    Iterator varnames = gatherStep.getTemplateParamMap().keySet().iterator();
                    while (varnames.hasNext()) {
                        System.out.println(varnames.next().toString());
                    }
                } else if (cmd.toLowerCase().startsWith("e")) {
                    debug = false;
                    return;
                } else if (cmd.toLowerCase().startsWith("q")) {
                    System.exit(-2);
                } else if (cmd.toLowerCase().startsWith("p")) {
                    if (cmd.trim().length() == 1) {
                        Object o = gatherStep.getTemplateParamMap().get("it");
                        System.out.println("it");
                        System.out.println(JSON.toJSONString(o));
                    } else {
                        String[] vars = cmd.substring(1).trim().split(" ");
                        for (String varName :vars) {
                            Object o = gatherStep.getTemplateParamMap().get(varName);
                            System.out.println(varName);
                            System.out.println(JSON.toJSONString(o));
                        }
                    }
                    
                } else if (cmd.toLowerCase().startsWith("s")) {
                    String[] vars = cmd.substring(1).trim().split(" ");
                    String varName = "it";
                    String charset = "gbk";
                    String fileName = varName + "_" + System.currentTimeMillis() + ".debug";
                    switch (vars.length) {
                        case 0:
                            break;
                        case 1:
                            varName = vars[0];
                            break;
                        case 2:
                            varName = vars[0];
                            fileName = vars[1];
                            break;
                        case 3:
                            varName = vars[0];
                            charset = vars[1];
                            fileName = vars[2];
                            break;
                        default:
                            System.out.println("正确的语法：s varname [charset] filename (保存变量到文件) ");
                            continue;
                    }
                    Object o = gatherStep.getTemplateParamMap().get(varName);
                    FileUtils.write(new File(fileName), JSON.toJSONString(o), charset);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static Object getContent(String key) {
        Object content = gatherStep.getTemplateParamMap().get(key);
        if (content == null) {
            content = BeeGather.getInstance().getVars().get(key);
        }
        if (content == null) {
            content = key;
        }
        return content;
    }
}
