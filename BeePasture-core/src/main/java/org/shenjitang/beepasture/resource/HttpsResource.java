/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.resource;

/**
 *
 * @author xiaolie
 */
public class HttpsResource extends HttpResource {

    public HttpsResource() throws Exception {
        ssl = true;
    }
    
}
