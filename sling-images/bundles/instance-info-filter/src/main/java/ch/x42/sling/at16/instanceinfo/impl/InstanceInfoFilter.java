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
package ch.x42.sling.at16.instanceinfo.impl;

import java.io.IOException;
import java.text.MessageFormat;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.ComponentContext;

/** Filter that adds info about the current instance to the response,
 *  for troubleshooting our multi-instance setup.
 */
@Component
@Service(value=Filter.class)
@Properties({
    @Property(name="sling.filter.scope", value="REQUEST")
})
public class InstanceInfoFilter implements Filter {

    public static String HEADER_NAME = "Sling-Instance-Info";
    private String envInfo;
    public static final String SLING_ENV_INFO = "sling.environment.info";
    
    @Reference
    private SlingSettingsService settings;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
    
    @Activate
    public void activate(ComponentContext ctx) {
        envInfo = ctx.getBundleContext().getProperty(SLING_ENV_INFO);
        if(envInfo == null) {
            envInfo = "NO INFO PROVIDED";
        }
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse servletResponse, FilterChain chain) 
    throws IOException, ServletException {
        final String headerValue = MessageFormat.format("SlingId:{0}; {1}:\"{2}\"", settings.getSlingId(), SLING_ENV_INFO, envInfo);
        final HttpServletResponse response = (HttpServletResponse)servletResponse;
        response.addHeader(HEADER_NAME, headerValue);
        
        // Log some useful info to the sling RequestProgressTracker if we have it
        if(request instanceof SlingHttpServletRequest) {
            RequestProgressTracker t = ((SlingHttpServletRequest)request).getRequestProgressTracker();
            t.log("{0}={1}", HEADER_NAME, headerValue);
            t.log("Content-Type={0}", request.getContentType());
        }
            
        chain.doFilter(request, response);
    }
}