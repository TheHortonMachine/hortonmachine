package org.jgrasstools.server.geopaparazzi.servlets;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class ProjectDownloadHandler extends AbstractHandler {

    private File gpapProjectsFolder;

    public ProjectDownloadHandler( File gpapProjectsFolder ) {
        this.gpapProjectsFolder = gpapProjectsFolder;
    }

    public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response )
            throws IOException, ServletException {
        try {
            String projectFileName = "";
            FileInputStream inputStream = null;
            ServletOutputStream outputStream = null;
            try {
                Map<String, String[]> parms = request.getParameterMap();
                String[] idParams = parms.get("id");
                if (idParams != null && idParams.length == 1) {
                    projectFileName = idParams[0];
                    inputStream = new FileInputStream(new File(gpapProjectsFolder, projectFileName));
                    String mimeType = "application/octet-stream";
                    response.setContentType(mimeType);
                    response.setHeader("Content-disposition", "attachment; filename="+ projectFileName);
                    outputStream = response.getOutputStream();
                    IOUtils.copy(inputStream, outputStream);
                }
            } finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        baseRequest.setHandled(true);
    }
}