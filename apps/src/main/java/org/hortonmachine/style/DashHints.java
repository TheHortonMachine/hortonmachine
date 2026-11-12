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
package org.hortonmachine.style;

import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * The hints of the fields of the dashes of a stroke, whose format is not evident.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
class DashHints {

    /** The client property of FlatLaf showing a text in an empty field. */
    private static final String PLACEHOLDER = "JTextField.placeholderText";

    private static final String DASH_TIP = "<html>The lengths of the dashes and of the gaps between them, in pixels,"
            + " separated by commas or spaces:<br>"
            + "<b>6,3</b> &nbsp;a dash of 6 and a gap of 3<br>"
            + "<b>1,3</b> &nbsp;dots<br>"
            + "<b>10,3,2,3</b> &nbsp;dash and dot<br>"
            + "Empty for a solid line.</html>";

    private static final String OFFSET_TIP = "<html>The pixels the dash pattern is moved along the line, as <b>3</b>,"
            + "<br>to start it with a part of a dash or of a gap. Usually empty.</html>";

    private DashHints() {
    }

    /**
     * Give the fields of the dashes an example and an explanation.
     */
    static void setup( JLabel dashLabel, JTextField dashField, JLabel offsetLabel, JTextField offsetField ) {
        dashLabel.setToolTipText(DASH_TIP);
        dashField.setToolTipText(DASH_TIP);
        dashField.putClientProperty(PLACEHOLDER, "e.g. 6,3 (dash, gap)");
        offsetLabel.setToolTipText(OFFSET_TIP);
        offsetField.setToolTipText(OFFSET_TIP);
        offsetField.putClientProperty(PLACEHOLDER, "e.g. 3");
    }
}
