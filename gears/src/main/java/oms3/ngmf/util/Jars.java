/*
 * $Id: Jars.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3.ngmf.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Jar file bundling.
 *
 * @author od
 */
public class Jars {

    public static final String MANIFEST_NAME_OMS_DSL = "OMS-DSL";
    public static final String MANIFEST_NAME_OMS_VERSION = "OMS-Version";


    /**
     * Create one jar file for an OMS project.
     *
     * @param targetJar the name of the target jar
     * @param sim the simulation file
     * @throws IOException produced by failed or interrupted I/O operations
     */
    public static void oneJar(String targetJar, File sim) throws IOException {
        System.out.println("Creating: " + targetJar + " ...");

        Set<String> dircache = new HashSet<>();

        String oms_home = System.getProperty("oms.home");
        String oms_version = System.getProperty("oms.version");

        //jar
        Manifest mf = createManifest(sim.getName(), oms_version);
        try (JarOutputStream target = new JarOutputStream(new FileOutputStream(targetJar), mf)) {
            target.setLevel(9);

            // add the simulation file to be executed.
            add(sim, "META-INF/" + sim.getName(), target);

            // add the project class files
            File dist = new File(System.getProperty("oms.prj"), "dist");
            if (dist.exists()) {
                for (File file : dist.listFiles()) {
                    add(file, target, dircache);
                }
            }

            File lib = new File(System.getProperty("oms.prj"), "lib");
            if (lib.exists()) {
                for (File file : lib.listFiles()) {
                    add(file, target, dircache);
                }
            }

            File[] fi = new File(oms_home).listFiles(new WildcardFileFilter(new String[]{"oms-all*.jar", "groovy-all-*.jar"}));
            for (int i = 0; i < fi.length; i++) {
                add(fi[i], target, dircache);
            }

            // add the core libs, change this later to directory pickup
//           add(new File(oms_home, "oms-all.jar"), target, dircache);
//            add(new File(oms_home, "groovy-all-2.3.9.jar"), target, dircache);
            //      add(new File(oms_home, "jfreechart-1.0.12.jar"), target);
            //      add(new File(oms_home, "jcommon-1.0.15.jar"), target);
        }
        System.out.println("\nSuccessfully created: " + targetJar);
    }


    private static Manifest createManifest(String sim, String version) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "oms3.EmbeddedCLI");
        manifest.getMainAttributes().putValue("Created-At", new Date().toString());
        manifest.getMainAttributes().putValue("Created-By", System.getProperty("java.runtime.version") + " (" + System.getProperty("java.vm.vendor") + ")");
        manifest.getMainAttributes().putValue("Built-By", System.getProperty("user.name"));
        manifest.getMainAttributes().putValue(MANIFEST_NAME_OMS_DSL, sim);
        manifest.getMainAttributes().putValue(MANIFEST_NAME_OMS_VERSION, version);
//        manifest.getMainAttributes().put(Attributes.Name.SEALED, "true");
        return manifest;
    }


    private static void add(File source, String name, JarOutputStream target) throws IOException {
        System.out.println(" adding " + source);
        JarEntry entry = new JarEntry(name);
        entry.setTime(source.lastModified());
        target.putNextEntry(entry);
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source))) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                target.write(buffer, 0, read);
            }
            target.closeEntry();
        }
    }


    private static void add(File source, JarOutputStream target, Set<String> dir) throws IOException {
        System.out.println(" adding " + source);
        if (source.getName().endsWith(".jar")) {
            copyJar(source, target, dir);
            return;
        }
        if (source.isDirectory()) {
            String name = source.getPath().replace("\\", "/");
            if (!name.isEmpty()) {
                if (!name.endsWith("/")) {
                    name += "/";
                }
                if (!dir.contains(name)) {
                    JarEntry entry = new JarEntry(name);
                    entry.setTime(source.lastModified());
                    target.putNextEntry(entry);
                    target.closeEntry();
                    dir.add(name);
                }
            }
            for (File nestedFile : source.listFiles()) {
                add(nestedFile, target, dir);
            }
            return;
        }
        JarEntry entry = new JarEntry(source.getPath().replace("\\", "/"));
        entry.setTime(source.lastModified());
        target.putNextEntry(entry);
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source))) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                target.write(buffer, 0, read);
            }
            target.closeEntry();
        }
    }


    /**
     * Copy the Jar file
     *
     * @param zip the zip file
     * @param target the jar target
     * @param dir directory
     * @throws IOException produced by failed or interrupted I/O operations
     */
    private static void copyJar(File zip, JarOutputStream target, Set<String> dir) throws IOException {
        byte[] buffer = new byte[4096];
        ZipFile archive = new ZipFile(zip);
        Enumeration<? extends ZipEntry> e = archive.entries();
        while (e.hasMoreElements()) {
            ZipEntry zentry = e.nextElement();
            String name = zentry.getName();
//            System.out.println("Entry " + name);
            // skip that
            if (name.contains("MANIFEST.MF")) {
                continue;
            }
            if (zentry.isDirectory()) {
                if (!name.isEmpty()) {
                    if (!name.endsWith("/")) {
                        name += "/";
                    }
                    if (!dir.contains(name)) {
                        JarEntry jentry = new JarEntry(name);
                        jentry.setTime(zentry.getTime());
                        target.putNextEntry(jentry);
                        target.closeEntry();
                        dir.add(name);
                    }
                }
                continue;
            }
            JarEntry entry = new JarEntry(name);
            entry.setTime(zentry.getTime());
            target.putNextEntry(entry);
            try (BufferedInputStream in = new BufferedInputStream(archive.getInputStream(zentry))) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    target.write(buffer, 0, read);
                }
            }
            target.closeEntry();
        }
    }
}
