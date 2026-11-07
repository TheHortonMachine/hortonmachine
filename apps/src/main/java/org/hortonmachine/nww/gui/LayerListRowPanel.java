
package org.hortonmachine.nww.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.UIManager;

import org.hortonmachine.gears.libs.modules.HMConstants;

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

    public LayerListRowPanel(LayerEventsListener layerListener, final NwwPanel wwdPanel, final Layer layer) {
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
        eastPanel.add(buttonsPanel, BorderLayout.EAST);
        add(eastPanel, BorderLayout.EAST);

        // vector and marker layers ignore the layer opacity, theirs is in the style
        JSlider opacitySlider = null;
        if (!(layer instanceof RenderableLayer) && !(layer instanceof MarkerLayer)) {
            int opacity = (int) Math.round(layer.getOpacity() * 100);
            opacitySlider = new JSlider(0, 100, opacity);
            opacitySlider.setPreferredSize(new Dimension(70, opacitySlider.getPreferredSize().height));
            opacitySlider.setFocusable(false);
            opacitySlider.setToolTipText("Opacity: " + opacity + "%");
            JSlider slider = opacitySlider;
            slider.addChangeListener(e -> {
                int value = slider.getValue();
                slider.setToolTipText("Opacity: " + value + "%");
                layer.setOpacity(value / 100.0);
                wwdPanel.getWwd().redraw();
            });
            eastPanel.add(slider, BorderLayout.WEST);
        }

        if (layer instanceof NwwVectorLayer) {
            StyleVectorLayerAction styleVectorLayerAction = new StyleVectorLayerAction(wwdPanel,
                    (NwwVectorLayer) layer);
            JButton styleButton = new JButton(styleVectorLayerAction);
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
            // rows with a slider have at most two buttons (zoom and delete): reserve the space
            // of both, so that the sliders of all rows are aligned
            int twoButtonsWidth = deleteButton.getPreferredSize().width * 2;
            Dimension size = buttonsPanel.getPreferredSize();
            buttonsPanel.setPreferredSize(new Dimension(Math.max(size.width, twoButtonsWidth), size.height));
        }
    }

    public Layer getLayer() {
        return layer;
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
