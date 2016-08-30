package ch.x42.sling.at16.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.metrics.MetricsService;
import org.apache.sling.commons.metrics.Timer;

@SlingServlet(
  resourceTypes="at16/root",
  methods="POST",
  extensions="txt"
)
@SuppressWarnings("serial")
/** Servlet that adds a resource with a unique path under the
 *  current resource. */
public class RootPostServlet extends SlingAllMethodsServlet {

    @Reference
    private MetricsService metrics;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException 
    {
        final Timer.Context t = metrics.timer(getClass().getSimpleName()).time();

        try {
            final String id = UUID.randomUUID().toString();
            Resource r = request.getResource();
            final Map<String, Object> props = new HashMap<String, Object>();
            r = addChild(r, getClass().getSimpleName(), props);
            for(int i=0 ; i < 4; i+=2) {
                r = addChild(r, id.substring(i,  i+2), props);
            }
            props.put(RootGetServlet.ID, id);
            r = addChild(r, id, props);
            r.getResourceResolver().commit();

            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/plain");
            final PrintWriter w = response.getWriter();
            w.print("Added ");
            w.println(r.getPath());
            w.flush();

        } finally {
            t.stop();
        }
    }

    private Resource addChild(Resource parent, String name, Map<String, Object> props) throws PersistenceException {
        final String fullPath = parent.getPath() + "/" + name;
        Resource child = parent.getResourceResolver().getResource(fullPath);
        if(child == null) {
            child = parent.getResourceResolver().create(parent, name, props);; 
        }
        return child;
    }
}