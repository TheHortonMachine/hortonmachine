package org.hortonmachine.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
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

/**
 * Modal dialog that displays SQL command history as a selectable list with a
 * full-text preview pane. Double-clicking an entry or pressing "Use" returns
 * the selected SQL; "Cancel" returns {@code null}.
 */
public class SqlHistoryDialog {

    private SqlHistoryDialog() {
    }

    /**
     * Opens the history dialog and returns the selected SQL, or {@code null} when
     * the user cancels.
     */
    public static String show( Component parent, List<String> history ) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for( String cmd : history ) {
            listModel.addElement(cmd);
        }

        JList<String> historyList = new JList<>(listModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setSelectedIndex(0);
        Border rowBorder = new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(2, 4, 2, 4));
        historyList.setCellRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent( JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus ) {
                String text = value.toString().replaceAll("\\s+", " ").trim();
                if (text.length() > 80) {
                    text = text.substring(0, 80) + "…";
                }
                super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
                setBorder(rowBorder);
                return this;
            }
        });

        JTextArea previewArea = new JTextArea();
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setFont(historyList.getFont().deriveFont(Font.PLAIN));
        previewArea.setText(listModel.getElementAt(0));

        historyList.addListSelectionListener(ev -> {
            if (!ev.getValueIsAdjusting()) {
                String sel = historyList.getSelectedValue();
                if (sel != null) {
                    previewArea.setText(sel);
                    previewArea.setCaretPosition(0);
                }
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(historyList), new JScrollPane(previewArea));
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(250);

        JButton okButton = new JButton("Use");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 4));
        contentPanel.add(new JLabel("Select a query from the history:"), BorderLayout.NORTH);
        contentPanel.add(splitPane, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "SQL History", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().add(contentPanel, BorderLayout.CENTER);
        dialog.setSize(800, 700);
        dialog.setLocationRelativeTo(parent);

        final String[] result = {null};

        okButton.addActionListener(ev -> {
            result[0] = historyList.getSelectedValue();
            dialog.dispose();
        });
        cancelButton.addActionListener(ev -> dialog.dispose());
        historyList.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked( MouseEvent ev ) {
                if (ev.getClickCount() == 2) {
                    result[0] = historyList.getSelectedValue();
                    dialog.dispose();
                }
            }
        });

        dialog.setVisible(true);
        return result[0];
    }
}
