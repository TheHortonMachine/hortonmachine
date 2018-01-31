/*
 * MmsParamsReader.java
 *
 * Created on June 23, 2005, 4:46 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package oms3.ngmf.ui.mms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class MmsParamsReader {
    private FileReader file;
    private Logger log;
    private String fileName;
    private ParameterSet mps;

    public MmsParamsReader(String fileName) throws IOException {
        this(new FileReader(fileName), Logger.getLogger(MmsParamsReader.class.getName()));
        this.fileName = fileName;
    }

    public MmsParamsReader(FileReader file) throws IOException {
        this (file, Logger.getLogger(MmsParamsReader.class.getName()));
    }

    public MmsParamsReader(FileReader file, Logger log) throws IOException {
        this.file = file;
        this.log = log;
    }

    public ParameterSet read() throws IOException {
        mps = new MmsParameterSet();
        mps.setFileName(fileName);

/*
 *  Read dimensions
 */

        String line;
        BufferedReader in = null;

        try {
            in = new BufferedReader(file);

            mps.setDescription(in.readLine());
            mps.setVersion(in.readLine());
            line = in.readLine();

/*
 *  Read Dimensions
 */
            while (line != null) {

                if (line.equals("** Parameters **")) {
                    break;

                } else if (line.startsWith("<history")) {
                    mps.addHistory (line);
                    line = in.readLine();

                } else if (line.startsWith("####")) {
                    line = in.readLine();
                    String name = line;
                    int size = Integer.valueOf(in.readLine()).intValue();
                    MmsDimension dim = new MmsDimension(name, size);
                    mps.addDimension(dim);
                    line = in.readLine();

                    int i = 0;
                    while (!(line.startsWith("####") || line.startsWith("** Parameters **"))) {

                        if (line.startsWith("@")) {
                            dim.addItemDesc((i - 1), line, size);
                        } else {
                            dim.addItemName(i, line, size);
                            i++;
                        }
                        line = in.readLine();
                    }
                } else {
                    line = in.readLine();
                }
            }

/*
 * Read Parameters
 */
            line = in.readLine();
            line = in.readLine();

            while (line != null) {
                StringTokenizer st;

                st = new StringTokenizer(line, " ");
                String name = st.nextToken();

                int width = 10;
                if (st.hasMoreTokens()) {
                    width = Integer.parseInt(st.nextToken());
                }

                st = new StringTokenizer(in.readLine(), " ");
                int num_dim = Integer.parseInt(st.nextToken());

                String dim1 = in.readLine();

                String dim2 = null;
                if (num_dim == 2) {
                    dim2 = in.readLine();
                }

                st = new StringTokenizer(in.readLine(), " ");
                int size = Integer.parseInt(st.nextToken());

                st = new StringTokenizer(in.readLine(), " ");
                int type = Integer.parseInt(st.nextToken());

                Object vals = null;
                Class type_class = null;

                if (type == 1) {
                    vals = new int[size];
                    type_class = Integer.class;

                    for (int i = 0; i < size; i++) {
                        st = new StringTokenizer(in.readLine(), " ");
                        ((int[])vals)[i] = Integer.parseInt(st.nextToken());
                    }

                } else {
                    vals = new double[size];
                    type_class = Double.class;

                    for (int i = 0; i < size; i++) {
                        st = new StringTokenizer(in.readLine(), " ");
                        ((double[])vals)[i] = Double.parseDouble(st.nextToken());
                    }

                }

                MmsDimension[] dims = new MmsDimension[num_dim];
                dims[0] = (MmsDimension)(mps.getDimension(dim1));

                if (num_dim > 1) {
                    dims[1] = (MmsDimension)(mps.getDimension(dim2));
                }

                mps.addParameter(new MmsParameter(name, width, dims, type_class, vals));

                line = in.readLine();
                line = in.readLine();
            }

        } catch (IOException ex) {
            log.severe("Problem reading parameters");
            ex.printStackTrace();
            throw ex;
        } catch (NumberFormatException ex) {
            log.severe("NumberFormatException while reading parameters");
        } finally {
            try {
                if (in!= null) {
                    in.close();
                    in = null;
                }
            }  catch (IOException E) {}
        }

        if (mps.getDims().isEmpty() && mps.getParams().isEmpty()) {
            mps = null;
            throw (new IOException ("Invalid MMS parameter file."));
        }
        return mps;
    }

    public static void main(String arg[]) {
        try {
            MmsParamsReader mp = new MmsParamsReader(new FileReader(arg[0]));
            ParameterSet ps = mp.read();

            System.out.println ("Dimensions = " + ps.getDims());
            System.out.println ("Parameters = " + ps.getParams());

        } catch (java.io.FileNotFoundException e) {
            System.out.println(arg[0] + " not found");
        } catch (IOException e) {
            System.out.println(arg[0] + " io exception");
        }
    }
}
