package org.hortonmachine.database;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

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
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;

/**
 * Modal dialog showing the last 10 recent connections as a selectable list
 * with a detail preview pane. Double-clicking or pressing "Connect" returns
 * the chosen {@link ConnectionData}; "Cancel" returns {@code null}.
 */
public class RecentConnectionsDialog {

    private RecentConnectionsDialog() {
    }

    public static ConnectionData show( Component parent, List<ConnectionData> recentList ) {
        DefaultListModel<ConnectionData> listModel = new DefaultListModel<>();
        for( ConnectionData cd : recentList ) {
            listModel.addElement(cd);
        }

        JList<ConnectionData> connectionList = new JList<>(listModel);
        connectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        connectionList.setSelectedIndex(0);

        Border rowBorder = new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(2, 4, 2, 4));
        connectionList.setCellRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent( JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus ) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ConnectionData) {
                    setText(((ConnectionData) value).connectionLabel);
                }
                setBorder(rowBorder);
                return this;
            }
        });

        JTextArea detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setLineWrap(false);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, connectionList.getFont().getSize()));
        if (!recentList.isEmpty()) {
            detailArea.setText(toDetailText(recentList.get(0)));
        }

        connectionList.addListSelectionListener(ev -> {
            if (!ev.getValueIsAdjusting()) {
                ConnectionData sel = connectionList.getSelectedValue();
                if (sel != null) {
                    detailArea.setText(toDetailText(sel));
                    detailArea.setCaretPosition(0);
                }
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(connectionList), new JScrollPane(detailArea));
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(200);

        JButton connectButton = new JButton("Connect");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 4));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPanel.add(new JLabel("Select a recent connection:"), BorderLayout.NORTH);
        contentPanel.add(splitPane, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Recent Connections", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().add(contentPanel, BorderLayout.CENTER);
        dialog.setSize(600, 450);
        dialog.setLocationRelativeTo(parent);

        final ConnectionData[] result = {null};

        connectButton.addActionListener(ev -> {
            result[0] = connectionList.getSelectedValue();
            dialog.dispose();
        });
        cancelButton.addActionListener(ev -> dialog.dispose());
        connectionList.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked( MouseEvent ev ) {
                if (ev.getClickCount() == 2) {
                    result[0] = connectionList.getSelectedValue();
                    dialog.dispose();
                }
            }
        });

        dialog.setVisible(true);
        return result[0];
    }

    private static String toDetailText( ConnectionData cd ) {
        String typeName;
        try {
            typeName = EDb.forCode(cd.dbType).name();
        } catch (Exception e) {
            typeName = "unknown (" + cd.dbType + ")";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Type:  ").append(typeName).append("\n");
        sb.append("URL:   ").append(cd.connectionUrl != null ? cd.connectionUrl : "").append("\n");
        sb.append("User:  ").append(cd.user != null ? cd.user : "");
        return sb.toString();
    }
}
