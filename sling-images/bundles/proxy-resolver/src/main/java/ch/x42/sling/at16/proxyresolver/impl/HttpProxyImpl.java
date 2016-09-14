/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package ch.x42.sling.at16.proxyresolver.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpHost;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

import ch.x42.sling.at16.proxyresolver.HttpProxy;

/** Proxy HTTP requests */
@Component(metatype=true, policy=ConfigurationPolicy.REQUIRE)
@Service(value=HttpProxy.class)
public class HttpProxyImpl implements HttpProxy {
    
    @Property
    public static final String PROP_PROXY_URL = "proxy.url";
    private String proxyUrl;
    
    private ProxyServlet proxyServlet;
 
    private static class LocalServletConfig implements ServletConfig {
        final Map<String, String> initParams = new HashMap<String, String>();
        
        {
            initParams.put(ProxyServlet.P_TARGET_URI, "http://TODO-whatDoesTargetURImean");
        }
        
        @Override
        public String getServletName() {
            return ProxyScriptEngineFactory.class.getSimpleName() + "-proxy";
        }

        @Override
        public ServletContext getServletContext() {
            return new MockServletContext();
        }
                
        @Override
        public String getInitParameter(String name) {
            return initParams.get(name);
        }

        @Override
        public Enumeration<?> getInitParameterNames() {
            return new Vector<Object>().elements();
        }
    };
    
    @Activate
    protected void activate(Map<String, Object> config) throws ServletException {
        proxyUrl = PropertiesUtil.toString(config.get(PROP_PROXY_URL), "");
        
        proxyServlet = new ProxyServlet();
        proxyServlet.init(new LocalServletConfig());
    }
    
    @Deactivate
    protected void deactivate(Map<String, Object> config) throws ServletException {
        proxyServlet.destroy();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + proxyUrl + ")";
    }
    
    @Override
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final URL targetURL = new URL(proxyUrl);
        final HttpHost host = new HttpHost(targetURL.getHost(), targetURL.getPort());
        request.setAttribute(ProxyServlet.ATTR_TARGET_URI, proxyUrl);
        request.setAttribute(ProxyServlet.ATTR_TARGET_HOST, host);
        proxyServlet.service(request, response);
    }
}