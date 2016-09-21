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
package ch.x42.sling.at16.processorselector.impl;

import java.io.Reader;

import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Output a processor role selector based on .processor scripts */
public class ProcessorSelectorScriptEngine extends AbstractSlingScriptEngine {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    ProcessorSelectorScriptEngine(ScriptEngineFactory factory) {
        super(factory);
    }
    
    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        try {
            final String processorRole = IOUtils.toString(reader).trim();
            final SlingHttpServletRequest request = getContext(context, "request", SlingHttpServletRequest.class); 
            final SlingHttpServletResponse response = getContext(context, "response", SlingHttpServletResponse.class);
            U.logAndRequestProgress(request, log, "Got role {0} from processor script", processorRole);
            U.outputRole(response, processorRole);
        } catch (Exception e) {
            final ScriptException se = new ScriptException("Processor role resolution setup or execution failed");
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
}