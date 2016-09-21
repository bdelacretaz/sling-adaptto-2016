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
package ch.x42.sling.at16.assets.internal;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import com.google.common.collect.ImmutableMap;
import org.apache.jackrabbit.oak.commons.PathUtils;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jobs.JobManager;
import org.apache.sling.jobs.Types;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class AssetChangeListener implements EventListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private SlingRepository repository;

    @Reference
    private JobManager jobManager;

    private Session session;

    @Activate
    private void activate() throws RepositoryException {
        session = newSession();
        session.getWorkspace().getObservationManager().addEventListener(this, Event.NODE_ADDED, "/content",
                true,
                null, new String[]{"nt:file"}, true);
    }

    @Deactivate
    private void deactivate() throws RepositoryException {
        if (session != null) {
            session.getWorkspace().getObservationManager().removeEventListener(this);
            session.logout();
        }
    }

    @Override
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            try {
                Event event = events.nextEvent();
                //Event path is for child node. So parent path is for actual node
                String nodeName = PathUtils.getName(event.getPath());
                if (!nodeName.equals("original")){
                    return;
                }
                //Path like foo.jpg/jcr:content/renditions/original/jcr:content
                String assetPath = PathUtils.getAncestorPath(event.getPath(), 3);
                jobManager.newJobBuilder(
                        Types.jobQueue("org/apache/sling/jobs/assets"),
                        Types.jobType("assets/generate/renditions")) //Defined in RenditionJobConsumer
                        .addProperties(ImmutableMap.<String, Object>of("path", assetPath))
                        .add();
                log.info("Scheduled rendition generation job for path [{}]", assetPath);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Session newSession() throws RepositoryException {
        //TODO Use service user
        return repository.loginAdministrative(null);
    }


}
