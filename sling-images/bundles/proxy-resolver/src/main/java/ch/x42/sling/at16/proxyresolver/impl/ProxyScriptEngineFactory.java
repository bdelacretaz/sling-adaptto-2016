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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

/** Proxy the current request based on the role defined by our proxy "script" */
@Component(
    metatype = true,
    label = "Sling adaptTo() 2016 demo - proxy script engine",
    description = "Script engine that proxies requests to backend workers"
)
@Service(ScriptEngineFactory.class)
public class ProxyScriptEngineFactory extends AbstractScriptEngineFactory {
    public static final List<String> EXTENSIONS = new ArrayList<String>();
    private ProxyServlet proxyServlet;
    
    static {
        EXTENSIONS.add("proxy");
    }
    
    @Property(value="")
    public static final String PROP_PROXY_URL = "proxy.url";
    private String proxyUrl;
 
    @Property(value={})
    public static final String PROP_HEADERS_FORMAT = "http.headers.format";
    private String [] headersFormat;
 
    @Activate
    protected void activate(Map<String, Object> config) throws ServletException {
        proxyUrl = PropertiesUtil.toString(config.get(PROP_PROXY_URL), "");
        headersFormat = PropertiesUtil.toStringArray(config.get(PROP_HEADERS_FORMAT), new String [] {});
        
        // Setup the ProxyServlet
        final ServletConfig cfg = new ServletConfig() {
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
        
        proxyServlet = new ProxyServlet();
        proxyServlet.init(cfg);
    }
    
    @Deactivate
    protected void deactivate(Map<String, Object> config) throws ServletException {
        proxyServlet.destroy();
    }
    
    @Override
    public String getLanguageName() {
        return getClass().getName();
    }

    @Override
    public String getLanguageVersion() {
        return "0.1";
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new ProxyScriptEngine(this, proxyServlet, proxyUrl, headersFormat);
    }

    @Override
    public List<String> getExtensions() {
        return EXTENSIONS;
    }
}