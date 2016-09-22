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

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.commons.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.jackrabbit.commons.JcrUtils.getOrAddNode;

class RenditionGenerator implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(RenditionGenerator.class);
    public static final String PATH_ORIGINAL = "jcr:content/renditions/original";
    private final JackrabbitSession session;
    private final String path;
    private final boolean useOakResource;
    private File workDir;

    public RenditionGenerator(Session session, String path) throws RepositoryException {
        this.session = (JackrabbitSession) session;
        this.path = checkNotNull(path);
        this.useOakResource = hasOakResourceNodeTypeDefined(session);
    }

    public void generate() throws RepositoryException, IOException {
        Stopwatch w = Stopwatch.createStarted();
        Node node = session.getNode(path);
        generate(node);
        session.save();
        FileUtils.deleteQuietly(workDir);
        log.info("Generated renditions for [{}] in {}", path, w);
    }

    @Override
    public void close() throws IOException {
        FileUtils.deleteQuietly(workDir);
        session.logout();
    }

    private void generate(Node node) throws RepositoryException, IOException {
        Node originalNode = node.getNode(PATH_ORIGINAL);

        if (!originalNode.hasProperty("jcr:content/jcr:mimeType")) {
            log.warn("Ignoring [{}] as it does not defined the jcr:mimeType", originalNode.getPath());
            return;
        }

        workDir = Files.createTempDir();
        InputStream is = JcrUtils.readFile(originalNode);

        File original = copy(is, "original");
        Map<String, File> renditions = generate(original);
        copy(renditions, node.getNode("jcr:content/renditions"));
    }

    private void copy(Map<String, File> renditions, Node renditionsNode) throws RepositoryException, IOException {
        String renditionMimeType = "image/png";
        for (Map.Entry<String, File> e : renditions.entrySet()) {
            String nodeName = e.getKey();
            try (InputStream is = new BufferedInputStream(FileUtils.openInputStream(e.getValue()))) {
                putFile(renditionsNode, nodeName, renditionMimeType, is);
            }
        }
    }

    private Map<String, File> generate(File original) {
        Map<String, File> renditions = new HashMap<>();
        //TODO Generate via ffmpeg
        renditions.put("1280x1200", original);
        return renditions;
    }

    private File copy(InputStream is, String fileName) throws IOException {
        File file = new File(workDir, fileName);
        OutputStream os = FileUtils.openOutputStream(file);
        try {
            IOUtils.copyLarge(is, os);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
        return file;
    }

    private Node putFile(
            Node parent, String name, String mime,
            InputStream data) throws RepositoryException {
        Binary binary = parent.getSession().getValueFactory().createBinary(data);
        try {
            Node file = getOrAddNode(parent, name, NodeType.NT_FILE);
            //oak:Resource is non referenceable hence less overhead
            String nodeType = useOakResource ? "oak:Resource" : NodeType.NT_RESOURCE;
            Node content = getOrAddNode(file, Node.JCR_CONTENT, nodeType);

            content.setProperty(Property.JCR_MIMETYPE, mime);
            String[] parameters = mime.split(";");
            for (int i = 1; i < parameters.length; i++) {
                int equals = parameters[i].indexOf('=');
                if (equals != -1) {
                    String parameter = parameters[i].substring(0, equals);
                    if ("charset".equalsIgnoreCase(parameter.trim())) {
                        content.setProperty(
                                Property.JCR_ENCODING,
                                parameters[i].substring(equals + 1).trim());
                    }
                }
            }

            content.setProperty(Property.JCR_LAST_MODIFIED, Calendar.getInstance());
            content.setProperty(Property.JCR_DATA, binary);
            return file;
        } finally {
            binary.dispose();
        }
    }

    private static boolean hasOakResourceNodeTypeDefined(Session session) throws RepositoryException {
        return session.getWorkspace().getNodeTypeManager().hasNodeType("oak:Resource");
    }
}
