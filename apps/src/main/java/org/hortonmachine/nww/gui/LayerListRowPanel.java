
package org.hortonmachine.nww.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.layers.RenderableLayer;

public class LayerListRowPanel extends JPanel {

    private final Layer layer;
    private final JPanel textPanel;
    private final JLabel nameLabel;
    /** The geopackage name, for geopackage tables, else <code>null</code>. */
    private final JLabel sourceLabel;
    private final JPanel eastPanel;
    private final JPanel buttonsPanel;

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
        String tooltip = layerName + " (drag to change the drawing order)";
        Cursor moveCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        textPanel = new JPanel(new GridLayout(0, 1, 0, 0));
        textPanel.setOpaque(false);
        int separatorIndex = layerName.lastIndexOf(HMConstants.DB_TABLE_PATH_SEPARATOR);
        if (separatorIndex > 0 && layerName.substring(0, separatorIndex).toLowerCase().endsWith("." + HMConstants.GPKG)) {
            // geopackage table: the table name, with the geopackage name smaller below
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
        add(textPanel, BorderLayout.CENTER);

        eastPanel = new JPanel(new BorderLayout(4, 0));
        buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        // transparent, so that the background of the current row shows through
        eastPanel.setOpaque(false);
        buttonsPanel.setOpaque(false);
        eastPanel.add(buttonsPanel, BorderLayout.EAST);
        add(eastPanel, BorderLayout.EAST);

        // vector and marker layers ignore the layer opacity, theirs is in the style
        JSlider opacitySlider = null;
        if (!(layer instanceof RenderableLayer) && !(layer instanceof MarkerLayer)) {
            opacitySlider = new JSlider();
            GuiUtilities.setupOpacitySlider(opacitySlider, layer.getOpacity(), opacity -> {
                layer.setOpacity(opacity);
                wwdPanel.getWwd().redraw();
            });
            opacitySlider.setPreferredSize(new Dimension(70, opacitySlider.getPreferredSize().height));
            eastPanel.add(opacitySlider, BorderLayout.WEST);
        }

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
            JButton styleButton = new JButton(styleAction);
            styleButton.setBorderPainted(false);
            styleButton.setFocusPainted(false);
            styleButton.setContentAreaFilled(false);
            buttonsPanel.add(styleButton);
        }

        if (layer instanceof NwwLayer) {
            ZoomToLayerAction zoomToLayerAction = new ZoomToLayerAction(wwdPanel, (NwwLayer) layer);
            JButton zoomToButton = new JButton(zoomToLayerAction);
            zoomToButton.setBorderPainted(false);
            zoomToButton.setFocusPainted(false);
            zoomToButton.setContentAreaFilled(false);
            buttonsPanel.add(zoomToButton);
        }

        DeleteLayerAction deleteAction = new DeleteLayerAction(layerListener, wwdPanel.getWwd(), layer);
        JButton deleteButton = new JButton(deleteAction);
        deleteButton.setBorderPainted(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setContentAreaFilled(false);
        buttonsPanel.add(deleteButton);

        if (opacitySlider != null) {
            // rows with a slider have at most three buttons (style of the rasterized vectors,
            // zoom and delete): reserve the space of all, so that the sliders of all rows are
            // aligned
            int threeButtonsWidth = deleteButton.getPreferredSize().width * 3;
            Dimension size = buttonsPanel.getPreferredSize();
            buttonsPanel.setPreferredSize(new Dimension(Math.max(size.width, threeButtonsWidth), size.height));
        }
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
            return new Component[]{this, textPanel, nameLabel, sourceLabel, eastPanel, buttonsPanel};
        }
        return new Component[]{this, textPanel, nameLabel, eastPanel, buttonsPanel};
    }

}
