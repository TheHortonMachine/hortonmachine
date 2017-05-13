package org.jgrasstools.server.jetty.examples;

import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jgrasstools.server.jetty.EmbeddedJspServer;

public class WithRealmAuthServer extends EmbeddedJspServer {

    private static String authRealm;

    public WithRealmAuthServer( Integer port, String webappFolder ) {
        super(port, webappFolder);
    }

    @Override
    protected void configureWebAppContext( WebAppContext webapp ) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void configureServletHandler( ServletHandler servletHandler ) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void doPreStart() {
        HashLoginService loginService = new HashLoginService("AuthRealm");
        loginService.setConfig(authRealm);
        _server.addBean(loginService);
    }

    public static void main( String[] args ) throws Exception {

        String webFolder = "/home/hydrologis/development/jgrasstools-git/server/src/main/webapp";
        authRealm = "/home/hydrologis/development/jgrasstools-git/server/src/main/resources/authrealm.txt";
        WithRealmAuthServer jspServer = new WithRealmAuthServer(null, webFolder);
        jspServer.start();
    }
}
