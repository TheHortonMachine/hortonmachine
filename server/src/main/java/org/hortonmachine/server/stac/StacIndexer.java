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
package org.hortonmachine.server.stac;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.db.ServerDb;
import org.hortonmachine.server.web.Html;
import org.hortonmachine.server.web.MediaTypes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Keeps the items of the shared data and of the uploads of the STAC catalog as the files are.
 *
 * <p>Each file is an item, with its sidecars (the parts of a shapefile, the georeferencing, statistics and styles of a
 * raster) as further assets. The index follows the files:
 * <ul>
 * <li>the uploads, changed only by the server, are synchronized at once by {@link #syncUploads(String)};</li>
 * <li>the data folder, changed from outside, is watched: a file is read once it stopped changing for
 *     {@value #STABLE_MILLIS} ms, as when copied;</li>
 * <li>everything is compared with the files at the start, when the watcher lost events and every
 *     {@link #setRescanMinutes(int) few minutes}, since network folders send no events;</li>
 * <li>{@link #current(StacRecord)} checks an item against its files when it is asked alone.</li>
 * </ul>
 * Only the files whose size or time changed are read again.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class StacIndexer implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(StacIndexer.class.getName());

    public static final String DATA_COLLECTION = "data";
    public static final String UPLOADS_COLLECTION = "uploads";
    /** The milliseconds a changed file must stay unchanged before it is read. */
    public static final long STABLE_MILLIS = 3000;

    private final ServerDb db;
    private final WorkspacePaths paths;
    private final Path dataFolder;
    private final Path uploadsFolder;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "hm-stac-indexer");
        thread.setDaemon(true);
        return thread;
    });
    /** The paths changed, with the time of the last change. */
    private final Map<Path, Long> pending = new ConcurrentHashMap<>();
    private final Map<WatchKey, Path> watchedFolders = new ConcurrentHashMap<>();
    /** A lock for the data and one for the uploads of each user, so that an upload doesn't wait for the data. */
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    private WatchService watcher;
    private Thread watchThread;
    private ScheduledFuture< ? > rescan;
    private volatile boolean closed;

    /**
     * The folder whose files are the items of a collection.
     */
    private static class Root {
        final String collection;
        /** The user of the uploads, <code>null</code> for the data. */
        final String ownerId;
        final Path folder;
        /** The path in the workspace of the folder, as uploads/&lt;user id&gt;. */
        final String workspacePath;
        /** The path of the folder as the users give it to the modules, as uploads. */
        final String userPath;

        Root( String collection, String ownerId, Path folder, String workspacePath, String userPath ) {
            this.collection = collection;
            this.ownerId = ownerId;
            this.folder = folder;
            this.workspacePath = workspacePath;
            this.userPath = userPath;
        }

        String relative( Path file ) {
            return folder.relativize(file).toString().replace('\\', '/');
        }

        String workspacePath( Path file ) {
            String relative = relative(file);
            return relative.isEmpty() ? workspacePath : workspacePath + "/" + relative;
        }
    }

    public StacIndexer( ServerDb db, WorkspacePaths paths ) {
        this.db = db;
        this.paths = paths;
        this.dataFolder = paths.getDataFolder().toPath().toAbsolutePath().normalize();
        this.uploadsFolder = paths.getAllUploadsFolder().toPath().toAbsolutePath().normalize();
    }

    /**
     * Start watching the folders and compare the index with the files, in the background.
     *
     * @param rescanMinutes the minutes between the comparisons of all the files, 0 for none.
     */
    public void start( int rescanMinutes ) throws IOException {
        watcher = FileSystems.getDefault().newWatchService();
        // watched before the comparison, so that no change is lost
        registerTree(dataFolder);
        registerTree(uploadsFolder);
        watchThread = new Thread(this::watch, "hm-stac-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
        scheduler.execute(this::reconcileAll);
        scheduler.scheduleWithFixedDelay(this::processPending, 1, 1, TimeUnit.SECONDS);
        setRescanMinutes(rescanMinutes);
    }

    /**
     * @param minutes the minutes between the comparisons of all the files with the index, 0 for none.
     */
    public synchronized void setRescanMinutes( int minutes ) {
        if (rescan != null) {
            rescan.cancel(false);
            rescan = null;
        }
        if (minutes > 0 && !closed) {
            rescan = scheduler.scheduleWithFixedDelay(this::reconcileAll, minutes, minutes, TimeUnit.MINUTES);
        }
    }

    private Root dataRoot() {
        return new Root(DATA_COLLECTION, null, dataFolder, WorkspacePaths.DATA, WorkspacePaths.DATA);
    }

    private Root uploadsRoot( String userId ) {
        return new Root(UPLOADS_COLLECTION, userId, uploadsFolder.resolve(userId), WorkspacePaths.UPLOADS + "/" + userId,
                WorkspacePaths.UPLOADS);
    }

    /**
     * @return the root of a file, or <code>null</code> if it is not in the data or in the uploads of a user.
     */
    private Root rootOf( Path file ) {
        Path path = file.toAbsolutePath().normalize();
        if (path.startsWith(dataFolder)) {
            return dataRoot();
        }
        if (path.startsWith(uploadsFolder) && !path.equals(uploadsFolder)) {
            return uploadsRoot(uploadsFolder.relativize(path).getName(0).toString());
        }
        return null;
    }

    private Object lockOf( Root root ) {
        return locks.computeIfAbsent(root.collection + "/" + root.ownerId, key -> new Object());
    }

    /**
     * @return the id of the item of a file: its path, with ~ for /, prefixed by the user for the uploads.
     */
    static String itemId( String ownerId, String relative ) {
        String id = relative.replace("~", "~~").replace("/", "~");
        return ownerId != null ? ownerId + "~" + id : id;
    }

    /**
     * @return <code>true</code> for the files that are never items: hidden and temporary.
     */
    static boolean isSkipped( String name ) {
        String lower = name.toLowerCase();
        return name.startsWith(".") || lower.endsWith(".tmp") || lower.endsWith(".part") || lower.endsWith("~");
    }

    /**
     * Bring the index of the uploads of a user up to date, at once, as after an upload or a deletion.
     */
    public void syncUploads( String userId ) {
        Root root = uploadsRoot(userId);
        reconcile(root, root.folder);
    }

    /**
     * Compare all the files with the index.
     */
    void reconcileAll() {
        try {
            reconcile(dataRoot(), dataFolder);
            Set<String> users = new HashSet<>();
            File[] userFolders = uploadsFolder.toFile().listFiles(File::isDirectory);
            if (userFolders != null) {
                for( File userFolder : userFolders ) {
                    users.add(userFolder.getName());
                }
            }
            // also the uploads of the users deleted meanwhile
            for( StacRecord record : db.listStacItems(UPLOADS_COLLECTION) ) {
                users.add(record.ownerId);
            }
            for( String userId : users ) {
                syncUploads(userId);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "The catalog can't be compared with the files.", e);
        }
    }

    /**
     * Compare the files of a folder, or a single file, with the items in the index under its path.
     */
    private void reconcile( Root root, Path under ) {
        synchronized (lockOf(root)) {
            try {
                Map<String, StacRecord> existing = new HashMap<>();
                for( StacRecord record : db.listStacItemsUnder(root.collection, root.workspacePath(under)) ) {
                    existing.put(record.path, record);
                }
                Set<String> seen = new HashSet<>();
                if (Files.isDirectory(under)) {
                    // the administrators can link folders in the data, never the users in the uploads
                    Set<FileVisitOption> options = root.ownerId == null ? EnumSet.of(FileVisitOption.FOLLOW_LINKS)
                            : EnumSet.noneOf(FileVisitOption.class);
                    Files.walkFileTree(under, options, Integer.MAX_VALUE, new SimpleFileVisitor<Path>(){
                        @Override
                        public FileVisitResult preVisitDirectory( Path folder, BasicFileAttributes attributes ) {
                            if (!folder.equals(under) && isSkipped(folder.getFileName().toString())) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            File[] files = folder.toFile().listFiles();
                            if (files != null) {
                                for( File file : files ) {
                                    if (file.isFile() && !isSkipped(file.getName()) && StacFiles.mainOf(file) == null) {
                                        String path = root.workspacePath(file.toPath());
                                        seen.add(path);
                                        try {
                                            index(root, file, StacFiles.sidecarsOf(file, files), existing.get(path));
                                        } catch (Exception e) {
                                            // the others are still indexed
                                            LOGGER.log(Level.WARNING, "The file " + file + " can't be indexed.", e);
                                        }
                                    }
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed( Path file, IOException e ) {
                            // a loop of links or a file gone meanwhile
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else if (Files.isRegularFile(under)) {
                    File file = under.toFile();
                    File[] files = file.getParentFile().listFiles();
                    String path = root.workspacePath(under);
                    if (!isSkipped(file.getName()) && StacFiles.mainOf(file) == null && files != null) {
                        seen.add(path);
                        index(root, file, StacFiles.sidecarsOf(file, files), existing.get(path));
                    }
                }
                for( StacRecord record : existing.values() ) {
                    if (!seen.contains(record.path)) {
                        db.deleteStacItem(record.id);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "The catalog of " + under + " can't be updated.", e);
            }
        }
    }

    /**
     * Put the item of a file in the index, if it changed since the existing one.
     *
     * @return the item.
     */
    private StacRecord index( Root root, File file, List<File> sidecars, StacRecord existing ) throws Exception {
        long size = file.length();
        long modified = file.lastModified();
        for( File sidecar : sidecars ) {
            size += sidecar.length();
            modified = Math.max(modified, sidecar.lastModified());
        }
        if (existing != null && existing.size == size && existing.modified == modified) {
            return existing;
        }
        String relative = root.relative(file.toPath());
        ObjectNode item = fileItem(root, file, relative, sidecars, modified);
        double[] bbox = null;
        if (item.has("bbox")) {
            ArrayNode array = (ArrayNode) item.get("bbox");
            bbox = new double[]{array.get(0).asDouble(), array.get(1).asDouble(), array.get(2).asDouble(), array.get(3).asDouble()};
        }
        StacRecord record = new StacRecord(itemId(root.ownerId, relative), root.collection, root.ownerId,
                root.workspacePath(file.toPath()), size, modified, bbox, modified, modified, mapper.writeValueAsString(item));
        db.putStacItem(record);
        // the sidecars were items while their file was missing
        for( File sidecar : sidecars ) {
            db.deleteStacItem(itemId(root.ownerId, root.relative(sidecar.toPath())));
        }
        return record;
    }

    private ObjectNode fileItem( Root root, File file, String relative, List<File> sidecars, long modified ) {
        ObjectNode item = mapper.createObjectNode();
        item.put("type", "Feature");
        item.put("stac_version", StacJobs.STAC_VERSION);
        ArrayNode extensions = item.putArray("stac_extensions");
        StacJobs.EXTENSIONS.stream().filter(extension -> !extension.contains("/processing/")).forEach(extensions::add);
        item.put("id", itemId(root.ownerId, relative));
        item.put("collection", root.collection);

        StacFiles.Description description = StacFiles.describe(file, file.getName());
        StacJobs.putGeometry(item, description.footprint);
        String userPath = root.userPath + "/" + relative;
        String time = Instant.ofEpochMilli(modified).toString();
        ObjectNode properties = item.putObject("properties");
        properties.put("title", userPath);
        properties.put("datetime", time);
        properties.put("created", time);
        properties.put("updated", time);
        // the path to give to the modules
        properties.put("hm:path", userPath);
        item.putArray("links");

        ObjectNode assets = item.putObject("assets");
        ObjectNode data = assets.putObject("data");
        data.put("href", "./" + Html.urlPath(file.getName()));
        data.setAll(description.asset);
        String name = file.getName();
        String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        for( File sidecar : sidecars ) {
            String key = sidecar.getName().length() > base.length() + 1 ? sidecar.getName().substring(base.length() + 1)
                    : sidecar.getName();
            if (assets.has(key)) {
                key = sidecar.getName();
            }
            ObjectNode asset = assets.putObject(key);
            asset.put("href", "./" + Html.urlPath(sidecar.getName()));
            asset.put("type", MediaTypes.forFile(sidecar.getName()));
            asset.put("title", sidecar.getName());
            asset.putArray("roles").add("metadata");
            asset.put("file:size", sidecar.length());
        }
        return item;
    }

    /**
     * Check an item asked alone against its files.
     *
     * @return the item as the files are now, or <code>null</code> if its files are gone.
     */
    public StacRecord current( StacRecord record ) throws Exception {
        if (record.collection.startsWith(StacJobs.COLLECTION_PREFIX)) {
            if (!new File(paths.getWorkspace(), record.path).isDirectory()) {
                db.deleteStacItem(record.id);
                return null;
            }
            return record;
        }
        Path file = paths.getWorkspace().toPath().resolve(record.path).toAbsolutePath().normalize();
        Root root = rootOf(file);
        if (root == null) {
            return record;
        }
        reconcile(root, file);
        return db.getStacItem(record.id);
    }

    /**
     * Tell that a file was asked: if it is gone its item goes at once, else it is checked soon.
     */
    public void fileRequested( Path file ) {
        if (rootOf(file) == null) {
            return;
        }
        if (!Files.exists(file)) {
            refresh(file);
        } else {
            pending.put(file.toAbsolutePath().normalize(), System.currentTimeMillis() - STABLE_MILLIS);
        }
    }

    /**
     * Update the index for a changed path: a file, a sidecar or a folder, existing or gone.
     */
    private void refresh( Path path ) {
        Root root = rootOf(path);
        if (root == null) {
            return;
        }
        if (root.ownerId != null && path.toAbsolutePath().normalize().equals(root.folder)) {
            syncUploads(root.ownerId);
            return;
        }
        File file = path.toFile();
        File main = file.isFile() ? StacFiles.mainOf(file) : null;
        reconcile(root, main != null ? main.toPath() : path);
    }

    private void processPending() {
        long now = System.currentTimeMillis();
        for( Map.Entry<Path, Long> entry : new ArrayList<>(pending.entrySet()) ) {
            if (now - entry.getValue() >= STABLE_MILLIS && pending.remove(entry.getKey(), entry.getValue())) {
                refresh(entry.getKey());
            }
        }
    }

    private void registerTree( Path folder ) {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try {
            Files.walkFileTree(folder, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attributes ) throws IOException {
                    watchedFolders.put(dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed( Path file, IOException e ) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "The folder " + folder + " can't be watched: it is compared every few minutes.", e);
        }
    }

    private void watch() {
        while( !closed ) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                return;
            }
            Path folder = watchedFolders.get(key);
            for( WatchEvent< ? > event : key.pollEvents() ) {
                if (event.kind() == OVERFLOW || folder == null) {
                    scheduler.execute(this::reconcileAll);
                    continue;
                }
                Path child = folder.resolve((Path) event.context());
                if (event.kind() == ENTRY_CREATE && Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    registerTree(child);
                }
                pending.put(child, System.currentTimeMillis());
            }
            if (!key.reset()) {
                watchedFolders.remove(key);
            }
        }
    }

    @Override
    public void close() throws Exception {
        closed = true;
        scheduler.shutdownNow();
        if (watcher != null) {
            watcher.close();
        }
    }
}
