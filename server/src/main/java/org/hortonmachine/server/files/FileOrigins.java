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
package org.hortonmachine.server.files;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.db.ServerDb;

/**
 * Keeps the origins of the files of the data as the server writes them: each upload and each output of a job records
 * its origin, and the origin of the file it replaces goes in its history.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class FileOrigins {
    /** The entries of the history kept for each file. */
    public static final int MAX_HISTORY = 50;

    private final ServerDb db;
    private final WorkspacePaths paths;

    public FileOrigins( ServerDb db, WorkspacePaths paths ) {
        this.db = db;
        this.paths = paths;
    }

    /**
     * Record the origin of a file the server is about to write, before it is written: if a file is at its path, its
     * origin goes in the history, as external if it has none.
     */
    public synchronized void record( FileOrigin origin ) throws Exception {
        long now = System.currentTimeMillis();
        List<FileOrigin> history = new ArrayList<>();
        FileOrigin previous = db.getFileOrigin(origin.path);
        File file = paths.toFile(origin.path);
        if (previous != null) {
            history.add(previous.asReplaced(now));
            history.addAll(previous.history);
        } else if (file.isFile()) {
            history.add(new FileOrigin(origin.path, FileOrigin.EXTERNAL, null, file.lastModified(), null, null, null, now, null));
        }
        if (history.size() > MAX_HISTORY) {
            history = history.subList(0, MAX_HISTORY);
        }
        db.putFileOrigin(origin.withHistory(history));
    }

    /**
     * @return the origin of a file, <code>null</code> for the files put in the data from outside.
     */
    public FileOrigin get( String path ) throws Exception {
        return db.getFileOrigin(path);
    }

    /**
     * Forget the origins of a deleted file or folder.
     */
    public void deleteUnder( String path ) throws Exception {
        db.deleteFileOriginsUnder(path);
    }
}
