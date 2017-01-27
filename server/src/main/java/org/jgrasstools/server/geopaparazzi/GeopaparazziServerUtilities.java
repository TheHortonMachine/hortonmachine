package org.jgrasstools.server.geopaparazzi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class GeopaparazziServerUtilities {

    public static String handleProjectUpload( File gpapProjectsFolder, HttpServletResponse response,
            HttpServletRequest request ) throws Exception {
        String projectFileName = "";
        String msg = "";
        PrintWriter outWriter = response.getWriter();
        Map<String, String[]> parms = request.getParameterMap();
        String[] nameParams = parms.get("name");
        if (nameParams != null && nameParams.length == 1) {
            projectFileName = nameParams[0];
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
        return projectFileName;
    }

    public static String handleProjectDownload( File gpapProjectsFolder, HttpServletRequest request,
            HttpServletResponse response ) throws Exception {
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

        return projectFileName;

    }

//    public static void handleProjectUpload( File gpapProjectsFolder, HttpServletRequest request, HttpServletResponse response )
//            throws Exception {
//        return getProjectFileFromDevice(gpapProjectsFolder, response, request);
//
//        // String projectFileName = "";
//        // FileInputStream inputStream = null;
//        // ServletOutputStream outputStream = null;
//        // try {
//        // Map<String, String[]> parms = request.getParameterMap();
//        // String[] idParams = parms.get("id");
//        // if (idParams != null && idParams.length == 1) {
//        // projectFileName = idParams[0];
//        // inputStream = new FileInputStream(new File(gpapProjectsFolder, projectFileName));
//        // String mimeType = "application/octet-stream";
//        // response.setContentType(mimeType);
//        // response.setHeader("Content-disposition", "attachment; filename=" + projectFileName);
//        // outputStream = response.getOutputStream();
//        // IOUtils.copy(inputStream, outputStream);
//        // }
//        // } finally {
//        // IOUtils.closeQuietly(inputStream);
//        // IOUtils.closeQuietly(outputStream);
//        // }
//    }

    public static void handleProjectList( HttpServletResponse response, File gpapProjectsFolder ) throws IOException {
        response.setContentType("text/json");
        PrintWriter out = response.getWriter();
        String projectsList = GeopaparazziWorkspaceUtilities.loadProjectsList(gpapProjectsFolder);
        out.write(projectsList);
    }
}
