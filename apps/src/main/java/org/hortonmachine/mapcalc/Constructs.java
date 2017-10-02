package org.hortonmachine.mapcalc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public enum Constructs {

    // CONTROL_FLOW
//    IMG("img", "images {\n    example = read;\n    result = write;\n} \n", "Initial image definition block",
//            Constructs.CONTROL_FLOW), //
    IF("if", "if (?) {\n    result = 1;\n} else {\n    result = 0;\n} \n", "If-else block", Constructs.CONTROL_FLOW), //
    CON("con", "con(?, ?, ?) \n", "Conditional block", Constructs.CONTROL_FLOW), //
    FOREACH("for", "foreach (dy in -1:1) {\n    foreach (dx in -1:1) {\n        n += srcimage[dx, dy] > someValue;\n    }\n} \n",
            "Foreach loop example which iterates through pixels in a 3x3 neighbourhood", Constructs.CONTROL_FLOW), //
    WHILE("while", "while ( ? ) {\n\n} \n",
            "A conditional loop which executes the target statement or block while its conditional expression is non-zero",
            Constructs.CONTROL_FLOW), //
    // GENERAL
    SEMICOLON(";", ";", "The semicolon, every statement", Constructs.GENERAL), //
    COMMENT("/* */", "/*\n* comment\n*/", "Block comment", Constructs.GENERAL), //
    COMMENTLINE("// ", "// comment", "Line comment", Constructs.GENERAL), //
    ARRAY("[a,b] ", "array = [1, 2, 3];", "Example declaration of an array", Constructs.GENERAL), //
    // LOGICAL
    LOGICAL1("AND", "&&", "logical AND", Constructs.LOGICAL), //
    LOGICAL2("OR", "||", "logical OR", Constructs.LOGICAL), //
    LOGICAL3("==", "==", "equality test", Constructs.LOGICAL), //
    LOGICAL4("!=", "!=", "inequality test", Constructs.LOGICAL), //
    LOGICAL5("> ", "> ", "greater than", Constructs.LOGICAL), //
    LOGICAL6(">=", ">=", "greater than or equal to", Constructs.LOGICAL), //
    LOGICAL7("<=", "<=", "less than", Constructs.LOGICAL), //
    LOGICAL8("< ", "< ", "less than or equal to", Constructs.LOGICAL), //
    // ARITHMETIC
    ARITHM1("^", "^", "Raise to power", Constructs.ARITHMETIC), //
    ARITHM2("*", "*", "Multiply", Constructs.ARITHMETIC), //
    ARITHM3("/", "/", "Divide", Constructs.ARITHMETIC), //
    ARITHM4("%", "%", "Modulo (remainder)", Constructs.ARITHMETIC), //
    ARITHM5("+", "+", "Add", Constructs.ARITHMETIC), //
    ARITHM6("-", "-", "Subtract", Constructs.ARITHMETIC), //
    ARITHM7("=", "=", "Assignment", Constructs.ARITHMETIC), //
    // NUMERIC
    SQRT("sqrt", "sqrt(?)", "Square root", Constructs.NUMERIC), //
    ISNULL("null?", "isnull(?)", "Is null test on value x", Constructs.NUMERIC), //
    ISINF("inf?", "isinf( ? )", "Is infinite test on value x", Constructs.NUMERIC), //
    ISNAN("nan?", "isnan( ? )", "Is not a number test on value x", Constructs.NUMERIC), //
    RADTODEG("r2d", "radToDeg( ? )", "Radians to degrees", Constructs.NUMERIC), //
    DEG2RAD("r2d", "degToRad( ? )", "Degrees to radians", Constructs.NUMERIC), //
    // STATISTICAL
    STATS1("max", "max(?, ?)", "Maximum", Constructs.STATISTICAL), //
    STATS3("mean", "mean(?)", "Mean", Constructs.STATISTICAL), //
    STATS4("min", "min(?, ?)", "Minimum", Constructs.STATISTICAL), //
    STATS6("med", "median(?)", "Median of an array of values", Constructs.STATISTICAL), //
    STATS7("mode", "mode(?)", "Mode of an array of values", Constructs.STATISTICAL), //
    STATS8("range", "range(?)", "Range of an array of values", Constructs.STATISTICAL), //
    STATS9("sdev", "sdev(?)", "Standard deviation of an array of values", Constructs.STATISTICAL), //
    STATS10("sum", "sum(?)", "Sum of an array of values", Constructs.STATISTICAL), //
    STATS11("var", "variance(?)", "Variance of an array of values", Constructs.STATISTICAL), //
    // PROCESSING
    PROCESSINGSAREA1("h", "height()", "Height of the processing area (world units)", Constructs.PROCESSING), //
    PROCESSINGSAREA2("w", "width()", "Width of the processing area (world units)", Constructs.PROCESSING), //
    PROCESSINGSAREA3("xmin", "xmin()", "Minimum X ordinate of the processing area (world units)", Constructs.PROCESSING), //
    PROCESSINGSAREA4("ymin", "ymin()", "Minimum Y ordinate of the processing area (world units)", Constructs.PROCESSING), //
    PROCESSINGSAREA5("xmax", "xmax()", "Maximum X ordinate of the processing area (world units)", Constructs.PROCESSING), //
    PROCESSINGSAREA6("ymax", "ymax()", "Maximum Y ordinate of the processing area (world units)", Constructs.PROCESSING), //
    PROCESSINGSAREA7("x", "x()", "X ordinate of the current processing position (world units)", Constructs.PROCESSING), //
    PROCESSINGSAREA8("y", "y()", "Y ordinate of the current processing position (world units)", Constructs.PROCESSING), //
    PROCESSINGSAREA9("xres", "xres()", "Pixel width (world units)", Constructs.PROCESSING), //
    PROCESSINGSAREA10("yres", "yres()", "Pixel height (world units)", Constructs.PROCESSING), //
    ;

    public static final String GENERAL = "general";
    public static final String CONTROL_FLOW = "control flow";
    public static final String LOGICAL = "logical";
    public static final String ARITHMETIC = "arithmetic";
    public static final String NUMERIC = "numeric";
    public static final String STATISTICAL = "statistical";
    public static final String PROCESSING = "processing area";

    String name;
    String toolTip;
    String construct;
    String category;
    private Constructs( String name, String construct, String toolTip, String category ) {
        this.name = name;
        this.toolTip = toolTip;
        this.construct = construct;
        this.category = category;
    }

    public static LinkedHashMap<String, List<Constructs>> getMapByCategory() {
        LinkedHashMap<String, List<Constructs>> map = new LinkedHashMap<>();
        for( Constructs construct : values() ) {
            String cat = construct.category;
            List<Constructs> list = map.get(cat);
            if (list == null) {
                list = new ArrayList<>();
                map.put(cat, list);
            }
            list.add(construct);
        }
        return map;
    }

}