package org.jgrasstools.server.geopaparazzi.servlets;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jgrasstools.gears.libs.logging.JGTLogger;

public class ProjectUploadHandler extends AbstractHandler {

    private File gpapProjectsFolder;

    public ProjectUploadHandler( File gpapProjectsFolder ) {
        this.gpapProjectsFolder = gpapProjectsFolder;
    }

    public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response )
            throws IOException, ServletException {
        try {

            getProjectFileFromDevice(baseRequest, response, request);

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
                    response.setHeader("Content-disposition", "attachment; filename=" + projectFileName);
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

    private void getProjectFileFromDevice( Request baseRequest, HttpServletResponse response, HttpServletRequest request ) {
        String msg = "";
        try {
            PrintWriter outWriter = response.getWriter();
            Map<String, String[]> parms = baseRequest.getParameterMap();
            String[] nameParams = parms.get("name");
            if (nameParams != null && nameParams.length == 1) {
                String projectFileName = nameParams[0];
                File file = new File(gpapProjectsFolder, projectFileName);
                if (file.exists()) {
                    msg = "File already exists on the server: " + projectFileName;
                    outWriter.write(msg);
                } else {
                    ServletInputStream inputStream = request.getInputStream();
                    Files.copy(inputStream, file.toPath());
                    msg = "Uploaded file: " + projectFileName;

                    // ...in case of multiparts
                    // StringBuilder sb = new StringBuilder();
                    // Collection<Part> parts = request.getParts();
                    // for( Part part : parts ) {
                    // long fileSize = part.getSize();
                    // String fileName = part.getSubmittedFileName();
                    // if (fileSize == 0 && (fileName == null || fileName.isEmpty())) {
                    // continue; // Ignore part, if not a file.
                    // }
                    // Files.copy(part.getInputStream(), file.toPath());
                    // sb.append(",").append(file.getName());
                    // }
                    // if (sb.length() > 0) {
                    // msg = "Uploaded files: " + sb.substring(1);
                    // }
                }
            }

            outWriter.write(msg);
        } catch (Exception e1) {
            JGTLogger.logError(this, msg, e1);
        }
    }

}