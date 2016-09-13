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
import java.io.Reader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Proxy the current request based on the worker role defined by our script */
public class ProxyScriptEngine extends AbstractSlingScriptEngine {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    //public static final String DEFAULT_ROLE = "default-role";
    //public static final String ROLE_PROPERTY = "sling:workerRole";
    
    private final String proxyUrl;
    private final String [] proxyHeadersFormat;
    private final ProxyServlet proxyServlet;
    
    ProxyScriptEngine(ScriptEngineFactory factory, ProxyServlet proxyServlet, String proxyUrl, String ... proxyHeadersFormat) {
        super(factory);
        this.proxyUrl = proxyUrl;
        this.proxyHeadersFormat = proxyHeadersFormat;
        this.proxyServlet = proxyServlet;
    }
    
    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        try {
            // TODO allow the Resource to override the role, using resolveWorkerRole
            final String workerRole = IOUtils.toString(reader).trim();
            final SlingHttpServletRequest request = getContext(context, "request", SlingHttpServletRequest.class); 
            final SlingHttpServletResponse response = getContext(context, "response", SlingHttpServletResponse.class); 
            proxy(request, response, workerRole);
        } catch (Exception e) {
            final ScriptException se = new ScriptException("Proxy setup or execution failed");
            se.initCause(e);
            throw se;
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private<T> T getContext(ScriptContext context, String key, Class <T> clazz) throws ScriptException {
        Object o = null;
        
        for(int scope : context.getScopes()) {
            o = context.getBindings(scope).get(key);
            if(o != null) {
                break;
            }
        }
        
        if(o == null) {
            throw new ScriptException(key + " not found in ScriptContext");
        }
        
        if(!(clazz.isAssignableFrom(o.getClass()))) {
            throw new ScriptException("For ScriptContext key " + key + ", " + o + " is not a " + clazz.getName());
        }
        
        return (T)o;
    }
    
    private String getPathInfo(SlingHttpServletRequest request) {
        String result = request.getPathInfo();
        if(result == null ) {
            result = "";
        }
        if(result.startsWith("/")) {
            return result.substring(1);
        }
        return result;
    }
    
    /** Proxy our request to the specified worker 
     * @throws IOException 
     * @throws ServletException */
    private void proxy(SlingHttpServletRequest request, SlingHttpServletResponse response, String workerRole) throws ServletException, IOException {
        final Object [] data = new Object[] { workerRole, getPathInfo(request) };
        
        // Configurable headers can be added to the request
        final Map<String, String> additionalHeaders = new HashMap<String, String>();
        for(String fmt : proxyHeadersFormat) {
            final String header = MessageFormat.format(fmt, data);
            final String [] parts = header.split(":");
            if(parts.length != 2) {
                throw new ServletException("Bad header format: " + header);
            }
            additionalHeaders.put(parts[0], parts[1]);
        }
        
        // Wrapper that adds our headers to the request
        final SlingHttpServletRequestWrapper w = new SlingHttpServletRequestWrapper(request) {

            @Override
            public String getHeader(String name) {
                if(additionalHeaders.containsKey(name)) {
                    return additionalHeaders.get(name);
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<?> getHeaders(String name) {
                if(additionalHeaders.containsKey(name)) {
                    final Vector<Object> v = new Vector<Object>();
                    v.add(additionalHeaders.get(name));
                    return v.elements();
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<?> getHeaderNames() {
                final Vector<Object> names = new Vector<Object>();
                final Enumeration<?> orig = super.getHeaderNames();
                while(orig.hasMoreElements()) {
                    names.add(orig.nextElement());
                }
                names.addAll(additionalHeaders.keySet());
                return names.elements();
            }
            
        };
 
        log.info("Proxying to {} with additional headers {}", proxyUrl, additionalHeaders);
        
        final URL targetURL = new URL(proxyUrl);
        final HttpHost host = new HttpHost(targetURL.getHost(), targetURL.getPort());
        request.setAttribute(ProxyServlet.ATTR_TARGET_URI, proxyUrl);
        request.setAttribute(ProxyServlet.ATTR_TARGET_HOST, host);
        proxyServlet.service(w,  response);
    }

    /*
    private String resolveWorkerRole(Resource r) {
        if(r == null) {
            return DEFAULT_ROLE;
        }
        
        String role = DEFAULT_ROLE;
        
        
        // Find a role property on r or its ancestors
        // TODO cache this?
        final ValueMap m = r.adaptTo(ValueMap.class);
        if(m != null) {
            final String resourceRole = m.get(ROLE_PROPERTY, String.class);
            if(resourceRole != null) {
                role = resourceRole;
                log.debug("Resource {} has {}, role set to {}", new Object [] { r.getPath(), ROLE_PROPERTY, role });
            } else {
                role = resolveWorkerRole(r.getParent());
            }
        }
        
        return role;
    }
    */
}