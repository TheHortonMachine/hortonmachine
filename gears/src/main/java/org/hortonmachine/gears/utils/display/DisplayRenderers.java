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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import org.codehaus.groovy.runtime.FormatHelper;

/**
 * The registry of the {@link IDisplayRenderer renderers}, used to show any value in the
 * most useful form.
 *
 * <p>The renderers are tried in this order: the ones added with {@link #register(IDisplayRenderer)}
 * (last added first), the ones found through the {@link ServiceLoader}, the built in ones. If
 * none handles the value, it is shown as text.</p>
 *
 * <pre>
 * List&lt;DisplayPart&gt; parts = DisplayRenderers.render(raster, DisplayOptions.defaults());
 * </pre>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class DisplayRenderers {

    private static final List<IDisplayRenderer> registered = new CopyOnWriteArrayList<>();
    private static List<IDisplayRenderer> serviceRenderers;

    private static final List<IDisplayRenderer> builtins = List.of(//
            new ImageDisplayRenderer(), //
            new RasterDisplayRenderer(), //
            new FeatureDisplayRenderer(), //
            new GeometryDisplayRenderer(), //
            new FileDisplayRenderer(), //
            new TableDisplayRenderer() //
    );

    private DisplayRenderers() {
    }

    /**
     * Add a renderer, it is tried before the others.
     *
     * @param renderer the renderer to add.
     */
    public static void register( IDisplayRenderer renderer ) {
        registered.add(0, renderer);
    }

    /**
     * Remove a renderer added with {@link #register(IDisplayRenderer)}.
     *
     * @param renderer the renderer to remove.
     */
    public static void unregister( IDisplayRenderer renderer ) {
        registered.remove(renderer);
    }

    /**
     * Render a value.
     *
     * <p>If the chosen renderer fails, the value is shown as text, followed by the error.</p>
     *
     * @param value the value to render.
     * @param options the sizes and limits to use.
     * @return the parts, empty for a <code>null</code> value.
     */
    public static List<DisplayPart> render( Object value, DisplayOptions options ) {
        if (value == null) {
            return List.of();
        }
        for( IDisplayRenderer renderer : renderers() ) {
            boolean canRender;
            try {
                canRender = renderer.canRender(value);
            } catch (Exception e) {
                canRender = false;
            }
            if (canRender) {
                try {
                    return renderer.render(value, options);
                } catch (Exception e) {
                    return List.of(new DisplayText(asText(value)),
                            new DisplayText("Could not render the value: " + e.getLocalizedMessage()));
                }
            }
        }
        return List.of(new DisplayText(asText(value)));
    }

    private static String asText( Object value ) {
        try {
            return FormatHelper.toString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static List<IDisplayRenderer> renderers() {
        List<IDisplayRenderer> all = new ArrayList<>(registered);
        all.addAll(serviceRenderers());
        all.addAll(builtins);
        return all;
    }

    private static synchronized List<IDisplayRenderer> serviceRenderers() {
        if (serviceRenderers == null) {
            List<IDisplayRenderer> found = new ArrayList<>();
            for( IDisplayRenderer renderer : ServiceLoader.load(IDisplayRenderer.class) ) {
                found.add(renderer);
            }
            serviceRenderers = found;
        }
        return serviceRenderers;
    }
}
