package ch.x42.sling.at16.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.metrics.MetricsService;
import org.apache.sling.commons.metrics.Timer;
import org.apache.sling.settings.SlingSettingsService;

@SlingServlet(
  resourceTypes="at16/root",
  methods="GET",
  extensions="txt"
)
@SuppressWarnings("serial")
/** Servlet that counts the child resources of the current resource */
public class RootGetServlet extends SlingSafeMethodsServlet {

    final static String ID = "id";

    @Reference
    private MetricsService metrics;

    @Reference
    private SlingSettingsService settings;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException 
    {
        final Timer.Context t = metrics.timer(getClass().getSimpleName()).time();

        try {
            // Output the count of our child nodes
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/plain");
            final PrintWriter w = response.getWriter();
            w.print(request.getResource().getPath());
            w.print(" has ");
            w.print(countChildren(request.getResource(), ID));
            w.print(" descendant nodes with an '");
            w.print(ID);
            w.print("' property. Sling instance ID is ");
            w.print(settings.getSlingId());
            w.println(".");
            w.flush();
        } finally {
            t.stop();
        }
    }

    private int countChildren(Resource r, String expectedProperty) {
        int count = 0;
        for(Resource child : r.getResourceResolver().getChildren(r)) {
            final ValueMap m = child.adaptTo(ValueMap.class);
            if(m.containsKey(ID)) {
                count++;
            }
            count += countChildren(child, expectedProperty);
        }
        return count;
    }
}
