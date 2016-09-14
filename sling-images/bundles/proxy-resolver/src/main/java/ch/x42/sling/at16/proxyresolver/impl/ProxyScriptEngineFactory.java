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

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;

import ch.x42.sling.at16.proxyresolver.WorkerProxy;

/** Proxy the current request based on the role defined by our proxy "script" */
@Component
@Service(ScriptEngineFactory.class)
public class ProxyScriptEngineFactory extends AbstractScriptEngineFactory {
    public static final List<String> EXTENSIONS = new ArrayList<String>();
    
    static {
        EXTENSIONS.add("proxy");
    }
    
    @Reference
    private WorkerProxy workerProxy;
    
    @Override
    public String getLanguageName() {
        return getClass().getName();
    }

    @Override
    public String getLanguageVersion() {
        return "0.1";
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new ProxyScriptEngine(this, workerProxy);
    }

    @Override
    public List<String> getExtensions() {
        return EXTENSIONS;
    }
}