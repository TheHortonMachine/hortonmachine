package org.hortonmachine.gforms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import org.hortonmachine.gears.io.geopaparazzi.forms.Section;
import org.hortonmachine.gears.io.geopaparazzi.forms.Utilities;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemBoolean;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemCombo;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemConnectedCombo;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemDate;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemDouble;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemDynamicText;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemInteger;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemLabel;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemMap;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemOneToManyConnectedCombo;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemPicture;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemSketch;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemText;
import org.hortonmachine.gears.io.geopaparazzi.forms.items.ItemTime;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.ImageCache;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings({"unchecked", "serial", "rawtypes"})
public class FormBuilderController extends FormBuilderView implements IOnCloseListener {
    private static final String LABELCOLOR = "#000000";
    private static final int DEFAULTWIDTH = 500;
    private File selectedFile;
    private LinkedHashMap<String, JSONObject> selectedSectionsMap;

    public FormBuilderController( File tagsFile ) throws Exception {
        setPreferredSize(new Dimension(900, 600));

        if (tagsFile != null && tagsFile.exists()) {
            selectedFile = tagsFile;
        }

        init();
    }

    private void init() throws Exception {
        _filePathtext.setEditable(false);

        _buttonsTabPane.setTabPlacement(JTabbedPane.LEFT);

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
                    return f.isDirectory() || f.getName().endsWith("_tags.json");
                }
            };
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setCurrentDirectory(GuiUtilities.getLastFile());
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                if (selectedFile != null) {
                    GuiUtilities.setLastPath(selectedFile.getAbsolutePath());
                    try {
                        openTagsFile();
                        _filePathtext.setText(selectedFile.getAbsolutePath());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            } else {
                selectedFile = null;
            }
        });

        _addSectionButton.addActionListener(e -> {
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
                FileUtilities.writeFile(rootString, selectedFile);
                openTagsFile();
                _buttonsCombo.setSelectedItem(newSectionName);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        _deleteSectionButton.addActionListener(e -> {
            String sectionToDelete = _buttonsCombo.getSelectedItem().toString();
            boolean doDelete = GuiUtilities.showYesNoDialog(this, "Are you sure you want to delete: " + sectionToDelete);

            if (doDelete) {
                selectedSectionsMap.remove(sectionToDelete);
                JSONArray rootArray = Utilities.formsRootFromSectionsMap(selectedSectionsMap);
                String rootString = rootArray.toString(2);
                try {
                    FileUtilities.writeFile(rootString, selectedFile);
                    openTagsFile();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        _buttonsCombo.addActionListener(e -> {
            String defaultSectionName = _buttonsCombo.getSelectedItem().toString();
            loadSection(defaultSectionName);
        });

        if (selectedFile != null) {
            openTagsFile();
        }

    }

    private void openTagsFile() throws IOException {
        selectedSectionsMap = Utilities.getSectionFromFile(selectedFile.getAbsolutePath());

        _buttonsCombo.setModel(new DefaultComboBoxModel<String>(selectedSectionsMap.keySet().toArray(new String[0])));

        String defaultSectionName = _buttonsCombo.getSelectedItem().toString();
        loadSection(defaultSectionName);
    }

    private void loadSection( String name ) {
        JSONObject curentSelectedSectionObject = selectedSectionsMap.get(name);

        List<String> formNames4Section = Utilities.getFormNames4Section(curentSelectedSectionObject);

        _buttonsTabPane.removeAll();
        for( String formName : formNames4Section ) {
            reloadFormTab(formName, curentSelectedSectionObject);
        }

    }

    public void reloadFormTab( String formName, JSONObject curentSelectedSectionObject ) {
        JPanel widgetsPanel = new JPanel();
        widgetsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        BoxLayout gridLayout = new BoxLayout(widgetsPanel, BoxLayout.Y_AXIS);
        widgetsPanel.setLayout(gridLayout);

//        JScrollPane scrollBar = new JScrollPane(widgetsPanel,
//                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
//                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//        scrollBar.setAlignmentX(LEFT_ALIGNMENT);
//        Dimension preferredSize = scrollBar.getPreferredSize();
//        scrollBar.setMaximumSize(new Dimension(300, preferredSize.height));

        _buttonsTabPane.addTab(formName, widgetsPanel);

        JSONObject formJson = Utilities.getForm4Name(formName, curentSelectedSectionObject);

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
                JLabel mainLabel = new JLabel(label); // $NON-NLS-1$
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
                    Map attributes = font.getAttributes();
                    attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    mainLabel.setFont(font.deriveFont(attributes).deriveFont(size));
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
                    break;
                case ItemConnectedCombo.TYPE:
                    JLabel iccProblemLabel = new JLabel("The type " + ItemConnectedCombo.TYPE + " is not supported yet.");
                    iccProblemLabel.setForeground(Color.red);
                    setSizes(iccProblemLabel);
                    widgetsPanel.add(iccProblemLabel);
                    iccProblemLabel.setAlignmentX(LEFT_ALIGNMENT);
                    break;
                case ItemOneToManyConnectedCombo.TYPE:
                    JLabel iomccProblemLabel = new JLabel(
                            "The type " + ItemOneToManyConnectedCombo.TYPE + " is not supported yet.");
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

        File openFile = null;
        if (args.length > 0 && new File(args[0]).exists()) {
            openFile = new File(args[0]);
        }

        final FormBuilderController controller = new FormBuilderController(openFile);

        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Geopaparazzi Form Builder");

        Class<FormBuilderController> class1 = FormBuilderController.class;
        ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));
        frame.setIconImage(icon.getImage());

        GuiUtilities.addClosingListener(frame, controller);

    }

    @Override
    public void onClose() {
        // TODO Auto-generated method stub

    }

}
