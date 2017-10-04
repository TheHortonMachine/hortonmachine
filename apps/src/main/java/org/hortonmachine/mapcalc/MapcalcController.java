package org.hortonmachine.mapcalc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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

import org.apache.commons.lang.text.StrBuilder;
import org.hortonmachine.database.DatabaseViewer;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gui.console.LogConsoleController;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;

public class MapcalcController extends MapcalcView implements IOnCloseListener {
    private static final String MAPCALC_HISTORY_KEY = "MAPCALC_HISTORY_KEY";
    private DefaultGuiBridgeImpl gBridge;
    private JTextPane _functionArea;

    private LinkedHashMap<String, String> name2PathMap = new LinkedHashMap<>();
    private List<String> historyList = new ArrayList<>();

    public MapcalcController( DefaultGuiBridgeImpl gBridge ) {
        this.gBridge = gBridge;
        setPreferredSize(new Dimension(900, 600));
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
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

        _addMapButton.addActionListener(( e ) -> {
            File[] files = gBridge.showOpenFileDialog("Select raster map file", GuiUtilities.getLastFile(),
                    HMConstants.rasterFileFilter);
            if (files != null && files.length > 0) {
                for( File file : files ) {
                    String absolutePath = file.getAbsolutePath();
                    GuiUtilities.setLastPath(absolutePath);
                    String name = FileUtilities.getNameWithoutExtention(file);
                    loadNewMap(name, absolutePath);
                }
            }
        });

        _outPathButton.addActionListener(( e ) -> {
            File[] files = gBridge.showSaveFileDialog("Choose output file", GuiUtilities.getLastFile(),
                    HMConstants.rasterFileFilter);
            if (files != null && files.length > 0) {
                for( File file : files ) {
                    String absolutePath = file.getAbsolutePath();
                    GuiUtilities.setLastPath(absolutePath);
                    _outputPathText.setText(absolutePath);
                }
            }
        });

        _runButton.addActionListener(( e ) -> {

            final String script = _functionArea.getText();
            if (!historyList.contains(script)) {
                historyList.add(0, script);
                _historyCombo.setModel(new DefaultComboBoxModel<>(historyList.toArray(new String[0])));
            }

            final LogConsoleController logConsole = new LogConsoleController(null);
            IHMProgressMonitor pm = logConsole.getProgressMonitor();
            JFrame window = gBridge.showWindow(logConsole.asJComponent(), "Console Log");
            new Thread(() -> {
                boolean hadErrors = false;
                try {

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

                    MapcalcJiffler j = new MapcalcJiffler(sb.toString(), _outputPathText.getText(), name2PathMap);
                    j.exec(pm);
                } catch (Exception ex) {
                    pm.errorMessage(ex.getLocalizedMessage());
                    hadErrors = true;
                } finally {
                    logConsole.finishProcess();
                    logConsole.stopLogging();
                    if (!hadErrors) {
                        logConsole.setVisible(false);
                        window.dispose();
                    }
                }
            }).start();

        });

        String[] historyArray = GuiUtilities.getPreference(MAPCALC_HISTORY_KEY, new String[]{""});
        for( String entry : historyArray ) {
            historyList.add(entry);
        }
        _historyCombo.setModel(new DefaultComboBoxModel<>(historyList.toArray(new String[0])));
        _historyCombo.addActionListener(e -> {
            String selectedItem = (String) _historyCombo.getSelectedItem();
            addTextToFunctionArea(selectedItem);
        });

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
        GuiUtilities.setPreference(MAPCALC_HISTORY_KEY, historyList.toArray(new String[0]));
    }

    public JComponent asJComponent() {
        return this;
    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        final MapcalcController controller = new MapcalcController(gBridge);
        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Mapcalc");

        Class<DatabaseViewer> class1 = DatabaseViewer.class;
        ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));
        frame.setIconImage(icon.getImage());

        GuiUtilities.addClosingListener(frame, controller);

    }

}
