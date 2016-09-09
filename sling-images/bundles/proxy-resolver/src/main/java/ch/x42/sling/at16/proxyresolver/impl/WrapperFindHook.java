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

import java.util.Collection;

import org.apache.sling.api.servlets.ServletResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.FindHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FindHook that hides the default ServletResolver and exposes
 *  only our wrapper.
 */
class WrapperFindHook implements FindHook {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String RESOLVER_CLASS_NAME = ServletResolver.class.getName();
    private final BundleContext myBundleContext;

    public WrapperFindHook(BundleContext ctx) {
        myBundleContext = ctx;
    }
    
    @Override
    public void find(BundleContext context, String name, String filter, boolean allServices, Collection<ServiceReference<?>> references) {
        if(myBundleContext.equals(myBundleContext) || context.getBundle().getBundleId() == 0) {
            // Don't hide anything for this bundle or system bundle
            return;
        }
        
        // Expose only our wrapper resolver, unless the filter is the one used by our wrapper
        // TODO more efficient way of finding out that the find is for the ServletResolver??
        if(RESOLVER_CLASS_NAME.equals(name) || (filter != null && filter.contains(RESOLVER_CLASS_NAME))) {
            for(ServiceReference<?> ref : references) {
                if(ref.getProperty(ServletResolverWrapper.PROXY_WRAPPER) == null) {
                    log.debug("Removing {} from found references", ref);
                    references.remove(ref);
                }
            }
        }
    }
}
