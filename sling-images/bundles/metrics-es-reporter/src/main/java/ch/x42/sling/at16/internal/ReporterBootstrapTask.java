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
package ch.x42.sling.at16.internal;

import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks for ES server to be ready and then initializes the template
 * for metrics. This is in addition to default template bootstrapped
 * by reporter to customize processing of custom fields
 */
class ReporterBootstrapTask implements Closeable{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Callable<Boolean> callback;
    private final ESReporterConfig config;
    private final Map<String, Object> additionalFields;
    private final Timer timer;
    private int timeout = 1000;

    ReporterBootstrapTask(ESReporterConfig config, Callable<Boolean> callback,
                          Map<String, Object> additionalFields) {
        this.config = config;
        this.callback = callback;
        this.additionalFields = additionalFields;
        this.timer = new Timer("ES Config Bootstrapper", true);
        //TODO Do exponential backoff
        timer.scheduleAtFixedRate(new BootstrapTask(), 0, TimeUnit.SECONDS.toMillis(5));
    }

    @Override
    public void close() throws IOException {
        timer.cancel();
    }

    private class BootstrapTask extends TimerTask {
        @Override
        public void run() {
            if (checkForIndexTemplate()){
                try {
                    callback.call();
                    log.info("Initialized Elasticsearch with metrics index config");
                } catch (Exception e){
                    log.error("Error occurred while initializing reporter", e);
                }
                cancel();
            } else {
                log.info("Not able to bootstrap config. Would try again");
            }
        }
    }

    //Taken from ElasticsearchReporter

    /**
     * Open a new HttpUrlConnection, in case it fails it tries for the next host in the configured list
     */
    private HttpURLConnection openConnection(String uri, String method) {
        for (String host : config.servers()) {
            try {
                URL templateUrl = new URL("http://" + host  + uri);
                HttpURLConnection connection = ( HttpURLConnection ) templateUrl.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(timeout);
                connection.setUseCaches(false);
                if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
                    connection.setDoOutput(true);
                }
                connection.connect();

                return connection;
            } catch (IOException e) {
                log.error("Error connecting to {}: {}", host, e.getMessage());
                log.debug("Error connecting to {}: {}", host, e);
            }
        }
        return null;
    }

    /**
     * This index template is automatically applied to all indices which start with the index name
     * The index template simply configures the name not to be analyzed
     */
    private boolean checkForIndexTemplate() {
        try {
            HttpURLConnection connection = openConnection( "/_template/sling_metrics_template", "HEAD");
            if (connection == null) {
                log.error("Could not connect to any configured elasticsearch instances: {}", Arrays.asList(config.servers()));
                return false;
            }
            connection.disconnect();

            boolean isTemplateMissing = connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND;

            // nothing there, lets create it
            if (isTemplateMissing) {
                log.debug("No metrics template found in elasticsearch. Adding...");
                HttpURLConnection putTemplateConnection = openConnection( "/_template/sling_metrics_template", "PUT");
                if(putTemplateConnection == null) {
                    log.error("Error adding metrics template to elasticsearch");
                    return false;
                }

                JsonGenerator json = new JsonFactory().createGenerator(putTemplateConnection.getOutputStream());
                json.writeStartObject();
                json.writeStringField("template", config.indexName() + "*");
                json.writeObjectFieldStart("mappings");

                json.writeObjectFieldStart("_default_");
                json.writeObjectFieldStart("_all");
                json.writeBooleanField("enabled", false);
                json.writeEndObject();
                json.writeObjectFieldStart("properties");

                for (String fieldName : additionalFields.keySet()) {
                    json.writeObjectFieldStart(fieldName);
                    json.writeObjectField("type", "string");
                    json.writeObjectField("index", "not_analyzed");
                    json.writeEndObject();
                }

                json.writeEndObject();
                json.writeEndObject();

                json.writeEndObject();
                json.writeEndObject();
                json.flush();

                putTemplateConnection.disconnect();
                if (putTemplateConnection.getResponseCode() != 200) {
                    log.error("Error adding metrics template to elasticsearch: {}/{}" + putTemplateConnection.getResponseCode(), putTemplateConnection.getResponseMessage());
                }
            }
            return true;
        } catch (IOException e) {
            log.error("Error when checking/adding metrics template to elasticsearch", e);
        }
        return false;
    }

}
