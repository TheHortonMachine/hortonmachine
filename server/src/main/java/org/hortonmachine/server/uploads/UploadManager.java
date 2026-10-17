/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.server.uploads;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.WorkspacePaths;

/**
 * The files the users upload, in their own folder of the workspace, reusable by all their jobs.
 *
 * <p>The folder is all there is: no database keeps the uploads. Every name given by a browser or found
 * in a zip file is cleaned of the characters that are not allowed in file names, and <code>..</code> is
 * refused, so nothing is written out of the folder of the user. A file is first written in the temporary
 * folder of the workspace and then moved in place, so a broken upload leaves nothing behind.
 * A file with the name of an existing one replaces it.
 *
 * <p>The zip files are unpacked without creating links, whatever their entries say, and with limits on the
 * size and number of the unpacked files, against the zip files built to fill the disk.
 *
 * <p>The quota is checked before every write; two uploads of the same user at the same time can go
 * over it by the size of one of them.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class UploadManager {
    /** The maximum number of entries of a zip file. */
    public static final int MAX_ZIP_ENTRIES = 10000;
    /** The maximum length of a file or folder name. */
    public static final int MAX_NAME_LENGTH = 200;
    /** The folders of zip files that hold no data, skipped when unpacking. */
    private static final String MACOS_FOLDER = "__MACOSX";

    private final WorkspacePaths paths;
    // changed by the settings page while running
    private volatile long quotaBytes;
    private volatile long maxUnpackedBytes;

    /**
     * A file or folder of the uploads of a user.
     */
    public static class Upload {
        /** The path in the uploads of the user, with / as separator. */
        public final String path;
        public final boolean isFolder;
        /** The size of the file, 0 for a folder. */
        public final long size;

        public Upload( String path, boolean isFolder, long size ) {
            this.path = path;
            this.isFolder = isFolder;
            this.size = size;
        }

        /**
         * @return the path as given to the modules, as <code>uploads/dtm/dtm.tif</code>.
         */
        public String getWorkspacePath() {
            return WorkspacePaths.UPLOADS + "/" + path;
        }
    }

    /**
     * @param paths the folders of the workspace.
     * @param quotaBytes the maximum size of the uploads of each user, 0 for no limit.
     * @param maxUnpackedBytes the maximum size of the files unpacked from a zip file, besides the quota.
     */
    public UploadManager( WorkspacePaths paths, long quotaBytes, long maxUnpackedBytes ) {
        this.paths = paths;
        this.quotaBytes = quotaBytes;
        this.maxUnpackedBytes = maxUnpackedBytes;
    }

    /**
     * @return the maximum size of the uploads of each user, 0 for no limit.
     */
    public long getQuotaBytes() {
        return quotaBytes;
    }

    /**
     * Change the limits, for the next uploads.
     *
     * @param quotaBytes the maximum size of the uploads of each user, 0 for no limit.
     * @param maxUnpackedBytes the maximum size of the files unpacked from a zip file, besides the quota.
     */
    public void setLimits( long quotaBytes, long maxUnpackedBytes ) {
        this.quotaBytes = quotaBytes;
        this.maxUnpackedBytes = maxUnpackedBytes;
    }

    /**
     * Clean a file or folder name given by a user: the characters not allowed in file names on some
     * systems become <code>_</code>.
     *
     * @param name the name, without separators.
     * @return the clean name.
     * @throws IllegalArgumentException if the name is empty, <code>.</code> or <code>..</code>.
     */
    public static String cleanName( String name ) {
        String clean = name == null ? "" : name.trim().replaceAll("[\\x00-\\x1f\\x7f<>:\"|?*\\\\/]", "_");
        if (clean.isEmpty() || clean.equals(".") || clean.equals("..")) {
            throw new IllegalArgumentException("Not a valid file name: " + name);
        }
        if (clean.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("The name is longer than " + MAX_NAME_LENGTH + " characters: " + name);
        }
        return clean;
    }

    /**
     * Clean a relative path given by a user, as <code>dtm/2024</code>: every name is cleaned with
     * {@link #cleanName(String)} and the empty ones and <code>.</code> are dropped.
     *
     * @param path the path, with / or \ as separators, can be <code>null</code> or empty.
     * @return the clean path with / as separator, empty for none.
     * @throws IllegalArgumentException if the path contains <code>..</code>.
     */
    public static String cleanPath( String path ) {
        if (path == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for( String segment : path.replace('\\', '/').split("/") ) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty() || trimmed.equals(".")) {
                continue;
            }
            if (trimmed.equals("..")) {
                throw new IllegalArgumentException("The path can't contain '..': " + path);
            }
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(cleanName(trimmed));
        }
        return sb.toString();
    }

    /**
     * Get a file or folder of the uploads of a user.
     *
     * @param userId the user.
     * @param path the path in the uploads, as listed by {@link #list(String)}.
     * @return the file, which may not exist.
     * @throws IllegalArgumentException if the path is not valid or is the uploads folder itself.
     */
    public File getFile( String userId, String path ) {
        String clean = cleanPath(path);
        if (clean.isEmpty() || !clean.equals(path.replace('\\', '/'))) {
            throw new IllegalArgumentException("Not a valid path: " + path);
        }
        Path root = paths.getUploadsFolder(userId).toPath();
        Path file = root.resolve(clean).normalize();
        if (!file.startsWith(root) || file.equals(root)) {
            throw new IllegalArgumentException("Not a valid path: " + path);
        }
        return file.toFile();
    }

    /**
     * @return the files and folders of the uploads of a user, sorted by path.
     */
    public List<Upload> list( String userId ) throws IOException {
        List<Upload> uploads = new ArrayList<>();
        Path root = paths.getUploadsFolder(userId).toPath();
        if (!Files.isDirectory(root)) {
            return uploads;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(path -> !path.equals(root)).sorted().forEach(path -> {
                String relative = root.relativize(path).toString().replace('\\', '/');
                boolean isFolder = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
                long size = 0;
                if (!isFolder) {
                    try {
                        size = Files.size(path);
                    } catch (IOException e) {
                        // removed meanwhile
                    }
                }
                uploads.add(new Upload(relative, isFolder, size));
            });
        }
        return uploads;
    }

    /**
     * @return the bytes used by the uploads of a user.
     */
    public long getUsedBytes( String userId ) throws IOException {
        return WorkspacePaths.folderSize(paths.getUploadsFolder(userId).toPath());
    }

    /**
     * @return the bytes the user can still upload, {@link Long#MAX_VALUE} without quota.
     */
    private long availableBytes( String userId ) throws IOException {
        if (quotaBytes <= 0) {
            return Long.MAX_VALUE;
        }
        return Math.max(0, quotaBytes - getUsedBytes(userId));
    }

    /**
     * Save an uploaded file.
     *
     * @param userId the user.
     * @param folder the folder in the uploads, as given by the user, can be empty.
     * @param fileName the name given by the browser; only the last name of a path is used.
     * @param size the size of the file.
     * @param content the content of the file, not closed.
     * @return the path of the file in the uploads.
     * @throws IllegalArgumentException if the names are not valid or the quota would be exceeded.
     */
    public String save( String userId, String folder, String fileName, long size, InputStream content ) throws IOException {
        String name = fileName == null ? "" : fileName.replace('\\', '/');
        name = cleanName(name.substring(name.lastIndexOf('/') + 1));
        String cleanFolder = cleanPath(folder);
        String path = cleanFolder.isEmpty() ? name : cleanFolder + "/" + name;
        File target = getFile(userId, path);
        if (target.isDirectory()) {
            throw new IllegalArgumentException("There is a folder with the name of the file: " + path);
        }

        // the file replaced frees its size
        long available = availableBytes(userId);
        if (available != Long.MAX_VALUE && target.isFile()) {
            available += target.length();
        }
        if (size > available) {
            throw new IllegalArgumentException(quotaMessage(path));
        }

        Path tmp = newTmpFile();
        try {
            try (OutputStream output = Files.newOutputStream(tmp)) {
                copy(content, output, available, quotaMessage(path));
            }
            moveInPlace(tmp, target.toPath());
        } finally {
            Files.deleteIfExists(tmp);
        }
        return path;
    }

    private String quotaMessage( String path ) {
        return "The uploads would be larger than the limit of " + (quotaBytes / (1024 * 1024)) + " MB: " + path
                + ". Delete some files first.";
    }

    private Path newTmpFile() throws IOException {
        File tmpFolder = paths.getTmpFolder();
        if (!tmpFolder.isDirectory() && !tmpFolder.mkdirs()) {
            throw new IOException("The temporary folder can't be created: " + tmpFolder);
        }
        return tmpFolder.toPath().resolve("upload-" + UUID.randomUUID());
    }

    /**
     * Copy at most a number of bytes.
     *
     * @return the bytes copied.
     * @throws IllegalArgumentException with the given message, if there are more.
     */
    private static long copy( InputStream input, OutputStream output, long maxBytes, String tooLargeMessage ) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        int read;
        while( (read = input.read(buffer)) >= 0 ) {
            total += read;
            if (total > maxBytes) {
                throw new IllegalArgumentException(tooLargeMessage);
            }
            output.write(buffer, 0, read);
        }
        return total;
    }

    private static void moveInPlace( Path source, Path target ) throws IOException {
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Unpack an uploaded zip file. Either all its files are unpacked, or none.
     *
     * @param userId the user.
     * @param folder the folder in the uploads to unpack into, as given by the user, can be empty.
     * @param zipName the name of the zip file, for the messages.
     * @param content the content of the zip file, not closed.
     * @return the paths of the unpacked files in the uploads.
     * @throws IllegalArgumentException if the file is not a zip file, or one of its names is not valid, or it is
     *         larger than allowed.
     */
    public List<String> unzip( String userId, String folder, String zipName, InputStream content ) throws IOException {
        String cleanFolder = cleanPath(folder);
        // the quota is checked at the end, when the files replaced are known
        long maxBytes = quotaBytes > 0 ? Math.min(quotaBytes, maxUnpackedBytes) : maxUnpackedBytes;
        String tooLargeMessage = quotaBytes > 0 && quotaBytes < maxUnpackedBytes ? quotaMessage(zipName)
                : "The unpacked files are larger than " + (maxUnpackedBytes / (1024 * 1024)) + " MB: " + zipName;
        Path tmp = newTmpFile();
        Files.createDirectories(tmp);
        List<String> files = new ArrayList<>();
        try {
            long total = 0;
            int entries = 0;
            try (ZipInputStream zip = new ZipInputStream(content)) {
                ZipEntry entry;
                while( (entry = zip.getNextEntry()) != null ) {
                    if (++entries > MAX_ZIP_ENTRIES) {
                        throw new IllegalArgumentException("The zip file has more than " + MAX_ZIP_ENTRIES + " entries: " + zipName);
                    }
                    String entryPath = cleanPath(entry.getName());
                    if (entryPath.isEmpty() || entryPath.equals(MACOS_FOLDER) || entryPath.startsWith(MACOS_FOLDER + "/")) {
                        continue;
                    }
                    Path target = tmp.resolve(entryPath).normalize();
                    if (!target.startsWith(tmp)) {
                        throw new IllegalArgumentException("Not a valid name in the zip file: " + entry.getName());
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                        continue;
                    }
                    if (Files.isDirectory(target)) {
                        throw new IllegalArgumentException("The zip file has a file and a folder with the same name: " + entryPath);
                    }
                    Files.createDirectories(target.getParent());
                    // a plain file, also if the entry is a link: links are never created
                    try (OutputStream output = Files.newOutputStream(target)) {
                        total += copy(zip, output, maxBytes - total, tooLargeMessage);
                    }
                    files.add(cleanFolder.isEmpty() ? entryPath : cleanFolder + "/" + entryPath);
                }
            }
            if (entries == 0) {
                throw new IllegalArgumentException("Not a zip file, or an empty one: " + zipName);
            }

            // the files replaced free their size
            if (quotaBytes > 0) {
                long replaced = 0;
                for( String file : files ) {
                    File existing = getFile(userId, file);
                    replaced += existing.isFile() ? existing.length() : 0;
                }
                if (total - replaced > availableBytes(userId)) {
                    throw new IllegalArgumentException(quotaMessage(zipName));
                }
            }
            for( String file : files ) {
                if (getFile(userId, file).isDirectory()) {
                    throw new IllegalArgumentException("There is a folder with the name of the file: " + file);
                }
            }
            Path base = cleanFolder.isEmpty() ? paths.getUploadsFolder(userId).toPath()
                    : getFile(userId, cleanFolder).toPath();
            try (Stream<Path> walk = Files.walk(tmp)) {
                for( Path path : (Iterable<Path>) walk.sorted()::iterator ) {
                    Path target = base.resolve(tmp.relativize(path).toString());
                    if (Files.isDirectory(path)) {
                        if (Files.isRegularFile(target)) {
                            throw new IllegalArgumentException("There is a file with the name of the folder: " + target.getFileName());
                        }
                        Files.createDirectories(target);
                    } else {
                        moveInPlace(path, target);
                    }
                }
            }
        } finally {
            FileUtilities.deleteFileOrDir(tmp.toFile());
        }
        return files;
    }

    /**
     * Delete a file or a folder with all its files.
     *
     * @return <code>false</code> if it doesn't exist.
     * @throws IllegalArgumentException if the path is not valid.
     */
    public boolean delete( String userId, String path ) throws IOException {
        File file = getFile(userId, path);
        if (!Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        // the result of deleteFileOrDir is false for the folders it deleted
        FileUtilities.deleteFileOrDir(file);
        if (file.exists()) {
            throw new IOException("The file can't be deleted: " + path);
        }
        return true;
    }

    /**
     * Delete all the uploads of a user, as when the user is deleted.
     */
    public void deleteAll( String userId ) throws IOException {
        File folder = paths.getUploadsFolder(userId);
        FileUtilities.deleteFileOrDir(folder);
        if (folder.exists()) {
            throw new IOException("The uploads can't be deleted: " + folder);
        }
    }

    /**
     * Remove the leftovers of the uploads interrupted by a stop of the server.
     */
    public void cleanTmpFolder() throws IOException {
        File[] files = paths.getTmpFolder().listFiles();
        if (files != null) {
            for( File file : files ) {
                FileUtilities.deleteFileOrDir(file);
            }
        }
    }
}
