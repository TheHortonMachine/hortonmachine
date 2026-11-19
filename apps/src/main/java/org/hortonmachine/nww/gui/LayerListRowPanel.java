
package org.hortonmachine.nww.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.UIManager;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.ImageCache;

import org.hortonmachine.nww.gui.actions.DeleteLayerAction;
import org.hortonmachine.nww.gui.actions.SelectLayerAction;
import org.hortonmachine.nww.gui.actions.StyleVectorLayerAction;
import org.hortonmachine.nww.gui.actions.ZoomToLayerAction;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.hortonmachine.nww.layers.defaults.NwwVectorLayer;
import org.hortonmachine.nww.layers.defaults.vector.FeatureCollectionLinesLayer;
import org.hortonmachine.nww.layers.defaults.vector.FeatureCollectionPointsLayer;
import org.hortonmachine.nww.layers.defaults.vector.FeatureCollectionPolygonLayer;
import org.hortonmachine.nww.tools.LayerSource;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.layers.RenderableLayer;

public class LayerListRowPanel extends JPanel {

    private final Layer layer;
    private final JPanel textPanel;
    private final JLabel nameLabel;
    /** The geopackage name, for geopackage tables, else <code>null</code>. */
    private final JLabel sourceLabel;
    private final JPanel centerPanel;
    private final JPopupMenu popupMenu;

    /**
     * @param layerListener told of the changes of the layer.
     * @param wwdPanel the map.
     * @param layer the layer of the row.
     * @param onStyle what the style button does, <code>null</code> to have the simple style dialog
     *          of the 3D vector layers, and no button for the other layers.
     */
    public LayerListRowPanel(LayerEventsListener layerListener, final NwwPanel wwdPanel, final Layer layer, Runnable onStyle) {
        super(new BorderLayout(4, 0));
        this.layer = layer;

        SelectLayerAction action = new SelectLayerAction(layerListener, wwdPanel.getWwd(), layer, layer.isEnabled());
        JCheckBox checkBox = new JCheckBox(action);
        // the name is a separate label, which is also the handle to drag the layer
        checkBox.setHideActionText(true);
        checkBox.setSelected(action.isSelected());
        add(checkBox, BorderLayout.WEST);

        String layerName = layer.getName();
        String tooltip = layerName + " (drag to change the drawing order, right click for the layer actions)";
        Cursor moveCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        textPanel = new JPanel(new GridLayout(0, 1, 0, 0));
        textPanel.setOpaque(false);
        int separatorIndex = layerName.lastIndexOf(HMConstants.DB_TABLE_PATH_SEPARATOR);
        LayerSource source = LayerSource.of(layer);
        boolean isTable = layerName.substring(0, Math.max(0, separatorIndex)).toLowerCase().endsWith("." + HMConstants.GPKG)
                || (source != null && (source.isDatabaseTable() || source.isWebService()));
        if (separatorIndex > 0 && isTable) {
            // geopackage, database table or service layer: the table or layer name, with the
            // geopackage, connection or service host smaller below
            nameLabel = new JLabel(layerName.substring(separatorIndex + HMConstants.DB_TABLE_PATH_SEPARATOR.length()));
            sourceLabel = new JLabel(layerName.substring(0, separatorIndex));
            Font font = nameLabel.getFont();
            sourceLabel.setFont(font.deriveFont(font.getSize2D() * 0.85f));
            Color dimmed = UIManager.getColor("Label.disabledForeground");
            if (dimmed != null) {
                sourceLabel.setForeground(dimmed);
            }
            sourceLabel.setToolTipText(tooltip);
            sourceLabel.setCursor(moveCursor);
        } else {
            nameLabel = new JLabel(layerName);
            sourceLabel = null;
        }
        nameLabel.setToolTipText(tooltip);
        nameLabel.setCursor(moveCursor);
        textPanel.add(nameLabel);
        if (sourceLabel != null) {
            textPanel.add(sourceLabel);
        }

        // the name, with the opacity slider below it
        centerPanel = new JPanel(new BorderLayout(0, 0));
        // transparent, so that the background of the current row shows through
        centerPanel.setOpaque(false);
        centerPanel.add(textPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        if (hasLayerOpacity(layer)) {
            JSlider opacitySlider = new JSlider();
            GuiUtilities.setupOpacitySlider(opacitySlider, layer.getOpacity(), opacity -> {
                layer.setOpacity(opacity);
                wwdPanel.getWwd().redraw();
            });
            opacitySlider.setOpaque(false);
            // small preferred width, so that the slider follows the width of the panel instead
            // of widening it
            opacitySlider.setPreferredSize(new Dimension(60, opacitySlider.getPreferredSize().height));
            centerPanel.add(opacitySlider, BorderLayout.SOUTH);
        }

        // style, zoom to and delete in the right click menu of the row
        popupMenu = new JPopupMenu();
        Action styleAction = null;
        if (onStyle != null) {
            styleAction = new AbstractAction("", ImageCache.get(ImageCache.PALETTE)){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    onStyle.run();
                }
            };
            styleAction.putValue(Action.SHORT_DESCRIPTION, "Style");
        } else if (layer instanceof NwwVectorLayer) {
            styleAction = new StyleVectorLayerAction(wwdPanel, (NwwVectorLayer) layer);
        }
        if (styleAction != null) {
            addMenuItem(styleAction, "Style...");
        }
        if (layer instanceof NwwLayer) {
            addMenuItem(new ZoomToLayerAction(wwdPanel, (NwwLayer) layer), "Zoom to layer");
        }
        if (popupMenu.getComponentCount() > 0) {
            popupMenu.addSeparator();
        }
        addMenuItem(new DeleteLayerAction(layerListener, wwdPanel.getWwd(), layer), "Remove layer");

        MouseAdapter popupListener = new MouseAdapter(){
            @Override
            public void mousePressed( MouseEvent e ) {
                showPopup(e);
            }

            @Override
            public void mouseReleased( MouseEvent e ) {
                // the popup trigger is the press on linux and mac, the release on windows
                showPopup(e);
            }

            private void showPopup( MouseEvent e ) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };
        for( Component component : getDragHandles() ) {
            component.addMouseListener(popupListener);
        }
    }

    /**
     * Add an item to the popup menu. The actions have only an icon, as they used to be buttons.
     */
    private void addMenuItem( Action action, String text ) {
        JMenuItem item = new JMenuItem(action);
        item.setText(text);
        popupMenu.add(item);
    }

    /**
     * @return <code>true</code> if the layer is drawn with its opacity: the image layers and
     *          the 3D feature layers, whose opacity multiplies the one of their style. The other
     *          vector and marker layers ignore it, theirs is only in the style.
     */
    private static boolean hasLayerOpacity( Layer layer ) {
        if (layer instanceof FeatureCollectionPolygonLayer || layer instanceof FeatureCollectionLinesLayer
                || layer instanceof FeatureCollectionPointsLayer) {
            return true;
        }
        return !(layer instanceof RenderableLayer) && !(layer instanceof MarkerLayer);
    }

    public Layer getLayer() {
        return layer;
    }

    /**
     * Highlight the row of the current layer, the one selected with a click.
     *
     * @param current <code>true</code> to highlight the row.
     */
    public void setCurrent( boolean current ) {
        Color background = UIManager.getColor("List.selectionInactiveBackground");
        if (background == null) {
            background = UIManager.getColor("List.selectionBackground");
        }
        setOpaque(current && background != null);
        if (current && background != null) {
            setBackground(background);
        }
        repaint();
    }

    /**
     * @return the components of the row that start the drag of the layer, i.e. all but the
     *          checkbox and the buttons.
     */
    public Component[] getDragHandles() {
        if (sourceLabel != null) {
            return new Component[]{this, centerPanel, textPanel, nameLabel, sourceLabel};
        }
        return new Component[]{this, centerPanel, textPanel, nameLabel};
    }

}
