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
 * An html report helper.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HtmlReport implements ISimpleReport {
    @Override
    public String getFileExtension() {
        return "html";
    }

    @Override
    public void newLine( StringBuilder sb, int n ) {
        for( int i = 0; i < n; i++ ) {
            sb.append("<br>").append("\n");
        }
    }
    @Override
    public void openReport( StringBuilder sb, String title ) {
        sb.append("<html>").append("\n");
        sb.append("<head>").append("\n");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">").append("\n");
        sb.append("<title>" + title + "</title>").append("\n");
        sb.append("</head>").append("\n");
        sb.append("<body>").append("\n");
    }
    @Override
    public void closeReport( StringBuilder sb ) {
        sb.append("</body>").append("\n");
        sb.append("</html>").append("\n");
    }
    @Override
    public void openTable( StringBuilder sb, int widthPercentage ) {
        sb.append("<table style=\"border: 2px solid black; border-collapse: collapse; width: " + widthPercentage
                + "%; word-wrap:break-word; table-layout: fixed;\">")
//        sb.append("<table style=\"table-layout: fixed;\" align=\"center\" border=\"1\" cellpadding=\"5\" cellspacing=\"2\" width=\"" + widthPercentage + "%\">")
                .append("\n");
        sb.append("<tbody>").append("\n");
    }
    @Override
    public void closeTable( StringBuilder sb ) {
        sb.append("</tbody>").append("\n");
        sb.append("</table>").append("\n");
    }
    @Override
    public void openTableRow( StringBuilder sb ) {
        sb.append("<tr style=\"border: 2px solid black; padding: 15px; ");
        sb.append("\">").append("\n");
    }
    @Override
    public void closeTableRow( StringBuilder sb ) {
        sb.append("</tr>").append("\n");
    }

    @Override
    public void openTableCell( StringBuilder sb, String color, String perc, String colSpan, String rowSpan ) {
        sb.append("<td style=\"border: 2px solid black; word-wrap: break-word; padding: 15px;\"").append("\n");
        if (color != null)
            sb.append(" bgcolor=\"" + color + "\"").append("\n");
        if (perc != null)
            sb.append(" width=\"" + perc + "%\"").append("\n");
        if (colSpan != null)
            sb.append(" colspan=\"" + colSpan + "\"").append("\n");
        if (rowSpan != null) {
            sb.append(" rowspan=\"" + rowSpan + "\"");
        }
        sb.append(" height=\"50%\" valign=\"top\" >").append("\n");
    }

    @Override
    public void closeTableCell( StringBuilder sb ) {
        sb.append("</td>").append("\n");
    }

    @Override
    public void titleH1( StringBuilder sb, String title ) {
        sb.append("<h1 align=\"center\">" + title + "</h1>").append("\n");
    }

    @Override
    public void titleH2( StringBuilder sb, String title ) {
        sb.append("<h2 align=\"center\">" + title + "</h2>").append("\n");
    }

    @Override
    public void titleH3( StringBuilder sb, String title ) {
        sb.append("<h3 align=\"center\">" + title + "</h3>").append("\n");
    }

    @Override
    public void titleH4( StringBuilder sb, String title ) {
        sb.append("<h4 align=\"center\">" + title + "</h4>").append("\n");
    }

    @Override
    public void bold( StringBuilder sb, String text ) {
        sb.append("<b>" + text + "</b>");
    }

}
