package org.hortonmachine.nww.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

/**
 * The layers panel. The layers are shown in drawing order, the top one drawn last. They can be
 * dragged by their name to change the order and files can be dropped on the panel to load them,
 * once a handler is set through {@link #setFileDropHandler(Consumer)}.
 */
public class LayersPanelController extends LayersPanelView implements LayerEventsListener {

    private static final int INDICATOR_SIZE = 2;
    private static final Border ROW_BORDER = BorderFactory.createEmptyBorder(INDICATOR_SIZE, 0, INDICATOR_SIZE, 0);
    private static final DataFlavor URI_LIST_FLAVOR = createUriListFlavor();

    private NwwPanel wwdPanel;

    /** The rows, in the order shown, i.e. the reverse of the layer list. */
    private final List<LayerListRowPanel> rows = new ArrayList<>();
    private final JPanel rowsPanel = new JPanel();
    private final RowDragHandler rowDragHandler = new RowDragHandler();

    private Consumer<List<File>> fileDropHandler;

    /**
    * Default constructor
     * @param wwdPanel
    */
    public LayersPanelController(NwwPanel wwdPanel) {
        this.wwdPanel = wwdPanel;

//        setMinimumSize(new Dimension(450, 300));
        setMaximumSize(new Dimension(400, 2300));

        // a vertical list of rows instead of the designer's grid, which has a fixed number of rows
        _layersGridView.removeAll();
        _layersGridView.setLayout(new BorderLayout());
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        _layersGridView.add(rowsPanel, BorderLayout.NORTH);

        FileDropTransferHandler dropHandler = new FileDropTransferHandler();
        setTransferHandler(dropHandler);
        _layersGridView.setTransferHandler(dropHandler);
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, _layersGridView);
        if (viewport != null) {
            viewport.setTransferHandler(dropHandler);
        }

        refreshLayersList();
    }

    /**
     * Set the handler of the files dropped on the panel. Without one, drops are not accepted.
     *
     * @param fileDropHandler the handler, called on the event dispatch thread.
     */
    public void setFileDropHandler( Consumer<List<File>> fileDropHandler ) {
        this.fileDropHandler = fileDropHandler;
        String tooltip = fileDropHandler != null ? "Drop files here to load them" : null;
        _layersGridView.setToolTipText(tooltip);
    }

    public void refreshLayersList() {
        rowsPanel.removeAll();
        rows.clear();

        for( Layer layer : getShownLayers() ) {
            LayerListRowPanel row = new LayerListRowPanel(this, wwdPanel, layer);
            row.setBorder(ROW_BORDER);
            for( Component handle : row.getDragHandles() ) {
                handle.addMouseListener(rowDragHandler);
                handle.addMouseMotionListener(rowDragHandler);
            }
            rows.add(row);
            rowsPanel.add(row);
        }

        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    /**
     * @return the layers shown in the panel, top drawn first.
     */
    private List<Layer> getShownLayers() {
        LayerList layerList = wwdPanel.getWwd().getModel().getLayers();
        List<Layer> layersReverse = new ArrayList<>();
        for( Layer layer : layerList ) {
            if (layer.getName().startsWith("hide")) {
                continue;
            }
            layersReverse.add(0, layer);
        }
        return layersReverse;
    }

    /**
     * Move a layer in the drawing order.
     *
     * @param layer the layer to move.
     * @param targetRow the row it gets in the panel, after the move.
     */
    private void moveLayer( Layer layer, int targetRow ) {
        List<Layer> shown = getShownLayers();
        if (!shown.remove(layer)) {
            return;
        }
        shown.add(targetRow, layer);

        // the hidden layers keep their position: place the layer right above the one shown
        // below it, or right below the one shown above it, if it is the last one.
        LayerList layerList = wwdPanel.getWwd().getModel().getLayers();
        layerList.remove(layer);
        if (targetRow + 1 < shown.size()) {
            int belowIndex = layerList.indexOf(shown.get(targetRow + 1));
            layerList.add(belowIndex + 1, layer);
        } else if (targetRow > 0) {
            int aboveIndex = layerList.indexOf(shown.get(targetRow - 1));
            layerList.add(aboveIndex, layer);
        } else {
            layerList.add(layer);
        }
        wwdPanel.getWwd().redraw();
        refreshLayersList();
    }

    @Override
    public void onLayerAdded(Layer addedLayer) {
        refreshLayersList();
    }

    @Override
    public void onLayerRemoved(Layer removeLayer) {
        refreshLayersList();
    }

    @Override
    public void onLayerSelected(Layer selectdLayer, boolean isSelected) {
    }

    private static DataFlavor createUriListFlavor() {
        try {
            return new DataFlavor("text/uri-list;class=java.lang.String");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Color getIndicatorColor() {
        Color color = UIManager.getColor("Component.accentColor");
        if (color == null) {
            color = UIManager.getColor("List.selectionBackground");
        }
        return color != null ? color : Color.BLUE;
    }

    /**
     * Drags a row by its name or background and drops it between two other rows. A line shows
     * where it lands.
     */
    private class RowDragHandler extends MouseAdapter {
        private static final int DRAG_THRESHOLD = 4;

        private LayerListRowPanel draggedRow;
        private Point pressPoint;
        private boolean dragging;
        /** The gap the row is dropped into, from 0 (above the first row) to rows.size(). */
        private int dropSlot = -1;

        @Override
        public void mousePressed( MouseEvent e ) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            draggedRow = (LayerListRowPanel) SwingUtilities.getAncestorOfClass(LayerListRowPanel.class, e.getComponent());
            if (draggedRow == null && e.getComponent() instanceof LayerListRowPanel) {
                draggedRow = (LayerListRowPanel) e.getComponent();
            }
            pressPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), rowsPanel);
            dragging = false;
        }

        @Override
        public void mouseDragged( MouseEvent e ) {
            if (draggedRow == null) {
                return;
            }
            Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), rowsPanel);
            if (!dragging) {
                if (Math.abs(p.y - pressPoint.y) < DRAG_THRESHOLD) {
                    return;
                }
                dragging = true;
                rowsPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
            rowsPanel.scrollRectToVisible(new Rectangle(p.x, p.y, 1, 1));
            setDropSlot(getSlotAt(p.y));
        }

        @Override
        public void mouseReleased( MouseEvent e ) {
            if (draggedRow == null) {
                return;
            }
            LayerListRowPanel row = draggedRow;
            int slot = dropSlot;
            boolean wasDragging = dragging;
            draggedRow = null;
            dragging = false;
            setDropSlot(-1);
            rowsPanel.setCursor(null);
            if (!wasDragging || slot < 0) {
                return;
            }
            int fromRow = rows.indexOf(row);
            int targetRow = slot > fromRow ? slot - 1 : slot;
            if (fromRow >= 0 && targetRow != fromRow) {
                moveLayer(row.getLayer(), targetRow);
            }
        }

        private int getSlotAt( int y ) {
            for( int i = 0; i < rows.size(); i++ ) {
                Rectangle bounds = rows.get(i).getBounds();
                if (y < bounds.y + bounds.height / 2) {
                    return i;
                }
            }
            return rows.size();
        }

        private void setDropSlot( int slot ) {
            if (slot == dropSlot) {
                return;
            }
            dropSlot = slot;
            for( LayerListRowPanel row : rows ) {
                row.setBorder(ROW_BORDER);
            }
            if (slot < 0 || rows.isEmpty()) {
                return;
            }
            Color color = getIndicatorColor();
            if (slot < rows.size()) {
                rows.get(slot).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(INDICATOR_SIZE, 0, 0, 0, color),
                        BorderFactory.createEmptyBorder(0, 0, INDICATOR_SIZE, 0)));
            } else {
                rows.get(rows.size() - 1).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, INDICATOR_SIZE, 0, color),
                        BorderFactory.createEmptyBorder(INDICATOR_SIZE, 0, 0, 0)));
            }
        }
    }

    /**
     * Accepts files dragged from the file manager and passes them to the file drop handler.
     */
    private class FileDropTransferHandler extends TransferHandler {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport( TransferSupport support ) {
            if (fileDropHandler == null || !support.isDrop() || (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                    && !support.isDataFlavorSupported(URI_LIST_FLAVOR))) {
                return false;
            }
            if ((support.getSourceDropActions() & COPY) == COPY) {
                support.setDropAction(COPY);
            }
            return true;
        }

        @Override
        public boolean importData( TransferSupport support ) {
            if (!canImport(support)) {
                return false;
            }
            try {
                List<File> files = getFiles(support.getTransferable());
                if (files.isEmpty()) {
                    return false;
                }
                List<File> filesCopy = new ArrayList<>(files);
                // load after the drop ended, since loading can open dialogs, which would block
                // the drag source until closed
                SwingUtilities.invokeLater(() -> fileDropHandler.accept(filesCopy));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * @return the dropped files, from the java file list or, as some linux file managers
         *          only offer that, from the list of uris.
         */
        @SuppressWarnings("unchecked")
        private List<File> getFiles( Transferable transferable ) throws Exception {
            List<File> files = new ArrayList<>();
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<File> list = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (list != null) {
                    files.addAll(list);
                }
            } else if (transferable.isDataFlavorSupported(URI_LIST_FLAVOR)) {
                String uriList = (String) transferable.getTransferData(URI_LIST_FLAVOR);
                for( String line : uriList.split("\\r?\\n") ) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    try {
                        files.add(new File(new URI(line)));
                    } catch (Exception e) {
                        // not a file uri
                    }
                }
            }
            return files;
        }
    }

}
