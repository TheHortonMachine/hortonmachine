/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.docs.ModuleDocsGenerator;

/**
 * The panel of the selected module: a header with name, folder, status and description,
 * the parameters and the help in tabs and the buttons to run the module.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ModulePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final String CARD_EMPTY = "empty";
    private static final String CARD_MODULE = "module";

    private final ParametersPanel parametersPanel;
    private final CardLayout cardLayout = new CardLayout();
    private final JLabel nameLabel = new JLabel();
    private final JLabel infoLabel = new JLabel();
    private final JTextArea descriptionArea = new JTextArea();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JEditorPane helpPane = new JEditorPane();
    private final JScrollPane parametersScrollPane;
    private final JScrollPane helpScrollPane;

    /**
     * @param parametersPanel the panel of the parameters.
     * @param runAction the action that runs the current module.
     * @param runIcon the icon of the run button, can be <code>null</code>.
     * @param scriptAction the action that saves the current module as script.
     * @param scriptIcon the icon of the script button, can be <code>null</code>.
     */
    public ModulePanel( ParametersPanel parametersPanel, ActionListener runAction, Icon runIcon, ActionListener scriptAction,
            Icon scriptIcon ) {
        this.parametersPanel = parametersPanel;
        setLayout(cardLayout);

        JLabel emptyLabel = new JLabel("Select a module in the tree to see its parameters.", SwingConstants.CENTER);
        emptyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        add(emptyLabel, CARD_EMPTY);

        JPanel modulePanel = new JPanel(new BorderLayout(0, 8));
        modulePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        modulePanel.add(createHeader(), BorderLayout.NORTH);

        parametersPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        parametersScrollPane = new JScrollPane(parametersPanel);
        parametersScrollPane.setBorder(BorderFactory.createEmptyBorder());
        parametersScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        parametersScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        helpPane.setEditable(false);
        helpPane.setContentType("text/html");
        helpPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        helpPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        styleHelp();
        helpScrollPane = new JScrollPane(helpPane);
        helpScrollPane.setBorder(BorderFactory.createEmptyBorder());

        tabbedPane.addTab("Parameters", parametersScrollPane);
        tabbedPane.addTab("Help", helpScrollPane);
        modulePanel.add(tabbedPane, BorderLayout.CENTER);

        JButton scriptButton = new JButton("Save as script...", scriptIcon);
        scriptButton.setToolTipText("Save the current module as a script to file.");
        scriptButton.addActionListener(scriptAction);
        JButton runButton = new JButton("Run", runIcon);
        runButton.setToolTipText("Start the current module.");
        runButton.setFont(runButton.getFont().deriveFont(Font.BOLD));
        runButton.addActionListener(runAction);
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttonsPanel.add(scriptButton);
        buttonsPanel.add(runButton);
        modulePanel.add(buttonsPanel, BorderLayout.SOUTH);

        add(modulePanel, CARD_MODULE);
        cardLayout.show(this, CARD_EMPTY);
    }

    /**
     * The wrapping texts ask for their whole length on one line: cap the width,
     * else the panel takes the space of the modules tree.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.min(size.width, 500), size.height);
    }

    private JPanel createHeader() {
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, nameLabel.getFont().getSize2D() + 5f));
        infoLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        descriptionArea.setEditable(false);
        descriptionArea.setFocusable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        descriptionArea.setFont(UIManager.getFont("Label.font"));
        descriptionArea.setForeground(UIManager.getColor("Label.foreground"));

        JPanel titlePanel = new JPanel(new BorderLayout(10, 0));
        titlePanel.add(nameLabel, BorderLayout.WEST);
        titlePanel.add(infoLabel, BorderLayout.CENTER);

        JPanel header = new JPanel(new BorderLayout());
        header.add(titlePanel, BorderLayout.NORTH);
        header.add(descriptionArea, BorderLayout.CENTER);
        return header;
    }

    private void styleHelp() {
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = kit.getStyleSheet();
        Color border = UIManager.getColor("Separator.foreground");
        String borderHex = border != null ? String.format("#%02x%02x%02x", border.getRed(), border.getGreen(), border.getBlue()) : "#cccccc";
        styleSheet.addRule("h3 { margin-top: 12px; margin-bottom: 4px; }");
        styleSheet.addRule("table { border-collapse: collapse; border-color: " + borderHex + "; }");
        styleSheet.addRule("th { border-color: " + borderHex + "; }");
        styleSheet.addRule("td { border-color: " + borderHex + "; }");
        styleSheet.addRule("code { font-family: monospaced; }");
        helpPane.setEditorKit(kit);
    }

    /**
     * Show a module, or the hint to select one if <code>null</code>.
     */
    public void setModule( ModuleDescription module ) {
        parametersPanel.setModule(module);
        if (module == null) {
            cardLayout.show(this, CARD_EMPTY);
            return;
        }

        nameLabel.setText(module.getName());
        String folder = module.getCategory().replaceFirst("^HortonMachine/", "").replace("/", " › ");
        String info = folder;
        if (module.getStatus() == ModuleDescription.Status.experimental) {
            info = info + "  ·  experimental";
        }
        infoLabel.setText(info);
        // the header shows the first sentence, the help the whole description
        String description = module.getDescription() != null ? module.getDescription().trim() : "";
        int sentenceEnd = description.indexOf(". ");
        String shortDescription = sentenceEnd > 0 ? description.substring(0, sentenceEnd + 1) : description;
        descriptionArea.setText(shortDescription);
        descriptionArea.setToolTipText(shortDescription.equals(description) ? null : description);
        descriptionArea.setVisible(!description.isEmpty());

        String help;
        try {
            help = ModuleDocsGenerator.generateHtml(module.getModuleClass());
        } catch (Exception e) {
            Logger.INSTANCE.insertError("ModulePanel", "Unable to create the help of " + module.getClassName(), e);
            help = "<html><body>No help available.</body></html>";
        }
        helpPane.setText(help);
        helpPane.setCaretPosition(0);

        parametersScrollPane.getVerticalScrollBar().setValue(0);
        cardLayout.show(this, CARD_MODULE);
        revalidate();
        repaint();
    }
}
