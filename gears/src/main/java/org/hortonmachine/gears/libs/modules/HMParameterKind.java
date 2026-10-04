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
package org.hortonmachine.gears.libs.modules;

/**
 * The role a module parameter has for the user, as used to group parameters in user interfaces.
 *
 * <p>The role is derived from the <code>@In</code>/<code>@Out</code> annotations and the UI hints,
 * so that the Spatial Toolbox, the documentation and the command line group them the same way.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public enum HMParameterKind {
    /**
     * Data read by the module: files and folders.
     */
    INPUT("Inputs"),
    /**
     * Values that tune the module.
     */
    PARAMETER("Parameters"),
    /**
     * Data produced by the module: files and folders written and the <code>@Out</code> values.
     */
    OUTPUT("Outputs");

    private final String title;

    private HMParameterKind( String title ) {
        this.title = title;
    }

    /**
     * @return the title to use for a group of parameters of this kind.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the kind of a parameter.
     *
     * @param isIn if the field is annotated with <code>@In</code>.
     * @param isOut if the field is annotated with <code>@Out</code>.
     * @param uiHint the value of the <code>@UI</code> annotation, can be <code>null</code>.
     * @return the kind of the parameter.
     */
    public static HMParameterKind of( boolean isIn, boolean isOut, String uiHint ) {
        if (uiHint == null) {
            uiHint = "";
        }
        // files written by the module are inputs of the class, but outputs for the user
        if (uiHint.contains(HMConstants.FILEOUT_UI_HINT) || uiHint.contains(HMConstants.FOLDEROUT_UI_HINT) || (isOut && !isIn)) {
            return OUTPUT;
        }
        // all the specific input file hints start with the generic one
        if (uiHint.contains(HMConstants.FILEIN_UI_HINT_GENERIC) || uiHint.contains(HMConstants.FOLDERIN_UI_HINT)) {
            return INPUT;
        }
        return PARAMETER;
    }
}
