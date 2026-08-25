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
package org.hortonmachine.database.csv;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.dbs.utils.csv.CsvColumnSchema;
import org.hortonmachine.dbs.utils.csv.CsvSchemaGuesser;
import org.hortonmachine.dbs.utils.csv.CsvTableImporter;
import org.hortonmachine.dbs.utils.csv.CsvUtils;
import org.hortonmachine.gears.libs.modules.HMFileFilter;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.console.LogConsoleController;
import org.hortonmachine.gui.utils.GuiBridgeHandler;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.ImageCache;

/**
 * Database-level action: picks a CSV file, lets the user review/correct its
 * guessed column schema (see {@link CsvSchemaEditorDialog}), then creates a
 * new table from that schema and imports every row into it.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ImportCsvAsNewTableAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private final ADb db;
    private final GuiBridgeHandler guiBridge;
    private final IHMProgressMonitor pm;
    private final Component parent;
    private final Runnable onImportComplete;

    public ImportCsvAsNewTableAction( ADb db, GuiBridgeHandler guiBridge, IHMProgressMonitor pm, Component parent,
            Runnable onImportComplete ) {
        super("Create table from CSV", ImageCache.get(ImageCache.IMPORT));
        this.db = db;
        this.guiBridge = guiBridge;
        this.pm = pm;
        this.parent = parent;
        this.onImportComplete = onImportComplete;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        File[] openFiles = guiBridge.showOpenFileDialog("Open CSV File", PreferencesHandler.getLastFile(),
                new HMFileFilter("CSV Files", new String[]{".csv"}));
        if (openFiles == null || openFiles.length == 0) {
            return;
        }
        File csvFile = openFiles[0];
        try {
            PreferencesHandler.setLastPath(csvFile.getAbsolutePath());
        } catch (Exception ex) {
            Logger.INSTANCE.insertError("ImportCsvAsNewTableAction", "ERROR", ex);
        }

        try {
            char delimiter = CsvUtils.detectDelimiter(CsvUtils.readRawHeaderLine(csvFile));
            List<CsvColumnSchema> schema = CsvSchemaGuesser.guessSchema(csvFile, delimiter,
                    CsvSchemaGuesser.DEFAULT_SAMPLE_ROWS);
            List<String> previewHeader = CsvUtils.readHeader(csvFile, delimiter);
            List<List<String>> previewRows = CsvUtils.readSampleRows(csvFile, delimiter, 8);

            CsvSchemaEditorDialog.Result result = CsvSchemaEditorDialog.show(parent, schema, previewHeader, previewRows, true);
            if (result == null) {
                return;
            }
            List<CsvColumnSchema> confirmedSchema = result.schema;

            String nameWithoutExtension = csvFile.getName().replaceFirst("\\.[^.]+$", "");
            String newTableName = GuiUtilities.showInputDialog(parent, "Set new table name (can't start with numbers)",
                    nameWithoutExtension);
            if (newTableName == null || newTableName.trim().isEmpty()) {
                return;
            }
            SqlName tableName = SqlName.m(newTableName.trim());

            final LogConsoleController logConsole = new LogConsoleController(pm);
            JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");
            new Thread(() -> {
                boolean hasErrors = false;
                logConsole.beginProcess("Importing CSV as new table...");
                try {
                    CsvTableImporter.createTableFromCsv(db, tableName, csvFile, delimiter, confirmedSchema, result.primaryKey);
                    onImportComplete.run();
                } catch (Exception ex) {
                    Logger.INSTANCE.insertError("ImportCsvAsNewTableAction", "Error importing CSV as new table", ex);
                    hasErrors = true;
                } finally {
                    logConsole.finishProcess();
                    logConsole.stopLogging();
                    if (!hasErrors) {
                        logConsole.setVisible(false);
                        window.dispose();
                    }
                }
            }, "ImportCsvAsNewTableAction->import").start();
        } catch (Exception ex) {
            GuiUtilities.handleError(parent, ex);
        }
    }
}
