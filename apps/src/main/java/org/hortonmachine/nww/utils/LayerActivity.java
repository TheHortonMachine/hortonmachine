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
package org.hortonmachine.nww.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The work the layers do in the background, as drawing their tiles from local data, which the
 * status bar shows next to the downloads of the web layers.
 *
 * <p>The work is counted by name of the layer: {@link #begin(String)} and {@link #end(String)}
 * are called in pairs, from any thread.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class LayerActivity {

    private static final Map<String, AtomicInteger> RUNNING = new ConcurrentHashMap<>();

    private LayerActivity() {
    }

    /**
     * @param name the name of the layer starting a work, as drawing a tile.
     */
    public static void begin( String name ) {
        RUNNING.computeIfAbsent(name, n -> new AtomicInteger()).incrementAndGet();
    }

    /**
     * @param name the name of the layer that finished a work begun with {@link #begin(String)}.
     */
    public static void end( String name ) {
        RUNNING.computeIfPresent(name, ( n, count ) -> count.decrementAndGet() <= 0 ? null : count);
    }

    /**
     * @return <code>true</code> if some layer is working.
     */
    public static boolean isActive() {
        return !RUNNING.isEmpty();
    }

    /**
     * @return a text of the working layers, as <i>mupolygon (4 tiles)</i>, empty if none works.
     */
    public static String describe() {
        List<String> parts = new ArrayList<>();
        int total = 0;
        for( Map.Entry<String, AtomicInteger> entry : RUNNING.entrySet() ) {
            int count = entry.getValue().get();
            if (count > 0) {
                parts.add(entry.getKey());
                total += count;
            }
        }
        if (parts.isEmpty()) {
            return "";
        }
        parts.sort(String::compareToIgnoreCase);
        String names = parts.size() > 2 ? parts.get(0) + ", " + parts.get(1) + " +" + (parts.size() - 2) : String.join(", ", parts);
        return names + " (" + total + (total == 1 ? " tile)" : " tiles)");
    }
}
