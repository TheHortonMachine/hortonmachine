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
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hortonmachine.Version;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.db.ServerDb;
import org.hortonmachine.server.files.FileOrigin;
import org.hortonmachine.server.users.User;
import org.hortonmachine.server.web.Html;
import org.hortonmachine.server.web.MediaTypes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Keeps the items of the STAC catalog as the files of the areas of the data are.
 *
 * <p>The collections are the folders: the files of <code>data/shared/flanginec</code> and of
 * <code>data/&lt;username&gt;/flanginec</code>, at any depth, are the items of the collection <code>flanginec</code>,
 * each user seeing the shared ones and the own ones; the files directly in an area are not in the catalog.
 *
 * <p>Each file is an item, with its sidecars (the parts of a shapefile, the georeferencing, statistics and styles of a
 * raster) as further assets, and with its {@link FileOrigin origin}: uploaded, written by a job, with its module and
 * parameters, or put there from outside, and the origins of the files it replaced. The index follows the files:
 * <ul>
 * <li>the changes made by the server, uploads, deletions and outputs of the jobs, are synchronized at once by
 *     {@link #syncArea(String)} and {@link #syncFolder(String)};</li>
 * <li>the data folder, changed also from outside, is watched: a file is read once it stopped changing for
 *     {@value #STABLE_MILLIS} ms, as when copied;</li>
 * <li>everything is compared with the files at the start, when the watcher lost events and every
 *     {@link #setRescanMinutes(int) few minutes}, since network folders send no events;</li>
 * <li>{@link #current(StacRecord)} checks an item against its files when it is asked alone.</li>
 * </ul>
 * Only the files whose size, time or origin changed are read again.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class StacIndexer implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(StacIndexer.class.getName());

    /** The start of the ids of the items of the shared area and of the areas of the users. */
    public static final String SHARED_PREFIX = WorkspacePaths.SHARED;
    public static final String USER_PREFIX = "user";
    /** The extension of the items written by a job. */
    public static final String PROCESSING_EXTENSION = "https://stac-extensions.github.io/processing/v1.2.0/schema.json";
    /** The milliseconds a changed file must stay unchanged before it is read. */
    public static final long STABLE_MILLIS = 3000;

    private final ServerDb db;
    private final WorkspacePaths paths;
    private final Path dataFolder;
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
     * An area of the data, whose folders are collections.
     */
    private static class Root {
        /** {@link WorkspacePaths#SHARED} or the id of the user. */
        final String area;
        /** The user of the area, <code>null</code> for the shared one. */
        final String ownerId;
        /** The name of the user of the area, <code>null</code> for the shared one. */
        final String ownerName;
        final Path folder;
        /** The path in the workspace of the folder, as data/&lt;username&gt;. */
        final String workspacePath;

        Root( String area, String ownerId, String ownerName, Path folder ) {
            this.area = area;
            this.ownerId = ownerId;
            this.ownerName = ownerName;
            this.folder = folder;
            this.workspacePath = WorkspacePaths.DATA + "/" + area;
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

    private Root areaRoot( String area ) {
        if (area.equals(WorkspacePaths.SHARED)) {
            return new Root(area, null, null, dataFolder.resolve(area));
        }
        // the area of a user is named by its username, which owns the items
        return new Root(area, area, area, dataFolder.resolve(area));
    }

    /**
     * @return the name of a user, its id if it doesn't exist anymore.
     */
    private String userName( String userId ) {
        try {
            User user = db.getUser(userId);
            if (user != null) {
                return user.username;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "The user " + userId + " can't be read.", e);
        }
        return userId;
    }

    /**
     * @param ownArea the area of the user, its username.
     * @return the collections a user sees: the folders of the shared area and of the area of the user, of all the areas
     *         for the administrators; by name.
     */
    public List<String> listCollections( String ownArea, boolean isAdmin ) {
        Set<String> collections = new TreeSet<>();
        File[] areas = dataFolder.toFile().listFiles(File::isDirectory);
        if (areas != null) {
            for( File area : areas ) {
                if (WorkspacePaths.isArea(area.getName()) && WorkspacePaths.canRead(area.getName(), ownArea, isAdmin)) {
                    addFolders(collections, area);
                }
            }
        }
        return new ArrayList<>(collections);
    }

    private static void addFolders( Set<String> names, File parent ) {
        File[] folders = parent.listFiles(File::isDirectory);
        if (folders != null) {
            for( File folder : folders ) {
                if (!isSkipped(folder.getName())) {
                    names.add(folder.getName());
                }
            }
        }
    }

    /**
     * @return the root of a file, or <code>null</code> if it is not in an area of the data.
     */
    private Root rootOf( Path file ) {
        Path path = file.toAbsolutePath().normalize();
        if (path.startsWith(dataFolder) && !path.equals(dataFolder)) {
            // the files directly in the data are in no area; a missing folder is the area of a deleted user
            String area = dataFolder.relativize(path).getName(0).toString();
            if (WorkspacePaths.isArea(area) && !Files.isRegularFile(dataFolder.resolve(area))) {
                return areaRoot(area);
            }
        }
        return null;
    }

    private Object lockOf( Root root ) {
        return locks.computeIfAbsent(root.area, key -> new Object());
    }

    /**
     * @param relative the path of a file in its root, as flanginec/dem/dtm.tif.
     * @return its collection, the first folder, or <code>null</code> for the files directly in the root.
     */
    static String collectionOf( String relative ) {
        int slash = relative.indexOf('/');
        return slash > 0 ? relative.substring(0, slash) : null;
    }

    /**
     * @param relative the path of a file in its root, as flanginec/dem/dtm.tif.
     * @return the id of its item, unique in its collection: the area and the path in the collection, with ~ for /, as
     *         shared~dem~dtm.tif or user~jane~dem~dtm.tif.
     */
    private static String itemId( Root root, String relative ) {
        String inCollection = relative.substring(relative.indexOf('/') + 1).replace("~", "~~").replace("/", "~");
        return (root.ownerId == null ? SHARED_PREFIX : USER_PREFIX + "~" + root.ownerName.replace("~", "~~")) + "~" + inCollection;
    }

    /**
     * @return <code>true</code> for the files that are never items: hidden and temporary.
     */
    static boolean isSkipped( String name ) {
        String lower = name.toLowerCase();
        return name.startsWith(".") || lower.endsWith(".tmp") || lower.endsWith(".part") || lower.endsWith("~");
    }

    /**
     * Bring the index of an area up to date, at once, as after an upload or a deletion.
     *
     * @param area {@link WorkspacePaths#SHARED} or the id of a user.
     */
    public void syncArea( String area ) {
        Root root = areaRoot(area);
        reconcile(root, root.folder);
    }

    /**
     * Bring the index of a folder of the data up to date, at once, as after a job wrote its outputs there.
     *
     * @param workspacePath the folder, as path in the workspace: data/&lt;area&gt;/....
     */
    public void syncFolder( String workspacePath ) {
        Path folder = paths.getWorkspace().toPath().resolve(workspacePath).toAbsolutePath().normalize();
        Root root = rootOf(folder);
        if (root != null) {
            reconcile(root, folder);
        }
    }

    /**
     * Compare all the files with the index.
     */
    void reconcileAll() {
        try {
            Set<String> areas = new HashSet<>();
            File[] areaFolders = dataFolder.toFile().listFiles(File::isDirectory);
            if (areaFolders != null) {
                for( File areaFolder : areaFolders ) {
                    if (WorkspacePaths.isArea(areaFolder.getName())) {
                        areas.add(areaFolder.getName());
                    }
                }
            }
            // also the areas of the users deleted meanwhile
            areas.addAll(db.listStacOwners());
            for( String area : areas ) {
                syncArea(area);
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
                for( StacRecord record : db.listStacItemsUnder(root.workspacePath(under)) ) {
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
                            // the files directly in the data or in the uploads of a user are in no collection
                            if (files != null && !folder.equals(root.folder)) {
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
                    if (!isSkipped(file.getName()) && StacFiles.mainOf(file) == null && files != null
                            && collectionOf(root.relative(under)) != null) {
                        seen.add(path);
                        index(root, file, StacFiles.sidecarsOf(file, files), existing.get(path));
                    }
                }
                for( StacRecord record : existing.values() ) {
                    if (!seen.contains(record.path)) {
                        db.deleteStacItem(record.path);
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
        String path = root.workspacePath(file.toPath());
        FileOrigin origin = db.getFileOrigin(path);
        // a new origin changes the item also if the file kept its time, as when moved in place
        long changed = origin != null ? Math.max(modified, origin.created) : modified;
        if (existing != null && existing.size == size && existing.modified == changed) {
            return existing;
        }
        String relative = root.relative(file.toPath());
        ObjectNode item = fileItem(root, file, relative, sidecars, modified, origin);
        double[] bbox = null;
        if (item.has("bbox")) {
            ArrayNode array = (ArrayNode) item.get("bbox");
            bbox = new double[]{array.get(0).asDouble(), array.get(1).asDouble(), array.get(2).asDouble(), array.get(3).asDouble()};
        }
        StacRecord record = new StacRecord(itemId(root, relative), collectionOf(relative), root.ownerId, path, size, changed,
                bbox, modified, modified, mapper.writeValueAsString(item));
        db.putStacItem(record);
        // the sidecars were items while their file was missing
        for( File sidecar : sidecars ) {
            db.deleteStacItem(root.workspacePath(sidecar.toPath()));
        }
        return record;
    }

    private ObjectNode fileItem( Root root, File file, String relative, List<File> sidecars, long modified, FileOrigin origin ) {
        ObjectNode item = mapper.createObjectNode();
        item.put("type", "Feature");
        item.put("stac_version", StacItems.STAC_VERSION);
        ArrayNode extensions = item.putArray("stac_extensions");
        StacItems.EXTENSIONS.forEach(extensions::add);
        boolean isProcessed = origin != null && origin.origin.equals(FileOrigin.PROCESSED);
        if (isProcessed) {
            extensions.add(PROCESSING_EXTENSION);
        }
        item.put("id", itemId(root, relative));
        item.put("collection", collectionOf(relative));

        StacFiles.Description description = StacFiles.describe(file, file.getName());
        StacItems.putGeometry(item, description.footprint);
        String userPath = root.workspacePath + "/" + relative;
        String time = Instant.ofEpochMilli(modified).toString();
        ObjectNode properties = item.putObject("properties");
        // the name of the file, with its folders in the collection: the id carries the area to be unique
        properties.put("title", relative.substring(relative.indexOf('/') + 1));
        properties.put("datetime", time);
        properties.put("created", Instant.ofEpochMilli(origin != null ? origin.created : modified).toString());
        properties.put("updated", time);
        // the path to give to the modules
        properties.put("hm:path", userPath);
        properties.put("hm:area", root.area);
        if (root.ownerName != null) {
            properties.put("hm:owner", root.ownerName);
        }
        putOrigin(properties, origin);
        if (isProcessed) {
            properties.putObject("processing:software").put("HortonMachine", Version.getVersion());
            properties.put("processing:lineage", "Written by the module " + origin.module + " of HortonMachine.");
        }
        if (origin != null && !origin.history.isEmpty()) {
            ArrayNode history = properties.putArray("hm:history");
            for( FileOrigin previous : origin.history ) {
                putOrigin(history.addObject(), previous);
            }
        }
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
     * Put the origin of a file in the properties of its item, or in an entry of its history: hm:origin, hm:user (the
     * name of who uploaded it or ran the job), hm:created, the job with its module and parameters, hm:replaced.
     *
     * @param origin the origin, <code>null</code> for the files put in the data from outside.
     */
    private void putOrigin( ObjectNode properties, FileOrigin origin ) {
        properties.put("hm:origin", origin != null ? origin.origin : FileOrigin.EXTERNAL);
        if (origin == null) {
            return;
        }
        if (origin.userId != null) {
            properties.put("hm:user", userName(origin.userId));
        }
        properties.put("hm:created", Instant.ofEpochMilli(origin.created).toString());
        if (origin.jobId != null) {
            properties.put("hm:job", origin.jobId);
            properties.put("hm:module", origin.module);
            properties.set("hm:parameters", mapper.valueToTree(origin.parameters));
        }
        if (origin.replaced != null) {
            properties.put("hm:replaced", Instant.ofEpochMilli(origin.replaced).toString());
        }
    }

    /**
     * Check an item asked alone against its files.
     *
     * @return the item as the files are now, or <code>null</code> if its files are gone.
     */
    public StacRecord current( StacRecord record ) throws Exception {
        Path file = paths.getWorkspace().toPath().resolve(record.path).toAbsolutePath().normalize();
        Root root = rootOf(file);
        if (root == null) {
            return record;
        }
        reconcile(root, file);
        return db.getStacItem(record.collection, record.id);
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
        if (path.toAbsolutePath().normalize().equals(root.folder)) {
            syncArea(root.area);
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
