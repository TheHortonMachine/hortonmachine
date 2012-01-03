/*
 * $Id:$
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *  3. This notice may not be removed or altered from any source
 *     distribution.
 */
package oms3;

import java.io.File;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversion Utilities. This class deals with conversion from string values
 * to some known types.
 * 
 * @author Olaf David
 */
public class Conversions {

    /** Conversion SPI */
    static private ServiceLoader<ConversionProvider> convServices = ServiceLoader.load(ConversionProvider.class);
    //
    static final char LB = '{';
    static final char RB = '}';
    static final char SEP = ',';
    static final Pattern pattern = Pattern.compile("(\\w+)\\s*(\\[[0-9]+\\])*?");
    static final Pattern splitP = Pattern.compile(Character.toString(SEP));
    public static boolean debug = false;
    /** Some common date patters */
    static private final String[] fmt = {
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd hh:mm",
        "yyyy-MM-dd hh:mm:ss",
        "yyyy-MM-dd", // ISO
        "MM-dd-yyyy", // US
        "MM/dd/yyyy",
        "dd.MM.yyyy", // DE
        "yyyy.MM.dd", // DE
        "yyyy MM dd H m s" // MMS
    };

    public static class Params {

        HashMap<String, Object> arg = new HashMap<String, Object>();

        public void add(Class from, Class to, Object par) {
            arg.put(key(from, to), par);
        }

        public Object get(Class from, Class to) {
            return arg.get(key(from, to));
        }
    }
    
    public static boolean canConvert(Class<?> from, Class<?> to) {
        if (from == String.class && to.isArray()) {
            return true;
        }
        // get it from the internal cache.new ArrayConverter((String) from).getArrayForType(to);
        Converter<Object, ?> c = co.get(key(from, to));
        if (c == null) {
            // service provider lookup
            c = lookupConversionService(from, to);
            if (c == null) {
                return false;
            }
        }
        return true;
    }

//    public static Params createDefault() {
//        Params p = new Params();
//        p.add(double.class, String.class, "10.5");
//        return p;
//    }
    /** Convert a String value into an object of a certain type
     *
     * @param to the type to convert to
     * @param from the value to convert
     * @param arg conversion argument (e.g. Date format)
     * @return the object of a certain type.
     */
    public static <T> T convert(Object from, Class<? extends T> to, Params arg) {
        if (from == null) {
            throw new NullPointerException("from");
        }
        if (to == null) {
            throw new NullPointerException("to");
        }
        if (from.getClass() == String.class && to.isArray()) {
            return new ArrayConverter((String) from).getArrayForType(to);
        }
        // get it from the internal cache.
        @SuppressWarnings("unchecked")
        Converter<Object, T> c = co.get(key(from.getClass(), to));
        if (c == null) {
            // service provider lookup
            c = lookupConversionService(from.getClass(), to);
            if (c == null) {
                throw new ComponentException("No Converter: " + from + " (" + from.getClass() + ") -> " + to);
            }
            co.put(key(from.getClass(), to), c);
        }
        Object param = null;
        if (arg != null) {
            param = arg.get(from.getClass(), to);
        }
        return (T) c.convert(from, param);
    }

    public static <T> T convert(Object from, Class<? extends T> to) {
        return convert(from, to, null);
    }
    // SPI

    /**
     * Lookup a conversion service
     * 
     * @param from
     * @param to
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <T> Converter<Object, T> lookupConversionService(Class from, Class to) {
        for (ConversionProvider converter : convServices) {
            Converter c = converter.getConverter(from, to);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    /**
     *
     * @param src  e.g  test[1][3]
     * @return     [test,1,2]
     */
    public static String[] parseArrayElement(String src) {
        Matcher matcher = pattern.matcher(src.trim());
        List<String> a = new ArrayList<String>();
        while (matcher.find()) {
            a.add(matcher.group(0));
        }
        return a.toArray(new String[0]);
    }

    /** Get the array base type
     * 
     * @param array
     * @return
     */
    private static Class getArrayBaseType(Class array) {
        while (array.isArray()) {
            array = array.getComponentType();
        }
        return array;
    }

    /** ArrayConverter
     * 
     */
    static class ArrayConverter {

        String[] content;
        String layout;
        /* 10 dimensions max.*/
        int[] dims = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

        private int count(String s, char c) {
            int count = 0;
            int len = s.length();
            for (int i = 0; i < len; i++) {
                if (s.charAt(i) == c) {
                    count++;
                }
            }
            return count;
        }

        ArrayConverter(String text) {
            text = text.trim();
            if (count(text, '}') != count(text, '}')) {
                throw new IllegalArgumentException("Brackets mismatch.");
            }
            if (debug) {
                System.out.println(text);
            }
            String mass = text.replace(LB, ' ').replace(RB, ' ').trim();
            content = splitP.split(mass);
            if (debug) {
                System.out.println(mass.isEmpty());
                System.out.println("content " + Arrays.toString(content));
            }
            layout = changed(text, mass.isEmpty() ? 0 : content.length);
            if (debug) {
                System.out.println("layout " + layout);
            }
            parseDims(layout);
        }

        private String changed(String inp, int len) {
            StringBuilder c = new StringBuilder();
            int count = 0;
            int b = 0;
            boolean first = true;
            for (int i = 0; i < inp.length(); i++) {
                char ch = inp.charAt(i);
                switch (ch) {
                    case ' ':
                        break;    // TODO, repect string quotes.
                    case LB:
                    case RB:
                    case SEP:
                        first = true;
                        c.append(ch);
                        break;
                    default:
                        if (first) {
                            c.append(count++);
                            first = false;
                        }
                }
            }
            if (count != len) {
                throw new IllegalArgumentException(count + "!=" + len);
            }
            return c.toString();
        }

        private Class[] types(int[] dims, Class basetype) {
            int i = 0;
            while (dims[i] > -1) {
                i++;
            }
            if (debug) {
                System.out.println(" rank " + i);
            }
            Class[] c = new Class[i];
            c[i - 1] = basetype;
            while (--i > 0) {
                c[i - 1] = Array.newInstance(c[i], 0).getClass();
            }
            if (debug) {
                System.out.println("Types " + java.util.Arrays.toString(c));
            }
            return c;
        }

        private void parseDims(String layout) {
            for (int i = 0, level = -1; i < layout.length(); i++) {
                char c = layout.charAt(i);
                switch (c) {
                    case LB:
                        if (level == dims.length - 1) {
                            throw new UnsupportedOperationException("Can only handle arrays with length " + dims.length);
                        }
                        if (i > 0 && layout.charAt(i - 1) == LB) {
                            dims[level]++;
                        }
                        dims[++level] = 0;
                        break;
                    case RB:
                        level--;
                        break;
                    case SEP:
                        dims[level]++;
                        break;
                    default:
                        if (dims[level] == 0) {
                            dims[level]++;
                        }
                }
            }
            if (debug) {
                System.out.println("dims :" + Arrays.toString(dims));
            }
        }

        @SuppressWarnings("unchecked")
        <T> T getArrayForType(Class<? extends T> type) {
            if (!type.isArray()) {
                throw new IllegalArgumentException("Not an array type");
            }
            Class cType = getArrayBaseType(type);
            Object o = new Parser(co.get(key(String.class, cType))).parse(0, layout, types(dims, cType));
            if (o.getClass() != type) {
                throw new RuntimeException(o.getClass() + "!=" + type);
            }
            return (T) o;
        }

        /**
         * Internal Parser.
         */
        private class Parser {

            int idx = 1;
            Converter converter;

            Parser(Converter converter) {
                if (converter == null) {
                    throw new RuntimeException("cannot convert.");
                }
                this.converter = converter;
            }

            @SuppressWarnings("unchecked")
            Object parse(int level, String layout, Class[] cc) {
                int arridx = 0;
                Object arr = Array.newInstance(cc[level], dims[level]);
                while (idx < layout.length()) {
                    char c = layout.charAt(idx);
                    switch (c) {
                        case LB:
                            idx++;
                            Object elem = parse(level + 1, layout, cc);
//                        System.out.println("Adding Array " + elem + " to " + arr.getClass() + " at " + arridx);
                            System.out.flush();
                            Array.set(arr, arridx, elem);
                            break;
                        case RB:
                            return arr;
                        case SEP:
                            arridx++;
                            break;
                        default:
                            int index = 0;
                            while (Character.isDigit(layout.charAt(idx))) {
                                index = index * 10 + (layout.charAt(idx++) - '0');
                            }
                            idx--;
                            Object o = converter.convert(content[index], null);
                            try {
                                Array.set(arr, arridx, o);
                            } catch (IllegalArgumentException E) {
                                System.out.println(arr.getClass() + " element " + arridx + " " + o + " " + o.getClass());
                                throw E;
                            }
                    }
                    idx++;
                }
                return arr;
            }
        }
    }

    public static String formatISO(Date date) {
        return ISO().format(date);
    }

    public static SimpleDateFormat ISO() {
        return new SimpleDateFormat(fmt[3]);
    }

    /**
     * parse the date using the formats above. This is thread safe.
     *
     * @param date
     * @return
     */
    private static Date parse(String date) {
        for (int i = 0; i < fmt.length; i++) {
            try {
                SimpleDateFormat df = new SimpleDateFormat(fmt[i]);
                return df.parse(date);
            } catch (ParseException E) {
                // keep trying
            }
        }
        throw new IllegalArgumentException(date);
    }

//    DecimalFormat dfmt
    private final static String key(Class from, Class to) {
        return from.getName() + "->" + to.getName();
    }
    /**
     * Converter table.
     */
    @SuppressWarnings("serial")
    private static Map<String, Converter> co = new HashMap<String, Converter>() {

        @Override
        public Converter put(String key, Converter value) {
            Converter v = super.put(key, value);
            if (v != null) {
                throw new IllegalArgumentException("Duplicate Converter for " + key);
            }
            return v;
        }

        {
            put(key(String.class, String.class), new Converter<String, String>() {

                @Override
                public String convert(String s, Object arg) {
                    return s;
                }
            });
            put(key(BigDecimal.class, Double.class), new Converter<BigDecimal, Double>() {

                @Override
                public Double convert(BigDecimal s, Object arg) {
                    return s.doubleValue();
                }
            });
            put(key(Integer.class, Double.class), new Converter<Integer, Double>() {

                @Override
                public Double convert(Integer s, Object arg) {
                    return s.doubleValue();
                }
            });

            put(key(double[].class, String.class), new Converter<double[], String>() {

                @Override
                public String convert(double[] s, Object arg) {
                    StringBuilder b = new StringBuilder("{");
                    if (s.length > 0) {
                        for (int i = 0; i < s.length - 1; i++) {
                            b.append(s[i] + ", ");
                        }
                        b.append(s[s.length - 1]);
                    }
                    b.append("}");
                    return b.toString();
                }
            });
            put(key(double[][].class, String.class), new Converter<double[][], String>() {

                @Override
                public String convert(double[][] s, Object arg) {
                    StringBuilder b = new StringBuilder("{");
                    if (s.length > 0) {
                        for (int i = 0; i < s.length - 1; i++) {
                            b.append(Conversions.convert(s[i], String.class) + ", ");
                        }
                        b.append(Conversions.convert(s[s.length - 1], String.class));
                    }
                    b.append("}");
                    return b.toString();
                }
            });
            put(key(int[].class, String.class), new Converter<int[], String>() {

                @Override
                public String convert(int[] s, Object arg) {
                    StringBuilder b = new StringBuilder("{");
                    if (s.length > 0) {
                        for (int i = 0; i < s.length - 1; i++) {
                            b.append(s[i] + ", ");
                        }
                        b.append(s[s.length - 1]);
                    }
                    b.append("}");
                    return b.toString();
                }
            });
            put(key(int[][].class, String.class), new Converter<int[][], String>() {

                @Override
                public String convert(int[][] s, Object arg) {
                    StringBuilder b = new StringBuilder("{");
                    if (s.length > 0) {
                        for (int i = 0; i < s.length - 1; i++) {
                            b.append(Conversions.convert(s[i], String.class) + ", ");
                        }
                        b.append(Conversions.convert(s[s.length - 1], String.class));
                    }
                    b.append("}");
                    return b.toString();
                }
            });
            put(key(String.class, URL.class), new Converter<String, URL>() {    // File

                @Override
                public URL convert(String s, Object arg) {
                    try {
                        return new URL(s);
                    } catch (MalformedURLException ex) {
                        throw new IllegalArgumentException(s, ex);
                    }
                }
            });
            put(key(String.class, File.class), new Converter<String, File>() {    // File

                @Override
                public File convert(String s, Object arg) {
                    return new File(s);
                }
            });
            put(key(String.class, Date.class), new Converter<String, Date>() {    // File

                @Override
                public Date convert(String s, Object arg) {
                    if (arg instanceof DateFormat) {
                        try {
                            return ((DateFormat) arg).parse(s);
                        } catch (ParseException E) {
                            throw new RuntimeException(E);
                        }
                    }
                    return parse(s);
                }
            });
            put(key(String.class, Calendar.class), new Converter<String, Calendar>() {    // Calendar

                @Override
                public Calendar convert(String s, Object arg) {
                    Date d = null;
                    if (arg instanceof DateFormat) {
                        try {
                            d = ((DateFormat) arg).parse(s);
                        } catch (ParseException E) {
                            throw new RuntimeException(E);
                        }
                    } else {
                        d = parse(s);
                    }
                    Calendar cal = new GregorianCalendar();
                    cal.setTime(d);
                    return cal;
                }
            });
            put(key(GregorianCalendar.class, String.class), new Converter<GregorianCalendar, String>() {    // Calendar

                @Override
                public String convert(GregorianCalendar s, Object arg) {
                    if (arg instanceof DateFormat) {
                        return ((DateFormat) arg).format(s.getTime());
                    } else {
                        return ISO().format(s.getTime());
                    }
                }
            });
            put(key(Double.class, String.class), new Converter<Double, String>() {    // Double

                @Override
                public String convert(Double s, Object arg) {
                    if (arg instanceof NumberFormat) {
                        NumberFormat nf = (NumberFormat) arg;
                        return nf.format(s);
                    }
                    return s.toString();
                }
            });
            put(key(Float.class, String.class), new Converter<Float, String>() {    // Double

                @Override
                public String convert(Float s, Object arg) {
                    if (arg instanceof NumberFormat) {
                        NumberFormat nf = (NumberFormat) arg;
                        return nf.format(s);
                    }
                    return s.toString();
                }
            });
            put(key(Integer.class, String.class), new Converter<Integer, String>() {    // Integer

                @Override
                public String convert(Integer s, Object arg) {
                    return s.toString();
                }
            });
            put(key(String.class, BigDecimal.class), new Converter<String, BigDecimal>() {    // File

                @Override
                public BigDecimal convert(String s, Object arg) {
                    return new BigDecimal(s.trim());
                }
            });
            put(key(String.class, double.class), new Converter<String, Double>() {    // double

                @Override
                public Double convert(String s, Object arg) {
                    return Double.parseDouble(s.trim());
                }
            });
            put(key(String.class, Double.class), get(key(String.class, double.class)));
            put(key(String.class, float.class), new Converter<String, Float>() {     // float

                @Override
                public Float convert(String s, Object arg) {
                    return Float.parseFloat(s.trim());
                }
            });
            put(key(String.class, Float.class), get(key(String.class, float.class)));
            put(key(String.class, long.class), new Converter<String, Long>() {       // long

                @Override
                public Long convert(String s, Object arg) {
                    return Long.parseLong(s.trim());
                }
            });
            put(key(String.class, Long.class), get(key(String.class, long.class)));
            put(key(String.class, int.class), new Converter<String, Integer>() {        // int

                @Override
                public Integer convert(String s, Object arg) {
                    return Integer.parseInt(s.trim());
                }
            });
            put(key(String.class, Integer.class), get(key(String.class, int.class)));
            put(key(String.class, short.class), new Converter<String, Short>() {      // short

                @Override
                public Short convert(String s, Object arg) {
                    return Short.parseShort(s.trim());
                }
            });
            put(key(String.class, Short.class), get(key(String.class, short.class)));
            put(key(String.class, byte.class), new Converter<String, Byte>() {         // byte

                @Override
                public Byte convert(String s, Object arg) {
                    return Byte.parseByte(s.trim());
                }
            });
            put(key(String.class, Byte.class), get(key(String.class, byte.class)));
            put(key(boolean.class, String.class), new Converter<Boolean, String>() {       // boolean

                @Override
                public String convert(Boolean s, Object arg) {
                    return s.toString();
                }
            });
            put(key(Boolean.class, String.class), get(key(boolean.class, String.class)));
            put(key(String.class, boolean.class), new Converter<String, Boolean>() {       // boolean

                @Override
                public Boolean convert(String s, Object arg) {
                    return Boolean.parseBoolean(s.trim());
                }
            });
            put(key(String.class, Boolean.class), get(key(String.class, boolean.class)));
            put(key(String.class, char.class), new Converter<String, Character>() {        // String

                @Override
                public Character convert(String s, Object arg) {
                    return s.trim().charAt(0);
                }
            });
            put(key(String.class, Character.class), get(key(String.class, char.class)));
            put(key(Integer.class, int.class), new Converter<Integer, Integer>() {         // byte

                @Override
                public Integer convert(Integer s, Object arg) {
                    return s;
                }
            });
            put(key(Double.class, double.class), new Converter<Double, Double>() {         // double

                @Override
                public Double convert(Double s, Object arg) {
                    return s;
                }
            });
            put(key(Float.class, float.class), new Converter<Float, Float>() {         // double

                @Override
                public Float convert(Float s, Object arg) {
                    return s;
                }
            });
            put(key(Boolean.class, boolean.class), new Converter<Boolean, Boolean>() {         // byte

                @Override
                public Boolean convert(Boolean s, Object arg) {
                    return s;
                }
            });
            // add more
        }
    };

    public static void main(String[] args) throws ParseException {
        
        String t = "t[10]";
        
        System.out.println(Arrays.toString(Conversions.parseArrayElement(t)));


        double a = 1231.2345672828288284;
        System.out.println(String.format(Locale.US, "'%2.5f'", a));

//        Formatter f = new Formatter();
//        NumberFormat formatter = new DecimalFormat("#0.000");
//        System.out.println(formatter.);

//        System.out.println("1 " + Double.class.isAssignableFrom(Object.class));
//        System.out.println("2 " + Object.class.isAssignableFrom(Double.class));
//        System.out.println("3 " + Number.class.isAssignableFrom(Double.class));
//        System.out.println("4 " + double.class.isAssignableFrom(Double.class));
//        System.out.println("5 " + Double.class.isAssignableFrom(double.class));
//        System.out.println("6 " + Double.class.getCanonicalName());
//        System.out.println("7 " + Double.class.getName());
//        System.out.println("8 " + Double.class.getSimpleName());
//
//        Currency c = convert("USD", Currency.class);
//        Color col = convert("ff0110", Color.class);
//        System.out.println(c);
//        System.out.println(col);


        System.out.print(co.keySet());

    }
////        String content = "{{   333.345, 12.23},  {444.1, 222.4},{1.2, 3.4}}";
////        String content3 = "{{   333.345, 12.23, 444.1},{ 222.4 , 1.2, 3.4}}";
////        String content1 = "{333.345, 12.23, 444.1, 222.4,1.2, 3.4}";
//////        String content11 = "[333.345     12.23 444.1 222.4 1.2 3.4]";
//        String b = "{{{333.345, 12.23},{444.1, 222.4},{1.2, 3.4}}}";
//
//        String b = "{ 0.0, 0.0,0.0,0.1500000059605,0.0,0.0,0.0,0.0,0.0,0.1500000059605,0.8000000119209,0.0,0.05000000074506,0.6998999714851,0.0,0.660000026226,1.759899973869,0.0,0.2599000036716,0.01989999972284,1.799900054932,1.840000033379,1.700000047684,1.070000052452,0.6800000071526,3.72000002861,3.410000085831,0.140000000596,3.650000095367,1.659899950027,1.070000052452,0.5400000214577,3.059900045395,3.25,3.910000085831,1.44000005722,3.089900016785,1.870000004768,1.539899945259,1.460000038147,2.599900007248,1.090000033379,2.420000076294,2.259900093079,2.900000095367,1.129899978638,3.789900064468,3.799900054932,3.0,1.389899969101,5.159900188446,1.700000047684,1.330000042915,0.7400000095367,1.049900054932,1.809900045395,0.3898999989033,0.0,0.9300000071526,0.5,2.700000047684,0.0,0.1098999977112,0.5,0.0,1.200000047684,0.2599000036716,0.1199000030756}";
//
////        String c = "t1_est(1,2)";
////          System.out.println(Arrays.asList(parseArrayElement(c)));
//
//        String c = "t1_est(1  ,2) ; basin; abc(1)";
////        System.out.println(Arrays.asList(parseArrayElement(c)));
//
//        String t[] = c.split("\\s*;\\s*");
//        for (String string : t) {
//            System.out.println("string '" + string + "'");
//        }
//        //this is a comment
//        //this is a comment
//
//        Object arr = convert(b, double[][][].class);
//        System.out.println(arr);
//        System.out.println(java.util.Arrays.toString(((double[]) arr)));
//////      System.out.println(Arrays.toString(((String[])new P(content1).getArray(String.class))));
//    }
}
