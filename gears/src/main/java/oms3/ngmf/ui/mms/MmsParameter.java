/*
 * MMSParameter.java
 *
 * Created on June 23, 2005, 4:54 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package oms3.ngmf.ui.mms;

public class MmsParameter implements Parameter, Comparable {
    private String name;
    private int width = 0;
    private Dimension[] dimensions;
    private int[] size;
    private Class type;
    private Object vals;
    private Object origVals;
    private int total_size = 0;

    public MmsParameter(String name, int width, Dimension[] dimensions, Class type, Object vals) {
        this.name = name;
        this.width = width;
        this.dimensions = dimensions;
        this.type = type;
        this.vals = vals;

        size = new int[dimensions.length];
        for (int i = 0; i < dimensions.length; i++) {
            size[i] = dimensions[i].getSize();
            if (i == 0) {
                total_size = size[0];
            } else {
                total_size = total_size * size[i];
            }
        }

        if (type == Integer.class) {
            int[] temp = new int[total_size];
            for (int i = 0; i < total_size; i++) {
                temp[i] = ((int[])(vals))[i];
            }
            origVals = temp;
        } else {
            double[] temp = new double[total_size];
            for (int i = 0; i < total_size; i++) {
                temp[i] = ((double[])(vals))[i];
            }
            origVals = temp;
        }
    }

    static void figureOutIndexes(int index, int[] sizes, int[] indexes) {
        int prev_div = 0;

        for (int i = (sizes.length - 1); i > 0; i--) {
            int div = sizes[0];
            for (int j = 1; j < i; j++) {
                div = div * sizes[j];
            }
            indexes[i] = (index - prev_div) / div;
            prev_div = (indexes[i] * div) + prev_div;
        }
        indexes[0] = index - prev_div;
    }

    static int figureOutIndex(Dimension[] dimensions, int[] indexes) {
        int index = 0;

        for (int i = (dimensions.length - 1); i > 0; i--) {
            int mult = dimensions[0].getSize();
            for (int j = 1; j < i; j++) {
                mult = mult * dimensions[j].getSize();
            }
            index = index + (mult * indexes[i]);
        }
        index = index + indexes[0];

        return index;
    }

    public String toString() {return name;}

    /*
     * Implements oui.util.Parameter
     */
    public String getName() {return (name);}
    public int getWidth() {return (width);}
    public int getNumDim() {return (dimensions.length);}
    public Dimension getDimension(int index) {return (dimensions[index]);}
    public int getSize() {return (total_size);}
    public Class getType() {return (type);}
    public Object getVals() {return (vals);}
    public void setVals(Object vals) {this.vals = vals;}

    public void  setValueAt(Object val, int index) {
        if (type == Integer.class) {
            int[] foo = (int[])vals;
            foo[index] = ((Integer)val).intValue();
        } else {
            double[] foo = (double[])vals;
            foo[index] = ((Double)val).doubleValue();
        }
    }

    public boolean  isDimensionedBy(Dimension dim) {
        for (int i = 0; i < dimensions.length; i++) {
            if (dimensions[i] == dim) return true;
        }
        return false;
    }

    public double getMean() {
        if (total_size == 0) return Double.NaN;

        double mean = 0.0;
        if (type == Integer.class) {
            for (int i = 0; i < total_size; i++) {
                mean += ((int[])vals)[i];
            }
            mean = mean / (double)total_size;
        } else {
            for (int i = 0; i < total_size; i++) {
                mean += ((double[])vals)[i];
            }
            mean = mean / (double)total_size;
        }
        return mean;
    }

    public double getMin() {
        if (total_size == 0) return Double.NaN;

        double min = Double.POSITIVE_INFINITY;
        if (type == Integer.class) {
            for (int i = 0; i < total_size; i++) {
                if (min > ((int[])vals)[i]) min = ((int[])vals)[i];
            }

        } else {
            for (int i = 0; i < total_size; i++) {
                if (min > ((double[])vals)[i]) min = ((double[])vals)[i];
            }
        }
        return min;
    }

    public double getMax() {
        if (total_size == 0) return Double.NaN;

        double max = Double.NEGATIVE_INFINITY;
        if (type == Integer.class) {
            for (int i = 0; i < total_size; i++) {
                if (max < ((int[])vals)[i]) max = ((int[])vals)[i];
            }

        } else {
            for (int i = 0; i < total_size; i++) {
                if (max < ((double[])vals)[i]) max = ((double[])vals)[i];
            }
        }
        return max;
    }

    public void resize() {

        int new_size = 0;
        for (int i = 0; i < dimensions.length; i++) {
            if (i == 0) {
                new_size = dimensions[0].getSize();
            } else {
                new_size = new_size * dimensions[i].getSize();
            }
        }

        if (type == Integer.class) {
            int[] temp = new int[new_size];
            int[] old_index = new int[dimensions.length];

            for (int i = 0; i < new_size; i++) {
                temp[i] = Integer.MIN_VALUE;
            }

            for (int i = 0; i < total_size; i++) {
//  Figure out old array indexes
                figureOutIndexes(i, size, old_index);
                boolean within_bounds = true;
                for (int j = 0; j < dimensions.length; j++) {
                    if (old_index[j] >= dimensions[j].getSize()) {
                        within_bounds = false;
                        break;
                    }

                    if (within_bounds) {
                        temp[figureOutIndex(dimensions, old_index)] = ((int[])vals)[i];
                    }
                }
            }
            vals = temp;
        } else {
            double[] temp = new double[new_size];
            int[] old_index = new int[dimensions.length];

            for (int i = 0; i < new_size; i++) {
                temp[i] = Double.MIN_VALUE;
            }

            for (int i = 0; i < total_size; i++) {
//  Figure out old array indexes
                figureOutIndexes(i, size, old_index);
                boolean within_bounds = true;
                for (int j = 0; j < dimensions.length; j++) {
                    if (old_index[j] >= dimensions[j].getSize()) {
                        within_bounds = false;
                        break;
                    }

                    if (within_bounds) {
                        temp[figureOutIndex(dimensions, old_index)] = ((double[])vals)[i];
                    }
                }
            }
            vals = temp;
        }

        for (int i = 0; i < dimensions.length; i++) {
            size[i] = dimensions[i].getSize();
            if (i == 0) {
                total_size = size[0];
            } else {
                total_size = total_size * size[i];
            }
        }
    }

    /*
     *  Implements java.lang.Comparable
     */
    public int compareTo(Object o) {
        if (o.getClass() == MmsParameter.class) {
            return name.compareTo(((MmsParameter)o).getName());
        }
        return name.compareTo((String)o);
    }

    public static void main(String arg[]) {

//        int[] sizes = {4};
//        int index = 4;
//        int[] indexes = new int[1];
//
//
//        for (int foo = 0; foo < 4; foo++) {
//            System.out.print(foo);
//             MmsParameter.figureOutIndexes(foo, sizes, indexes);
//
//            for (int i = 0; i < indexes.length; i++) {
//                System.out.print("  " + indexes[i]);
//            }
//            System.out.println("");
//        }

        int[] indexes = new int[3];
        MmsDimension[] dims = new MmsDimension[3];
        dims[0] = new MmsDimension("nlapse", 4);
        dims[1] = new MmsDimension("nmonth", 3);
        dims[2] = new MmsDimension("foo", 2);

        for (int ifoo = 0; ifoo < 2; ifoo++) {
            for (int imonth = 0; imonth < 3; imonth++) {
                for (int ilapse = 0; ilapse < 4; ilapse++) {
                    indexes[0] = ilapse;
                    indexes[1] = imonth;
                    indexes[2] = ifoo;
                    System.out.println(MmsParameter.figureOutIndex(dims, indexes) + " " + indexes[0] + " " + indexes[1] + " " + indexes[2]);
                }
            }
        }
    }
}


