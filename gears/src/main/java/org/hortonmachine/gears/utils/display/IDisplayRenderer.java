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
package org.hortonmachine.gears.utils.display;

import java.util.List;

/**
 * A renderer that turns values of some types into {@link DisplayPart display parts}.
 *
 * <p>Renderers of other modules are found through the {@link java.util.ServiceLoader},
 * listing them in <code>META-INF/services/org.hortonmachine.gears.utils.display.IDisplayRenderer</code>,
 * or can be added with {@link DisplayRenderers#register(IDisplayRenderer)}. Both come
 * before the built in renderers.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public interface IDisplayRenderer {

    /**
     * @param value the value to check, never <code>null</code>.
     * @return <code>true</code> if this renderer handles the value.
     */
    boolean canRender( Object value );

    /**
     * Render a value.
     *
     * @param value the value, one for which {@link #canRender(Object)} is <code>true</code>.
     * @param options the sizes and limits to use.
     * @return the parts, in the order they should be shown.
     * @throws Exception
     */
    List<DisplayPart> render( Object value, DisplayOptions options ) throws Exception;
}
