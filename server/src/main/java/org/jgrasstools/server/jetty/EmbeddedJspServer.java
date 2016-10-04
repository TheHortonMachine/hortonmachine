package org.jgrasstools.server.jetty;

import org.apache.jasper.servlet.JspServlet;
import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jgrasstools.server.jetty.utils.DisabledLogging;

public abstract class EmbeddedJspServer {

    protected Server _server;
    protected WebAppContext webapp;

    public EmbeddedJspServer( Integer port, String webappFolder ) {
        if (port == null) {
            port = 8080;
        }
        _server = new Server(port);
        try {

            ServletHandler servletHandler = new ServletHandler();
            _server.setHandler(servletHandler);

            configureServletHandler(servletHandler);

            // add jsp servlet mappings
            String[] jspExtensions = {"*.jsp", "*.jspf", "*.jspx", "*.xsp", "*.JSP", "*.JSPF", "*.JSPX", "*.XSP"};
            Class<JspServlet> jspServletClass = org.apache.jasper.servlet.JspServlet.class;
            for( String jspExt : jspExtensions ) {
                ServletHolder servletHolder = servletHandler.addServletWithMapping(jspServletClass, jspExt);
                servletHolder.setInitParameter("logVerbosityLevel", "INFO");
                servletHolder.setInitParameter("fork", "false");
                servletHolder.setInitParameter("keepgenerated", "true");
                servletHolder.setInitParameter("keepgenerated", "true");
                // <load-on-startup>0</load-on-startup>
            }

            webapp = getWebAppContext(_server, webappFolder);
            configureWebAppContext(webapp);
            _server.setHandler(webapp);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Configure the webapp context after the basic jsp configurations have been done.
     * 
     * <p>Note that setContextPath has been set to "/" by default.</p> 
     * 
     * @param webapp the webapp context.
     */
    protected abstract void configureWebAppContext( WebAppContext webapp );

    /**
     * Configure the servletHandler if necessary.
     * 
     * @param servletHandler the handler to configure.
     */
    protected abstract void configureServletHandler( ServletHandler servletHandler );

    /**
     * Run code before server start.
     */
    protected abstract void doPreStart();

    public void start() throws Exception {
        doPreStart();
        _server.start();
        _server.dumpStdErr();
        // _server.join();
    }

    public void stop() throws Exception {
        _server.stop();
    }

    private WebAppContext getWebAppContext( Server server, String webFolder ) {
        // 2. Creating the WebAppContext for the created content
        WebAppContext ctx = new WebAppContext();
        ctx.setResourceBase(webFolder);
        ctx.setContextPath("/");

        // 3. Including the JSTL jars for the webapp.
        ctx.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*/[^/]*jstl.*\\.jar$");

        // 4. Enabling the Annotation based configuration
        org.eclipse.jetty.webapp.Configuration.ClassList classlist = org.eclipse.jetty.webapp.Configuration.ClassList
                .setServerDefault(server);
        classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration", "org.eclipse.jetty.plus.webapp.EnvConfiguration",
                "org.eclipse.jetty.plus.webapp.PlusConfiguration");
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration");
        return ctx;

    }

    public static void main( String[] args ) throws Exception {
        BasicConfigurator.configure();
        org.eclipse.jetty.util.log.Log.setLog(new DisabledLogging());

        String webFolder = "/home/hydrologis/development/jgrasstools-git/server/src/main/webapp";
        EmbeddedJspServer jspServer = new EmbeddedJspServer(null, webFolder){
            @Override
            protected void configureWebAppContext( WebAppContext webapp ) {
            }

            @Override
            protected void configureServletHandler( ServletHandler servletHandler ) {
            }

            @Override
            protected void doPreStart() {
            }
        };
        jspServer.start();
    }

}
