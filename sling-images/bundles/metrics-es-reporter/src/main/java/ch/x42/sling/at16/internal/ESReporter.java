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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;
import org.apache.sling.settings.SlingSettingsService;
import org.elasticsearch.metrics.ElasticsearchReporter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = ESReporterConfig.class)
public class ESReporter {
    /**
     * Service property name which indicates the name of registry.
     * Can be null
     */
    private static final String PROP_REGISTRY_NAME = "name";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final MetricRegistry compositeRegistry = new MetricRegistry();
    private final Map<MetricRegistry, MetricCopyingListener> listeners = new IdentityHashMap<>();
    private ElasticsearchReporter reporter;
    private ReporterBootstrapTask bootstrapTask;

    @Reference
    private SlingSettingsService settingsService;

    @Activate
    private void activate(ESReporterConfig config) throws IOException {
        Map<String, Object> additionalFields = getAdditionalFields();
        Callable<Boolean> callback = createCallback(config, additionalFields);
        bootstrapTask = new ReporterBootstrapTask(config, callback, additionalFields);
    }

    private Callable<Boolean> createCallback(final ESReporterConfig config, final Map<String, Object> additionalFields){
        return new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                //TODO Possible support for metric filter
                reporter = ElasticsearchReporter.forRegistry(compositeRegistry)
                        .hosts(config.servers())
                        .index(config.indexName())
                        .additionalFields(additionalFields)
                        .prefixedWith(config.metricsPrefix())
                        .build();
                reporter.start(config.reportingTimeIntervalInSecs(), TimeUnit.SECONDS);
                return true;
            }
        };
    }

    @Deactivate
    private void deactivate() throws IOException {
        if (bootstrapTask != null) {
            bootstrapTask.close();
        }
        unregisterListeners();
        if (reporter != null) {
            reporter.close();
            log.info("Elasticsearch report shutdown");
        }
    }

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC
    )
    protected void bindMetricRegistry(MetricRegistry registry, Map<String, Object> props) {
        MetricCopyingListener listener = new MetricCopyingListener(compositeRegistry, registry, getName(props));
        synchronized (listeners){
            listeners.put(registry, listener);
        }
        listener.register();
    }

    protected void unbindMetricRegistry(MetricRegistry registry) {
        MetricCopyingListener listener;
        synchronized (listeners){
            listener = listeners.remove(registry);
        }
        if (listener != null) {
            listener.unregister();
        }
    }

    private void unregisterListeners() {
        List<MetricCopyingListener> listenerInstances;
        synchronized (listeners) {
            listenerInstances = new ArrayList<>(listeners.values());
            listeners.clear();
        }
        for (MetricCopyingListener l : listenerInstances){
            l.unregister();
        }
    }

    private Map<String, Object> getAdditionalFields() {
        //TODO Possibly add other props like name from DiscoveryService
        //TODO See if dependency on Sling API can be made optional
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("slingId", settingsService.getSlingId());
        return additionalFields;
    }

    private static String getName(Map<String, Object> props) {
        return (String) props.get(PROP_REGISTRY_NAME);
    }
}
