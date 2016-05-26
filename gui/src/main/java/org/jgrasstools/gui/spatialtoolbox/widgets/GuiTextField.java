/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gui.spatialtoolbox.widgets;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.text.MessageFormat;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.geotools.geometry.Envelope2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTProcessingRegion;
import org.jgrasstools.gui.spatialtoolbox.core.FieldData;
import org.jgrasstools.gui.spatialtoolbox.core.SpatialToolboxConstants;
import org.jgrasstools.gui.spatialtoolbox.core.SpatialToolboxUtils;
import org.jgrasstools.gui.utils.DefaultGuiBridgeImpl;
import org.jgrasstools.gui.utils.GuiUtilities;
import org.jgrasstools.gui.utils.GuiBridgeHandler;

/**
 * Class representing an swt textfield gui.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GuiTextField extends ModuleGuiElement implements KeyListener, FocusListener {

    private JTextComponent text;
    private final FieldData data;
    private boolean isFileOrFolder;
    private boolean isInFile;
    private boolean isInFolder;
    private boolean isOutFile;
    private boolean isOutFolder;
    private boolean isCrs;
    // private boolean isEastingNorthing;
    // private boolean isNorthing;
    // private boolean isEasting;
    private boolean isMultiline;
    private boolean isProcessingNorth;
    private boolean isProcessingSouth;
    private boolean isProcessingWest;
    private boolean isProcessingEast;
    private boolean isProcessingCols;
    private boolean isProcessingRows;
    private boolean isProcessingXres;
    private boolean isProcessingYres;
    private boolean isMapcalc;
    private boolean isGrassfile;

    private GuiBridgeHandler dialogManager = new DefaultGuiBridgeImpl();

    // private MapMouseListener currentMapMouseListener;
    // private IBlackboardListener currentBlackboardListener;

    private int rows;

    public GuiTextField( FieldData data, String constraints, GuiBridgeHandler dialogManager ) {
        this(data, constraints);
        if (dialogManager != null) {
            this.dialogManager = dialogManager;
        }

    }

    public GuiTextField( FieldData data, String constraints ) {
        this.data = data;

        if (data.guiHints != null) {
            if (data.guiHints.contains(JGTConstants.FILEIN_UI_HINT)) {
                isInFile = true;
                isFileOrFolder = true;
            }
            if (data.guiHints.contains(JGTConstants.FOLDERIN_UI_HINT)) {
                isInFolder = true;
                isFileOrFolder = true;
            }
            if (data.guiHints.contains(JGTConstants.FILEOUT_UI_HINT)) {
                isOutFile = true;
                isFileOrFolder = true;
            }
            if (data.guiHints.contains(JGTConstants.FOLDEROUT_UI_HINT)) {
                isOutFolder = true;
                isFileOrFolder = true;
            }
            if (data.guiHints.contains(JGTConstants.CRS_UI_HINT)) {
                isCrs = true;
            }
            // if (data.guiHints.contains(OmsBoxConstants.EASTINGNORTHING_UI_HINT)) {
            // isEastingNorthing = true;
            // } else if (data.guiHints.contains(OmsBoxConstants.NORTHING_UI_HINT)) {
            // isNorthing = true;
            // } else if (data.guiHints.contains(OmsBoxConstants.EASTING_UI_HINT)) {
            // isEasting = true;
            // }
            if (data.guiHints.contains(JGTConstants.MULTILINE_UI_HINT)) {
                isMultiline = true;

                String[] split = data.guiHints.split(","); //$NON-NLS-1$
                for( String string : split ) {
                    String hint = string.trim();
                    if (hint.startsWith(JGTConstants.MULTILINE_UI_HINT)) {
                        hint = hint.replaceFirst(JGTConstants.MULTILINE_UI_HINT, ""); //$NON-NLS-1$
                        rows = 1;
                        try {
                            rows = Integer.parseInt(hint);
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
            if (data.guiHints.contains(JGTConstants.MAPCALC_UI_HINT)) {
                isMapcalc = true;
            }
            if (data.guiHints.contains(SpatialToolboxConstants.GRASSFILE_UI_HINT)) {
                isGrassfile = true;
            }
            if (data.guiHints.contains(JGTConstants.PROCESS_NORTH_UI_HINT)) {
                isProcessingNorth = true;
            } else if (data.guiHints.contains(JGTConstants.PROCESS_SOUTH_UI_HINT)) {
                isProcessingSouth = true;
            } else if (data.guiHints.contains(JGTConstants.PROCESS_WEST_UI_HINT)) {
                isProcessingWest = true;
            } else if (data.guiHints.contains(JGTConstants.PROCESS_EAST_UI_HINT)) {
                isProcessingEast = true;
            } else if (data.guiHints.contains(JGTConstants.PROCESS_COLS_UI_HINT)) {
                isProcessingCols = true;
            } else if (data.guiHints.contains(JGTConstants.PROCESS_ROWS_UI_HINT)) {
                isProcessingRows = true;
            } else if (data.guiHints.contains(JGTConstants.PROCESS_XRES_UI_HINT)) {
                isProcessingXres = true;
            } else if (data.guiHints.contains(JGTConstants.PROCESS_YRES_UI_HINT)) {
                isProcessingYres = true;
            }

        }
    }

    @Override
    public JComponent makeGui( JComponent parent ) {
        int cols = 1;
        if (isInFile || isInFolder || isOutFile || isOutFolder || isCrs || isMultiline) {
            cols = 2;
        }

        final boolean isFile = isInFile || isOutFile;
        final boolean isFolder = isInFolder || isOutFolder;

        parent.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;

        if (!isMultiline) {
            text = new JTextField();
        } else {
            text = new JTextArea(rows, 20);
        }
        parent.add(text, c);
        text.addKeyListener(this);
        text.addFocusListener(this);

        if (isProcessing()) {
            JGTProcessingRegion processingRegion = new JGTProcessingRegion(new Envelope2D());// FIXME
                                                                                             // SpatialToolboxSessionPluginSingleton.getInstance().getProcessingRegion();
            if (processingRegion != null) {
                if (isProcessingNorth) {
                    text.setText(String.valueOf(processingRegion.getNorth()));
                } else if (isProcessingSouth) {
                    text.setText(String.valueOf(processingRegion.getSouth()));
                } else if (isProcessingWest) {
                    text.setText(String.valueOf(processingRegion.getWest()));
                } else if (isProcessingEast) {
                    text.setText(String.valueOf(processingRegion.getEast()));
                } else if (isProcessingCols) {
                    text.setText(String.valueOf(processingRegion.getCols()));
                } else if (isProcessingRows) {
                    text.setText(String.valueOf(processingRegion.getRows()));
                } else if (isProcessingXres) {
                    text.setText(String.valueOf(processingRegion.getWEResolution()));
                } else if (isProcessingYres) {
                    text.setText(String.valueOf(processingRegion.getNSResolution()));
                }
            }
        }

        if (data.fieldValue != null) {
            String tmp = data.fieldValue;

            if (tmp.contains(JGTConstants.WORKINGFOLDER)) {
                // check if there is a working folder defined
                String workingFolder = "";// TODO check
                                          // SpatialToolboxSessionPluginSingleton.getInstance().getWorkingFolder();
                workingFolder = checkBackSlash(workingFolder);
                if (workingFolder != null) {
                    tmp = tmp.replaceFirst(JGTConstants.WORKINGFOLDER, workingFolder);
                    data.fieldValue = tmp;
                } else {
                    data.fieldValue = "";
                }
            }

            if (isFileOrFolder)
                data.fieldValue = checkBackSlash(data.fieldValue);
            text.setText(data.fieldValue);
            // text.setSelection(text.getCharCount());
        }

        if (isFile) {
            final JButton browseButton = new JButton();
            browseButton.setText("...");
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 0;
            parent.add(browseButton, c);
            browseButton.addActionListener(new ActionListener(){
                public void actionPerformed( ActionEvent e ) {
                    File[] files;
                    if (isInFile) {
                        files = dialogManager.showOpenFileDialog("Open", GuiUtilities.getLastFile());
                    } else {
                        files = dialogManager.showSaveFileDialog("Save", GuiUtilities.getLastFile());
                    }
                    if (files != null && files.length > 0) {
                        String absolutePath = files[0].getAbsolutePath();
                        GuiUtilities.setLastPath(absolutePath);
                        text.setText(absolutePath);
                        text.setCaretPosition(text.getDocument().getLength());
                        setDataValue();
                    }
                }
            });
        }
        if (isFolder) {
            final JButton browseButton = new JButton();
            browseButton.setText("...");
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 0;
            parent.add(browseButton, c);
            browseButton.addActionListener(new ActionListener(){
                public void actionPerformed( ActionEvent e ) {
                    File[] files;
                    if (isInFolder) {
                        files = dialogManager.showOpenDirectoryDialog("Open", GuiUtilities.getLastFile());
                    } else {
                        // there is no folder save
                        files = dialogManager.showOpenDirectoryDialog("Save", GuiUtilities.getLastFile());
                    }
                    if (files != null && files.length > 0) {
                        String absolutePath = files[0].getAbsolutePath();
                        GuiUtilities.setLastPath(absolutePath);
                        text.setText(absolutePath);
                        text.setCaretPosition(text.getDocument().getLength());
                        setDataValue();
                    }
                }
            });
        }
        if (isCrs) {
            // // the crs choice group
            // final Button crsButton = new Button(parent, SWT.BORDER);
            // crsButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
            // crsButton.setText("..."); //$NON-NLS-1$
            // crsButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter(){
            //
            // public void widgetSelected( org.eclipse.swt.events.SelectionEvent e ) {
            // Shell shell = new Shell(text.getShell(), SWT.SHELL_TRIM);
            // Dialog dialog = new Dialog(shell){
            //
            // private CRSChooser chooser;
            // private CoordinateReferenceSystem crs;
            //
            // @Override
            // protected void configureShell( Shell shell ) {
            // super.configureShell(shell);
            // shell.setText("Choose CRS");
            // }
            //
            // @Override
            // protected Control createDialogArea( Composite parent ) {
            // Composite comp = (Composite) super.createDialogArea(parent);
            // GridLayout gLayout = (GridLayout) comp.getLayout();
            //
            // gLayout.numColumns = 1;
            //
            // chooser = new CRSChooser(new Controller(){
            //
            // public void handleClose() {
            // buttonPressed(OK);
            // }
            //
            // public void handleOk() {
            // buttonPressed(OK);
            // }
            //
            // });
            //
            // return chooser.createControl(parent);
            // }
            //
            // @Override
            // protected void buttonPressed( int buttonId ) {
            // if (buttonId == OK) {
            // crs = chooser.getCRS();
            //
            // try {
            // String codeFromCrs = OmsBoxUtils.getCodeFromCrs(crs);
            // text.setText(codeFromCrs);
            // } catch (Exception e) {
            // e.printStackTrace();
            // }
            //
            // }
            // close();
            // }
            //
            // };
            //
            // dialog.setBlockOnOpen(true);
            // dialog.open();
            // }
            // });

            // // initially set to map's crs
            // IMap activeMap = ApplicationGIS.getActiveMap();
            // if (activeMap != null) {
            // try {
            // CoordinateReferenceSystem crs = activeMap.getViewportModel().getCRS();
            // String codeFromCrs = OmsBoxUtils.getCodeFromCrs(crs);
            // text.setText(codeFromCrs);
            // setDataValue();
            // } catch (Exception e) {
            // e.printStackTrace();
            // }
            // }
        }

        // if (isNorthing || isEasting || isEastingNorthing) {
        // addMapMouseListener();
        // }

        // if (isProcessing()) {
        // addRegionListener();
        // ILayer processingRegionLayer = OmsBoxPlugin.getDefault().getProcessingRegionMapGraphic();
        // IStyleBlackboard blackboard = processingRegionLayer.getStyleBlackboard();
        // Object object = blackboard.get(ProcessingRegionStyleContent.ID);
        // if (object instanceof ProcessingRegionStyle) {
        // ProcessingRegionStyle processingStyle = (ProcessingRegionStyle) object;
        // setRegion(processingStyle);
        // }
        // }

        // text.addDisposeListener(new DisposeListener(){
        // public void widgetDisposed( DisposeEvent e ) {
        // if (isNorthing || isEasting || isEastingNorthing) {
        // removeMapMouseListener();
        // }
        // if (isProcessing()) {
        // removeRegionListener();
        // }
        // }
        // });

        // addDrop(); FIXME

        return text;
    }

    public FieldData getFieldData() {
        // FIXME
        // if (isMapcalc) {
        // MapcalculatorUtils.saveMapcalcHistory(text.getText());
        // }
        return data;
    }

    private void setDataValue() {
        String textStr = text.getText();
        String tmpTextStr = textStr;
        if (isFileOrFolder)
            tmpTextStr = checkBackSlash(textStr);
        if (!tmpTextStr.equals(textStr)) {
            // changed
            text.removeKeyListener(this);
            textStr = tmpTextStr;
            text.setText(textStr);
            text.addKeyListener(this);
        }
        if (isFileOrFolder && tmpTextStr.length() > 0) {
            File file = new File(tmpTextStr);
            String path = file.getAbsolutePath();
            if (isFileOrFolder)
                path = checkBackSlash(path);
            data.fieldValue = path;
        } else {
            data.fieldValue = textStr;
        }
    }

    public boolean hasData() {
        return true;
    }

    // private void removeMapMouseListener() {
    // if (currentMapMouseListener != null) {
    // final IMap activeMap = ApplicationGIS.getActiveMap();
    // final IRenderManager renderManager = activeMap.getRenderManager();
    // final ViewportPane viewportPane = (ViewportPane) renderManager.getMapDisplay();
    // viewportPane.removeMouseListener(currentMapMouseListener);
    // }
    // }

    // private void addMapMouseListener() {
    // final IMap activeMap = ApplicationGIS.getActiveMap();
    // if (activeMap == null) {
    // return;
    // }
    // final IRenderManager renderManager = activeMap.getRenderManager();
    // if (renderManager == null) {
    // return;
    // }
    // final ViewportPane viewportPane = (ViewportPane) renderManager.getMapDisplay();
    // if (viewportPane == null) {
    // return;
    // }
    //
    // currentMapMouseListener = new MapMouseListener(){
    // public void mouseReleased( MapMouseEvent event ) {
    // Point point = event.getPoint();
    // Coordinate worldClick = activeMap.getViewportModel().pixelToWorld(point.x, point.y);
    // if (isEastingNorthing) {
    // text.setText(String.valueOf(worldClick.x) + "," + String.valueOf(worldClick.y));
    // }
    // if (isNorthing) {
    // text.setText(String.valueOf(worldClick.y));
    // }
    // if (isEasting) {
    // text.setText(String.valueOf(worldClick.x));
    // }
    // }
    // public void mousePressed( MapMouseEvent event ) {
    // }
    // public void mouseExited( MapMouseEvent event ) {
    // }
    // public void mouseEntered( MapMouseEvent event ) {
    // }
    // public void mouseDoubleClicked( MapMouseEvent event ) {
    // }
    // };
    // viewportPane.addMouseListener(currentMapMouseListener);
    // }

    // private void removeRegionListener() {
    // if (currentBlackboardListener != null) {
    // ILayer processingRegionLayer = OmsBoxPlugin.getDefault().getProcessingRegionMapGraphic();
    // IStyleBlackboard blackboard = processingRegionLayer.getStyleBlackboard();
    // blackboard.removeListener(currentBlackboardListener);
    // }
    // }

    // private void addRegionListener() {
    // ILayer processingRegionLayer = OmsBoxPlugin.getDefault().getProcessingRegionMapGraphic();
    // IStyleBlackboard blackboard = processingRegionLayer.getStyleBlackboard();
    // currentBlackboardListener = new IBlackboardListener(){
    // public void blackBoardCleared( IBlackboard source ) {
    // }
    // public void blackBoardChanged( BlackboardEvent event ) {
    // Object key = event.getKey();
    // if (key.equals(ProcessingRegionStyleContent.ID)) {
    // Object newValue = event.getNewValue();
    // if (newValue instanceof ProcessingRegionStyle) {
    // ProcessingRegionStyle processingStyle = (ProcessingRegionStyle) newValue;
    // setRegion(processingStyle);
    // }
    // }
    // }
    // };
    // blackboard.addListener(currentBlackboardListener);
    // }

    /**
     * @param values the region info as [w, e, s, n, xRes, yRes, cols, rows]
     */
    public void setRegion( double[] values ) {
        if (isProcessingNorth) {
            text.setText(String.valueOf(values[3]));
        } else if (isProcessingSouth) {
            text.setText(String.valueOf(values[2]));
        } else if (isProcessingWest) {
            text.setText(String.valueOf(values[0]));
        } else if (isProcessingEast) {
            text.setText(String.valueOf(values[1]));
        } else if (isProcessingCols) {
            text.setText(String.valueOf(values[6]));
        } else if (isProcessingRows) {
            text.setText(String.valueOf(values[7]));
        } else if (isProcessingXres) {
            text.setText(String.valueOf(values[4]));
        } else if (isProcessingYres) {
            text.setText(String.valueOf(values[5]));
        }

        keyReleased(null);
    }

    public boolean isProcessing() {
        return isProcessingNorth || isProcessingSouth || isProcessingEast || isProcessingWest || isProcessingCols
                || isProcessingRows || isProcessingXres || isProcessingYres;
    }

    // private void addDrop() {
    // int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT | DND.DROP_LINK;
    // DropTarget dropTarget = new DropTarget(text, operations);
    //
    // final TextTransfer textTransfer = TextTransfer.getInstance();
    // final FileTransfer fileTransfer = FileTransfer.getInstance();
    // final URLTransfer urlTransfer = URLTransfer.getInstance();
    // final ClientFileTransfer clientFileTransfer = ClientFileTransfer.getInstance();
    // Transfer[] types = new Transfer[]{fileTransfer, textTransfer, urlTransfer,
    // clientFileTransfer};
    // dropTarget.setTransfer(types);
    // dropTarget.addDropListener(new DropTargetListener(){
    // public void drop( DropTargetEvent event ) {
    // if (textTransfer.isSupportedType(event.currentDataType)) {
    // String text = (String) event.data;
    // System.out.println(text);
    // }
    // if (clientFileTransfer.isSupportedType(event.currentDataType)) {
    // Object dataObj = event.data;
    // if (dataObj instanceof ClientFile[]) {
    // ClientFile[] clientFileImpl = (ClientFile[]) dataObj;
    // if (clientFileImpl.length > 0) {
    // String name = clientFileImpl[0].getName();
    // String tmpText = text.getText();
    // File possibleFolder = new File(tmpText);
    // if (possibleFolder.exists() && possibleFolder.isDirectory()) {
    // File newFile = new File(possibleFolder, name);
    // setTextContent(newFile);
    // }
    // }
    // }
    // }
    // if (fileTransfer.isSupportedType(event.currentDataType)) {
    // String[] files = (String[]) event.data;
    // if (files.length > 0) {
    // File file = new File(files[0]);
    // if (file.exists()) {
    // String folder;
    // if ((isInFolder || isOutFolder) && !file.isDirectory()) {
    // File folderFile = file.getParentFile();
    // setTextContent(folderFile);
    // folder = folderFile.getAbsolutePath();
    // } else {
    // setTextContent(file);
    // folder = file.getParentFile().getAbsolutePath();
    // }
    // SpatialToolboxSessionPluginSingleton.getInstance().setLastFolderChosen(folder);
    // }
    // }
    // }
    // if (urlTransfer.isSupportedType(event.currentDataType)) {
    // Object data2 = event.data;
    // System.out.println(data2);
    // }
    // // if (omsboxTransfer.isSupportedType(event.currentDataType)) {
    // // try {
    // // Object data = event.data;
    // // if (data instanceof TreeSelection) {
    // // TreeSelection selection = (TreeSelection) data;
    // // Object firstElement = selection.getFirstElement();
    // //
    // // IGeoResource geoResource = null;
    // // if (firstElement instanceof LayerImpl) {
    // // LayerImpl layer = (LayerImpl) firstElement;
    // // geoResource = layer.getGeoResource();
    // //
    // // }
    // // if (firstElement instanceof IService) {
    // // IService service = (IService) firstElement;
    // // List< ? extends IGeoResource> resources = service.resources(new
    // // NullProgressMonitor());
    // // if (resources.size() > 0) {
    // // geoResource = resources.get(0);
    // // }
    // // }
    // // if (geoResource != null) {
    // // ID id = geoResource.getID();
    // // if (id != null)
    // // if (id.isFile()) {
    // // File file = id.toFile();
    // // if (file.exists()) {
    // // setTextContent(file);
    // // OmsBoxPlugin.getDefault().setLastFolderChosen(file.getParentFile().getAbsolutePath());
    // // }
    // // } else if (id.toString().contains("#") && id.toString().startsWith("file")) {
    // // // try to get the file
    // // String string = id.toString().replaceAll("#", "");
    // // URL url = new URL(string);
    // // File file = new File(url.toURI());
    // // if (file.exists()) {
    // // setTextContent(file);
    // // OmsBoxPlugin.getDefault().setLastFolderChosen(file.getParentFile().getAbsolutePath());
    // // }
    // // } else {
    // // System.out.println("Not a file: " + id.toString());
    // // }
    // // }
    // //
    // // }
    // // } catch (Exception e) {
    // // e.printStackTrace();
    // // }
    // // }
    // modifyText(null);
    // }
    // public void dragEnter( DropTargetEvent event ) {
    // }
    // public void dragLeave( DropTargetEvent event ) {
    // }
    // public void dragOperationChanged( DropTargetEvent event ) {
    // }
    // public void dragOver( DropTargetEvent event ) {
    // }
    // public void dropAccept( DropTargetEvent event ) {
    // }
    // });
    //
    // }

    private void setTextContent( File file ) {
        // FIXME
        // if (isMapcalc) {
        // String map = file.getName();
        // insertTextAtCaretPosition(text, map);
        // } else {
        String absolutePath = file.getAbsolutePath();
        if (isFileOrFolder)
            absolutePath = checkBackSlash(absolutePath);
        text.setText(absolutePath);
        text.setCaretPosition(text.getDocument().getLength());
        // }
    }

    // private static void insertTextAtCaretPosition( StyledText text, String string ) {
    // int caretPosition = text.getCaretOffset();
    //
    // String textStr = text.getText();
    // String sub1 = textStr.substring(0, caretPosition);
    // String sub2 = textStr.substring(caretPosition);
    //
    // StringBuilder sb = new StringBuilder();
    // sb.append(sub1);
    // sb.append(string);
    // sb.append(sub2);
    //
    // text.setText(sb.toString());
    // }

    @Override
    public String validateContent() {
        StringBuilder sb = new StringBuilder();
        String textStr = text.getText();
        int length = textStr.length();
        if (isInFile || isInFolder) {
            if (length != 0) {
                File file = new File(textStr);
                // first try absolute path, if it is used locally
                if (!file.exists()) {
                    sb.append(MessageFormat.format("File {0} dosen''t exist.\n", textStr));
                }
            }
        }
        if (isMapcalc) {
            if (length == 0) {
                sb.append("The function is mandatory for the mapcalc module.\n");
            }
        }
        if (isGrassfile) {
            File file = new File(textStr);
            if (length != 0 && !SpatialToolboxUtils.isGrass(file.getAbsolutePath())) {
                sb.append(
                        "Grass modules currently work only with data contained in a GRASS mapset (which doesn't seem to be the case for: "
                                + file.getName() + ").\n");
            }
        }

        if (sb.length() > 0) {
            return sb.toString();
        } else {
            return null;
        }
    }

    public void focusGained( FocusEvent e ) {
    }

    public void focusLost( FocusEvent e ) {
        setDataValue();
    }

    public void keyTyped( KeyEvent e ) {
    }

    public void keyPressed( KeyEvent e ) {
    }

    public void keyReleased( KeyEvent e ) {
        setDataValue();
    }

    /**
     * Replaces backslashes with /.
     * 
     * @param textStr the string to check.
     * @return the string without backslashes.
     */
    public static String checkBackSlash( String textStr ) {
        if (textStr != null) {
            textStr = textStr.replaceAll("\\\\", "/");
        }
        return textStr;
    }

}
