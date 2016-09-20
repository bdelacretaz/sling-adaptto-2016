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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.BrandingPlugin;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

/** Add info about this instance to the Felix webconsole.
 *  Assumes that logo etc. is provided by the Sling webconsole
 *  branding bundle at the standard locations, as we only
 *  want to change the page title to show our instance info. */
@Component
@Service(value=BrandingPlugin.class)
@Property(name=Constants.SERVICE_RANKING, intValue=10000)
public class WebConsoleBranding implements BrandingPlugin {

    private String envInfo;
    public static final String PRODUCT = "Apache Sling";
    
    @Activate
    public void activate(ComponentContext ctx) {
        envInfo = U.getEnvInfo(ctx);
    }
    
    @Override
    public String getBrandName() {
        return PRODUCT + " - " + envInfo;
    }

    @Override
    public String getProductName() {
        return getBrandName();
    }

    @Override
    public String getProductURL() {
        return "http://sling.apache.org";
    }

    @Override
    public String getProductImage() {
        return "/res/sling/logo.png";
    }

    @Override
    public String getVendorName() {
        return "";
    }

    @Override
    public String getVendorURL() {
        return "";
    }

    @Override
    public String getVendorImage() {
        return "";
    }

    @Override
    public String getFavIcon() {
        return "/res/sling/favicon.ico";
    }

    @Override
    public String getMainStyleSheet() {
        return "/res/ui/webconsole.css";
    }
}