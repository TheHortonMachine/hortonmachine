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
package org.hortonmachine.nww.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.time.DatedFilesScanner;
import org.hortonmachine.gears.utils.time.DatedFilesScanner.DatedFile;
import org.hortonmachine.gears.utils.time.DatedFilesScanner.ScanResult;
import org.hortonmachine.gui.utils.GuiUtilities;

/**
 * Choose a folder of rasters with a date in their names, as monthly or daily GeoTIFFs, to show
 * them as a time series. The dates are read with the first fitting pattern of
 * {@link DatedFilesScanner#AUTO_PATTERNS}; if none fits the names, the user is asked to write
 * the pattern, with a preview of the files and dates it finds.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TemporalRasterDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    /** The fewest dates of a series. */
    private static final int MIN_DATES = 2;

    /** FlatLaf client properties. */
    private static final String PLACEHOLDER = "JTextField.placeholderText";
    private static final String OUTLINE = "JComponent.outline";

    /**
     * The chosen series.
     *
     * @param name the name of the layer.
     * @param folder the folder.
     * @param pattern the pattern of the dates.
     * @param files the dated files, sorted by date.
     */
    public record Choice( String name, File folder, String pattern, List<DatedFile> files ) {
    }

    private final JTextField folderField = new JTextField(30);
    private final JButton browseButton = new JButton("Browse...");
    private final JCheckBox recursiveCheck = new JCheckBox("Include the sub folders", true);
    private final JTextField patternField = new JTextField(16);
    private final JTextField nameField = new JTextField(20);
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Date", "File"}, 0){
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable( int row, int column ) {
            return false;
        }
    };
    private final JButton okButton = new JButton("Load");

    /** The files listed in the folder, <code>null</code> while listing. */
    private List<File> listedFiles;
    private File listedFolder;
    private ScanResult result;
    private Choice choice;
    /** The count of the listings, of which only the last is used. */
    private int listRequest;
    /** Scans again a while after the last key in the pattern field. */
    private final Timer patternTimer = new Timer(300, e -> rescan());

    private TemporalRasterDialog( Window owner, File folder ) {
        super(owner, "Open temporal raster folder", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        folderField.setText(folder.getAbsolutePath());
        folderField.setEditable(false);
        browseButton.addActionListener(e -> browse());
        recursiveCheck.setToolTipText("Also look for the files in the folders inside the chosen one");
        recursiveCheck.addActionListener(e -> listFolder());
        patternField.setToolTipText("<html>Empty to find the pattern of the dates automatically, among "
                + String.join(", ", DatedFilesScanner.AUTO_PATTERNS) + ".<br>"
                + "Letters: <code>yyyy</code> year, <code>MM</code> month, <code>dd</code> day, <code>DDD</code> day of the year, "
                + "<code>HH</code> hour, <code>mm</code> minutes;<br>the other characters are taken as they are. "
                + "With <code>/</code> the pattern is looked for in the path from the folder, as <code>yyyy/DDD</code>.</html>");
        patternTimer.setRepeats(false);
        patternField.getDocument().addDocumentListener(new DocumentListener(){
            @Override
            public void insertUpdate( DocumentEvent e ) {
                patternTimer.restart();
            }

            @Override
            public void removeUpdate( DocumentEvent e ) {
                patternTimer.restart();
            }

            @Override
            public void changedUpdate( DocumentEvent e ) {
                patternTimer.restart();
            }
        });
        nameField.setText(folder.getName());
        nameField.setToolTipText("The name of the layer");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        c.gridx = 0;
        form.add(new JLabel("Folder"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(folderField, c);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(browseButton, c);
        c.gridy = 1;
        c.gridx = 1;
        form.add(recursiveCheck, c);
        c.gridy = 2;
        c.gridx = 0;
        form.add(new JLabel("Date pattern"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(patternField, c);
        c.gridy = 3;
        c.gridx = 1;
        c.gridwidth = 2;
        JLabel help = new JLabel("<html>Empty for automatic. As <code>yyyy_DDD</code> for ndvi_2018_045.tif, "
                + "<code>yyyy/MM/dd</code> for 2018/01/07.tif.</html>");
        help.setForeground(UIManager.getColor("Label.disabledForeground"));
        form.add(help, c);
        c.gridwidth = 1;
        c.gridy = 4;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Layer name"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(nameField, c);

        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(420);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(600, 260));

        okButton.setEnabled(false);
        okButton.addActionListener(e -> accept());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.add(okButton);
        buttons.add(cancelButton);
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.add(statusLabel, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(form, BorderLayout.NORTH);
        content.add(tableScroll, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(okButton);
        pack();
        setLocationRelativeTo(owner);

        listFolder();
    }

    /**
     * Choose a folder, then show the dialog of its dates.
     *
     * @param owner the window of the dialog.
     * @return the chosen series, or <code>null</code> if cancelled.
     */
    public static Choice choose( Window owner ) {
        File[] folders = GuiUtilities.showOpenFolderDialog(owner, "Folder of the dated rasters", false,
                PreferencesHandler.getLastFile());
        if (folders == null || folders.length == 0 || folders[0] == null || !folders[0].isDirectory()) {
            return null;
        }
        TemporalRasterDialog dialog = new TemporalRasterDialog(owner, folders[0]);
        dialog.setVisible(true);
        return dialog.choice;
    }

    private void browse() {
        File[] folders = GuiUtilities.showOpenFolderDialog(this, "Folder of the dated rasters", false,
                new File(folderField.getText()));
        if (folders != null && folders.length > 0 && folders[0] != null && folders[0].isDirectory()) {
            folderField.setText(folders[0].getAbsolutePath());
            nameField.setText(folders[0].getName());
            listFolder();
        }
    }

    /**
     * List the raster files of the folder in the background, then date them.
     */
    private void listFolder() {
        File folder = new File(folderField.getText());
        boolean recursive = recursiveCheck.isSelected();
        int request = ++listRequest;
        listedFiles = null;
        result = null;
        model.setRowCount(0);
        okButton.setEnabled(false);
        setStatus("Looking for the raster files...", false);
        new SwingWorker<List<File>, Void>(){
            @Override
            protected List<File> doInBackground() throws Exception {
                return DatedFilesScanner.listFiles(folder, recursive, DatedFilesScanner.RASTER_EXTENSIONS);
            }

            @Override
            protected void done() {
                if (request != listRequest) {
                    return;
                }
                try {
                    listedFiles = get();
                    listedFolder = folder;
                    rescan();
                } catch (Exception e) {
                    e.printStackTrace();
                    setStatus("Unable to list the files: " + e.getMessage(), true);
                }
            }
        }.execute();
    }

    /**
     * Date the listed files with the pattern, or automatically, and show them.
     */
    private void rescan() {
        if (listedFiles == null) {
            return;
        }
        model.setRowCount(0);
        result = null;
        okButton.setEnabled(false);
        patternField.putClientProperty(OUTLINE, null);
        if (listedFiles.isEmpty()) {
            setStatus("No raster files (" + String.join(", ", DatedFilesScanner.RASTER_EXTENSIONS) + ") in the folder"
                    + (recursiveCheck.isSelected() ? "." : ": include the sub folders?"), true);
            return;
        }
        String pattern = patternField.getText().trim();
        boolean auto = pattern.isEmpty();
        if (!auto) {
            String problem = DatedFilesScanner.validate(pattern);
            if (problem != null) {
                patternField.putClientProperty(OUTLINE, "error");
                setStatus(problem, true);
                return;
            }
        }
        ScanResult scan = DatedFilesScanner.scan(listedFolder, listedFiles, auto ? null : pattern);
        patternField.putClientProperty(PLACEHOLDER, auto && scan.pattern() != null ? "automatic: " + scan.pattern() : "automatic");
        fillTable(scan);

        int dated = scan.files().size();
        String counts = dated + (dated == 1 ? " date of " : " dates of ") + listedFiles.size() + " raster files" + (scan.pattern() != null ? ", with " + scan.pattern() : "")
                + (scan.undated().isEmpty() ? "" : ", " + scan.undated().size() + " without date")
                + (scan.duplicates().isEmpty() ? "" : ", " + scan.duplicates().size() + " of dates already found, skipped");
        // the automatic patterns fit if they date most of the files, one per date
        boolean fits = dated >= MIN_DATES && dated >= scan.undated().size() + scan.duplicates().size();
        if (auto && !fits) {
            patternField.putClientProperty(OUTLINE, "warning");
            patternField.requestFocusInWindow();
            setStatus("<html>" + counts + ".<br><b>The automatic patterns don't fit the names of these files: "
                    + "write the pattern of their dates.</b></html>", true);
        } else if (dated < MIN_DATES) {
            setStatus(counts + ": at least " + MIN_DATES + " dates are needed.", true);
        } else {
            setStatus(counts + ".", false);
        }
        if (dated >= MIN_DATES) {
            result = scan;
            okButton.setEnabled(true);
        }
    }

    private void fillTable( ScanResult scan ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(scan.hasTime() ? "yyyy-MM-dd HH:mm" : "yyyy-MM-dd");
        for( DatedFile datedFile : scan.files() ) {
            model.addRow(new Object[]{datedFile.dateTime().format(formatter), relativePath(datedFile.file())});
        }
        for( DatedFile datedFile : scan.duplicates() ) {
            model.addRow(new Object[]{datedFile.dateTime().format(formatter) + " (skipped)", relativePath(datedFile.file())});
        }
        for( File file : scan.undated() ) {
            model.addRow(new Object[]{"no date", relativePath(file)});
        }
    }

    private String relativePath( File file ) {
        String folderPath = listedFolder.getAbsolutePath();
        String path = file.getAbsolutePath();
        return path.startsWith(folderPath) ? path.substring(folderPath.length() + 1) : path;
    }

    private void accept() {
        if (result == null) {
            return;
        }
        String name = nameField.getText().trim();
        choice = new Choice(name.isEmpty() ? listedFolder.getName() : name, listedFolder, result.pattern(), result.files());
        dispose();
    }

    private void setStatus( String text, boolean problem ) {
        Color color = problem ? UIManager.getColor("Component.error.focusedBorderColor") : UIManager.getColor("Label.foreground");
        statusLabel.setForeground(color != null ? color : problem ? Color.RED : Color.BLACK);
        statusLabel.setText(text);
    }
}
