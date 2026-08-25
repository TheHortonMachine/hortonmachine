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
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.utils.csv.CsvColumnSchema;
import org.hortonmachine.dbs.utils.csv.CsvSchemaGuesser;
import org.hortonmachine.dbs.utils.csv.CsvTableImporter;
import org.hortonmachine.dbs.utils.csv.CsvTableImporter.ColumnMatch;
import org.hortonmachine.dbs.utils.csv.CsvUtils;
import org.hortonmachine.gears.libs.modules.HMFileFilter;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.console.LogConsoleController;
import org.hortonmachine.gui.utils.GuiBridgeHandler;
import org.hortonmachine.gui.utils.GuiUtilities;

/**
 * Table-level action: picks a CSV file, lets the user review/correct its
 * guessed column schema (see {@link CsvSchemaEditorDialog}), matches the CSV's
 * columns against {@code table}'s existing columns by name, shows the user any
 * mismatch before proceeding, then imports the matched columns' rows into the
 * table.
 *
 * <p>
 * Used both as the always-available generic "Import CSV into table" action,
 * and, under a different display name, as the "Import timeseries from CSV"
 * action shown only for tables that look timeseries-shaped (see
 * {@code TimeseriesTableUtils#isTimeseriesTable(TableLevel)}) - the behavior
 * is identical either way; only the label and when it's offered differ.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ImportCsvIntoTableAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private final ADb db;
    private final TableLevel table;
    private final GuiBridgeHandler guiBridge;
    private final IHMProgressMonitor pm;
    private final Component parent;
    private final Runnable onImportComplete;

    public ImportCsvIntoTableAction( String actionLabel, ADb db, TableLevel table, GuiBridgeHandler guiBridge,
            IHMProgressMonitor pm, Component parent, Runnable onImportComplete ) {
        super(actionLabel);
        this.db = db;
        this.table = table;
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
            Logger.INSTANCE.insertError("ImportCsvIntoTableAction", "ERROR", ex);
        }

        try {
            char delimiter = CsvUtils.detectDelimiter(CsvUtils.readRawHeaderLine(csvFile));
            List<CsvColumnSchema> schema = CsvSchemaGuesser.guessSchema(csvFile, delimiter,
                    CsvSchemaGuesser.DEFAULT_SAMPLE_ROWS);
            List<String> previewHeader = CsvUtils.readHeader(csvFile, delimiter);
            List<List<String>> previewRows = CsvUtils.readSampleRows(csvFile, delimiter, 8);

            List<CsvColumnSchema> confirmedSchema = CsvSchemaEditorDialog.show(parent, schema, previewHeader, previewRows);
            if (confirmedSchema == null) {
                return;
            }

            List<String> tableColumnNames = new ArrayList<>();
            for( ColumnLevel column : table.columnsList ) {
                tableColumnNames.add(column.columnName);
            }
            ColumnMatch match = CsvTableImporter.matchColumns(confirmedSchema, tableColumnNames);
            if (match.matched.isEmpty()) {
                GuiUtilities.showWarningMessage(parent,
                        "None of the CSV's columns match any column of table " + table.tableName.getName() + ".");
                return;
            }
            if (!match.unmatchedCsvColumns.isEmpty() || !match.unmatchedTableColumns.isEmpty()) {
                StringBuilder msg = new StringBuilder();
                msg.append("Columns to import: ").append(String.join(", ", match.matched)).append("\n");
                if (!match.unmatchedCsvColumns.isEmpty()) {
                    msg.append("CSV columns with no match in the table (ignored): ")
                            .append(String.join(", ", match.unmatchedCsvColumns)).append("\n");
                }
                if (!match.unmatchedTableColumns.isEmpty()) {
                    msg.append("Table columns with no match in the CSV (left untouched): ")
                            .append(String.join(", ", match.unmatchedTableColumns)).append("\n");
                }
                msg.append("\nProceed with the import?");
                int result = JOptionPane.showConfirmDialog(parent, msg.toString(), "Column mismatch",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            final LogConsoleController logConsole = new LogConsoleController(pm);
            JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");
            new Thread(() -> {
                boolean hasErrors = false;
                logConsole.beginProcess("Importing CSV into table " + table.tableName.getName() + "...");
                try {
                    CsvTableImporter.importCsvIntoTable(db, table.tableName, csvFile, delimiter, confirmedSchema, match);
                    onImportComplete.run();
                } catch (Exception ex) {
                    Logger.INSTANCE.insertError("ImportCsvIntoTableAction", "Error importing CSV into table", ex);
                    hasErrors = true;
                } finally {
                    logConsole.finishProcess();
                    logConsole.stopLogging();
                    if (!hasErrors) {
                        logConsole.setVisible(false);
                        window.dispose();
                    }
                }
            }, "ImportCsvIntoTableAction->import").start();
        } catch (Exception ex) {
            GuiUtilities.handleError(parent, ex);
        }
    }
}
