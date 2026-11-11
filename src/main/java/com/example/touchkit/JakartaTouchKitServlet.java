package com.example.touchkit;

import java.io.IOException;
import java.net.URL;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;

import com.vaadin.addon.touchkit.settings.TouchKitSettings;
import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.VaadinServlet;

/**
 * Drop-in replacement for {@code com.vaadin.addon.touchkit.server.TouchKitServlet}
 * for Jakarta EE 9+/10 (Servlet 5/6) containers such as Payara 6.
 * <p>
 * The original TouchKit 4.1.0 TouchKitServlet is compiled against
 * {@code javax.servlet} and therefore can't be loaded by a Jakarta container.
 * It is the only class in TouchKit that depends on the Servlet API, so this
 * class is a 1:1 port of it with {@code javax.servlet} replaced by
 * {@code jakarta.servlet}. It extends the {@code VaadinServlet} from
 * {@code vaadin-server-mpr-jakarta}.
 * <p>
 * Do NOT use/extend the original TouchKitServlet anywhere in the application.
 */
public class JakartaTouchKitServlet extends VaadinServlet {

    private TouchKitSettings touchKitSettings;

    @Override
    protected void servletInitialized() throws ServletException {
        super.servletInitialized();
        touchKitSettings = new TouchKitSettings(getService());
        getService().addSessionInitListener(
                e -> e.getSession().addBootstrapListener(APPCACHE_SHIM));
    }

    /**
     * Not related to Jakarta: the TouchKit client side
     * (CacheManifestStatusIndicator) calls {@code window.applicationCache},
     * the HTML5 AppCache API that current Chrome / Android WebView, Firefox
     * and Safari have removed. Without it the UI never renders (blank page,
     * "Cannot read properties of undefined (reading 'addEventListener')").
     * This injects a no-op stub reporting status UNCACHED before the
     * widgetset loads.
     */
    private static final String APPCACHE_SHIM_JS = "if(!window.applicationCache){"
            + "window.applicationCache={status:0,"
            + "addEventListener:function(){},removeEventListener:function(){},"
            + "update:function(){},swapCache:function(){},abort:function(){}};}";

    private static final BootstrapListener APPCACHE_SHIM = new BootstrapListener() {
        @Override
        public void modifyBootstrapFragment(BootstrapFragmentResponse response) {
        }

        @Override
        public void modifyBootstrapPage(BootstrapPageResponse response) {
            Element head = response.getDocument().head();
            head.prependElement("script").attr("type", "text/javascript")
                    .appendChild(new DataNode(APPCACHE_SHIM_JS));
        }
    };

    /**
     * @return the TouchKitSettings bound to the VaadinService of this servlet
     */
    public TouchKitSettings getTouchKitSettings() {
        return touchKitSettings;
    }

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            if (pathInfo.startsWith("/VAADIN/themes/touchkit/styles.css")) {
                serveDummyFile(response, "max-age=1000000");
                return;
            } else if (pathInfo.startsWith("/PING")) {
                serveDummyFile(response,
                        "no-store, no-cache, max-age=0, must-revalidate");
                return;
            }
        }
        super.service(request, response);
    }

    @Override
    protected void writeStaticResourceResponse(HttpServletRequest request,
            HttpServletResponse response, URL resourceUrl) throws IOException {
        String file = resourceUrl.getFile();
        if (file.endsWith(".manifest")) {
            response.setContentType("text/cache-manifest");
            response.setHeader("Cache-Control", "max-age=1, must-revalidate");
        } else if (file.endsWith(".otf")) {
            response.setContentType("font/opentype");
        } else if (file.endsWith(".ttf")) {
            response.setContentType("font/ttf");
        } else if (file.endsWith(".woff")) {
            response.setContentType("application/font-woff");
        }
        super.writeStaticResourceResponse(request, response, resourceUrl);
    }

    private void serveDummyFile(HttpServletResponse response,
            String cacheControl) throws IOException {
        response.setContentType("text/css");
        response.setHeader("Cache-Control", cacheControl);
        response.getOutputStream().write("\n".getBytes());
    }
}
