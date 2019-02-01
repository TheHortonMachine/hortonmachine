package org.hortonmachine.style;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.hortonmachine.gears.utils.style.FeatureTypeStyleWrapper;
import org.hortonmachine.gears.utils.style.LineSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.PointSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.PolygonSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.RasterSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.RuleWrapper;
import org.hortonmachine.gears.utils.style.StyleWrapper;
import org.hortonmachine.gears.utils.style.TextSymbolizerWrapper;
import org.hortonmachine.gui.utils.ImageCache;

public class CustomTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 1L;

    public Component getTreeCellRendererComponent( JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus ) {

        Component ret = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        JLabel label = (JLabel) ret;

        if (value instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            if (userObject instanceof StyleWrapper) {
                label.setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_FOLDER));
            } else if (userObject instanceof FeatureTypeStyleWrapper) {
                label.setIcon(ImageCache.getInstance().getImage(ImageCache.MODULE_TEMPLATE));
            } else if (userObject instanceof RuleWrapper) {
                label.setIcon(ImageCache.getInstance().getImage(ImageCache.MODULE));
            } else if (userObject instanceof PolygonSymbolizerWrapper) {
                label.setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_POLYGON));
            } else if (userObject instanceof LineSymbolizerWrapper) {
                label.setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_LINE));
            } else if (userObject instanceof PointSymbolizerWrapper) {
                label.setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_POINT));
            } else if (userObject instanceof TextSymbolizerWrapper) {
                label.setIcon(ImageCache.getInstance().getImage(ImageCache.FONT));
            } else if (userObject instanceof RasterSymbolizerWrapper) {
                label.setIcon(ImageCache.getInstance().getImage(ImageCache.DEM));
            } else if (userObject instanceof FeatureAttributeNode) {
                label.setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN));
            } else if (userObject instanceof String) {
                String userObjectStr = (String) userObject;
                switch( userObjectStr ) {
                case MainController.ATTRIBUTES:
                    label.setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE));
                    break;
                case MainController.DATASTORE_INFORMATION:
                    label.setIcon(ImageCache.getInstance().getImage(ImageCache.INFOTOOL_ON));
                    break;
                case MainController.STYLE_GROUPS_AND_RULES:
                    label.setIcon(ImageCache.getInstance().getImage(ImageCache.PALETTE));
                    break;
                case MainController.RASTER_BOUNDS:
                case MainController.VECTOR_BOUNDS:
                    label.setIcon(ImageCache.getInstance().getImage(ImageCache.SELECTION_MODE));
                    break;

                default:
                    if (userObjectStr.startsWith(MainController.PROJECTION)) {
                        label.setIcon(ImageCache.getInstance().getImage(ImageCache.GLOBE));
                    } else {
                        label.setIcon(ImageCache.getInstance().getImage(ImageCache.TREE_CLOSED));
                    }
                    break;
                }
            }
        }

        return ret;
    }
}