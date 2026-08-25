/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com
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
package org.hortonmachine.database.spi;

import java.awt.Component;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.DbLevel;
import org.hortonmachine.dbs.compat.objects.LeafLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;

/**
 * Extension point for the database viewer's right-click context menu actions.
 *
 * <p>Implementations are discovered at runtime through the standard java
 * {@link java.util.ServiceLoader} mechanism: a downstream project (for example
 * a WHETGEO or GeoFrame specific one) contributes actions by implementing this
 * interface and listing the implementation's fully qualified class name in a
 * {@code META-INF/services/org.hortonmachine.database.spi.IDbViewerActionProvider}
 * file on its classpath. The database viewer never needs to depend on, or even
 * know about, such downstream projects at compile time.
 *
 * <p>{@link #supportsDatabase(ADb)} is called once, right after a database
 * connection is opened, so the viewer can cheaply decide which providers are
 * relevant for that particular connection (for example by checking for the
 * presence of a few marker tables) and only consult those for every
 * subsequent right click, instead of re-probing the schema on every popup.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IDbViewerActionProvider {

    /**
     * Cheaply checks whether this provider is relevant for the given, freshly
     * opened database connection.
     *
     * <p>Called once when a database is opened. Providers for which this
     * returns {@code false} are not consulted again for that connection's
     * context menus, so this should be an inexpensive check (e.g. checking
     * whether a handful of marker tables exist) rather than a full schema
     * scan.
     *
     * @param db the just-opened database connection.
     * @return {@code true} if this provider may contribute actions for
     *          {@code db}.
     */
    boolean supportsDatabase( ADb db );

    /**
     * @return an icon representing this provider (e.g. a small WHETGEO or
     *          GeoFrame badge), or {@code null} for none. Actions returned by
     *          this provider that don't set their own {@code Action.SMALL_ICON}
     *          get this icon applied to them automatically; it may also be
     *          used elsewhere to mark a database/table recognized by this
     *          provider.
     */
    default Icon getIcon() {
        return null;
    }

    /**
     * @return actions to contribute to the database-node context menu, or an
     *          empty list if none apply. Only called for providers for which
     *          {@link #supportsDatabase(ADb)} returned {@code true}.
     */
    default List<Action> getDatabaseActions( ADb db, DbLevel dbLevel, Component parent ) {
        return Collections.emptyList();
    }

    /**
     * @return actions to contribute to the table-node context menu, or an
     *          empty list if none apply. Only called for providers for which
     *          {@link #supportsDatabase(ADb)} returned {@code true}.
     */
    default List<Action> getTableActions( ADb db, TableLevel tableLevel, Component parent ) {
        return Collections.emptyList();
    }

    /**
     * @return actions to contribute to the column-node context menu, or an
     *          empty list if none apply. Only called for providers for which
     *          {@link #supportsDatabase(ADb)} returned {@code true}.
     */
    default List<Action> getColumnActions( ADb db, ColumnLevel columnLevel, Component parent ) {
        return Collections.emptyList();
    }

    /**
     * @return actions to contribute to the leaf-node context menu, or an
     *          empty list if none apply. Only called for providers for which
     *          {@link #supportsDatabase(ADb)} returned {@code true}.
     */
    default List<Action> getLeafActions( ADb db, LeafLevel leafLevel, Component parent ) {
        return Collections.emptyList();
    }
}
