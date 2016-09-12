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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Metrics Reporting : Elasticsearch Reporter Configuration")
public @interface ESReporterConfig {

    @AttributeDefinition(
            name = "Elasticsearch Servers",
            description = "Elasticsearch server addresses ( e.g. localhost:9200 )"
    )
    String[] servers() default {"localhost:9200"};

    @AttributeDefinition(
            name = "Index Name",
            description = "Elasticsearch index name used to store the metrics data"
    )
    String indexName() default "metrics";

    @AttributeDefinition(
            name = "Metrics Prefix",
            description = "Prefix string to be added to every metric name"
    )
    String metricsPrefix() default "";

    @AttributeDefinition(
            name = "Time interval",
            description = "Time interval in seconds for sending the report"
    )
    int reportingTimeIntervalInSecs() default 60;
}
