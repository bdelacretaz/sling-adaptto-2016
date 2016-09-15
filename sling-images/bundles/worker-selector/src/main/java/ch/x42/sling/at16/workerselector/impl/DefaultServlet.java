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

/** Servlet that handles requests for which a specific worker
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
    
    public static final String ROLE_PROPERTY = "sling:workerRole";
    
    public static final String DEFAULT_ROLE_VALUE = "default";
    
    @Property(value=DEFAULT_ROLE_VALUE)
    public static final String PROP_DEFAULT_ROLE = "default.role";
    private String defaultRole;
    
    @Activate
    protected void activate(Map<String, Object> config) throws ServletException {
        defaultRole = PropertiesUtil.toString(config.get(PROP_DEFAULT_ROLE), DEFAULT_ROLE_VALUE);
    }
    
    @Override
    protected void service(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException, IOException {
        
        final Resource r = request.getResource();
                
        // Worker role defined in content has priority
        String workerRole = getWorkerRoleFromContent(request, r);
        
        if(workerRole != null) {
            U.logAndRequestProgress(request, log, "Worker role {0} set from content for {1}", workerRole, r.getPath());
        } else {
            // Else try with resource type and supertype
            workerRole = getWorkerRoleFromResourceType(r);
        }
        
        if(workerRole == null) {
            workerRole = defaultRole;
            U.logAndRequestProgress(request, log, "Worker role {0} set by default for {1}", workerRole, r.getPath());
        }
        
        U.logAndRequestProgress(request, log, "No proxy script for current request to {0}, proxying to {1}", r.getPath(), workerRole);
        U.outputRole(response, workerRole);
    }
    
    /** Find worker role property on the resource itself, or its ancestors.
     *  TODO should be cached.
     */
    private String getWorkerRoleFromContent(SlingHttpServletRequest request, Resource r) {
        if(r == null) {
            return null;
        }
        
        String role = null;
        
        final ValueMap m = r.adaptTo(ValueMap.class);
        if(m != null) {
            final String resourceRole = m.get(ROLE_PROPERTY, String.class);
            if(resourceRole != null) {
                role = resourceRole;
                U.logAndRequestProgress(request, log, "Resource {0} has property {1}, role set to {2}", new Object [] { r.getPath(), ROLE_PROPERTY, role });
            } else {
                role = getWorkerRoleFromContent(request, r.getParent());
            }
        }
        
        return role;
    }
    
    /** Find worker role set on the resource type or supertype path under our search root. */
    private String getWorkerRoleFromResourceType(Resource r) {
        String role = null;
        // TODO
        return role;
    }
}