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
package org.hortonmachine.llm.sql;

/**
 * How the HortonMachine database viewer creates and opens databases, given to the model for the
 * free questions, so that it can guide users through connecting.
 *
 * <p>It follows the toolbar of the viewer (new, connect, remote) and the database types and url
 * forms it accepts.</p>
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ConnectionGuide {

    /**
     * The guide.
     */
    public static final String TEXT = """
            How the HortonMachine database viewer works with databases:
            - Supported types: SpatiaLite (.sqlite), GeoPackage (.gpkg), H2GIS (.mv.db), plain SQLite and
              MBTiles (.sqlite, .mbtiles), and PostGIS (PostgreSQL with the PostGIS extension, on a server).
            - Toolbar button "new": creates a new database file of the chosen type.
            - Toolbar button "connect": opens an existing database file; choose the type and the file.
              Opening a file path that doesn't exist creates a new, empty database, so check the path.
              For H2GIS, "connect in network mode (port 9092)" opens the file through an H2 server
              running on this machine, as jdbc:h2:tcp://localhost:9092/<absolute path without .mv.db>.
            - Toolbar button "remote": connects through a JDBC url with user and password. Only two
              forms are accepted:
                PostGIS: jdbc:postgresql://<host>:<port, usually 5432>/<database name>
                H2GIS:   jdbc:h2:tcp://<host>:<port, usually 9092>/<absolute path of the database on the server>
            - User and password are typed in the dialog by the user; never ask for or repeat passwords.
            - Once connected, the tree on the left shows schemas, tables and columns; the SQL editor runs
              queries, and the "assistant" can write, fix and explain SQL for the connected database.
            - Common problems: PostGIS refusing connections (server not listening on the network, firewall,
              pg_hba.conf), wrong database name, an H2 database already opened by another program
              (use network mode), a file opened with the wrong type.
            """;

    private ConnectionGuide() {
    }
}
