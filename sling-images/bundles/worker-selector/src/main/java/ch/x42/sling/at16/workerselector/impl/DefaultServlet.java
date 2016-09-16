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
package ch.x42.sling.at16.workerselector.impl;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Servlet that handles requests for which a specific processor
 *  role script is not found. Looks at the resource and resource
 *  type and supertype to see if a role is defined, and if not 
 *  falls back to a default role. 
 */
@SuppressWarnings("serial")
@SlingServlet(
        resourceTypes="sling/servlet/default",
        methods={ "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "MKCOL", "PROPFIND" })
public class DefaultServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String ROLE_PROPERTY = "sling:processorRole";
    
    public static final String DEFAULT_ROLE_VALUE = "default";
    
    @Property(value=DEFAULT_ROLE_VALUE)
    public static final String PROP_DEFAULT_ROLE = "default.role";
    private String defaultRole;
    
    /** Path under which routing is defined */
    public static final String DEFAULT_ROUTING_PATH = "/cluster/routing";
    
    @Property(value=DEFAULT_ROUTING_PATH)
    public static final String PROP_ROUTING_PATH = "routing.path";
    private String routingPath;
    
    @Activate
    protected void activate(Map<String, Object> config) throws ServletException {
        defaultRole = PropertiesUtil.toString(config.get(PROP_DEFAULT_ROLE), DEFAULT_ROLE_VALUE);
        routingPath = PropertiesUtil.toString(config.get(PROP_ROUTING_PATH), DEFAULT_ROUTING_PATH);
    }
    
    @Override
    protected void service(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException, IOException {
        
        final Resource r = request.getResource();
                
        // TODO roles should be cached??
        
        // Role defined in content has priority
        String role = getRoleFromContent(request, r);
        if(role == null) {
            // Else try with resource type and supertype
            role = getRoleFromResourceType(request, r);
        }
        
        // Else try with request attributes
        if(role == null) {
            role = getRoleFromRequest(request);
        }
        
        // Else use our default role
        if(role == null) {
            role = defaultRole;
            U.logAndRequestProgress(request, log, "Processor role {0} set by default for {1}", role, r.getPath());
        }
        
        U.logAndRequestProgress(request, log, "No routing script for current request to {0}, processor role set to {1}", r.getPath(), role);
        U.outputRole(response, role);
    }
    
    private String getRoleFromRequest(SlingHttpServletRequest request) {
        String role = null;
        final String methodPath = routingPath + "/methods/" + request.getMethod();
        final Resource r = request.getResourceResolver().resolve(methodPath);
        if(r != null) {
            final ValueMap m = r.adaptTo(ValueMap.class);
            if(m != null && m.containsKey(ROLE_PROPERTY)) {
                role = m.get(ROLE_PROPERTY, String.class).trim();
            }
        }
        
        if(role != null) {
            U.logAndRequestProgress(request, log, "Processor role {0} set from {1}/{2}", 
                    role, request.getMethod(), r.getPath(), ROLE_PROPERTY);
        }
        
        return role;
    }
    
    private String getRoleFromContent(SlingHttpServletRequest request, Resource r) {
        if(r == null) {
            return null;
        }
        
        String role = resourceToRole(request, r);
        if(role != null) {
            U.logAndRequestProgress(request, log, "Resource {0} has property {1}, role set to {2}", r.getPath(), ROLE_PROPERTY, role);
        } else {
            role = getRoleFromContent(request, r.getParent());
        }
            
        if(role != null) {
            U.logAndRequestProgress(request, log, "Processor role {0} set from content for {1}", role, r.getPath());
        }
        
        return role;
    }
    
    private String resourceToRole(SlingHttpServletRequest request, Resource r) {
        final ValueMap m = r.adaptTo(ValueMap.class);
        String role = null;
        if(m != null) {
            role = m.get(ROLE_PROPERTY, String.class);
        }
        if(role != null) {
            U.logAndRequestProgress(request, log, "Processor role {0} set from resource {1}", role, r.getPath());
        }
        return role;
    }
    
    private String getRoleFromResourceType(SlingHttpServletRequest request, Resource r) {
        String role = resourceTypeToRole(request, r.getResourceType());
        if(role == null) {
            role = resourceTypeToRole(request, r.getResourceSuperType());
        }
        return role;
    }
    
    private String resourceTypeToRole(SlingHttpServletRequest request, String type) {
        final String path = routingPath + "/resource-types/" + type;
        final Resource r = request.getResourceResolver().resolve(path);
        if(r==null) {
            return null;
        }
        
        String role = resourceToRole(request, r);
        if(role == null) {
            role = resourceToRole(request, r.getParent());
        }
        return role;
    }
}