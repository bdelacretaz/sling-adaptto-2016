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

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Proxy the current request based on the role computed by ServletResolverWrapper */
@SuppressWarnings("serial")
class ProxyServlet extends SlingAllMethodsServlet {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String DEFAULT_ROLE = "default-role";
    public static final String ROLE_PROPERTY = "sling:workerRole";
    
    private final Servlet defaultServlet;
    private final String proxyUrlFormat;
    
    ProxyServlet(Servlet defaultServlet, String proxyUrlFormat) {
        this.defaultServlet = defaultServlet;
        this.proxyUrlFormat = proxyUrlFormat;
    }

    @Override
    protected void service(SlingHttpServletRequest request, SlingHttpServletResponse response) 
        throws ServletException, IOException {
        
        // Role can be set on the resource (or its ancestors) and if not by the servlet
        final Resource r = request.getResource();
        String role = resolveWorkerRole(r);
        if(role == null) {
            role = resolveWorkerRole(defaultServlet); 
        }
        if(role == null) {
            role = DEFAULT_ROLE;
        }

        String path = request.getPathInfo();
        if(path == null) {
            path = "";
        } else if(path.startsWith("/")) {
            path = path.substring(1);
        }
        final String proxyTo = MessageFormat.format(proxyUrlFormat, role, path);
        log.debug("TODO - would proxy to {} for {} (default servlet)", 
                new Object [] { proxyTo, r.getPath(), defaultServlet });
        defaultServlet.service(request, response);
    }
    
    /** Find which Sling worker should be used to process the request, given that the supplied
     *  Servlet would handle it if we did that in the same instance.
     */
    private String resolveWorkerRole(Servlet s) {
        String role = DEFAULT_ROLE;
        
        // TODO hacky hacky for now
        if(s.getClass().getName().startsWith("org.apache.sling.servlets")) {
            role = "default-servlets";
            log.debug("Role set to {} from servlet class name {}", role, s.getClass().getName());
        } else if(s instanceof SlingScript) {
            final Resource r = ((SlingScript)s).getScriptResource();
            role = resolveWorkerRole(r);
            log.debug("Role set to {} from resource {}", role, r.getPath());
        }
        
        return role;
    }
    
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
}