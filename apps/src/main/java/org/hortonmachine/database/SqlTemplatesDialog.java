package org.hortonmachine.database;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

/**
 * Modal dialog that displays named SQL templates as a selectable list with a
 * full-text preview pane. Double-clicking an entry or pressing "Use" returns
 * the SQL for the selected template; "Cancel" returns {@code null}.
 */
public class SqlTemplatesDialog {

    private SqlTemplatesDialog() {
    }

    /**
     * Opens the templates dialog and returns the SQL of the selected template, or
     * {@code null} when the user cancels.
     */
    public static String show( Component parent, LinkedHashMap<String, String> templatesMap ) {
        return show(parent, templatesMap, "SQL Templates", "Select a template:");
    }

    /**
     * Opens the dialog with a custom title and label, as for other kinds of scripts.
     */
    public static String show( Component parent, LinkedHashMap<String, String> templatesMap, String title, String label ) {
        return show(parent, templatesMap, title, label, null);
    }

    /**
     * Opens the dialog with a custom title, label and preview component, as a text pane with
     * syntax highlighting. A <code>null</code> preview gives a plain text area.
     */
    public static String show( Component parent, LinkedHashMap<String, String> templatesMap, String title, String label,
            JTextComponent preview ) {
        String[] names = templatesMap.keySet().toArray(new String[0]);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for( String name : names ) {
            listModel.addElement(name);
        }

        JList<String> templateList = new JList<>(listModel);
        templateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateList.setSelectedIndex(0);

        Border rowBorder = new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(2, 4, 2, 4));
        templateList.setCellRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent( JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus ) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(rowBorder);
                return this;
            }
        });

        JTextComponent previewComponent = preview;
        if (previewComponent == null) {
            JTextArea textArea = new JTextArea();
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFont(templateList.getFont().deriveFont(Font.PLAIN));
            previewComponent = textArea;
        }
        JTextComponent previewArea = previewComponent;
        previewArea.setEditable(false);
        previewArea.setText(templatesMap.get(names[0]));

        templateList.addListSelectionListener(ev -> {
            if (!ev.getValueIsAdjusting()) {
                String sel = templateList.getSelectedValue();
                if (sel != null) {
                    previewArea.setText(templatesMap.get(sel));
                    previewArea.setCaretPosition(0);
                }
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(templateList), new JScrollPane(previewArea));
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(250);

        JButton okButton = new JButton("Use");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 4));
        contentPanel.add(new JLabel(label), BorderLayout.NORTH);
        contentPanel.add(splitPane, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().add(contentPanel, BorderLayout.CENTER);
        dialog.setSize(800, 700);
        dialog.setLocationRelativeTo(parent);

        final String[] result = {null};

        okButton.addActionListener(ev -> {
            String sel = templateList.getSelectedValue();
            if (sel != null) {
                result[0] = templatesMap.get(sel);
            }
            dialog.dispose();
        });
        cancelButton.addActionListener(ev -> dialog.dispose());
        templateList.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked( MouseEvent ev ) {
                if (ev.getClickCount() == 2) {
                    String sel = templateList.getSelectedValue();
                    if (sel != null) {
                        result[0] = templatesMap.get(sel);
                    }
                    dialog.dispose();
                }
            }
        });

        dialog.setVisible(true);
        return result[0];
    }
}
