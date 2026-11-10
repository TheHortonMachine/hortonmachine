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
package org.hortonmachine.nww.tools;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * The tabs at the side of the map of the viewer: the general tools first, then one tab per
 * {@link INwwTool}, whose panel is created the first time the tab is selected.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NwwToolsTabs extends JTabbedPane {
    private static final long serialVersionUID = 1L;

    private final NwwContext context;
    /** The tools, at the index of their tab minus one. */
    private final List<INwwTool> tools = new ArrayList<>();
    /** If the panel of the tool at the same index has been created. */
    private final List<Boolean> created = new ArrayList<>();
    /** The tool of the selected tab, <code>null</code> for the general tools. */
    private INwwTool activeTool;

    /**
     * @param generalTools the panel of the general tools, in the first tab.
     * @param context the access to the viewer given to the tools.
     */
    public NwwToolsTabs( Component generalTools, NwwContext context ) {
        this.context = context;
        addTab("Tools", generalTools);
        addChangeListener(e -> onTabSelected());
    }

    /**
     * Add the tab of a tool, its panel is created when the tab is first selected.
     *
     * @param tool the tool to add.
     */
    public void addTool( INwwTool tool ) {
        tools.add(tool);
        created.add(false);
        addTab(tool.getName(), tool.getIcon(), new JPanel());
    }

    private void onTabSelected() {
        int index = getSelectedIndex();
        INwwTool selected = index > 0 ? tools.get(index - 1) : null;
        if (selected == activeTool) {
            return;
        }
        if (activeTool != null) {
            activeTool.onDeactivated();
        }
        activeTool = selected;
        if (selected == null) {
            return;
        }
        if (!created.get(index - 1)) {
            created.set(index - 1, true);
            JComponent panel = selected.createPanel(context);
            setComponentAt(index, panel);
        }
        selected.onActivated();
    }

    /**
     * Tell the tools whose panel has been created that the viewer closes.
     */
    public void closeTools() {
        for( int i = 0; i < tools.size(); i++ ) {
            if (created.get(i)) {
                tools.get(i).onClose();
            }
        }
    }
}
