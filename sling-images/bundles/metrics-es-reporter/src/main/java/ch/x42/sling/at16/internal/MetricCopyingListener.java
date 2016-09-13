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

import javax.annotation.Nullable;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;

/**
 * A MetricRegistryListener which registers itself with the provided
 * registry and copies the metrics to the host registry
 */
class MetricCopyingListener implements MetricRegistryListener{
    private final MetricRegistry compositeRegistry;
    private final MetricRegistry sourceRegistry;
    private final String registryName;

    MetricCopyingListener(MetricRegistry compositeRegistry, MetricRegistry sourceRegistry,
                          @Nullable String registryName) {
        this.compositeRegistry = compositeRegistry;
        this.sourceRegistry = sourceRegistry;
        this.registryName = registryName;
    }

    void register(){
        sourceRegistry.addListener(this);
    }

    void unregister(){
        sourceRegistry.removeListener(this);
    }

    @Override
    public void onGaugeAdded(String name, Gauge<?> gauge) {
        register(name, gauge);
    }

    @Override
    public void onGaugeRemoved(String name) {
        remove(name);
    }

    @Override
    public void onCounterAdded(String name, Counter counter) {
        register(name, counter);
    }

    @Override
    public void onCounterRemoved(String name) {
        remove(name);
    }

    @Override
    public void onHistogramAdded(String name, Histogram histogram) {
        register(name, histogram);
    }

    @Override
    public void onHistogramRemoved(String name) {
        remove(name);
    }

    @Override
    public void onMeterAdded(String name, Meter meter) {
        register(name, meter);
    }

    @Override
    public void onMeterRemoved(String name) {
        remove(name);
    }

    @Override
    public void onTimerAdded(String name, Timer timer) {
        register(name, timer);
    }

    @Override
    public void onTimerRemoved(String name) {
        remove(name);
    }

    private void register(String name, Metric gauge) {
        compositeRegistry.register(decorate(name), gauge);
    }

    private void remove(String name) {
        compositeRegistry.remove(decorate(name));
    }

    private String decorate(String name) {
        if (registryName != null){
            name = registryName + ":" + name;
        }
        return name;
    }
}
