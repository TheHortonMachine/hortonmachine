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
 * An csv report helper.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CsvReport implements ISimpleReport {
    private String separator = ",";

    /**
     * Constructor.
     * 
     * @param separator the separator to use. Default is ",".
     */
    public CsvReport( String separator ) {
        this.separator = separator;
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }
    
    @Override
    public void newLine( StringBuilder sb, int n ) {
        for( int i = 0; i < n; i++ ) {
            sb.append("\n");
        }
    }
    @Override
    public void openReport( StringBuilder sb, String title ) {
        sb.append("#").append(title).append("\n");
    }

    @Override
    public void closeReport( StringBuilder sb ) {
        // nothing needed
    }

    @Override
    public void openTable( StringBuilder sb, int widthPercentage ) {
        // nothing needed
    }
    @Override
    public void closeTable( StringBuilder sb ) {
        // nothing needed
    }

    @Override
    public void openTableRow( StringBuilder sb) {
        // nothing needed
    }

    @Override
    public void closeTableRow( StringBuilder sb ) {
        sb.append("\n");
    }

    @Override
    public void openTableCell( StringBuilder sb, String color, String perc, String colSpan, String rowSpan ) {
        // nothing needed
    }

    @Override
    public void closeTableCell( StringBuilder sb ) {
        sb.append(separator);
    }

    @Override
    public void titleH1( StringBuilder sb, String title ) {
        sb.append(title).append("\n");
    }

    @Override
    public void titleH2( StringBuilder sb, String title ) {
        titleH1(sb, title);
    }

    @Override
    public void titleH3( StringBuilder sb, String title ) {
        titleH1(sb, title);
    }

    @Override
    public void titleH4( StringBuilder sb, String title ) {
        titleH1(sb, title);
    }

    @Override
    public void bold( StringBuilder sb, String text ) {
        // not supported
        sb.append(text);
    }

}
