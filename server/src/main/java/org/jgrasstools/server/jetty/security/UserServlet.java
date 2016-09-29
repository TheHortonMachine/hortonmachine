package org.jgrasstools.server.jetty.security;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        doPost(request, response);
    }

    public void doPost( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        Principal userPrincipal = request.getUserPrincipal();
        String name = userPrincipal.getName();
        out.println("<h1>Hello user: " + name + "</h1>");

    }
}