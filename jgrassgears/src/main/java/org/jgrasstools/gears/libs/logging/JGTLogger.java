/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.libs.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A simple logger, to be properly implemented.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class JGTLogger {

    public static final boolean LOG_INFO = true;
    public static final boolean LOG_DEBUG = false;
    public static final boolean LOG_ERROR = true;

    private static final String SEP = ":: ";

    private static SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void logInfo( Object owner, String msg ) {
        if (LOG_INFO) {
            msg = toMessage(owner, msg);
            System.out.println(msg);
        }
    }

    public static void logDebug( Object owner, String msg ) {
        if (LOG_DEBUG) {
            msg = toMessage(owner, msg);
            System.out.println(msg);
        }
    }

    public static void logError( Object owner, String msg, Throwable e ) {
        if (LOG_ERROR) {
            msg = toMessage(owner, msg);
            System.err.println(msg);
            e.printStackTrace();
        }
    }

    public static void logError( Object owner, Throwable e ) {
        logError(owner, null, e);
    }

    private static String toMessage( Object owner, String msg ) {
        if (msg == null)
            msg = "";
        String newMsg = f.format(new Date()) + SEP;
        if (owner instanceof String) {
            newMsg = newMsg + owner + SEP;
        } else {
            newMsg = newMsg + owner.getClass().getSimpleName() + SEP;
        }
        newMsg = newMsg + msg;
        return newMsg;
    }

}
