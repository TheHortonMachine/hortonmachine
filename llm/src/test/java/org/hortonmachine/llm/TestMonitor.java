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
package org.hortonmachine.llm;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;

/**
 * A monitor for the tests, that can be canceled and records tasks and work.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestMonitor extends DummyProgressMonitor {

    public volatile boolean canceled = false;
    public final List<String> tasks = new ArrayList<>();
    public int worked = 0;
    public boolean done = false;

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
        tasks.add(name);
    }

    @Override
    public void worked( int work ) {
        worked += work;
    }

    @Override
    public void done() {
        done = true;
    }
}
