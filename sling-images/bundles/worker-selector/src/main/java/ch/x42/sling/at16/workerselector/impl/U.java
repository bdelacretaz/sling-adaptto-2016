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
import java.text.MessageFormat;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;

/** General utilities */ 
class U {
    static void logAndRequestProgress(SlingHttpServletRequest request, Logger log, String format, Object ... arguments) {
        final String msg = MessageFormat.format(format, arguments);
        request.getRequestProgressTracker().log(msg);
        log.info(msg);
    }
    
    static void outputRole(HttpServletResponse response, String role) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(role);
        response.getWriter().write("\n");
        response.getWriter().flush();
    }
}