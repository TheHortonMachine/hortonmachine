package org.jgrasstools.gears.utils;

public class HtmlUtils {

    public static void newLine( StringBuilder sb, int n ) {
        for( int i = 0; i < n; i++ ) {
            sb.append("<br>").append("\n");
        }
    }
    public static void openHtml( StringBuilder sb, String title ) {
        sb.append("<html>").append("\n");
        sb.append("<head>").append("\n");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">").append("\n");
        sb.append("<title>" + title + "</title>").append("\n");
        sb.append("</head>").append("\n");
        sb.append("<body>").append("\n");
    }
    public static void closeHtml( StringBuilder sb ) {
        sb.append("</body>").append("\n");
        sb.append("</html>").append("\n");
    }
    public static void openTable( StringBuilder sb, int widthPercentage ) {
        sb.append("<table align=\"center\" border=\"1\" cellpadding=\"2\" cellspacing=\"2\" width=\"" + widthPercentage + "%\">")
                .append("\n");
        sb.append("<tbody>").append("\n");
    }
    public static void closeTable( StringBuilder sb ) {
        sb.append("</tbody>").append("\n");
        sb.append("</table>").append("\n");
    }
    public static void openRow( StringBuilder sb ) {
        sb.append("<tr>").append("\n");
    }
    public static void closeRow( StringBuilder sb ) {
        sb.append("</tr>").append("\n");
    }

    public static void openTableCell( StringBuilder sb, String color, String perc, String span ) {
        sb.append("<td align=\"center\"").append("\n");
        if (color != null)
            sb.append(" bgcolor=\"" + color + "\"").append("\n");
        if (perc != null)
            sb.append(" width=\"" + perc + "% ").append("\n");
        if (span != null)
            sb.append(" colspan=\"" + span + "\"").append("\n");
        sb.append(" height=\"50%\" valign=\"middle\" >").append("\n");
    }

    public static void closeTableCell( StringBuilder sb ) {
        sb.append("</td>").append("\n");
    }
    public static void titleH1( StringBuilder sb, String title ) {
        sb.append("<h1 align=\"center\">" + title + "</h1>").append("\n");
    }
    public static void titleH2( StringBuilder sb, String title ) {
        sb.append("<h2 align=\"center\">" + title + "</h2>").append("\n");
    }
    public static void titleH3( StringBuilder sb, String title ) {
        sb.append("<h3 align=\"center\">" + title + "</h3>").append("\n");
    }
    public static void titleH4( StringBuilder sb, String title ) {
        sb.append("<h4 align=\"center\">" + title + "</h4>").append("\n");
    }

    public static void bold( StringBuilder sb, String text ) {
        sb.append("<b>" + text + "</b>");
    }

}
