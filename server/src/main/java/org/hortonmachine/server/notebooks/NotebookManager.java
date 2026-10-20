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
package org.hortonmachine.server.notebooks;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * The notebooks of a folder and their open sessions.
 *
 * <p>The kernels run in the folder, so that the relative paths of the cells are next to the notebooks,
 * as in Jupyter. The kernels of the notebooks that no page shows stop after {@value #IDLE_MINUTES}
 * minutes without runs.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class NotebookManager implements AutoCloseable {

    public static final String EXTENSION = ".ipynb";
    /** The minutes after which an unused kernel stops. */
    public static final int IDLE_MINUTES = 30;

    private static final Pattern NAME = Pattern.compile("[\\p{L}\\p{N}_][\\p{L}\\p{N}_ .()-]{0,99}");

    /**
     * A notebook file.
     *
     * @param name the name, without extension.
     * @param modified the time of the last change.
     */
    public record NotebookInfo( String name, long modified ) {
    }

    private final File folder;
    private final Supplier<List<String>> javaCommand;
    private final Map<String, NotebookSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "notebook-reaper");
        thread.setDaemon(true);
        return thread;
    });
    private long idleMillis = TimeUnit.MINUTES.toMillis(IDLE_MINUTES);

    /**
     * @param folder the folder of the notebooks, created if missing.
     * @param javaCommand gives the start of the java command of the kernels.
     */
    public NotebookManager( File folder, Supplier<List<String>> javaCommand ) {
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IllegalStateException("The notebooks folder can't be created: " + folder.getAbsolutePath());
        }
        this.folder = folder;
        this.javaCommand = javaCommand;
        reaper.scheduleWithFixedDelay(this::stopIdleKernels, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Change the time after which unused kernels stop, for the tests.
     */
    void setIdleMillis( long idleMillis ) {
        this.idleMillis = idleMillis;
    }

    /**
     * @return the folder of the notebooks.
     */
    public File getFolder() {
        return folder;
    }

    /**
     * @return the notebooks, the last changed first.
     */
    public List<NotebookInfo> list() {
        List<NotebookInfo> notebooks = new ArrayList<>();
        File[] files = folder.listFiles(( dir, fileName ) -> fileName.endsWith(EXTENSION));
        if (files != null) {
            for( File file : files ) {
                String name = file.getName().substring(0, file.getName().length() - EXTENSION.length());
                if (isValidName(name)) {
                    notebooks.add(new NotebookInfo(name, file.lastModified()));
                }
            }
        }
        notebooks.sort(Comparator.comparingLong(NotebookInfo::modified).reversed());
        return notebooks;
    }

    /**
     * @return <code>true</code> if the name can be the name of a notebook.
     */
    public static boolean isValidName( String name ) {
        return name != null && NAME.matcher(name).matches() && !name.contains("..");
    }

    /**
     * Create a new notebook with an empty code cell.
     *
     * @throws IllegalArgumentException if the name is not valid or the notebook exists.
     */
    public void create( String name ) throws Exception {
        File file = getFile(name);
        if (file.exists()) {
            throw new IllegalArgumentException("A notebook named " + name + " exists already.");
        }
        NotebookDocument.create().write(file);
    }

    /**
     * Delete a notebook, stopping its kernel.
     */
    public void delete( String name ) throws Exception {
        File file = getFile(name);
        NotebookSession session = sessions.remove(name);
        if (session != null) {
            session.shutdownKernel();
        }
        if (file.exists() && !file.delete()) {
            throw new IllegalStateException("The notebook can't be deleted.");
        }
    }

    /**
     * @return the open session of a notebook, opened if needed.
     * @throws IllegalArgumentException if the notebook doesn't exist.
     */
    public NotebookSession open( String name ) throws Exception {
        File file = getFile(name);
        synchronized (sessions) {
            NotebookSession session = sessions.get(name);
            if (session == null) {
                if (!file.isFile()) {
                    throw new IllegalArgumentException("The notebook " + name + " doesn't exist.");
                }
                session = new NotebookSession(name, file, folder, javaCommand);
                sessions.put(name, session);
            }
            return session;
        }
    }

    private File getFile( String name ) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Not a valid name for a notebook: use letters, numbers, spaces and _ . ( ) -");
        }
        return new File(folder, name + EXTENSION);
    }

    /**
     * Stop the kernels that no page shows and that ran nothing for a while.
     */
    void stopIdleKernels() {
        long now = System.currentTimeMillis();
        for( NotebookSession session : sessions.values() ) {
            if (!session.hasListeners() && NotebookSession.IDLE.equals(session.getState())
                    && now - session.getLastActivity() > idleMillis) {
                session.shutdownKernel();
            }
        }
    }

    /**
     * Stop all the kernels.
     */
    @Override
    public void close() {
        reaper.shutdownNow();
        for( NotebookSession session : sessions.values() ) {
            session.shutdownKernel();
        }
        sessions.clear();
    }
}
