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
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.x42.sling.at16.proxyresolver.HttpProxy;
import ch.x42.sling.at16.proxyresolver.WorkerProxy;

@Component(metatype=true, policy=ConfigurationPolicy.REQUIRE)
@Service(value=WorkerProxy.class)
public class WorkerProxyImpl implements WorkerProxy {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private HttpProxy httpProxy;
    
    @Property(value={})
    public static final String PROP_HEADERS_FORMAT = "http.headers.format";
    private String [] headersFormat;
 
    @Activate
    protected void activate(Map<String, Object> config) throws ServletException {
        headersFormat = PropertiesUtil.toStringArray(config.get(PROP_HEADERS_FORMAT), new String [] {});
    }
    
    @Override
    public void proxy(SlingHttpServletRequest request, SlingHttpServletResponse response, String workerRole) throws ServletException, IOException {
        final Object [] data = new Object[] { workerRole };
        
        // Configurable headers can be added to the request
        final Map<String, String> additionalHeaders = new HashMap<String, String>();
        for(String fmt : headersFormat) {
            final String header = MessageFormat.format(fmt, data);
            final String [] parts = header.split(":");
            if(parts.length != 2) {
                throw new ServletException("Bad header format: " + header);
            }
            additionalHeaders.put(parts[0], parts[1]);
        }
        
        // Wrapper that adds our headers to the request
        final SlingHttpServletRequestWrapper wrappedRequest = new SlingHttpServletRequestWrapper(request) {

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
        
        U.logAndRequestProgress(request, log, "Proxying via {0} with additional headers {1}", httpProxy, additionalHeaders);
        httpProxy.proxy(wrappedRequest, response);
    }
}