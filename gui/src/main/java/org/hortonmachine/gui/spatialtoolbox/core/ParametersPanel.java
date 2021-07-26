/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gui.spatialtoolbox.core;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.JGrassGears;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.utils.GuiBridgeHandler;
import org.hortonmachine.hmachine.HortonMachine;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The parameters panel.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ParametersPanel extends JPanel implements MouseListener {
    private static final long serialVersionUID = 1L;

    private static final String PM_VAR_NAME = "pm";

    private String[] rasterLayers;

    private String[] vectorLayers;

    private List<JTextField> eastingListeningFields = new ArrayList<JTextField>();
    private List<JTextField> northingListeningFields = new ArrayList<JTextField>();
    private LinkedHashMap<String, Object> fieldName2ValueHolderMap = new LinkedHashMap<String, Object>();
    private List<String> outputFieldNames = new ArrayList<String>();
    private List<JComboBox<String>> rasterComboList = new ArrayList<>();
    private List<JComboBox<String>> vectorComboList = new ArrayList<>();

    private ModuleDescription module;

    private GuiBridgeHandler guiBridge;

    private Class< ? > parentClass;

    public ParametersPanel( GuiBridgeHandler guiBridge ) {
        this.guiBridge = guiBridge;
    }

    public ModuleDescription getModule() {
        return module;
    }

    public HashMap<String, Object> getFieldName2ValueHolderMap() {
        return fieldName2ValueHolderMap;
    }

    public List<String> getOutputFieldNames() {
        return outputFieldNames;
    }

    public void setVectorRasterLayers( String[] vectorLayers, String[] rasterLayers ) {
        this.vectorLayers = vectorLayers;
        this.rasterLayers = rasterLayers;

        for( JComboBox<String> rasterCombo : rasterComboList ) {
            try {
                Object selectedRaster = rasterCombo.getSelectedItem();
                if (rasterLayers != null) {
                    String[] tmpRasterLayers = new String[rasterLayers.length + 1];
                    tmpRasterLayers[0] = "";
                    System.arraycopy(rasterLayers, 0, tmpRasterLayers, 1, rasterLayers.length);
                    rasterCombo.setModel(new DefaultComboBoxModel<String>(tmpRasterLayers));
                }
                if (selectedRaster != null) {
                    rasterCombo.setSelectedItem(selectedRaster);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for( JComboBox<String> vectorCombo : vectorComboList ) {
            try {
                Object selectedVector = vectorCombo.getSelectedItem();
                if (vectorLayers != null) {
                    String[] tmpVectorLayers = new String[vectorLayers.length + 1];
                    tmpVectorLayers[0] = "";
                    System.arraycopy(vectorLayers, 0, tmpVectorLayers, 1, vectorLayers.length);
                    vectorCombo.setModel(new DefaultComboBoxModel<String>(tmpVectorLayers));
                }
                if (selectedVector != null) {
                    vectorCombo.setSelectedItem(selectedVector);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void setModule( ModuleDescription module ) {
        this.module = module;
        clear();

        if (module == null) {
            return;
        }

        parentClass = getParentClass(module);

        final List<FieldData> inputsList = module.getInputsList();
        // final List<FieldData> outputsList = module.getOutputsList();
        int allRows = inputsList.size();// + outputsList.size();
        String rowsEnc = "";
        for( int i = 0; i < allRows; i++ ) {
            if (i == 0) {
                rowsEnc = "pref";
            } else
                rowsEnc = rowsEnc + ", 3dlu, max(50dlu;pref)";
        }
        // max(75dlu;pref)
        this.setLayout(new FormLayout("fill:250px:grow(.35), fill:5px, fill:250px:grow(.55)", rowsEnc));
        // this.setLayout(new FormLayout("left:pref, 2dlu, pref:grow", rowsEnc));

        addInputs(inputsList);

        // NO OUTPUTS AVAILABLE
        // for( FieldData outputField : outputsList ) {
        // String fieldLabel = null;
        // String fieldDescription = outputField.fieldDescription;
        // if (fieldDescription.length() > labelTrim) {
        // fieldLabel = fieldDescription.substring(0, labelTrim) + "...";
        // } else {
        // fieldLabel = fieldDescription;
        // }
        // String fieldTooltip = fieldDescription;
        // JLabel nameLabel = new JLabel(fieldLabel);
        // nameLabel.setToolTipText(fieldTooltip);
        // this.add(nameLabel, cc.xy(1, row));
        // JTextField f = new JTextField();
        // this.add(f, cc.xy(2, row));
        // row = row + 2;
        // }

    }

    private Class< ? > getParentClass( ModuleDescription module ) {
        Class< ? > parentOmsClass = null;
        try {
            Class< ? > moduleClass = module.getModuleClass();
            String simpleName = "Oms" + moduleClass.getSimpleName();
            Class< ? > pClass = HortonMachine.getInstance().moduleName2Class.get(simpleName);
            if (pClass == null) {
                pClass = JGrassGears.getInstance().moduleName2Class.get(simpleName);
            }
            if (pClass != null)
                parentOmsClass = pClass;
        } catch (Exception e) {
            // ignore and return null
            return null;
        }
        return parentOmsClass;
    }

    private void addInputs( final List<FieldData> inputsList ) {
        rasterComboList = new ArrayList<>();
        vectorComboList = new ArrayList<>();
        CellConstraints cc = new CellConstraints();
        int row = 1;
        for( FieldData inputField : inputsList ) {
            if (inputField.fieldName.equals(PM_VAR_NAME)) {
                continue;
            }
            String fieldDescription = inputField.fieldDescription;
            String fieldLabel = fieldDescription;
            String fieldTooltip = fieldDescription;

            JTextArea nameLabel = new JTextArea();
            nameLabel.setOpaque(false);
            nameLabel.setLineWrap(true);
            nameLabel.setWrapStyleWord(true);
            nameLabel.setText(fieldLabel);
            nameLabel.setToolTipText(fieldTooltip);
            nameLabel.setEditable(false);

            TypeCheck fileCheck = getFileCheck(inputField);
            if (fileCheck.isOutput) {
                Font font = nameLabel.getFont();
                Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
                nameLabel.setFont(boldFont);
            }
            this.add(nameLabel, cc.xy(1, row));

            // the rest of it
            int col = 3;

            if (isAtLeastOneAssignable(inputField.fieldType, String.class)) {
                if (inputField.guiHints != null && inputField.guiHints.startsWith(HMConstants.MULTILINE_UI_HINT)) {
                    handleTextArea(inputField, row, col, cc);
                } else if (inputField.guiHints != null && inputField.guiHints.startsWith(HMConstants.COMBO_UI_HINT)) {
                    handleComboField(inputField, row, col, cc);
                } else {
                    handleTextField(inputField, row, col, cc, false, fileCheck);
                }
            } else if (isAtLeastOneAssignable(inputField.fieldType, Double.class, double.class)) {
                handleTextField(inputField, row, col, cc, true, fileCheck);
            } else if (isAtLeastOneAssignable(inputField.fieldType, Float.class, float.class)) {
                handleTextField(inputField, row, col, cc, true, fileCheck);
            } else if (isAtLeastOneAssignable(inputField.fieldType, Integer.class, int.class)) {
                handleTextField(inputField, row, col, cc, true, fileCheck);
            } else if (isAtLeastOneAssignable(inputField.fieldType, Short.class, short.class)) {
                handleTextField(inputField, row, col, cc, true, fileCheck);
            } else if (isAtLeastOneAssignable(inputField.fieldType, Boolean.class, boolean.class)) {
                handleBooleanField(inputField, row, col, cc);
                // } else if (isAtLeastOneAssignable(inputField.fieldType, GridCoverage2D.class)) {
                // handleGridcoverageInputField(inputField, row, col, cc);
                // } else if (isAtLeastOneAssignable(inputField.fieldType, GridGeometry2D.class)) {
                // handleGridgeometryInputField(inputField, row, col, cc);
                // } else if (isAtLeastOneAssignable(inputField.fieldType,
                // SimpleFeatureCollection.class)) {
                // handleFeatureInputField(inputField, row, col, cc);
                // } else if (isAtLeastOneAssignable(inputField.fieldType, HashMap.class)) {
                // handleHashMapInputField(inputField, row, col, cc);
                // } else if (isAtLeastOneAssignable(inputField.fieldType, List.class)) {
                // if (inputField.guiHints != null &&
                // inputField.guiHints.equals(OmsBoxConstants.FILESPATHLIST_UI_HINT)) {
                // handleFilesPathListInputField(inputField, row, col, cc);
                // } else {
                // handleListInputField(inputField, row, col, cc);
            }

            row++;
            this.add(new JSeparator(JSeparator.HORIZONTAL), cc.xy(1, row));
            this.add(new JSeparator(JSeparator.HORIZONTAL), cc.xy(2, row));
            this.add(new JSeparator(JSeparator.HORIZONTAL), cc.xy(3, row));
            row++;

            // row = row + 3;
        }
    }

    private TypeCheck getFileCheck( FieldData inputField ) {
        TypeCheck f = new TypeCheck();
        String guiHints = inputField.guiHints;
        if (guiHints != null) {
            if (guiHints.contains(HMConstants.FILEIN_UI_HINT_GENERIC)) {
                f.isFile = true;
            } else if (guiHints.contains(HMConstants.FILEOUT_UI_HINT)) {
                f.isFile = true;
                f.isOutput = true;
            } else if (guiHints.contains(HMConstants.FOLDERIN_UI_HINT)) {
                f.isFile = true;
                f.isFolder = true;
            } else if (guiHints.contains(HMConstants.FOLDEROUT_UI_HINT)) {
                f.isFile = true;
                f.isFolder = true;
                f.isOutput = true;
            } else if (guiHints.contains(HMConstants.CRS_UI_HINT)) {
                f.isCrs = true;
            } else if (guiHints.contains(HMConstants.MAPCALC_UI_HINT)) {
                f.isMapcalc = true;
            } else if (guiHints.contains(SpatialToolboxConstants.GRASSFILE_UI_HINT)) {
                f.isGrassfile = true;
            } else if (guiHints.contains(HMConstants.PROCESS_NORTH_UI_HINT)) {
                f.isProcessingNorth = true;
            } else if (guiHints.contains(HMConstants.PROCESS_SOUTH_UI_HINT)) {
                f.isProcessingSouth = true;
            } else if (guiHints.contains(HMConstants.PROCESS_WEST_UI_HINT)) {
                f.isProcessingWest = true;
            } else if (guiHints.contains(HMConstants.PROCESS_EAST_UI_HINT)) {
                f.isProcessingEast = true;
            } else if (guiHints.contains(HMConstants.PROCESS_COLS_UI_HINT)) {
                f.isProcessingCols = true;
            } else if (guiHints.contains(HMConstants.PROCESS_ROWS_UI_HINT)) {
                f.isProcessingRows = true;
            } else if (guiHints.contains(HMConstants.PROCESS_XRES_UI_HINT)) {
                f.isProcessingXres = true;
            } else if (guiHints.contains(HMConstants.PROCESS_YRES_UI_HINT)) {
                f.isProcessingYres = true;
            } else if (guiHints.contains(HMConstants.NORTHING_UI_HINT)) {
                f.isNorthing = true;
            } else if (guiHints.contains(HMConstants.EASTING_UI_HINT)) {
                f.isEasting = true;
            } else if (guiHints.contains(HMConstants.EASTINGNORTHING_UI_HINT)) {
                f.isEastingNorthing = true;
            }
        }
        return f;
    }

    @SuppressWarnings("serial")
    private void handleTextField( FieldData inputField, int row, int col, CellConstraints cc, boolean onlyNumbers,
            TypeCheck typeCheck ) {
        String defaultFieldValue = inputField.fieldValue;
        if (!typeCheck.isFile) {
            JTextField textField;
//            if (!onlyNumbers) {
                textField = new JTextField();
//            } else {
//                NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US); // always use
//                                                                                       // dot as
//                                                                                       // comma
//                DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
//                decimalFormat.setGroupingUsed(false);
//                textField = new JFormattedTextField(decimalFormat);
//            }
            if (typeCheck.isEasting) {
                eastingListeningFields.add(textField);
                this.add(textField, cc.xy(col, row));
                fieldName2ValueHolderMap.put(inputField.fieldName, textField);
            } else if (typeCheck.isNorthing) {
                northingListeningFields.add(textField);
                this.add(textField, cc.xy(col, row));
                fieldName2ValueHolderMap.put(inputField.fieldName, textField);
            } else if (typeCheck.isCrs && guiBridge.supportsMapContext()) {
                JPanel subPanel = new JPanel();
                subPanel.setLayout(new BorderLayout());

                subPanel.add(textField, BorderLayout.CENTER);
                fieldName2ValueHolderMap.put(inputField.fieldName, textField);

                final JTextField fTextField = textField;
                JButton crsButton = new JButton("...");
                subPanel.add(crsButton, BorderLayout.EAST);
                crsButton.addActionListener(new ActionListener(){
                    public void actionPerformed( ActionEvent e ) {

                        String epsg = guiBridge.promptForCrs();
                        if (epsg != null) {
                            fTextField.setText(epsg);
                        }
                    }
                });
                this.add(subPanel, cc.xy(col, row));
            } else {
                this.add(textField, cc.xy(col, row));
                fieldName2ValueHolderMap.put(inputField.fieldName, textField);
            }
            textField.setText(defaultFieldValue);
        } else {

            boolean isVector = false;
            boolean isRaster = false;
            boolean isLas = false;

            String guiHints = inputField.guiHints;
            if (guiHints.contains(HMConstants.FILEIN_UI_HINT_RASTER)) {
                isRaster = true;
            } else if (guiHints.contains(HMConstants.FILEIN_UI_HINT_VECTOR)) {
                isVector = true;
            } else if (guiHints.contains(HMConstants.FILEIN_UI_HINT_LAS)) {
                isLas = true;
            } else if (guiHints.contains(HMConstants.FILEOUT_UI_HINT) && parentClass != null) {
                // TODO change all annotations as done for inputs to better check type
                try {
                    Field field = parentClass.getField(inputField.fieldName);
                    if (field != null) {
                        Class< ? > clazz = field.getType();
                        if (clazz.isAssignableFrom(GridCoverage2D.class)) {
                            isRaster = true;
                        } else if (clazz.isAssignableFrom(SimpleFeatureCollection.class)) {
                            isVector = true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (isVector && vectorLayers != null && !typeCheck.isOutput) {
                String[] tmpVectorLayers = new String[vectorLayers.length + 1];
                tmpVectorLayers[0] = "";
                System.arraycopy(vectorLayers, 0, tmpVectorLayers, 1, vectorLayers.length);
                JComboBox<String> comboBox = new JComboBox<String>(tmpVectorLayers);
                this.add(comboBox, cc.xy(col, row));
                vectorComboList.add(comboBox);
                fieldName2ValueHolderMap.put(inputField.fieldName, comboBox);
            } else if (isRaster && rasterLayers != null && !typeCheck.isOutput) {
                String[] tmpRasterLayers = new String[rasterLayers.length + 1];
                tmpRasterLayers[0] = "";
                System.arraycopy(rasterLayers, 0, tmpRasterLayers, 1, rasterLayers.length);
                JComboBox<String> comboBox = new JComboBox<String>(tmpRasterLayers);
                this.add(comboBox, cc.xy(col, row));
                rasterComboList.add(comboBox);
                fieldName2ValueHolderMap.put(inputField.fieldName, comboBox);
            } else {
                JPanel subPanel = new JPanel();
                this.add(subPanel, cc.xy(col, row));
                subPanel.setLayout(new BorderLayout());

                final JTextField textField = new JTextField();
                subPanel.add(textField, BorderLayout.CENTER);
                fieldName2ValueHolderMap.put(inputField.fieldName, textField);

                JButton browseButton = new JButton("...");
                subPanel.add(browseButton, BorderLayout.EAST);

                FileFilter fileFilter = null;
                if (isRaster) {
                    fileFilter = HMConstants.rasterFileFilter;
                } else if (isVector) {
                    fileFilter = HMConstants.vectorFileFilter;
                } else if (isLas) {
                    fileFilter = HMConstants.lasFileFilter;
                }
                FileFilter _fileFilter = fileFilter;

                if (!typeCheck.isFolder && !typeCheck.isOutput) {
                    // input file
                    browseButton.addActionListener(new ActionListener(){
                        public void actionPerformed( ActionEvent e ) {
                            File[] files = guiBridge.showOpenFileDialog("Select input file", PreferencesHandler.getLastFile(),
                                    _fileFilter);
                            setSelectedFile(textField, files);
                        }

                    });
                } else if (!typeCheck.isFolder && typeCheck.isOutput) {
                    outputFieldNames.add(inputField.fieldName);
                    // output file
                    browseButton.addActionListener(new ActionListener(){
                        public void actionPerformed( ActionEvent e ) {
                            File[] files = guiBridge.showSaveFileDialog("Select file to save", PreferencesHandler.getLastFile(),
                                    _fileFilter);
                            setSelectedFile(textField, files);
                        }
                    });
                } else if (typeCheck.isFolder && !typeCheck.isOutput) {
                    // input folder
                    browseButton.addActionListener(new ActionListener(){
                        public void actionPerformed( ActionEvent e ) {
                            File[] files = guiBridge.showOpenDirectoryDialog("Select folder", PreferencesHandler.getLastFile());
                            setSelectedFile(textField, files);
                        }
                    });
                } else if (typeCheck.isFolder && typeCheck.isOutput) {
                    outputFieldNames.add(inputField.fieldName);
                    // output folder
                    browseButton.addActionListener(new ActionListener(){
                        public void actionPerformed( ActionEvent e ) {
                            File[] files = guiBridge.showOpenDirectoryDialog("Select folder", PreferencesHandler.getLastFile());
                            setSelectedFile(textField, files);
                        }
                    });
                }

                textField.setText(defaultFieldValue);
                textField.setDropTarget(new DropTarget(){
                    public synchronized void drop( DropTargetDropEvent evt ) {
                        try {
                            evt.acceptDrop(DnDConstants.ACTION_COPY);
                            @SuppressWarnings("unchecked")
                            List<File> droppedFiles = (List<File>) evt.getTransferable()
                                    .getTransferData(DataFlavor.javaFileListFlavor);
                            for( File file : droppedFiles ) {
                                textField.setText(file.getAbsolutePath());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }

        }
    }

    private void setSelectedFile( final JTextField textField, File[] files ) {
        if (files != null && files.length > 0) {
            final File gpapFile = files[0];
            PreferencesHandler.setLastPath(gpapFile.getAbsolutePath());

            textField.setText(gpapFile.getAbsolutePath());
        }
    }

    private void handleTextArea( FieldData inputField, int row, int col, CellConstraints cc ) {
        String hint = extractSingleGuiHint(HMConstants.MULTILINE_UI_HINT, inputField.guiHints);
        String rowsStr = hint.replaceFirst(HMConstants.MULTILINE_UI_HINT, "");
        int areaRows = Integer.parseInt(rowsStr);

        JTextArea textArea = new JTextArea();
        textArea.setRows(areaRows);
        this.add(textArea, cc.xy(col, row));
        fieldName2ValueHolderMap.put(inputField.fieldName, textArea);
        textArea.setText(inputField.fieldValue);
    }

    private void handleBooleanField( FieldData inputField, int row, int col, CellConstraints cc ) {
        JCheckBox checkBox = new JCheckBox("");
        this.add(checkBox, cc.xy(col, row));
        fieldName2ValueHolderMap.put(inputField.fieldName, checkBox);

        boolean select = inputField.fieldValue.equalsIgnoreCase("true") ? true : false;
        checkBox.setSelected(select);
    }

    private void handleComboField( FieldData inputField, int row, int col, CellConstraints cc ) {
        String[] guiHintsSplit = inputField.guiHints.split(";");
        String[] imtemsSplit = new String[]{" - "};
        for( String guiHint : guiHintsSplit ) {
            if (guiHint.startsWith(HMConstants.COMBO_UI_HINT)) {
                String items = guiHint.replaceFirst(HMConstants.COMBO_UI_HINT, "").replaceFirst(":", "").trim();
                imtemsSplit = items.split(",");
                break;
            }
        }
        JComboBox<String> comboBox = new JComboBox<String>(imtemsSplit);
        this.add(comboBox, cc.xy(col, row));
        fieldName2ValueHolderMap.put(inputField.fieldName, comboBox);

        if (inputField.fieldValue.length() > 0) {
            comboBox.setSelectedItem(inputField.fieldValue);
        }
    }

    /**
     * Checks if one class is assignable from at least one of the others.
     * 
     * @param main the canonical name of class to check.
     * @param classes the other classes.
     * @return true if at least one of the other classes match.
     */
    private boolean isAtLeastOneAssignable( String main, Class< ? >... classes ) {
        for( Class< ? > clazz : classes ) {
            if (clazz.getCanonicalName().equals(main)) {
                return true;
            }
        }
        return false;
    }

    private String extractSingleGuiHint( String pattern, String guiHints ) {
        String[] split = guiHints.split(",");
        for( String hint : split ) {
            hint = hint.trim();
            if (hint.contains(pattern)) {
                return hint;
            }
        }
        return null;
    }

    public void clear() {
        this.removeAll();
        eastingListeningFields.clear();
        northingListeningFields.clear();
        fieldName2ValueHolderMap.clear();
        outputFieldNames.clear();
        rasterComboList.clear();
        vectorComboList.clear();
    }

    public void freeResources() {

    }

    public void mouseClicked( MouseEvent e ) {
        if (!guiBridge.supportsMapContext()) {
            return;
        }
        int x = e.getX();
        int y = e.getY();

        Point2D mapPoint = guiBridge.getWorldPoint(x, y);
        if (mapPoint != null) {
            for( JTextField textField : eastingListeningFields ) {
                textField.setText("" + mapPoint.getX());
            }
            for( JTextField textField : northingListeningFields ) {
                textField.setText("" + mapPoint.getY());
            }
        }
    }

    public void mousePressed( MouseEvent e ) {
    }

    public void mouseReleased( MouseEvent e ) {
    }

    public void mouseEntered( MouseEvent e ) {
    }

    public void mouseExited( MouseEvent e ) {
    }

}
