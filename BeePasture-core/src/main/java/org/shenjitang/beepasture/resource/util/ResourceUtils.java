/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource.util;

import java.util.Map;

/**
 *
 * @author xiaolie
 */
public class ResourceUtils {
//    public static String get(Map map, String key, String def) {
//        Object value = map.get(key);
//        if (value == null) {
//            value = def;
//        }
//        return value.toString();
//    }
    
    public static <T> T get(Map map, String key, T def) {
        Object value = map.get(key);
        if (value == null) {
            return def;
        } else {
            return (T)value;
        }
    }
    
    public static String substringHead(String str, String chars) {
        int idx = str.indexOf(chars);
        if (idx >= 0) {
            return str.substring(0, idx);
        } else {
            return str;
        }
    }
    
    public static String substringTail(String str, String chars) {
        int idx = str.lastIndexOf(chars);
        if (idx >= 0) {
            return str.substring(idx + 1);
        } else {
            return str;
        }
    }
    
    public static String getMiddle(String str, String beginChar, String endChar) {
        int idx1 = str.indexOf(beginChar);
        if (idx1< 0) {
            return null;
        }
        String sub1 = str.substring(idx1 + 1);
        int idx2 = sub1.indexOf(endChar);
        if (idx2 < 0) {
            return null;
        }
        return sub1.substring(0, idx2);
    }

    public static String assembleUrl(String fileUrl, Map<String, String> fileUrlParams) {
        if (fileUrlParams == null || fileUrlParams.isEmpty()) {
            return fileUrl;
        }
        StringBuilder sb = new StringBuilder().append(fileUrl).append("?");
        for (String key : fileUrlParams.keySet()) {
            String value = fileUrlParams.get(key);
            sb.append(key).append("=").append(value).append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
    

}
