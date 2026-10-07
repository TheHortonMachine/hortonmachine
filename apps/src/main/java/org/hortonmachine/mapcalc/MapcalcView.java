package org.hortonmachine.mapcalc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 * The layout of the map calculator: the script with its helpers on the left, the maps and the
 * execution options on the right.
 */
public class MapcalcView extends JPanel {
    JButton _examplesButton = new JButton();
    JButton _historyButton = new JButton();
    JPanel _functionAreaPanel = new JPanel();
    JButton _runButton = new JButton();
    JTable _availableMapsTable = new JTable();
    JPanel _manualAddFileLayout = new JPanel();
    JLabel _allMapsLabel = new JLabel();
    JButton _addMapButton = new JButton();
    JPanel _comboAddLayerlayout = new JPanel();
    JButton _addMapFromComboButton = new JButton();
    JComboBox _layerCombo = new JComboBox();
    JTextField _outputPathText = new JTextField();
    JButton _outPathButton = new JButton();
    JTabbedPane _syntaxHelpTab = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    JCheckBox _debugCheckbox = new JCheckBox();
    JComboBox _heapCombo = new JComboBox();

    public MapcalcView() {
        initializePanel();
    }

    private static TitledBorder titledBorder( String title ) {
        return new TitledBorder(null, title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null,
                new Color(33, 33, 33));
    }

    private JPanel createScriptPanel() {
        _examplesButton.setName("examplesButton");
        _examplesButton.setText("examples");
        _examplesButton.setToolTipText("Fill the function area with one of the examples of the manual");
        _historyButton.setName("historyButton");
        _historyButton.setText("history");
        _historyButton.setToolTipText("Recall one of the scripts run so far");
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonsPanel.add(_examplesButton);
        buttonsPanel.add(_historyButton);

        _functionAreaPanel.setName("functionAreaPanel");
        JPanel functionPanel = new JPanel(new BorderLayout(0, 4));
        functionPanel.setBorder(titledBorder("Function Area"));
        functionPanel.add(buttonsPanel, BorderLayout.NORTH);
        functionPanel.add(_functionAreaPanel, BorderLayout.CENTER);

        _syntaxHelpTab.setName("syntaxHelpTab");
        _syntaxHelpTab.setBorder(titledBorder("Syntax Help"));

        JPanel scriptPanel = new JPanel(new BorderLayout(0, 4));
        scriptPanel.add(functionPanel, BorderLayout.CENTER);
        scriptPanel.add(_syntaxHelpTab, BorderLayout.SOUTH);
        return scriptPanel;
    }

    private JPanel createMapsPanel() {
        _allMapsLabel.setName("allMapsLabel");
        _allMapsLabel.setText("add map files from filesystem");
        _addMapButton.setName("addMapButton");
        _addMapButton.setText("...");
        _manualAddFileLayout.setName("manualAddFileLayout");
        _manualAddFileLayout.setLayout(new BorderLayout(4, 0));
        _manualAddFileLayout.add(_allMapsLabel, BorderLayout.CENTER);
        _manualAddFileLayout.add(_addMapButton, BorderLayout.EAST);

        _layerCombo.setName("layerCombo");
        _addMapFromComboButton.setName("addMapFromComboButton");
        _addMapFromComboButton.setText("+");
        _comboAddLayerlayout.setName("comboAddLayerlayout");
        _comboAddLayerlayout.setLayout(new BorderLayout(4, 0));
        _comboAddLayerlayout.add(_layerCombo, BorderLayout.CENTER);
        _comboAddLayerlayout.add(_addMapFromComboButton, BorderLayout.EAST);

        JPanel addPanel = new JPanel(new BorderLayout(0, 4));
        addPanel.add(_manualAddFileLayout, BorderLayout.NORTH);
        addPanel.add(_comboAddLayerlayout, BorderLayout.SOUTH);

        _availableMapsTable.setName("availableMapsTable");
        JPanel mapsPanel = new JPanel(new BorderLayout(0, 4));
        mapsPanel.setBorder(titledBorder("Available maps"));
        mapsPanel.add(addPanel, BorderLayout.NORTH);
        mapsPanel.add(new JScrollPane(_availableMapsTable), BorderLayout.CENTER);
        return mapsPanel;
    }

    private JPanel createOptionsPanel() {
        _debugCheckbox.setName("debugCheckbox");
        _debugCheckbox.setText("Debug");
        _heapCombo.setName("heapCombo");

        _outputPathText.setName("outputPathText");
        _outPathButton.setName("outPathButton");
        _outPathButton.setText("...");
        JPanel outputPanel = new JPanel(new BorderLayout(4, 0));
        outputPanel.setBorder(titledBorder("Output Path"));
        outputPanel.add(_outputPathText, BorderLayout.CENTER);
        outputPanel.add(_outPathButton, BorderLayout.EAST);

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        optionsPanel.add(_debugCheckbox, c);
        c.gridy = 1;
        c.gridwidth = 1;
        optionsPanel.add(new JLabel("Heap [MB]"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        optionsPanel.add(_heapCombo, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        optionsPanel.add(outputPanel, c);
        return optionsPanel;
    }

    protected void initializePanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(0, 4));
        rightPanel.add(createMapsPanel(), BorderLayout.CENTER);
        rightPanel.add(createOptionsPanel(), BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createScriptPanel(), rightPanel);
        splitPane.setResizeWeight(0.65);
        splitPane.setBorder(null);

        _runButton.setName("runButton");
        _runButton.setText("run");
        JPanel runPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        runPanel.add(_runButton);

        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(splitPane, BorderLayout.CENTER);
        add(runPanel, BorderLayout.SOUTH);
    }
}
