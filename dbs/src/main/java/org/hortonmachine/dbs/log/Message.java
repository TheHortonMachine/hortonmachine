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
package org.hortonmachine.dbs.log;

import java.util.Date;

import org.hortonmachine.dbs.utils.DbsUtilities;

/**
 * A message to be used for logging.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class Message {
    public long id;
    public long ts = 0;
    /**
     * One of {@link EMessageType}.
     */
    public int type = EMessageType.INFO.getCode();
    public String tag = "";
    public String msg = "";
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Message [id=");
        builder.append(id);
        builder.append(", ts=");
        builder.append(DbsUtilities.dbDateFormatter.format(new Date(ts)));
        builder.append(", type=");
        builder.append(EMessageType.fromCode(type).name());
        builder.append(", tag=");
        builder.append(tag);
        builder.append(", msg=");
        builder.append(msg);
        builder.append("]");
        return builder.toString();
    }

}
