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
package org.hortonmachine.assistant;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;

/**
 * A monitor for the assistant requests, shown in a progress bar and a label, and canceled
 * from the UI through {@link #setCanceled(boolean)}.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class AssistantProgressMonitor extends DummyProgressMonitor {

    private final JProgressBar progressBar;
    private final JLabel label;
    private volatile boolean canceled = false;
    private int worked = 0;
    private int total = 0;
    private String taskName = "";

    /**
     * @param progressBar the bar to update, or <code>null</code>.
     * @param label the label for the task names, or <code>null</code>.
     */
    public AssistantProgressMonitor( JProgressBar progressBar, JLabel label ) {
        this.progressBar = progressBar;
        this.label = label;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled( boolean value ) {
        canceled = value;
    }

    @Override
    public void beginTask( String name, int totalWork ) {
        worked = 0;
        total = totalWork;
        taskName = name;
        SwingUtilities.invokeLater(() -> {
            if (progressBar != null) {
                progressBar.setIndeterminate(totalWork <= 0);
                progressBar.setMaximum(Math.max(totalWork, 1));
                progressBar.setValue(0);
                progressBar.setString(name);
            }
            if (label != null) {
                label.setText(name);
            }
        });
    }

    @Override
    public void worked( int work ) {
        worked += work;
        int value = worked;
        String text = total > 0 ? taskName + " " + Math.min(100, value * 100 / total) + "%" : taskName;
        SwingUtilities.invokeLater(() -> {
            if (progressBar != null) {
                progressBar.setValue(value);
                progressBar.setString(text);
            }
        });
    }

    @Override
    public void done() {
        SwingUtilities.invokeLater(() -> {
            if (progressBar != null) {
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
                progressBar.setString("");
            }
        });
    }
}
