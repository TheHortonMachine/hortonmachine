/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gears.utils.simplereport;

/**
 * A simple report interface.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface ISimpleReport {
    
    /**
     * @return the file extention.
     */
    public String getFileExtension();

    /**
     * Insert a newline in the report.
     * 
     * @param sb the {@link StringBuilder} to write to.
     * @param n the number of newlines to add.
     */
    public abstract void newLine( StringBuilder sb, int n );

    /**
     * Open the report.
     * 
     * @param sb the {@link StringBuilder} to write to.
     * @param title a title for the report.
     */
    public abstract void openReport( StringBuilder sb, String title );

    /**
     * Close the report.
     * 
     * @param sb the {@link StringBuilder} to write to.
     */
    public abstract void closeReport( StringBuilder sb );

    /**
     * Open a table.
     * 
     * @param sb the {@link StringBuilder} to write to.
     * @param widthPercentage
     */
    public abstract void openTable( StringBuilder sb, int widthPercentage );

    /**
     * Close a table.
     * 
     * @param sb the {@link StringBuilder} to write to.
     */
    public abstract void closeTable( StringBuilder sb );

    /**
     * Open a row.
     * 
     * @param sb the {@link StringBuilder} to write to.
     * @param span the vertical span of the row.
     */
    public abstract void openTableRow( StringBuilder sb );

    /**
     * Close a row.
     * 
     * @param sb the {@link StringBuilder} to write to.
     */
    public abstract void closeTableRow( StringBuilder sb );

    /**
     * Open a table cell.
     * 
     * @param sb the {@link StringBuilder} to write to.
     * @param color background color for the cell. 
     * @param perc percentage of the cell for the width.
     * @param colSpan col span of the cell.
     * @param rowSpan row span of the cell.
     */
    public abstract void openTableCell( StringBuilder sb, String color, String perc, String colSpan, String rowSpan );

    /**
     * Close a table.
     * 
     * @param sb the {@link StringBuilder} to write to.
     */
    public abstract void closeTableCell( StringBuilder sb );

    /**
     * Heading 1 title.
     * 
     * @param sb the {@link StringBuilder} to write to.
     * @param title the title text.
     */
    public abstract void titleH1( StringBuilder sb, String title );

    /**
     * Heading 2 title.
     * 
     * @param sb the {@link StringBuilder} to write to.
     * @param title the title text.
     */
    public abstract void titleH2( StringBuilder sb, String title );

    /**
     * Heading 3 title.
     * 
     * @param sb the {@link StringBuilder} to write to.
     * @param title the title text.
     */
    public abstract void titleH3( StringBuilder sb, String title );

    /**
     * Heading 4 title.
     * 
     * @param sb the {@link StringBuilder} to write to.
     * @param title the title text.
     */
    public abstract void titleH4( StringBuilder sb, String title );

    /**
     * Style text as bold.
     * 
     * @param sb the {@link StringBuilder} to write to.
     * @param text the text.
     */
    public abstract void bold( StringBuilder sb, String text );

}