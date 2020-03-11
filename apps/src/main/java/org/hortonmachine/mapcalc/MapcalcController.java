package org.hortonmachine.mapcalc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;

import org.hortonmachine.database.DatabaseViewer;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.ETimeUtilities;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.DataUtilities;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gui.console.ProcessLogConsoleController;
import org.hortonmachine.gui.spatialtoolbox.core.SpatialToolboxConstants;
import org.hortonmachine.gui.spatialtoolbox.core.StageScriptExecutor;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiBridgeHandler;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;

public class MapcalcController extends MapcalcView implements IOnCloseListener {
    private static final long serialVersionUID = 1L;
    private static final String MAPCALC_HISTORY_KEY = "MAPCALC_HISTORY_KEY";
    private GuiBridgeHandler guiBridge;
    private JTextPane _functionArea;

    private LinkedHashMap<String, String> name2PathMap = new LinkedHashMap<>();
    private List<String> historyList = new ArrayList<>();
    private boolean manualAdd;
    protected HashMap<String, String> prefsMap = new HashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapcalcController( GuiBridgeHandler gBridge, boolean manualAdd ) {
        this.guiBridge = gBridge;
        this.manualAdd = manualAdd;

        HashMap<String, String> prefsMapTmp = guiBridge.getSpatialToolboxPreferencesMap();
        if (prefsMapTmp != null) {
            prefsMap = (HashMap) prefsMapTmp;
        }

        setPreferredSize(new Dimension(900, 600));
        init();
    }

    protected void preInit() {

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void setLayerNames( String[] names ) {
        _layerCombo.setModel(new DefaultComboBoxModel(names));
    }

    @SuppressWarnings("unchecked")
    private void init() {
        preInit();
        LinkedHashMap<String, List<Constructs>> mapByCategory = Constructs.getMapByCategory();
        for( Entry<String, List<Constructs>> entry : mapByCategory.entrySet() ) {
            addTab(entry);
        }

        _functionArea = new JTextPane();
        JScrollPane _sqlEditorAreaScrollpane = new JScrollPane(_functionArea);

        _functionAreaPanel.setLayout(new BorderLayout());
        _functionAreaPanel.add(_sqlEditorAreaScrollpane, BorderLayout.CENTER);
        // SqlDocument doc = new SqlDocument();

        MapcalcDocument doc = new MapcalcDocument();
        _functionArea.setDocument(doc);

        if (manualAdd) {
            _comboAddLayerlayout.removeAll();
            _addMapButton.addActionListener(( e ) -> {
                File[] files = guiBridge.showOpenFileDialog("Select raster map file", PreferencesHandler.getLastFile(),
                        HMConstants.rasterFileFilter);
                if (files != null && files.length > 0) {
                    for( File file : files ) {
                        String absolutePath = file.getAbsolutePath();
                        PreferencesHandler.setLastPath(absolutePath);
                        String name = FileUtilities.getNameWithoutExtention(file);
                        
                        absolutePath = FileUtilities.replaceBackSlashesWithSlashes(absolutePath);
                        loadNewMap(name, absolutePath);
                    }
                }
            });
        } else {
            _manualAddFileLayout.removeAll();
            _addMapFromComboButton.addActionListener(( e ) -> {
                Object selectedItem = _layerCombo.getSelectedItem();
                if (selectedItem != null) {
                    String layerName = selectedItem.toString();
                    File file = getFileForLayerName(layerName);
                    if (file != null) {
                        String absolutePath = file.getAbsolutePath();
                        PreferencesHandler.setLastPath(absolutePath);
                        String name = FileUtilities.getNameWithoutExtention(file);
                        
                        absolutePath = FileUtilities.replaceBackSlashesWithSlashes(absolutePath);
                        loadNewMap(name, absolutePath);
                    }
                }
            });
        }

        _outPathButton.addActionListener(( e ) -> {
            File[] files = guiBridge.showSaveFileDialog("Choose output file", PreferencesHandler.getLastFile(),
                    HMConstants.rasterFileFilter);
            if (files != null && files.length > 0) {
                for( File file : files ) {
                    String absolutePath = file.getAbsolutePath();
                    PreferencesHandler.setLastPath(absolutePath);
                    absolutePath = FileUtilities.replaceBackSlashesWithSlashes(absolutePath);
                    _outputPathText.setText(absolutePath);
                }
            }
        });

        _runButton.addActionListener(( e ) -> {

            String script = _functionArea.getText();
            if (!historyList.contains(script)) {
                historyList.add(0, script);
                _historyCombo.setModel(new DefaultComboBoxModel<>(historyList.toArray(new String[0])));
            }

            final ProcessLogConsoleController logConsole = new ProcessLogConsoleController();
            guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

            StringBuilder sb = new StringBuilder();
            sb.append("images {\n");
            for( String name : name2PathMap.keySet() ) {
                if (script.contains(name)) {
                    sb.append("   " + name + "=read;\n");
                }
            }
            sb.append("   " + MapcalcJiffler.RESULT_MAP_NAME + " = write;\n");
            sb.append("}\n");
            sb.append(script);
            script = sb.toString();

            String resultPath = _outputPathText.getText();

            try {
                runModuleInNewJVM(logConsole, script, resultPath, name2PathMap);
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            // final LogConsoleController logConsole = new LogConsoleController(null);
            // IHMProgressMonitor pm = logConsole.getProgressMonitor();
            // JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");
            // new Thread(() -> {
            // boolean hadErrors = false;
            // try {
            //
            // StringBuilder sb = new StringBuilder();
            // sb.append("images {\n");
            // for( String name : name2PathMap.keySet() ) {
            // if (script.contains(name)) {
            // sb.append(" " + name + "=read;\n");
            // }
            // }
            // sb.append(" " + MapcalcJiffler.RESULT_MAP_NAME + " = write;\n");
            // sb.append("}\n");
            // sb.append(script);
            //
            // String resultPath = _outputPathText.getText();
            //
            //
            // List<GridCoverage2D> gcList = new ArrayList<>();
            // for( Entry<String, String> entry : name2PathMap.entrySet() ) {
            // String mapName = entry.getKey();
            // if (script.indexOf(mapName) != -1) {
            // GridCoverage2D gridCoverage2D = OmsRasterReader.readRaster(entry.getValue());
            // // TODO
            // gcList.add(gridCoverage2D);
            // }
            // }
            // OmsMapcalc mc = new OmsMapcalc();
            // mc.inRasters = gcList;
            // mc.pFunction = sb.toString();
            // mc.process();
            // GridCoverage2D out = mc.outRaster;
            // pm.message(out.toString());
            //
            //// MapcalcJiffler j = new MapcalcJiffler(sb.toString(), resultPath, name2PathMap);
            //// j.exec(pm);
            //
            // File resultFile = new File(resultPath);
            // if (resultFile.exists()) {
            // loadRasterLayer(resultFile);
            // }
            // } catch (Exception ex) {
            // pm.errorMessage(ex.getLocalizedMessage());
            // hadErrors = true;
            // } finally {
            // logConsole.finishProcess();
            // logConsole.stopLogging();
            // if (!hadErrors) {
            // logConsole.setVisible(false);
            // window.dispose();
            // }
            // }
            // }).start();

        });

        String[] historyArray = PreferencesHandler.getPreference(MAPCALC_HISTORY_KEY, new String[]{""});
        for( String entry : historyArray ) {
            historyList.add(entry);
        }
        _historyCombo.setModel(new DefaultComboBoxModel<>(historyList.toArray(new String[0])));
        _historyCombo.addActionListener(e -> {
            String selectedItem = (String) _historyCombo.getSelectedItem();
            addTextToFunctionArea(selectedItem);
        });

        boolean doDebug = false;
        String debugStr = prefsMap.get(GuiBridgeHandler.DEBUG_KEY);
        if (debugStr != null && debugStr.trim().length() > 0) {
            doDebug = Boolean.parseBoolean(debugStr);
        }
        _debugCheckbox.setSelected(doDebug);

        _heapCombo.setModel(new DefaultComboBoxModel<>(SpatialToolboxConstants.HEAPLEVELS));
        String heapStr = prefsMap.get(GuiBridgeHandler.HEAP_KEY);
        if (heapStr == null) {
            heapStr = SpatialToolboxConstants.HEAPLEVELS[0];
        }
        _heapCombo.setSelectedItem(heapStr);

        _debugCheckbox.addActionListener(e -> {
            prefsMap.put(GuiBridgeHandler.DEBUG_KEY, _debugCheckbox.isSelected() + "");
            guiBridge.setSpatialToolboxPreferencesMap(prefsMap);
        });
        _heapCombo.addActionListener(e -> {
            String ramLevel = _heapCombo.getSelectedItem().toString();
            prefsMap.put(GuiBridgeHandler.HEAP_KEY, ramLevel);
            guiBridge.setSpatialToolboxPreferencesMap(prefsMap);
        });
    }

    protected File getFileForLayerName( String layerName ) {
        return null;
    }

    private void runModuleInNewJVM( ProcessLogConsoleController logConsole, String script, final String resultPath,
            LinkedHashMap<String, String> name2PathMap ) throws Exception {
        StringBuilder mc = new StringBuilder();

        mc.append("org.hortonmachine.modules.Mapcalc _mapcalc = new org.hortonmachine.modules.Mapcalc();").append("\n");
        mc.append("_mapcalc.pFunction = \"\"\"");
        mc.append(script);
        mc.append("\"\"\"\n");
        mc.append("_mapcalc.outRaster =\"\"\"");
        mc.append(resultPath);
        mc.append("\"\"\";\n");
        int inputCount = 1;
        for( Entry<String, String> entry : name2PathMap.entrySet() ) {
            String mapName = entry.getKey();
            if (script.indexOf(mapName) != -1) {
                String path = entry.getValue();
                mc.append("_mapcalc.inRaster" + inputCount + " =\"\"\"");
                path = FileUtilities.replaceBackSlashesWithSlashes(path);
                mc.append(path);
                mc.append("\"\"\";\n");
                inputCount++;
            }
        }
        mc.append("_mapcalc.process()\n");

        StageScriptExecutor exec = new StageScriptExecutor(guiBridge.getLibsFolder());
        exec.addProcessListener(logConsole);

        Runnable finishRunnable = new Runnable(){
            public void run() {
                try {
                    File file = new File(resultPath);
                    if (file.exists()) {
                        if (DataUtilities.isSupportedRasterExtension(resultPath)) {
                            loadRasterLayer(file);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        logConsole.addFinishRunnable(finishRunnable);

        String logLevel = _debugCheckbox.isSelected()
                ? SpatialToolboxConstants.LOGLEVEL_GUI_ON
                : SpatialToolboxConstants.LOGLEVEL_GUI_OFF;
        String ramLevel = _heapCombo.getSelectedItem().toString();

        String sessionId = "Mapcalc " + ETimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date());
        Process process = exec.exec(sessionId, mc.toString(), logLevel, ramLevel, null);
        logConsole.beginProcess(process, sessionId);
    }

    /**
     * Override if loading of layers is supported.
     * 
     * @param file
     */
    protected void loadRasterLayer( File file ) {
    }

    public void loadNewMap( String name, String path ) {
        name2PathMap.put(name, path);

        Object[] names = {"Map Name", "Path"};

        String[] namesArray = name2PathMap.keySet().toArray(new String[0]);
        String[] pathsArrays = name2PathMap.values().toArray(new String[0]);

        Object[][] values = new Object[namesArray.length][2];
        for( int i = 0; i < pathsArrays.length; i++ ) {
            values[i][0] = namesArray[i];
            values[i][1] = pathsArrays[i];
        }

        DefaultTableModel dataModel = getTableModel(names, values);
        _availableMapsTable.setModel(dataModel);
        _availableMapsTable.setCellSelectionEnabled(false);

        _availableMapsTable.addMouseListener(new MouseAdapter(){
            public void mousePressed( MouseEvent me ) {
                JTable table = (JTable) me.getSource();
                Point p = me.getPoint();
                int row = table.rowAtPoint(p);
                if (me.getClickCount() == 2) {
                    int count = 0;
                    for( Entry<String, String> entry : name2PathMap.entrySet() ) {
                        if (count == row) {
                            insertTextToFunctionArea(entry.getKey());
                            return;
                        }
                        count++;
                    }
                }
            }
        });

    }

    @SuppressWarnings("serial")
    private DefaultTableModel getTableModel( Object[] names, Object[][] values ) {
        DefaultTableModel dataModel = new DefaultTableModel(values, names){
            @Override
            public boolean isCellEditable( int row, int column ) {
                if (column == 0) {
                    return false;
                }
                return true;
            }
        };
        return dataModel;
    }

    protected void addTab( Entry<String, List<Constructs>> entry ) {
        JPanel jplPanel = new JPanel();
        jplPanel.setLayout(new FlowLayout());

        List<Constructs> constructs = entry.getValue();
        for( Constructs c : constructs ) {
            JButton button = new JButton(c.name);
            button.setToolTipText(c.toolTip);
            jplPanel.add(button);
            button.addActionListener(( e ) -> {
                insertTextToFunctionArea(c.construct);
            });
        }

        _syntaxHelpTab.add(entry.getKey(), jplPanel);
    }

    protected void addTextToFunctionArea( String newText ) {
        String text = _functionArea.getText();
        if (text.trim().length() != 0) {
            text += "\n";
        }
        text += newText;
        _functionArea.setText(text);
    }

    protected void insertTextToFunctionArea( String newText ) {
        // AttributeSet attrs=((StyledEditorKit)_functionArea.getEditorKit()).getInputAttributes();
        try {
            _functionArea.getDocument().insertString(_functionArea.getCaretPosition(), newText, null);
        } catch (BadLocationException e) {
            addTextToFunctionArea(newText);
        }
    }

    public void onClose() {
        String ramLevel = _heapCombo.getSelectedItem().toString();
        prefsMap.put(GuiBridgeHandler.DEBUG_KEY, _debugCheckbox.isSelected() + "");
        prefsMap.put(GuiBridgeHandler.HEAP_KEY, ramLevel);
        guiBridge.setSpatialToolboxPreferencesMap(prefsMap);

        PreferencesHandler.setPreference(MAPCALC_HISTORY_KEY, historyList.toArray(new String[0]));
    }

    public boolean canCloseWithoutPrompt() {
        return false;
    }
    
    public JComponent asJComponent() {
        return this;
    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();

        File libsFile = null;
        try {
            String libsPath = args[0];
            libsFile = new File(libsPath);
        } catch (Exception e1) {
            // IGNORE
        }
        if (libsFile == null || !libsFile.exists() || !libsFile.isDirectory()) {
            Logger.INSTANCE.insertWarning("", "The libraries folder is missing or not properly set.");
            libsFile = new File("/home/hydrologis/development/hortonmachine-git/extras/export/libs");
            // System.exit(1);
        }

        Logger.INSTANCE.insertInfo("", "Libraries folder used: " + libsFile.getAbsolutePath());

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        gBridge.setLibsFolder(libsFile);

        final MapcalcController controller = new MapcalcController(gBridge, true);
        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Mapcalc");

        GuiUtilities.setDefaultFrameIcon(frame);

        GuiUtilities.addClosingListener(frame, controller);

    }

}
