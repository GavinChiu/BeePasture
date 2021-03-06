/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;


/**
 *
 * @author xiaolie
 */
public class ResourceMng {
    private final Map<String, BeeResource> resourceMap = new HashMap();
    private final static Pattern RESOURCE_PATTERN = Pattern.compile("^[a-zA-z0-9]{2,18}:[^\\s]*");
    

    public ResourceMng() {
    }
    
    public BeeResource getResource(String name, Boolean save) {
        if (StringUtils.isBlank(name)) {
            name = "camel";
        }
        try {
            BeeResource beeResurce = resourceMap.get(name);
            if (beeResurce == null) {
                if (maybeResource(name)) {
                    Map params = new HashMap();
                    params.put("url", name);
                    beeResurce = initOneResource(name, params, save);
                } else {
                    //throw new RuntimeException("不认识的资源或url: " + name);
                    return null;
                }
            }
            return beeResurce;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public BeeResource getResource(String name) {
        return getResource(name, true);
    }
    
    public BeeResource addResource(String name, BeeResource resource) {
        resourceMap.put(name, resource);
        return resource;
    }

    public BeeResource initOneResource(String name, Map params, Boolean save) {
        String url = (String) params.get("url");
        String scheme = null;
        try {
            System.out.println("init resource : " + url);
            URI uri = URI.create(url);
            scheme = uri.getScheme();
        } catch (IllegalArgumentException e) {
            scheme = url.substring(0, url.indexOf(":"));
        }
        try {
            String resourceClassname = "org.shenjitang.beepasture.resource." + StringUtils.capitalize(scheme) + "Resource";
            BeeResource resource = (BeeResource) Class.forName(resourceClassname).newInstance();
            resource.setName(name);
            resource.init(url, params);
            if (save) {
                resourceMap.put(name, resource);
            }
            return resource;
        } catch (Exception e) {
            throw new RuntimeException("Don't know resource :" + url, e);
        }
    }
    
    public void init(List resources) {
        for (Object oone : resources) {
            Map one = (Map)oone;
            String name = (String)one.get("name");
            if (StringUtils.isBlank(name)) {
                URI uri = URI.create((String)one.get("url"));
                name = uri.getHost();
            }
            initOneResource(name, one, true);
        }
    }

    public void init(Map resources) {
        try {
            for (Object key : resources.keySet() ) {
                initOneResource((String)key, (Map)resources.get(key), true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public Map<String, BeeResource> getResourceMap() {
        return resourceMap;
    }
    
    static public final boolean maybeResource(String str) {
        return RESOURCE_PATTERN.matcher(str).find();
    }

    
}
