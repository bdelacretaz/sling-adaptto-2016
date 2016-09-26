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
package ch.x42.sling.at16.worker.internal;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Throwables;
import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.metrics.MetricsService;
import org.apache.sling.commons.metrics.Timer;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.JobCallback;
import org.apache.sling.jobs.JobConsumer;
import org.apache.sling.jobs.JobUpdate;
import org.apache.sling.jobs.JobUpdateListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Component(
        service = JobConsumer.class,
        property = {
                "job.types=assets/generate/renditions"
        }
)
public class RenditionJobConsumer implements JobConsumer {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private SlingRepository repository;
    @Reference
    private ThreadPoolManager threadPoolManager;
    private ThreadPool threadPool;

    @Reference
    private MetricsService metrics;
    private Timer jobTimer;

    @Activate
    private void activate(){
        threadPool = threadPoolManager.get("rendition-generator");
        jobTimer = metrics.timer("rendition-generator");
    }

    @Deactivate
    private void deactivate(){
        threadPoolManager.release(threadPool);
    }

    @Nonnull
    @Override
    public void execute(@Nonnull final Job initialState, @Nonnull final JobUpdateListener listener,
                        @Nonnull final JobCallback callback) {
        final String imagePath = (String) initialState.getProperties().get("path");
        checkNotNull(imagePath, "Node path not specified via 'path' property");

        //Due to eventual consistent nature of repository its possible that newly
        //added path is not visible on this cluster node
        //So if not present exception would be thrown and job would be reattempted
        String originalPath = imagePath + "/" + RenditionGenerator.PATH_ORIGINAL;
        checkArgument(pathExists(originalPath), "No node found at path [%s]", originalPath);
        initialState.setState(Job.JobState.QUEUED);

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                initialState.setState(Job.JobState.ACTIVE);
                RenditionGenerator generator = null;
                Timer.Context context = jobTimer.time();
                try {
                    generator = new RenditionGenerator(newSession(), imagePath);
                    generator.generate();
                    initialState.setState(Job.JobState.SUCCEEDED);
                    callback.callback(initialState);
                } catch (Exception e) {
                    listener.update(initialState.newJobUpdateBuilder()
                            .command(JobUpdate.JobUpdateCommand.UPDATE_JOB)
                            .put("error", Throwables.getStackTraceAsString(e))
                            .build()
                    );
                    initialState.setState(Job.JobState.ERROR);
                    log.warn("Error occurred while processing path [{}]", imagePath, e);
                } finally {
                    context.stop();
                    IOUtils.closeQuietly(generator);
                }
            }
        });

    }

    private boolean pathExists(String path){
        boolean nodeExists = false;
        try {
            Session session = newSession();
            nodeExists = session.nodeExists(path);
            session.logout();
        } catch (RepositoryException e) {
            log.warn("Error occurred while accessing repository", e);
        }
        return nodeExists;
    }

    private Session newSession() throws RepositoryException {
        //TODO Use service user
        return repository.loginAdministrative(null);
    }
}
