package org.hortonmachine.gforms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.font.TextAttribute;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.hortonmachine.gears.io.geopaparazzi.forms.Form;
import org.hortonmachine.gears.io.geopaparazzi.forms.Section;
import org.hortonmachine.gears.io.geopaparazzi.forms.Utilities;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemBoolean;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemCombo;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemConnectedCombo;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemDate;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemDouble;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemDynamicText;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemImagelib;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemInteger;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemLabel;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemMap;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemOneToManyConnectedCombo;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemPicture;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemSketch;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemText;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemTime;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gui.settings.SettingsController;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.ImageCache;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings({"unchecked", "serial", "rawtypes"})
public class FormBuilderController extends FormBuilderView implements IOnCloseListener, ChangeListener {
    private static final String SUFFIX = "_tags.json";
    private static final String OMCOMBO_DOESNT_EXIST = "The type " + ItemOneToManyConnectedCombo.TYPE + " is not supported yet.";
    private static final String CCOMBO_DOESNT_EXIST = "The type " + ItemConnectedCombo.TYPE + " is not supported yet.";
    private static final String FIRST_OPEN_FILE_PROMPT = "Please first open a form file or create a new one.";
    private static final String LABELCOLOR = "#000000";
    private static final int DEFAULTWIDTH = 500;
    private IFormHandler formHandler;
    private LinkedHashMap<String, JSONObject> selectedSectionsMap;
    private String currentSelectedSectionName;
    private JSONObject currentSelectedSectionObject;
    private String currentSelectedFormName;
    private ComponentOrientation co;

    private static enum IMAGEWIDGET {
        PICTURE, IMAGELIB, SKETCH, MAP
    };

    public FormBuilderController( IFormHandler formHandler ) throws Exception {
        setPreferredSize(new Dimension(1000, 800));

        if (formHandler != null) {
            this.formHandler = formHandler;
        }

        init();
    }

    public boolean canCloseWithoutPrompt() {
        return formHandler == null;
    }

    private void init() throws Exception {
        _filePathtext.setEditable(false);
        if (formHandler != null) {
            _filePathtext.setText(formHandler.getLabel());
        }

        co = PreferencesHandler.getComponentOrientation();
        _buttonsTabPane.setTabPlacement(co.isLeftToRight() ? JTabbedPane.LEFT : JTabbedPane.RIGHT);
        _buttonsTabPane.addChangeListener(this);
        _buttonsTabPane.setBounds(20, 20, 500, 500);

        if (formHandler != null && !formHandler.isFileBased()) {
            _browseButton.setEnabled(false);
            _newButton.setEnabled(false);
        } else {
            _browseButton.setEnabled(true);
            _browseButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setMultiSelectionEnabled(false);
                FileFilter fileFilter = new FileFilter(){

                    @Override
                    public String getDescription() {
                        return "*_tags.json";
                    }

                    @Override
                    public boolean accept( File f ) {
                        return f.isDirectory() || f.getName().endsWith(SUFFIX);
                    }
                };
                fileChooser.setFileFilter(fileFilter);
                fileChooser.setCurrentDirectory(PreferencesHandler.getLastFile());
                int result = fileChooser.showOpenDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (selectedFile != null) {
                        PreferencesHandler.setLastPath(selectedFile.getAbsolutePath());
                        try {
                            formHandler = new FileFormHandler(selectedFile);

                            openTagsFile();
                            _filePathtext.setText(formHandler.getLabel());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    formHandler = null;
                }
            });

            _newButton.setText("new");
            _newButton.addActionListener(e -> {
                File createdFile = GuiUtilities.showSaveFileDialog(this, "Create new tags file",
                        PreferencesHandler.getLastFile());
                try {
                    if (createdFile != null) {
                        String absolutePath = createdFile.getAbsolutePath();
                        if (!absolutePath.endsWith(SUFFIX)) {
                            absolutePath += SUFFIX;
                            createdFile = new File(absolutePath);
                        }
                        formHandler = new FileFormHandler(createdFile);
                        formHandler.saveForm("[]");
                        PreferencesHandler.setLastPath(absolutePath);
                        try {
                            _buttonsTabPane.removeAll();
                            openTagsFile();
                            _filePathtext.setText(formHandler.getLabel());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            });
        }

        _addSectionButton.setText("add");
        _addSectionButton.addActionListener(e -> {
            if (selectedSectionsMap == null) {
                GuiUtilities.showInfoMessage(this, FIRST_OPEN_FILE_PROMPT);
                return;
            }
            String newSectionName = GuiUtilities.showInputDialog(this, "Enter new section name", "new section");

            JSONObject sectionObj = selectedSectionsMap.get(newSectionName);
            if (sectionObj != null) {
                GuiUtilities.showWarningMessage(this, "The inserted section name already exists!");
                return;
            }

            Section newSection = new Section(newSectionName);

            JSONObject sectionJson = new JSONObject(newSection.toString());
            selectedSectionsMap.put(newSectionName, sectionJson);

            JSONArray rootArray = Utilities.formsRootFromSectionsMap(selectedSectionsMap);
            String rootString = rootArray.toString(2);
            try {
                formHandler.saveForm(rootString);
                openTagsFile();
                _buttonsCombo.setSelectedItem(newSectionName);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        _deleteSectionButton.setText("del");
        _deleteSectionButton.addActionListener(e -> {
            if (selectedSectionsMap == null) {
                GuiUtilities.showInfoMessage(this, FIRST_OPEN_FILE_PROMPT);
                return;
            }
            String sectionToDelete = _buttonsCombo.getSelectedItem().toString();
            boolean doDelete = GuiUtilities.showYesNoDialog(this, "Are you sure you want to delete: " + sectionToDelete);

            if (doDelete) {
                selectedSectionsMap.remove(sectionToDelete);
                JSONArray rootArray = Utilities.formsRootFromSectionsMap(selectedSectionsMap);
                String rootString = rootArray.toString(2);
                try {
                    formHandler.saveForm(rootString);
                    openTagsFile();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        _addFormButton.setText("add");
        _addFormButton.addActionListener(e -> {
            if (selectedSectionsMap == null) {
                GuiUtilities.showInfoMessage(this, FIRST_OPEN_FILE_PROMPT);
                return;
            }
            String newFormName = GuiUtilities.showInputDialog(this, "Enter new form name", "new form");
            if (newFormName == null) {
                return;
            }
            JSONObject sectionObject = selectedSectionsMap.get(currentSelectedSectionName);
            List<String> formNames = Utilities.getFormNames4Section(sectionObject);

            if (formNames.contains(newFormName)) {
                GuiUtilities.showWarningMessage(this, "The inserted form name already exists!");
                return;
            }

            Form newForm = new Form(newFormName);

            JSONObject formJson = new JSONObject(newForm.toString());
            JSONArray formsArray = sectionObject.getJSONArray(Utilities.ATTR_FORMS);
            formsArray.put(formJson);

            selectedSectionsMap.put(currentSelectedSectionName, sectionObject);
            currentSelectedSectionObject = sectionObject;

            JSONArray rootArray = Utilities.formsRootFromSectionsMap(selectedSectionsMap);
            String rootString = rootArray.toString(2);
            try {
                formHandler.saveForm(rootString);
                reloadFormTab(newFormName);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        _deleteFormButton.setText("del");
        _deleteFormButton.addActionListener(e -> {
            if (selectedSectionsMap == null) {
                GuiUtilities.showInfoMessage(this, FIRST_OPEN_FILE_PROMPT);
                return;
            }
            if (currentSelectedFormName == null) {
                return;
            }
            boolean doDelete = GuiUtilities.showYesNoDialog(this, "Are you sure you want to delete: " + currentSelectedFormName);
            if (doDelete) {
                Utilities.removeFormFromSection(currentSelectedFormName, currentSelectedSectionObject);

                selectedSectionsMap.put(currentSelectedSectionName, currentSelectedSectionObject);
                JSONArray rootArray = Utilities.formsRootFromSectionsMap(selectedSectionsMap);
                String rootString = rootArray.toString(2);

                try {
                    formHandler.saveForm(rootString);
                    loadSection(currentSelectedSectionName);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        _addWidgetButton.setText("add");
        _addWidgetButton.addActionListener(e -> {
            if (selectedSectionsMap == null) {
                GuiUtilities.showInfoMessage(this, FIRST_OPEN_FILE_PROMPT);
                return;
            }
            if (currentSelectedFormName == null) {
                return;
            }
            addNewWidget(_widgetsCombo.getSelectedItem().toString());
        });
        _deleteWidgetButton.setText("del");
        _deleteWidgetButton.addActionListener(e -> {
            if (selectedSectionsMap == null) {
                GuiUtilities.showInfoMessage(this, FIRST_OPEN_FILE_PROMPT);
                return;
            }
            if (currentSelectedFormName == null) {
                return;
            }

            Object selectedItem = _loadedWidgetsCombo.getSelectedItem();
            if (selectedItem instanceof String) {
                String widgetNameToRemove = (String) selectedItem;

                boolean doDelete = GuiUtilities.showYesNoDialog(this, "Are you sure you want to delete: " + widgetNameToRemove);
                if (doDelete) {
                    JSONObject form4Name = Utilities.getForm4Name(currentSelectedFormName, currentSelectedSectionObject);
                    JSONArray formItems = Utilities.getFormItems(form4Name);
                    int length = formItems.length();
                    if (length == 0) {
                        return;
                    }
                    int indexToRemove = -1;
                    for( int i = 0; i < length; i++ ) {
                        JSONObject jsonObject = formItems.getJSONObject(i);
                        String name = null;
                        if (jsonObject.has(Utilities.TAG_KEY)) {
                            name = jsonObject.getString(Utilities.TAG_KEY).trim();
                        } else if (jsonObject.has(Utilities.TAG_VALUE)) {
                            name = jsonObject.getString(Utilities.TAG_VALUE).trim();
                        }
                        if (name != null && name.equals(widgetNameToRemove)) {
                            indexToRemove = i;
                            break;
                        }
                    }

                    if (indexToRemove != -1) {
                        formItems.remove(indexToRemove);

                        rewriteFileAndLoadCurrentTab();
                    }
                }
            }

        });

        _buttonsCombo.addActionListener(e -> {
            currentSelectedSectionName = _buttonsCombo.getSelectedItem().toString();
            loadSection(currentSelectedSectionName);
        });

        List<String> widgetNames = Arrays.asList(Utilities.ITEM_NAMES).stream().filter(name -> {
            boolean isUnsupported = name.equals(ItemConnectedCombo.TYPE) || name.equals(ItemOneToManyConnectedCombo.TYPE);
            return !isUnsupported;
        }).sorted().collect(Collectors.toList());

        _widgetsCombo.setModel(new DefaultComboBoxModel<String>(widgetNames.toArray(new String[0])));

        if (formHandler != null) {
            openTagsFile();
        }

    }

    private void openTagsFile() throws Exception {
        String form = formHandler.getForm();
        if (form == null) {
            form = "[]";
        }
        selectedSectionsMap = Utilities.getSectionsFromJsonString(form);

        _buttonsCombo.setModel(new DefaultComboBoxModel<String>(selectedSectionsMap.keySet().toArray(new String[0])));

        Object selectedItem = _buttonsCombo.getSelectedItem();
        if (selectedItem != null) {
            String defaultSectionName = selectedItem.toString();
            loadSection(defaultSectionName);
        }
    }

    private void loadSection( String name ) {
        currentSelectedSectionObject = selectedSectionsMap.get(name);

        List<String> formNames4Section = Utilities.getFormNames4Section(currentSelectedSectionObject);

        _buttonsTabPane.removeChangeListener(this);
        _buttonsTabPane.removeAll();
        for( String formName : formNames4Section ) {
            reloadFormTab(formName);
        }
        _buttonsTabPane.addChangeListener(this);

        if (formNames4Section.size() > 0) {
            currentSelectedFormName = formNames4Section.get(0);
            refreshFormWidgetsCombo();
        }

    }

    private void refreshFormWidgetsCombo() {
        JSONObject form4Name = Utilities.getForm4Name(currentSelectedFormName, currentSelectedSectionObject);
        JSONArray formItems = Utilities.getFormItems(form4Name);

        int length = formItems.length();
        if (length == 0) {
            _loadedWidgetsCombo.setModel(new DefaultComboBoxModel<String>());
            return;
        }
        String[] names = new String[length];
        for( int i = 0; i < length; i++ ) {
            JSONObject jsonObject = formItems.getJSONObject(i);
            if (jsonObject.has(Utilities.TAG_KEY)) {
                names[i] = jsonObject.getString(Utilities.TAG_KEY).trim();
            } else if (jsonObject.has(Utilities.TAG_VALUE)) {
                names[i] = jsonObject.getString(Utilities.TAG_VALUE).trim();
            }
        }

        _loadedWidgetsCombo.setModel(new DefaultComboBoxModel<String>(names));
    }

    private boolean checkKeyExistanceInSection( String keyToCheck ) {
        List<String> formNames = Utilities.getFormNames4Section(currentSelectedSectionObject);
        for( String formName : formNames ) {
            JSONObject form4Name = Utilities.getForm4Name(formName, currentSelectedSectionObject);
            JSONArray formItems = Utilities.getFormItems(form4Name);
            int length = formItems.length();
            for( int i = 0; i < length; i++ ) {
                JSONObject jsonObject = formItems.getJSONObject(i);
                if (jsonObject.has(Utilities.TAG_KEY)) {
                    String key = jsonObject.getString(Utilities.TAG_KEY).trim();
                    if (key.equals(keyToCheck)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void reloadFormTab( String formName ) {

        int index = -1;
        int tabCount = _buttonsTabPane.getTabCount();
        for( int i = 0; i < tabCount; i++ ) {
            String tabTitle = _buttonsTabPane.getTitleAt(i);
            if (tabTitle.equals(formName)) {
                index = i;
                break;
            }
        }

        JPanel widgetsPanel = null;
        if (index == -1) {
            widgetsPanel = new JPanel();
            widgetsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            widgetsPanel.putClientProperty("id", "widgets");

            BoxLayout boxLayout = new BoxLayout(widgetsPanel, BoxLayout.Y_AXIS);
            widgetsPanel.setLayout(boxLayout);

            JPanel holderPanel = new JPanel(new BorderLayout());
            Dimension preferredSize = holderPanel.getPreferredSize();
            holderPanel.setMaximumSize(new Dimension(300, preferredSize.height));
            JScrollPane scrollBar = new JScrollPane(widgetsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            holderPanel.add(scrollBar);
            _buttonsTabPane.addTab(formName, holderPanel);
        } else {
            JPanel mainTabPanel = (JPanel) _buttonsTabPane.getComponentAt(index);
            Component[] components = mainTabPanel.getComponents();
            if (components[0] instanceof JScrollPane) {
                JScrollPane scrollpane = (JScrollPane) components[0];
                Component[] components2 = scrollpane.getComponents();
                for( Component component : components2 ) {
                    if (component instanceof JViewport) {
                        JViewport vp = (JViewport) component;
                        Component[] components3 = vp.getComponents();
                        if (components3[0] instanceof JPanel) {
                            JPanel p = (JPanel) components3[0];
                            Object id = p.getClientProperty("id");
                            if (id instanceof String) {
                                String idStr = (String) id;
                                if (idStr.equals("widgets")) {
                                    widgetsPanel = p;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            widgetsPanel.removeAll();
            widgetsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            BoxLayout gridLayout = new BoxLayout(widgetsPanel, BoxLayout.Y_AXIS);
            widgetsPanel.setLayout(gridLayout);
        }

        JSONObject formJson = Utilities.getForm4Name(formName, currentSelectedSectionObject);

        JSONArray formItems = Utilities.getFormItems(formJson);
        for( int i = 0; i < formItems.length(); i++ ) {
            JSONObject jsonObject = formItems.getJSONObject(i);
            if (jsonObject.has(Utilities.TAG_TYPE)) {
                String type = jsonObject.getString(Utilities.TAG_TYPE).trim();

                String key = null;
                if (jsonObject.has(Utilities.TAG_KEY)) {
                    key = jsonObject.getString(Utilities.TAG_KEY).trim();
                }
                String label = null;
                if (jsonObject.has(Utilities.TAG_LABEL)) {
                    label = jsonObject.get(Utilities.TAG_LABEL).toString().trim();
                }
                if (label == null && key != null) {
                    label = key;
                }
                String defaultValue = null;
                if (jsonObject.has(Utilities.TAG_VALUE)) {
                    defaultValue = jsonObject.get(Utilities.TAG_VALUE).toString().trim();
                }
                if (defaultValue == null) {
                    defaultValue = ""; //$NON-NLS-1$
                }

                List<String> attributes = new ArrayList<>();
                if (jsonObject.has(Utilities.TAG_ISLABEL)) {
                    String isLabelString = jsonObject.get(Utilities.TAG_ISLABEL).toString().trim();
                    isLabelString = isLabelString.replace("no", "false").replace("yes", "true");
                    boolean isLabel = Boolean.parseBoolean(isLabelString);
                    if (isLabel) {
                        attributes.add("is label");
                    }
                }
                if (jsonObject.has(Utilities.TAG_ISMANDATORY)) {
                    String isMandatoryString = jsonObject.get(Utilities.TAG_ISMANDATORY).toString().trim();
                    isMandatoryString = isMandatoryString.replace("no", "false").replace("yes", "true");
                    boolean isMandatory = Boolean.parseBoolean(isMandatoryString);
                    if (isMandatory) {
                        attributes.add("mandatory");
                    }
                }
                if (jsonObject.has(Utilities.TAG_RANGE)) {
                    String rangeString = jsonObject.get(Utilities.TAG_RANGE).toString().trim();
                    if (rangeString.length() > 0) {
                        attributes.add(rangeString);
                    }
                }
                String collected = attributes.stream().collect(Collectors.joining("; "));
                String finalLabel = label;
                if (collected.trim().length() > 0) {
                    finalLabel += "     -> " + collected;
                }

                JLabel mainLabel = new JLabel(finalLabel);
                mainLabel.setForeground(ColorUtilities.fromHex(LABELCOLOR));
                mainLabel.setFont(mainLabel.getFont().deriveFont(20f));
                widgetsPanel.add(mainLabel);
                mainLabel.setAlignmentX(LEFT_ALIGNMENT);
                setSizes(mainLabel);

                switch( type ) {
                case ItemLabel.TYPE:
                    float size = 20f;
                    if (jsonObject.has(Utilities.TAG_SIZE)) {
                        size = Integer.parseInt(jsonObject.get(Utilities.TAG_SIZE).toString().trim());
                    }

                    mainLabel.setText(defaultValue);
                    mainLabel.setFont(mainLabel.getFont().deriveFont(size));
                    mainLabel.setForeground(ColorUtilities.fromHex(LABELCOLOR));
                    break;
                case ItemLabel.TYPE_WITHLINE:
                    size = 20;
                    if (jsonObject.has(Utilities.TAG_SIZE)) {
                        size = Integer.parseInt(jsonObject.get(Utilities.TAG_SIZE).toString().trim());
                    }
                    mainLabel.setText(defaultValue);
                    Font font = mainLabel.getFont();
                    Map fontAttributes = font.getAttributes();
                    fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    mainLabel.setFont(font.deriveFont(fontAttributes).deriveFont(size));
                    mainLabel.setForeground(ColorUtilities.fromHex(LABELCOLOR));
                    break;
                case ItemBoolean.TYPE:
                    JCheckBox checkBox = new JCheckBox();
                    if (defaultValue != null && defaultValue.equals("true")) { //$NON-NLS-1$
                        checkBox.setSelected(true);
                    }
                    widgetsPanel.add(checkBox);
                    checkBox.setAlignmentX(LEFT_ALIGNMENT);
                    setSizes(checkBox);
                    break;
                case ItemCombo.TYPE:
                    String[] values = new String[0];
                    if (jsonObject.has(Utilities.TAG_VALUES)) {
                        JSONObject valuesObject = jsonObject.getJSONObject(Utilities.TAG_VALUES);
                        if (valuesObject.has(Utilities.TAG_ITEMS)) {
                            JSONArray valuesArray = valuesObject.getJSONArray(Utilities.TAG_ITEMS);
                            values = new String[valuesArray.length()];
                            for( int j = 0; j < valuesArray.length(); j++ ) {
                                JSONObject itemObj = valuesArray.getJSONObject(j);
                                values[j] = itemObj.getString(Utilities.TAG_ITEM);
                            }
                        }
                    }

                    JComboBox<String> comboBox = new JComboBox<>();
                    comboBox.setModel(new DefaultComboBoxModel<String>(values));

                    if (defaultValue != null) {
                        comboBox.setSelectedItem(defaultValue);
                    }
                    widgetsPanel.add(comboBox);
                    comboBox.setAlignmentX(LEFT_ALIGNMENT);
                    comboBox.setEditable(false);
                    setSizes(comboBox);
                    break;
                case ItemCombo.MULTI_TYPE:
                    String[] multiValues = new String[0];
                    if (jsonObject.has(Utilities.TAG_VALUES)) {
                        JSONObject valuesObject = jsonObject.getJSONObject(Utilities.TAG_VALUES);
                        if (valuesObject.has(Utilities.TAG_ITEMS)) {
                            JSONArray valuesArray = valuesObject.getJSONArray(Utilities.TAG_ITEMS);
                            multiValues = new String[valuesArray.length()];
                            for( int j = 0; j < valuesArray.length(); j++ ) {
                                JSONObject itemObj = valuesArray.getJSONObject(j);
                                multiValues[j] = itemObj.getString(Utilities.TAG_ITEM);
                            }
                        }
                    }

                    JList<String> multiComboBox = new JList<>();
                    DefaultListModel<String> model = new DefaultListModel<String>();
                    for( String string : multiValues ) {
                        model.addElement(string);
                    }
                    multiComboBox.setModel(model);
                    if (defaultValue != null) {
                        multiComboBox.setSelectedValue(defaultValue, true);
                    }
                    widgetsPanel.add(multiComboBox);
                    setSizes(multiComboBox);
                    multiComboBox.setAlignmentX(LEFT_ALIGNMENT);
                    break;
                case ItemDate.TYPE:
                    JPanel datePanel = new JPanel();
                    BoxLayout dateBoxLayout = new BoxLayout(datePanel, BoxLayout.Y_AXIS);
                    datePanel.setLayout(dateBoxLayout);
                    JLabel dateImage = new JLabel(ImageCache.getInstance().getImage(ImageCache.FORM_DATE));
                    datePanel.add(dateImage, BorderLayout.CENTER);
                    dateImage.setAlignmentX(LEFT_ALIGNMENT);
                    if (defaultValue.length() != 0) {
                        JLabel dateTextLabel = new JLabel(defaultValue);
                        datePanel.add(dateTextLabel, BorderLayout.SOUTH);
                        dateTextLabel.setAlignmentX(LEFT_ALIGNMENT);
                    }
                    datePanel.setAlignmentX(LEFT_ALIGNMENT);
                    setSizes(datePanel);
                    widgetsPanel.add(datePanel);
                    break;
                case ItemTime.TYPE:
                    JPanel timePanel = new JPanel();
                    BoxLayout timeBoxLayout = new BoxLayout(timePanel, BoxLayout.Y_AXIS);
                    timePanel.setLayout(timeBoxLayout);
                    JLabel timeImage = new JLabel(ImageCache.getInstance().getImage(ImageCache.FORM_TIME));
                    timePanel.add(timeImage, BorderLayout.CENTER);
                    timeImage.setAlignmentX(LEFT_ALIGNMENT);
                    if (defaultValue.length() != 0) {
                        JLabel timeTextLabel = new JLabel(defaultValue);
                        timePanel.add(timeTextLabel, BorderLayout.SOUTH);
                        timeTextLabel.setAlignmentX(LEFT_ALIGNMENT);
                    }
                    setSizes(timePanel);
                    widgetsPanel.add(timePanel);
                    timePanel.setAlignmentX(LEFT_ALIGNMENT);
                    break;
                case ItemInteger.TYPE:
                    JTextField integerField = new JTextField();
                    integerField.setText(defaultValue);
                    setSizes(integerField);
                    widgetsPanel.add(integerField);
                    integerField.setAlignmentX(LEFT_ALIGNMENT);
                    break;
                case ItemDouble.TYPE:
                    JTextField doubleField = new JTextField();
                    doubleField.setText(defaultValue);
                    setSizes(doubleField);
                    widgetsPanel.add(doubleField);
                    doubleField.setAlignmentX(LEFT_ALIGNMENT);
                    doubleField.setEditable(false);
                    break;
                case ItemDynamicText.TYPE:
                    JPanel dynPanel = new JPanel();
                    BoxLayout boxLayout = new BoxLayout(dynPanel, BoxLayout.Y_AXIS);
                    dynPanel.setLayout(boxLayout);
                    String[] split = defaultValue.split(";"); //$NON-NLS-1$
                    for( String string : split ) {
                        JTextField dynamicField = new JTextField();
                        dynamicField.setText(string.trim());
                        dynamicField.setAlignmentX(LEFT_ALIGNMENT);
                        dynamicField.setEditable(false);
                        setSizes(dynamicField);
                        dynPanel.add(dynamicField);
                    }
                    JButton addButton = new JButton(ImageCache.getInstance().getImage(ImageCache.FORM_PLUS));
                    dynPanel.add(addButton);
                    widgetsPanel.add(dynPanel);

                    setSizes(dynPanel);
                    addButton.setAlignmentX(LEFT_ALIGNMENT);
                    dynPanel.setAlignmentX(LEFT_ALIGNMENT);
                    break;
                case ItemPicture.TYPE:
                case ItemImagelib.TYPE:
                    JLabel pictureImage = new JLabel(ImageCache.getInstance().getImage(ImageCache.FORM_PICTURE));
                    setSizes(pictureImage);
                    widgetsPanel.add(pictureImage);
                    pictureImage.setAlignmentX(LEFT_ALIGNMENT);
                    break;
                case ItemSketch.TYPE:
                    JLabel sketchImage = new JLabel(ImageCache.getInstance().getImage(ImageCache.FORM_SKETCH));
                    setSizes(sketchImage);
                    widgetsPanel.add(sketchImage);
                    sketchImage.setAlignmentX(LEFT_ALIGNMENT);
                    break;
                case ItemMap.TYPE:
                    JLabel mapImage = new JLabel(ImageCache.getInstance().getImage(ImageCache.FORM_MAP));
                    setSizes(mapImage);
                    widgetsPanel.add(mapImage);
                    mapImage.setAlignmentX(LEFT_ALIGNMENT);
                    break;
                case ItemText.TYPE:
                    JTextField textField = new JTextField();
                    textField.setText(defaultValue);
                    setSizes(textField);
                    widgetsPanel.add(textField);
                    textField.setAlignmentX(LEFT_ALIGNMENT);
                    textField.setEditable(false);
                    break;
                case ItemConnectedCombo.TYPE:
                    JLabel iccProblemLabel = new JLabel(CCOMBO_DOESNT_EXIST);
                    iccProblemLabel.setForeground(Color.red);
                    setSizes(iccProblemLabel);
                    widgetsPanel.add(iccProblemLabel);
                    iccProblemLabel.setAlignmentX(LEFT_ALIGNMENT);
                    break;
                case ItemOneToManyConnectedCombo.TYPE:
                    JLabel iomccProblemLabel = new JLabel(OMCOMBO_DOESNT_EXIST);
                    iomccProblemLabel.setForeground(Color.red);
                    setSizes(iomccProblemLabel);
                    widgetsPanel.add(iomccProblemLabel);
                    iomccProblemLabel.setAlignmentX(LEFT_ALIGNMENT);
                    break;
                default:
                    break;
                }

            }

            widgetsPanel.add(Box.createRigidArea(new Dimension(5, 15)));
        }

        widgetsPanel.add(Box.createVerticalGlue());

        GuiUtilities.applyComponentOrientation(widgetsPanel, co);
    }

    private void addNewWidget( String widgetName ) {
        if (selectedSectionsMap == null) {
            GuiUtilities.showInfoMessage(this, FIRST_OPEN_FILE_PROMPT);
            return;
        }
        if (currentSelectedFormName == null) {
            return;
        }

        JSONObject form4Name = Utilities.getForm4Name(currentSelectedFormName, currentSelectedSectionObject);
        JSONArray formItems = Utilities.getFormItems(form4Name);

        boolean labelWithLine = false;
        boolean comboIsMultiType = false;
        boolean textIsInteger = false;
        IMAGEWIDGET imageType = IMAGEWIDGET.PICTURE; // $NON-NLS-1$

        switch( widgetName ) {
        case ItemLabel.TYPE_WITHLINE:
            labelWithLine = true;
        case ItemLabel.TYPE:
            JPanel labelPanel = new JPanel(new BorderLayout());
            JTextField labelTextField = new JTextField();
            labelTextField.setText("enter label text");
            JComboBox<Integer> sizeCombo = new JComboBox<>();
            sizeCombo.setModel(new DefaultComboBoxModel<>(new Integer[]{10, 14, 16, 18, 20, 24, 28, 30, 34, 38, 40, 50, 60}));
            sizeCombo.setSelectedItem(20);
            sizeCombo.setToolTipText("Text size");
            JCheckBox underLineCheck = new JCheckBox("underline");
            labelPanel.add(labelTextField, BorderLayout.NORTH);
            labelPanel.add(sizeCombo, BorderLayout.CENTER);

            if (!labelWithLine)
                labelPanel.add(underLineCheck, BorderLayout.SOUTH);
            underLineCheck.setSelected(labelWithLine);

            boolean okPushed = GuiUtilities.openConfirmDialogWithPanel(this, labelPanel, "Add label");
            if (okPushed) {
                String labelString = labelTextField.getText();
                Integer size = (Integer) sizeCombo.getSelectedItem();
                boolean doUnderline = underLineCheck.isSelected();

                ItemLabel il = new ItemLabel(labelString, size, doUnderline);
                String labelJson = il.toString();
                formItems.put(new JSONObject(labelJson));

                rewriteFileAndLoadCurrentTab();
            }
            break;
        case ItemBoolean.TYPE:
            JPanel booleanPanel = new JPanel(new BorderLayout());
            KeyComponent keyBooleanField = new KeyComponent(formHandler);
            JTextField labelBooleanTextField = new JTextField();
            labelBooleanTextField.setText("enter label");

            JCheckBox defaultBooleanCheckbox = new JCheckBox("default value");
            JCheckBox mandatoryBooleanCheckbox = new JCheckBox("is mandatory?");

            JPanel textFieldsBooleanPanel = new JPanel(new BorderLayout());
            textFieldsBooleanPanel.add(keyBooleanField.getComponent(), BorderLayout.NORTH);
            textFieldsBooleanPanel.add(labelBooleanTextField, BorderLayout.CENTER);
            booleanPanel.add(textFieldsBooleanPanel, BorderLayout.NORTH);
            booleanPanel.add(defaultBooleanCheckbox, BorderLayout.CENTER);
            booleanPanel.add(mandatoryBooleanCheckbox, BorderLayout.SOUTH);

            boolean okBooleanPushed = GuiUtilities.openConfirmDialogWithPanel(this, booleanPanel, "Add Boolean");
            if (okBooleanPushed) {
                checkKeyLabelAndRun(keyBooleanField, labelBooleanTextField, () -> {
                    String key = keyBooleanField.getText().trim();
                    String label = labelBooleanTextField.getText().trim();
                    Boolean defaultValue = defaultBooleanCheckbox.isSelected();
                    Boolean isMandatory = mandatoryBooleanCheckbox.isSelected();

                    ItemBoolean ib = new ItemBoolean(key, label, defaultValue.toString(), isMandatory);
                    String booleanJson = ib.toString();
                    formItems.put(new JSONObject(booleanJson));

                    rewriteFileAndLoadCurrentTab();
                });
            }
            break;
        case ItemCombo.MULTI_TYPE:
            comboIsMultiType = true;
        case ItemCombo.TYPE:
            BorderLayout comboLayout = new BorderLayout();
            comboLayout.setVgap(10);
            JPanel comboPanel = new JPanel(comboLayout);
            KeyComponent keyComboField = new KeyComponent(formHandler);
            JTextField labelComboTextField = new JTextField();
            labelComboTextField.setText("enter label");

            JTextArea itemsComboArea = new JTextArea(10, 25);
            itemsComboArea.setText("enter items");
            itemsComboArea.setBorder(new JTextField().getBorder());

            JTextField defaultComboTextField = new JTextField();
            defaultComboTextField.setText("default value");

            JCheckBox mandatoryComboCheckbox = new JCheckBox("is mandatory?");

            JPanel helpComboPanel = new JPanel(new BorderLayout());
            helpComboPanel.add(keyComboField.getComponent(), BorderLayout.NORTH);
            helpComboPanel.add(labelComboTextField, BorderLayout.CENTER);
            comboPanel.add(helpComboPanel, BorderLayout.NORTH);
            comboPanel.add(itemsComboArea, BorderLayout.CENTER);
            JPanel helpComboPanel2 = new JPanel(new BorderLayout());
            helpComboPanel2.add(defaultComboTextField, BorderLayout.NORTH);
            helpComboPanel2.add(mandatoryComboCheckbox, BorderLayout.SOUTH);
            comboPanel.add(helpComboPanel2, BorderLayout.SOUTH);

            boolean okComboPushed = GuiUtilities.openConfirmDialogWithPanel(this, comboPanel, "Add Combo");
            if (okComboPushed) {
                boolean _comboIsMultiType = comboIsMultiType;
                checkKeyLabelAndRun(keyComboField, labelComboTextField, () -> {
                    String key = keyComboField.getText().trim();
                    String label = labelComboTextField.getText().trim();
                    String[] items = itemsComboArea.getText().split("\n");
                    String defaultItem = defaultComboTextField.getText().trim();
                    Boolean isMandatory = mandatoryComboCheckbox.isSelected();

                    ItemCombo ic = new ItemCombo(key, label, items, defaultItem, _comboIsMultiType, isMandatory); // $NON-NLS-1$
                    String comboJson = ic.toString();
                    formItems.put(new JSONObject(comboJson));

                    rewriteFileAndLoadCurrentTab();
                });
            }
            break;
        case ItemText.TYPE:
            JPanel textPanel = new JPanel(new BorderLayout());
            KeyComponent keyTextField = new KeyComponent(formHandler);
            JTextField labelTextTextField = new JTextField();
            labelTextTextField.setText("enter label");
            JTextField defaultTextTextField = new JTextField();
            defaultTextTextField.setText("enter default");

            JCheckBox isLabelTextCheckbox = new JCheckBox("is Label?");
            JCheckBox mandatoryTextCheckbox = new JCheckBox("is mandatory?");

            JPanel helpTextPanel = new JPanel(new BorderLayout());
            helpTextPanel.add(keyTextField.getComponent(), BorderLayout.NORTH);
            helpTextPanel.add(labelTextTextField, BorderLayout.CENTER);
            textPanel.add(helpTextPanel, BorderLayout.NORTH);
            textPanel.add(defaultTextTextField, BorderLayout.CENTER);
            JPanel helpTextPanel2 = new JPanel(new BorderLayout());
            helpTextPanel2.add(isLabelTextCheckbox, BorderLayout.NORTH);
            helpTextPanel2.add(mandatoryTextCheckbox, BorderLayout.SOUTH);
            textPanel.add(helpTextPanel2, BorderLayout.SOUTH);

            boolean okTextPushed = GuiUtilities.openConfirmDialogWithPanel(this, textPanel, "Add Text");
            if (okTextPushed) {
                checkKeyLabelAndRun(keyTextField, labelTextTextField, () -> {
                    String key = keyTextField.getText().trim();
                    String label = labelTextTextField.getText().trim();
                    String defaultValue = defaultTextTextField.getText().trim();
                    Boolean isLabel = isLabelTextCheckbox.isSelected();
                    Boolean isMandatory = mandatoryTextCheckbox.isSelected();

                    ItemText ib = new ItemText(key, label, defaultValue, isMandatory, isLabel);
                    String textJson = ib.toString();
                    formItems.put(new JSONObject(textJson));
                    rewriteFileAndLoadCurrentTab();
                });
            }
            break;
        case ItemInteger.TYPE:
            textIsInteger = true;
        case ItemDouble.TYPE:
            JPanel numericPanel = new JPanel(new BorderLayout());
            KeyComponent keyNumericField = new KeyComponent(formHandler);
            JTextField labelNumericTextField = new JTextField();
            labelNumericTextField.setText("enter label");
            JTextField defaultNumericTextField = new JTextField();
            defaultNumericTextField.setText("enter default");
            boolean _textIsInteger = textIsInteger;
            addNumericCheck(defaultNumericTextField, textIsInteger);

            JCheckBox isLabelNumericCheckbox = new JCheckBox("is Label?");
            JCheckBox mandatoryNumericCheckbox = new JCheckBox("is mandatory?");

            JPanel helpNumericPanel = new JPanel(new BorderLayout());
            helpNumericPanel.add(keyNumericField.getComponent(), BorderLayout.NORTH);
            helpNumericPanel.add(labelNumericTextField, BorderLayout.CENTER);
            helpNumericPanel.add(defaultNumericTextField, BorderLayout.SOUTH);
            numericPanel.add(helpNumericPanel, BorderLayout.NORTH);

            GridLayout rangeLayout = new GridLayout(2, 2);
            rangeLayout.setHgap(10);
            rangeLayout.setVgap(10);
            JPanel rangePanel = new JPanel(rangeLayout);
            JTextField minNumericTextfield = new JTextField();
            addNumericCheck(minNumericTextfield, textIsInteger);
            JCheckBox minIncludeNumericCheck = new JCheckBox("include?");
            JTextField maxNumericTextfield = new JTextField();
            addNumericCheck(maxNumericTextfield, textIsInteger);
            JCheckBox maxIncludeNumericCheck = new JCheckBox("include?");
            rangePanel.add(minNumericTextfield);
            rangePanel.add(minIncludeNumericCheck);
            rangePanel.add(maxNumericTextfield);
            rangePanel.add(maxIncludeNumericCheck);
            numericPanel.add(rangePanel, BorderLayout.CENTER);

            JPanel helpNumericPanel2 = new JPanel(new BorderLayout());
            helpNumericPanel2.add(rangePanel, BorderLayout.NORTH);
            helpNumericPanel2.add(isLabelNumericCheckbox, BorderLayout.CENTER);
            helpNumericPanel2.add(mandatoryNumericCheckbox, BorderLayout.SOUTH);
            numericPanel.add(helpNumericPanel2, BorderLayout.SOUTH);

            String title = "Add" + (textIsInteger ? " integer" : " double");
            boolean okNumericPushed = GuiUtilities.openConfirmDialogWithPanel(this, numericPanel, title);
            if (okNumericPushed) {
                checkKeyLabelAndRun(keyNumericField, labelNumericTextField, () -> {
                    String key = keyNumericField.getText().trim();
                    String label = labelNumericTextField.getText().trim();
                    String defaultValue = defaultNumericTextField.getText().trim();
                    Boolean isLabel = isLabelNumericCheckbox.isSelected();
                    Boolean isMandatory = mandatoryNumericCheckbox.isSelected();

                    String textJson = null;
                    if (_textIsInteger) {
                        Integer defaultInt = null;
                        if (defaultValue.length() > 0) {
                            defaultInt = Integer.parseInt(defaultValue);
                        }
                        String rangeMinStr = minNumericTextfield.getText().trim();
                        String rangeMaxStr = maxNumericTextfield.getText().trim();
                        int[] range = null;
                        if (rangeMinStr.length() > 0 && rangeMaxStr.length() > 0) {
                            range = new int[]{Integer.parseInt(rangeMinStr), Integer.parseInt(rangeMaxStr)};
                        }
                        boolean[] inclusiveness = new boolean[]{minIncludeNumericCheck.isSelected(),
                                maxIncludeNumericCheck.isSelected()};
                        ItemInteger ii = new ItemInteger(key, label, defaultInt, isMandatory, isLabel, range, inclusiveness);
                        textJson = ii.toString();
                    } else {
                        Double defaultDouble = null;
                        if (defaultValue.length() > 0) {
                            defaultDouble = Double.parseDouble(defaultValue);
                        }
                        String rangeMinStr = minNumericTextfield.getText().trim();
                        String rangeMaxStr = maxNumericTextfield.getText().trim();
                        double[] range = null;
                        if (rangeMinStr.length() > 0 && rangeMaxStr.length() > 0) {
                            range = new double[]{Double.parseDouble(rangeMinStr), Double.parseDouble(rangeMaxStr)};
                        }
                        boolean[] inclusiveness = new boolean[]{minIncludeNumericCheck.isSelected(),
                                maxIncludeNumericCheck.isSelected()};
                        ItemDouble id = new ItemDouble(key, label, defaultDouble, isMandatory, isLabel, range, inclusiveness);
                        textJson = id.toString();
                    }
                    formItems.put(new JSONObject(textJson));
                    rewriteFileAndLoadCurrentTab();
                });
            }
            break;
        case ItemDynamicText.TYPE:
            BorderLayout dynamictextLayout = new BorderLayout();
            dynamictextLayout.setVgap(10);
            JPanel dynamictextPanel = new JPanel(dynamictextLayout);
            KeyComponent keyDynamicTextField = new KeyComponent(formHandler);
            JTextField labelDynamicTextField = new JTextField();
            labelDynamicTextField.setText("enter label");

            JTextArea itemsDynamicArea = new JTextArea(10, 25);
            itemsDynamicArea.setText("enter default items");
            itemsDynamicArea.setBorder(new JTextField().getBorder());

            JCheckBox islabelDynamicCheckbox = new JCheckBox("is label?");
            JCheckBox mandatoryDynamicCheckbox = new JCheckBox("is mandatory?");

            JPanel helpDynamicPanel = new JPanel(new BorderLayout());
            helpDynamicPanel.add(keyDynamicTextField.getComponent(), BorderLayout.NORTH);
            helpDynamicPanel.add(labelDynamicTextField, BorderLayout.CENTER);
            dynamictextPanel.add(helpDynamicPanel, BorderLayout.NORTH);
            dynamictextPanel.add(itemsDynamicArea, BorderLayout.CENTER);
            JPanel helpDynamicPanel2 = new JPanel(new BorderLayout());
            helpDynamicPanel2.add(islabelDynamicCheckbox, BorderLayout.NORTH);
            helpDynamicPanel2.add(mandatoryDynamicCheckbox, BorderLayout.SOUTH);
            dynamictextPanel.add(helpDynamicPanel2, BorderLayout.SOUTH);

            boolean okDynamicPushed = GuiUtilities.openConfirmDialogWithPanel(this, dynamictextPanel, "Add Dynamic Text");
            if (okDynamicPushed) {
                checkKeyLabelAndRun(keyDynamicTextField, labelDynamicTextField, () -> {
                    String key = keyDynamicTextField.getText().trim();
                    String label = labelDynamicTextField.getText().trim();
                    String itemsValue = itemsDynamicArea.getText();
                    itemsValue = itemsValue.replace('\n', ';');
                    Boolean isLabel = islabelDynamicCheckbox.isSelected();
                    Boolean isMandatory = mandatoryDynamicCheckbox.isSelected();

                    ItemDynamicText ic = new ItemDynamicText(key, label, itemsValue, isMandatory, isLabel); // $NON-NLS-1$
                    String dynamictextJson = ic.toString();
                    formItems.put(new JSONObject(dynamictextJson));

                    rewriteFileAndLoadCurrentTab();
                });
            }
            break;
        case ItemDate.TYPE:
            JPanel datePanel = new JPanel(new BorderLayout());
            KeyComponent keyDateField = new KeyComponent(formHandler);
            JTextField labelDateTextField = new JTextField();
            labelDateTextField.setText("enter label");
            JTextField defaultDateTextField = new JTextField();
            defaultDateTextField.setText("enter date as YYYY-MM-DD");

            JCheckBox mandatoryDateCheckbox = new JCheckBox("is mandatory?");

            JPanel helpDatePanel = new JPanel(new BorderLayout());
            helpDatePanel.add(keyDateField.getComponent(), BorderLayout.NORTH);
            helpDatePanel.add(labelDateTextField, BorderLayout.CENTER);
            datePanel.add(helpDatePanel, BorderLayout.NORTH);
            datePanel.add(defaultDateTextField, BorderLayout.CENTER);
            datePanel.add(mandatoryDateCheckbox, BorderLayout.SOUTH);

            boolean okDatePushed = GuiUtilities.openConfirmDialogWithPanel(this, datePanel, "Add Date");
            if (okDatePushed) {
                checkKeyLabelAndRun(keyDateField, labelDateTextField, () -> {
                    String key = keyDateField.getText().trim();
                    String label = labelDateTextField.getText().trim();
                    String defaultValue = defaultDateTextField.getText().trim();
                    Boolean isMandatory = mandatoryDateCheckbox.isSelected();

                    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
                    Date defaultDate = null;
                    try {
                        defaultDate = dateFormatter.parse(defaultValue);
                    } catch (Exception e) {
                        // ignore
                    }
                    ItemDate ib = new ItemDate(key, label, defaultDate, isMandatory);
                    String dateJson = ib.toString();
                    formItems.put(new JSONObject(dateJson));
                    rewriteFileAndLoadCurrentTab();
                });
            }
            break;
        case ItemTime.TYPE:
            JPanel timePanel = new JPanel(new BorderLayout());
            KeyComponent keyTimeField = new KeyComponent(formHandler);
            JTextField labelTimeTextField = new JTextField();
            labelTimeTextField.setText("enter label");
            JTextField defaultTimeTextField = new JTextField();
            defaultTimeTextField.setText("enter time as HH:MM:SS");

            JCheckBox mandatoryTimeCheckbox = new JCheckBox("is mandatory?");

            JPanel helpTimePanel = new JPanel(new BorderLayout());
            helpTimePanel.add(keyTimeField.getComponent(), BorderLayout.NORTH);
            helpTimePanel.add(labelTimeTextField, BorderLayout.CENTER);
            timePanel.add(helpTimePanel, BorderLayout.NORTH);
            timePanel.add(defaultTimeTextField, BorderLayout.CENTER);
            timePanel.add(mandatoryTimeCheckbox, BorderLayout.SOUTH);

            boolean okTimePushed = GuiUtilities.openConfirmDialogWithPanel(this, timePanel, "Add Time");
            if (okTimePushed) {
                checkKeyLabelAndRun(keyTimeField, labelTimeTextField, () -> {
                    String key = keyTimeField.getText().trim();
                    String label = labelTimeTextField.getText().trim();
                    String defaultValue = defaultTimeTextField.getText().trim();
                    Boolean isMandatory = mandatoryTimeCheckbox.isSelected();

                    SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date defaultTime = null;
                    try {
                        defaultTime = timeFormatter.parse("1970-01-01 " + defaultValue);
                    } catch (Exception e) {
                        // ignore
                    }

                    ItemTime ib = new ItemTime(key, label, defaultTime, isMandatory);
                    String timeJson = ib.toString();
                    formItems.put(new JSONObject(timeJson));
                    rewriteFileAndLoadCurrentTab();
                });
            }
            break;
        case ItemSketch.TYPE:
            imageType = IMAGEWIDGET.SKETCH;
        case ItemMap.TYPE:
            if (imageType == IMAGEWIDGET.PICTURE) // change only if untouched
                imageType = IMAGEWIDGET.MAP;
        case ItemImagelib.TYPE:
            imageType = IMAGEWIDGET.IMAGELIB;
        case ItemPicture.TYPE:
            JPanel picturesPanel = new JPanel(new BorderLayout());
            KeyComponent keyPicturesField = new KeyComponent(formHandler);
            JTextField labelPicturesTextField = new JTextField();
            labelPicturesTextField.setText("enter label");

            JCheckBox mandatoryPicturesCheckbox = new JCheckBox("is mandatory?");

            picturesPanel.add(keyPicturesField.getComponent(), BorderLayout.NORTH);
            picturesPanel.add(labelPicturesTextField, BorderLayout.CENTER);
            picturesPanel.add(mandatoryPicturesCheckbox, BorderLayout.SOUTH);

            IMAGEWIDGET _imageType = imageType;
            boolean okPicturesPushed = GuiUtilities.openConfirmDialogWithPanel(this, picturesPanel, "Add Pictures");
            if (okPicturesPushed) {
                checkKeyLabelAndRun(keyPicturesField, labelPicturesTextField, () -> {
                    String key = keyPicturesField.getText().trim();
                    String label = labelPicturesTextField.getText().trim();
                    Boolean isMandatory = mandatoryPicturesCheckbox.isSelected();

                    String textJson;
                    switch( _imageType ) {
                    case SKETCH:
                        ItemSketch is = new ItemSketch(key, label, null, isMandatory);
                        textJson = is.toString();
                        break;
                    case MAP:
                        ItemMap im = new ItemMap(key, label, null, isMandatory);
                        textJson = im.toString();
                        break;
                    case PICTURE:
                        ItemPicture ip = new ItemPicture(key, label, null, isMandatory);
                        textJson = ip.toString();
                        break;
                    case IMAGELIB:
                        ItemImagelib il = new ItemImagelib(key, label, null, isMandatory);
                        textJson = il.toString();
                        break;
                    default:
                        return;
                    }

                    formItems.put(new JSONObject(textJson));

                    rewriteFileAndLoadCurrentTab();
                });
            }
            break;
        case ItemConnectedCombo.TYPE:
            GuiUtilities.showWarningMessage(this, CCOMBO_DOESNT_EXIST);
            break;
        case ItemOneToManyConnectedCombo.TYPE:
            GuiUtilities.showWarningMessage(this, OMCOMBO_DOESNT_EXIST);
            break;
        default:
            GuiUtilities.showWarningMessage(this, "The selected widget doesn't exist.");
            break;
        }

    }

    private void addNumericCheck( JTextField textField, boolean textIsInteger ) {
        textField.addKeyListener(new KeyListener(){
            public void keyTyped( KeyEvent e ) {
            }
            public void keyReleased( KeyEvent e ) {
                try {
                    String text = textField.getText();
                    if (text.length() > 0) {
                        if (textIsInteger) {
                            Integer.parseInt(text);
                        } else {
                            Double.parseDouble(text);
                        }
                    }
                } catch (Exception e2) {
                    textField.setText("");
                }
            }
            public void keyPressed( KeyEvent e ) {
            }
        });
    }

    private void checkKeyLabelAndRun( KeyComponent keyField, JTextField labelTextField, Runnable runOnOk ) {
        String key = keyField.getText().trim();
        String label = labelTextField.getText().trim();

        if (key.length() != 0 && label.length() != 0) {
            if (checkKeyExistanceInSection(key)) {
                GuiUtilities.showWarningMessage(keyField.getComponent(), "The inserted key already exists!");
                return;
            }
            runOnOk.run();
            return;
        }
        GuiUtilities.showWarningMessage(keyField.getComponent(), "The key and label fields are mandatory!");
    }

    private void rewriteFileAndLoadCurrentTab() {
        JSONArray rootArray = Utilities.formsRootFromSectionsMap(selectedSectionsMap);
        String rootString = rootArray.toString(2);
        try {
            formHandler.saveForm(rootString);
            reloadFormTab(currentSelectedFormName);
            refreshFormWidgetsCombo();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void setSizes( JComponent component ) {
        Dimension preferredSize = component.getPreferredSize();
        component.setMaximumSize(new Dimension(DEFAULTWIDTH, preferredSize.height));
    }

    public JComponent asJComponent() {
        return this;
    }

    public static void main( String[] args ) throws Exception {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();

        IFormHandler handler = null;
        if (args.length > 0 && new File(args[0]).exists()) {
            File openFile = new File(args[0]);
            handler = new FileFormHandler(openFile);
        }

        final FormBuilderController controller = new FormBuilderController(handler);
        SettingsController.applySettings(controller);

        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Geopaparazzi Form Builder");

        GuiUtilities.setDefaultFrameIcon(frame);

        GuiUtilities.addClosingListener(frame, controller);

    }

    @Override
    public void onClose() {
        SettingsController.onCloseHandleSettings();
    }

    @Override
    public void stateChanged( ChangeEvent e ) {
        if (e.getSource() instanceof JTabbedPane) {
            int selectedIndex = _buttonsTabPane.getSelectedIndex();
            if (selectedIndex >= 0) {
                currentSelectedFormName = _buttonsTabPane.getTitleAt(selectedIndex);
                refreshFormWidgetsCombo();
            }
        }

    }

}
