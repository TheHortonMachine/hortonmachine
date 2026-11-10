package org.hortonmachine.gears.io.geopaparazzi.forms.items;
import java.util.ArrayList;
import java.util.List;

/**
 * A list that also holds a name.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class NamedList<T> {
    public String name = "";

    public List<T> items = new ArrayList<>();
}