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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

/**
 * The toolbar at the side of the map with the {@link INwwMapMode}s, of which a single one is
 * active. The first mode added is the default one, to which Esc goes back.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class NwwMapModesBar extends JToolBar {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MODE_ACTION = "nwwDefaultMapMode";

    private final NwwContext context;
    private final ButtonGroup group = new ButtonGroup();
    private final Map<INwwMapMode, JToggleButton> buttons = new IdentityHashMap<>();
    private INwwMapMode defaultMode;
    private INwwMapMode activeMode;

    /**
     * @param context the access to the viewer given to the modes.
     */
    public NwwMapModesBar( NwwContext context ) {
        super(JToolBar.VERTICAL);
        this.context = context;
        setFloatable(false);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), DEFAULT_MODE_ACTION);
        getActionMap().put(DEFAULT_MODE_ACTION, new AbstractAction(){
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed( ActionEvent e ) {
                setMode(defaultMode);
            }
        });
    }

    /**
     * Add the button of a mode. The first mode added is the default one and is activated.
     *
     * @param mode the mode to add.
     */
    public void addMode( INwwMapMode mode ) {
        JToggleButton button = new JToggleButton();
        if (mode.getIcon() != null) {
            button.setIcon(mode.getIcon());
        } else {
            button.setText(mode.getName());
        }
        button.setToolTipText(mode.getDescription());
        button.setFocusable(false);
        button.addActionListener(e -> setMode(mode));
        group.add(button);
        buttons.put(mode, button);
        add(button);
        if (defaultMode == null) {
            defaultMode = mode;
            setMode(mode);
        }
    }

    /**
     * Activate a mode, deactivating the active one.
     *
     * @param mode the mode, one of those added.
     */
    public void setMode( INwwMapMode mode ) {
        if (mode == null || mode == activeMode || !buttons.containsKey(mode)) {
            return;
        }
        if (activeMode != null) {
            activeMode.deactivate(context);
        }
        activeMode = mode;
        buttons.get(mode).setSelected(true);
        mode.activate(context);
    }

    /**
     * @return the active mode, or <code>null</code> if no mode was added.
     */
    public INwwMapMode getMode() {
        return activeMode;
    }

    /**
     * Deactivate the active mode, as the viewer closes.
     */
    public void close() {
        if (activeMode != null) {
            activeMode.deactivate(context);
            activeMode = null;
        }
    }
}
