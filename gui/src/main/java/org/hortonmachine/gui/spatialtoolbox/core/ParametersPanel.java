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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultCaret;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModelRegistry;
import org.hortonmachine.gears.libs.modules.HMParameterKind;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.utils.GuiBridgeHandler;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * The parameters panel.
 *
 * <p>The fields of the module are grouped in collapsible sections of inputs, parameters and outputs
 * (see {@link HMParameterKind}). Each field has its description above the editor and, if available,
 * its unit and range next to it.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ParametersPanel extends JPanel implements MouseListener, Scrollable {
    private static final long serialVersionUID = 1L;

    private static final String PM_VAR_NAME = "pm";

    private static final int SECTION_GAP = 12;

    private String[] rasterLayers;

    private String[] vectorLayers;

    private List<JTextField> eastingListeningFields = new ArrayList<JTextField>();
    private List<JTextField> northingListeningFields = new ArrayList<JTextField>();
    private LinkedHashMap<String, Object> fieldName2ValueHolderMap = new LinkedHashMap<String, Object>();
    private List<String> outputFieldNames = new ArrayList<String>();
    private List<JComboBox<String>> rasterComboList = new ArrayList<>();
    private List<JComboBox<String>> vectorComboList = new ArrayList<>();

    /**
     * Output file fields, with the kind of data written (raster/vector), if known.
     */
    private Map<JTextField, String> outputFileFields = new LinkedHashMap<>();
    /**
     * The output names last suggested, to only replace names the user didn't change.
     */
    private Map<JTextField, String> suggestedOutputNames = new HashMap<>();

    private List<JTextArea> wrappingLabels = new ArrayList<>();
    private int lastLayoutWidth = -1;

    private ModuleDescription module;

    private GuiBridgeHandler guiBridge;

    private Class< ? > parentClass;

    public ParametersPanel( GuiBridgeHandler guiBridge ) {
        this.guiBridge = guiBridge;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
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
                    rasterCombo.setModel(new DefaultComboBoxModel<String>(withEmptyItem(rasterLayers)));
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
                    vectorCombo.setModel(new DefaultComboBoxModel<String>(withEmptyItem(vectorLayers)));
                }
                if (selectedVector != null) {
                    vectorCombo.setSelectedItem(selectedVector);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static String[] withEmptyItem( String[] items ) {
        String[] tmp = new String[items.length + 1];
        tmp[0] = "";
        System.arraycopy(items, 0, tmp, 1, items.length);
        return tmp;
    }

    public void setModule( ModuleDescription module ) {
        this.module = module;
        clear();

        if (module != null) {
            parentClass = getParentClass(module);

            Map<HMParameterKind, List<FieldData>> kind2Fields = new EnumMap<>(HMParameterKind.class);
            for( FieldData inputField : module.getInputsList() ) {
                if (inputField.fieldName.equals(PM_VAR_NAME)) {
                    continue;
                }
                HMParameterKind kind = HMParameterKind.of(true, false, inputField.guiHints);
                kind2Fields.computeIfAbsent(kind, k -> new ArrayList<>()).add(inputField);
            }
            for( HMParameterKind kind : HMParameterKind.values() ) {
                List<FieldData> fields = kind2Fields.get(kind);
                if (fields != null) {
                    if (getComponentCount() > 0) {
                        add(Box.createVerticalStrut(SECTION_GAP));
                    }
                    add(new Section(kind.getTitle(), createFieldsPanel(fields)));
                }
            }
            add(Box.createVerticalGlue());
            installOutputNameSuggestions(kind2Fields.get(HMParameterKind.INPUT));
        }
        revalidate();
        repaint();
    }

    /**
     * The height of the wrapping descriptions depends on their width, which is known only after
     * a layout: when the width changes, measure them again in a second layout.
     */
    @Override
    public void doLayout() {
        super.doLayout();
        if (getWidth() != lastLayoutWidth) {
            lastLayoutWidth = getWidth();
            SwingUtilities.invokeLater(() -> {
                for( JTextArea label : wrappingLabels ) {
                    label.invalidate();
                }
                revalidate();
                repaint();
            });
        }
    }

    private Class< ? > getParentClass( ModuleDescription module ) {
        Class< ? > parentOmsClass = null;
        try {
            Class< ? > moduleClass = module.getModuleClass();
            String simpleName = "Oms" + moduleClass.getSimpleName();
            Class< ? > pClass = HMModelRegistry.getModelClass(simpleName);
            if (pClass != null)
                parentOmsClass = pClass;
        } catch (Exception e) {
            // ignore and return null
            return null;
        }
        return parentOmsClass;
    }

    /**
     * Create the panel with the description and editor of each field.
     */
    private JPanel createFieldsPanel( List<FieldData> fields ) {
        StringBuilder rows = new StringBuilder();
        for( int i = 0; i < fields.size(); i++ ) {
            if (i > 0) {
                rows.append(", 8dlu, ");
            }
            if (isBoolean(fields.get(i))) {
                rows.append("pref");
            } else {
                rows.append("pref, 2dlu, pref");
            }
        }
        JPanel panel = new JPanel(new FormLayout("fill:0px:grow, 4dlu, pref", rows.toString()));
        panel.setOpaque(false);
        CellConstraints cc = new CellConstraints();
        int row = 1;
        for( FieldData field : fields ) {
            String tooltip = "<html><b>" + field.fieldName + "</b><br>" + field.fieldDescription + "</html>";
            if (isBoolean(field)) {
                JCheckBox checkBox = handleBooleanField(field);
                checkBox.setToolTipText(tooltip);
                JTextArea label = createWrappingLabel(field.fieldDescription);
                label.setToolTipText(tooltip);
                label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                label.addMouseListener(new MouseAdapter(){
                    @Override
                    public void mouseClicked( MouseEvent e ) {
                        checkBox.doClick();
                    }
                });
                JPanel checkPanel = new JPanel(new BorderLayout(4, 0));
                checkPanel.setOpaque(false);
                checkPanel.add(checkBox, BorderLayout.WEST);
                checkPanel.add(label, BorderLayout.CENTER);
                panel.add(checkPanel, cc.xyw(1, row, 3));
                row += 2;
            } else {
                JTextArea label = createWrappingLabel(field.fieldDescription);
                label.setToolTipText(tooltip);
                panel.add(label, cc.xyw(1, row, 3));
                row += 2;

                JComponent editor = createEditor(field);
                if (editor != null) {
                    editor.setToolTipText(tooltip);
                    panel.add(editor, cc.xy(1, row));
                }
                String suffix = getSuffix(field);
                if (suffix != null) {
                    JLabel suffixLabel = new JLabel(suffix);
                    suffixLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                    panel.add(suffixLabel, cc.xy(3, row));
                }
                row += 2;
            }
        }
        return panel;
    }

    private static String getSuffix( FieldData field ) {
        if (field.unit == null && field.range == null) {
            return null;
        }
        if (field.unit == null) {
            return field.range;
        }
        if (field.range == null) {
            return field.unit;
        }
        return field.unit + "  " + field.range;
    }

    private boolean isBoolean( FieldData field ) {
        return isAtLeastOneAssignable(field.fieldType, Boolean.class, boolean.class);
    }

    private JTextArea createWrappingLabel( String text ) {
        JTextArea label = new JTextArea();
        // else the caret scrolls the parameters to the last label
        ((DefaultCaret) label.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        label.setText(text);
        wrappingLabels.add(label);
        label.setEditable(false);
        label.setFocusable(false);
        label.setOpaque(false);
        label.setLineWrap(true);
        label.setWrapStyleWord(true);
        label.setBorder(BorderFactory.createEmptyBorder());
        label.setFont(UIManager.getFont("Label.font"));
        label.setForeground(UIManager.getColor("Label.foreground"));
        return label;
    }

    private JComponent createEditor( FieldData inputField ) {
        TypeCheck fileCheck = getFileCheck(inputField);
        if (isAtLeastOneAssignable(inputField.fieldType, String.class)) {
            if (inputField.guiHints != null && inputField.guiHints.startsWith(HMConstants.MULTILINE_UI_HINT)) {
                return handleTextArea(inputField);
            } else if (inputField.guiHints != null && inputField.guiHints.startsWith(HMConstants.COMBO_UI_HINT)) {
                return handleComboField(inputField);
            } else {
                return handleTextField(inputField, fileCheck);
            }
        } else if (isAtLeastOneAssignable(inputField.fieldType, Double.class, double.class, Float.class, float.class,
                Integer.class, int.class, Short.class, short.class)) {
            return handleTextField(inputField, fileCheck);
        }
        return null;
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
    private JComponent handleTextField( FieldData inputField, TypeCheck typeCheck ) {
        String defaultFieldValue = inputField.fieldValue;
        if (!typeCheck.isFile) {
            JTextField textField = new JTextField();
            textField.setText(defaultFieldValue);
            fieldName2ValueHolderMap.put(inputField.fieldName, textField);
            if (typeCheck.isEasting) {
                eastingListeningFields.add(textField);
            } else if (typeCheck.isNorthing) {
                northingListeningFields.add(textField);
            } else if (typeCheck.isCrs && guiBridge.supportsMapContext()) {
                JButton crsButton = new JButton("...");
                crsButton.addActionListener(e -> {
                    String epsg = guiBridge.promptForCrs();
                    if (epsg != null) {
                        textField.setText(epsg);
                    }
                });
                return withButton(textField, crsButton);
            }
            return textField;
        }

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
                Class< ? > clazz = field.getType();
                if (clazz.isAssignableFrom(GridCoverage2D.class)) {
                    isRaster = true;
                } else if (clazz.isAssignableFrom(SimpleFeatureCollection.class)) {
                    isVector = true;
                }
            } catch (NoSuchFieldException e) {
                // the wrapper field has no counterpart in the parent class
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (isVector && vectorLayers != null && !typeCheck.isOutput) {
            JComboBox<String> comboBox = new JComboBox<String>(withEmptyItem(vectorLayers));
            vectorComboList.add(comboBox);
            fieldName2ValueHolderMap.put(inputField.fieldName, comboBox);
            return comboBox;
        } else if (isRaster && rasterLayers != null && !typeCheck.isOutput) {
            JComboBox<String> comboBox = new JComboBox<String>(withEmptyItem(rasterLayers));
            rasterComboList.add(comboBox);
            fieldName2ValueHolderMap.put(inputField.fieldName, comboBox);
            return comboBox;
        }

        final JTextField textField = new JTextField();
        fieldName2ValueHolderMap.put(inputField.fieldName, textField);
        JButton browseButton = new JButton("...");

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
            browseButton.setToolTipText("Select input file");
            browseButton.addActionListener(e -> {
                File[] files = guiBridge.showOpenFileDialog("Select input file", PreferencesHandler.getLastFile(),
                        _fileFilter);
                setSelectedFile(textField, files);
            });
        } else if (!typeCheck.isFolder && typeCheck.isOutput) {
            outputFieldNames.add(inputField.fieldName);
            outputFileFields.put(textField, isRaster ? "raster" : isVector ? "vector" : null);
            browseButton.setToolTipText("Select file to save");
            browseButton.addActionListener(e -> {
                File[] files = guiBridge.showSaveFileDialog("Select file to save", PreferencesHandler.getLastFile(),
                        _fileFilter);
                setSelectedFile(textField, files);
            });
        } else {
            if (typeCheck.isOutput) {
                outputFieldNames.add(inputField.fieldName);
            }
            browseButton.setToolTipText("Select folder");
            browseButton.addActionListener(e -> {
                File[] files = guiBridge.showOpenDirectoryDialog("Select folder", PreferencesHandler.getLastFile());
                setSelectedFile(textField, files);
            });
        }

        textField.setText(defaultFieldValue);
        textField.setDropTarget(new DropTarget(){
            public synchronized void drop( DropTargetDropEvent evt ) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for( File file : droppedFiles ) {
                        textField.setText(file.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        return withButton(textField, browseButton);
    }

    private static JPanel withButton( JComponent field, JButton button ) {
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.setOpaque(false);
        panel.add(field, BorderLayout.CENTER);
        panel.add(button, BorderLayout.EAST);
        return panel;
    }

    private void setSelectedFile( final JTextField textField, File[] files ) {
        if (files != null && files.length > 0) {
            final File gpapFile = files[0];
            PreferencesHandler.setLastPath(gpapFile.getAbsolutePath());

            textField.setText(gpapFile.getAbsolutePath());
        }
    }

    /**
     * When the first input file changes, suggest names for the empty output files of the same kind of data,
     * in the same folder and with the same extension. Input names like <code>pit_flanginec</code> get the kind
     * of data replaced by the output name (<code>slope_flanginec</code>), others get it as prefix.
     */
    private void installOutputNameSuggestions( List<FieldData> inputFields ) {
        if (inputFields == null || outputFileFields.isEmpty()) {
            return;
        }
        for( FieldData inputField : inputFields ) {
            String hints = inputField.guiHints;
            String dataKind = hints.contains(HMConstants.FILEIN_UI_HINT_RASTER)
                    ? "raster"
                    : hints.contains(HMConstants.FILEIN_UI_HINT_VECTOR) ? "vector" : null;
            Object holder = fieldName2ValueHolderMap.get(inputField.fieldName);
            if (dataKind == null || !(holder instanceof JTextField)) {
                continue;
            }
            JTextField inputTextField = (JTextField) holder;
            inputTextField.getDocument().addDocumentListener(new DocumentListener(){
                public void insertUpdate( DocumentEvent e ) {
                    suggestOutputNames(inputTextField.getText(), dataKind);
                }
                public void removeUpdate( DocumentEvent e ) {
                    suggestOutputNames(inputTextField.getText(), dataKind);
                }
                public void changedUpdate( DocumentEvent e ) {
                }
            });
            // only the first input file drives the suggestions
            break;
        }
    }

    private void suggestOutputNames( String inputPath, String dataKind ) {
        File inputFile = new File(inputPath.trim());
        String inputName = inputFile.getName();
        int dotIndex = inputName.lastIndexOf('.');
        if (inputPath.isBlank() || inputFile.getParentFile() == null || dotIndex <= 0) {
            return;
        }
        String stem = inputName.substring(0, dotIndex);
        String extension = inputName.substring(dotIndex);
        for( Map.Entry<JTextField, String> entry : outputFileFields.entrySet() ) {
            JTextField outputField = entry.getKey();
            if (!dataKind.equals(entry.getValue())) {
                continue;
            }
            String current = outputField.getText().trim();
            String lastSuggestion = suggestedOutputNames.get(outputField);
            if (!current.isEmpty() && !current.equals(lastSuggestion)) {
                // the user chose a name
                continue;
            }
            String outputName = getOutputName(outputField);
            // names like pit_flanginec: replace the kind of data, as in slope_flanginec
            int underscoreIndex = stem.indexOf('_');
            File suggestedFile = new File(inputFile.getParentFile(),
                    outputName + (underscoreIndex > 0 ? stem.substring(underscoreIndex) : "_" + stem) + extension);
            if (suggestedFile.getAbsoluteFile().equals(inputFile.getAbsoluteFile())) {
                // never suggest to overwrite the input
                suggestedFile = new File(inputFile.getParentFile(), outputName + "_" + stem + extension);
            }
            String suggestion = suggestedFile.getAbsolutePath();
            suggestedOutputNames.put(outputField, suggestion);
            outputField.setText(suggestion);
        }
    }

    /**
     * @return the name of the output from its field name, ex. outPit -> pit.
     */
    private String getOutputName( JTextField outputField ) {
        for( Map.Entry<String, Object> entry : fieldName2ValueHolderMap.entrySet() ) {
            if (entry.getValue() == outputField) {
                String name = entry.getKey().replaceFirst("^out", "");
                if (name.isEmpty()) {
                    return "out";
                }
                return Character.toLowerCase(name.charAt(0)) + name.substring(1);
            }
        }
        return "out";
    }

    private JComponent handleTextArea( FieldData inputField ) {
        String hint = extractSingleGuiHint(HMConstants.MULTILINE_UI_HINT, inputField.guiHints);
        String rowsStr = hint.replaceFirst(HMConstants.MULTILINE_UI_HINT, "");
        int areaRows = Integer.parseInt(rowsStr);

        JTextArea textArea = new JTextArea();
        textArea.setRows(areaRows);
        fieldName2ValueHolderMap.put(inputField.fieldName, textArea);
        textArea.setText(inputField.fieldValue);
        return new JScrollPane(textArea);
    }

    private JCheckBox handleBooleanField( FieldData inputField ) {
        JCheckBox checkBox = new JCheckBox("");
        fieldName2ValueHolderMap.put(inputField.fieldName, checkBox);

        boolean select = inputField.fieldValue.equalsIgnoreCase("true") ? true : false;
        checkBox.setSelected(select);
        return checkBox;
    }

    private JComponent handleComboField( FieldData inputField ) {
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
        fieldName2ValueHolderMap.put(inputField.fieldName, comboBox);

        if (inputField.fieldValue.length() > 0) {
            comboBox.setSelectedItem(inputField.fieldValue);
        }
        return comboBox;
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
        wrappingLabels.clear();
        lastLayoutWidth = -1;
        eastingListeningFields.clear();
        northingListeningFields.clear();
        fieldName2ValueHolderMap.clear();
        outputFieldNames.clear();
        rasterComboList.clear();
        vectorComboList.clear();
        outputFileFields.clear();
        suggestedOutputNames.clear();
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

    // the panel follows the width of the scrollpane, so that the descriptions wrap
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement( Rectangle visibleRect, int orientation, int direction ) {
        return 16;
    }

    public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ) {
        return Math.max(visibleRect.height - 32, 16);
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /**
     * A section with a clickable title, that shows and hides its content.
     */
    private static class Section extends JPanel {
        private static final long serialVersionUID = 1L;

        Section( String title, JComponent content ) {
            super(new BorderLayout(0, 6));
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);

            JLabel titleLabel = new JLabel();
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() + 1f));
            titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
            titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            titleLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")),
                    BorderFactory.createEmptyBorder(0, 0, 3, 0)));
            setTitle(titleLabel, title, true);
            titleLabel.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked( MouseEvent e ) {
                    content.setVisible(!content.isVisible());
                    setTitle(titleLabel, title, content.isVisible());
                    revalidate();
                }
            });
            content.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
            add(titleLabel, BorderLayout.NORTH);
            add(content, BorderLayout.CENTER);
        }

        private static void setTitle( JLabel label, String title, boolean expanded ) {
            label.setText((expanded ? "▾ " : "▸ ") + title);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }
}
