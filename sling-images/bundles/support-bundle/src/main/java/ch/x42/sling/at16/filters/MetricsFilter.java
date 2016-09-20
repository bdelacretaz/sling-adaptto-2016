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
package ch.x42.sling.at16.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.metrics.MetricsService;
import org.apache.sling.commons.metrics.Timer;

/** Sling Filter that provides useful metrics for our demo */
@Component(immediate=true, metatype=false)
@Service(value=javax.servlet.Filter.class)
@Properties({
    @Property(name="sling.filter.scope", value="request")
})
public class MetricsFilter implements Filter {

    public static final String T_PREFIX = MetricsFilter.class.getSimpleName() + ".requests";

    @Reference
    private MetricsService metrics;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
        throws IOException, ServletException
    {
        final HttpServletRequest request = (HttpServletRequest)servletRequest;

        final Timer.Context requestTimer = metrics.timer(T_PREFIX).time();
        String extension = "";
        if(request instanceof SlingHttpServletRequest) {
            extension = ((SlingHttpServletRequest)request).getRequestPathInfo().getExtension();
        }
        final String methodTimerName = T_PREFIX + "." + request.getMethod() + "." + extension;
        final Timer.Context methodTimer = metrics.timer(methodTimerName).time();

        try {
            chain.doFilter(servletRequest, servletResponse);
        } finally {
            requestTimer.stop();
            methodTimer.stop();
        }
    }

}
