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

import java.util.Map;

import javax.servlet.Servlet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;

/** Wraps the default ServletResolver to always return our ProxyServlet,
 *  after computing the instance role for the proxy based on the script
 *  or servlet found by the default resolver.
 */
@Component()
@Service(value=ServletResolver.class)
@Property(name=ServletResolverWrapper.PROXY_WRAPPER, boolValue=true)
public class ServletResolverWrapper implements ServletResolver {

    public static final String PROXY_WRAPPER = "proxyWrapper";
    public static final String DEFAULT_RESOLVER_FILTER ="(!(proxyWrapper=true))";
    
    public static final String DEFAULT_PROXY_URL_FORMAT = "http://sling-workers/role:{0}/{1}";
    @Property(value=DEFAULT_PROXY_URL_FORMAT)
    public static final String PROP_PROXY_URL_FORMAT = "proxy.url.format";
    private String proxyUrlFormat;
    
    @Activate
    protected void activate(Map<String, Object> config) {
        proxyUrlFormat = PropertiesUtil.toString(config.get(PROP_PROXY_URL_FORMAT), DEFAULT_PROXY_URL_FORMAT);
    }
    
    @Reference(target=DEFAULT_RESOLVER_FILTER)
    private ServletResolver defaultResolver;
    
    @Override
    public Servlet resolveServlet(SlingHttpServletRequest request) {
        return new ProxyServlet(defaultResolver.resolveServlet(request), proxyUrlFormat);
    }

    @Override
    public Servlet resolveServlet(Resource resource, String scriptName) {
        return new ProxyServlet(defaultResolver.resolveServlet(resource, scriptName), proxyUrlFormat); 
    }

    @Override
    public Servlet resolveServlet(ResourceResolver resolver, String scriptName) {
        return new ProxyServlet(defaultResolver.resolveServlet(resolver, scriptName), proxyUrlFormat); 
    }    
}