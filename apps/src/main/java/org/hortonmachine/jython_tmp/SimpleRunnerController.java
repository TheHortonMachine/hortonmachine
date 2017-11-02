package org.hortonmachine.jython_tmp;
//package org.hortonmachine.jython;
//
//import java.awt.Dimension;
//import java.io.File;
//
//import javax.swing.ImageIcon;
//import javax.swing.JComponent;
//import javax.swing.JFrame;
//
//import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
//import org.hortonmachine.gui.utils.GuiBridgeHandler;
//import org.hortonmachine.gui.utils.GuiUtilities;
//import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
//
//public class SimpleRunnerController extends SimpleRunnerGui implements IOnCloseListener {
//
//    private GuiBridgeHandler guiBridge;
//
//    public SimpleRunnerController( GuiBridgeHandler guiBridge ) {
//        this.guiBridge = guiBridge;
//        setPreferredSize(new Dimension(900, 600));
//        init();
//    }
//
//    private void init() {
//        _filePathButton.addActionListener(e -> {
//            File[] openFiles = guiBridge.showOpenFileDialog("Select jython script", GuiUtilities.getLastFile());
//            if (openFiles != null && openFiles.length > 0) {
//                try {
//                    GuiUtilities.setLastPath(openFiles[0].getAbsolutePath());
//                } catch (Exception e1) {
//                    //logger.error("ERROR", e1);
//                }
//            } else {
//                return;
//            }
//
//            final File selectedFile = openFiles[0];
//            if (selectedFile != null) {
//                _filePathField.setText(selectedFile.getAbsolutePath());
//            }
//        });
//
//        // _scriptArea.setEditorKit(new WrapEditorKit());
//        _scriptArea.setDocument(new HighlightingStyledDocument());
//
//        _runButton.addActionListener(e -> {
//            String text = _scriptArea.getText();
//            if (text.trim().length() == 0) {
//                // use file
//                text = _filePathField.getText();
//            }
//            if (text.trim().length() > 0) {
//                JythonRunner jythonRunner = new JythonRunner(guiBridge);
//                jythonRunner.run(text);
//            }
//        });
//
//        _closeButton.addActionListener(e -> {
//            System.exit(0);
//        });
//
//    }
//
//    public JComponent asJComponent() {
//        return this;
//    }
//
//    @Override
//    public void onClose() {
//
//    }
//
//    public static void main( String[] args ) throws Exception {
//        GuiUtilities.setDefaultLookAndFeel();
//
//        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
//        final SimpleRunnerController controller = new SimpleRunnerController(gBridge);
//        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "hortonmachine' Jython Runner");
//
//        Class<SimpleRunnerController> class1 = SimpleRunnerController.class;
//        ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));
//        frame.setIconImage(icon.getImage());
//
//        GuiUtilities.addClosingListener(frame, controller);
//    }
//
//}
