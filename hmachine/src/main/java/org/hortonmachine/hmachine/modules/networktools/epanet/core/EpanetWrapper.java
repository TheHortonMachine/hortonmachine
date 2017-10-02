/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.hmachine.modules.networktools.epanet.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

/**
 * A wrapper for epanet functions.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class EpanetWrapper {

    private static EpanetNativeFunctions epanet;

    /**
     * A warning message related to the last method called.
     */
    private String warningMessage = null;

    /**
     * Contructor for the {@link EpanetWrapper}.
     * 
     * <p>This also takes care to load the native library, if needed. 
     * 
     * @param lib the name of the library (ex. "epanet2_64bit").
     * @param nativeLibPath the path in which to search the native library.
     *              If the native library is already in the java library path
     *              this is not needed and may be <code>null</code>
     * @throws Exception if the library could not be loaded.
     */
    public EpanetWrapper( String lib, String nativeLibPath ) throws Exception {
        if (epanet == null) {
            try {
                if (nativeLibPath != null)
                    NativeLibrary.addSearchPath(lib, nativeLibPath);
                epanet = (EpanetNativeFunctions) Native.loadLibrary(lib, EpanetNativeFunctions.class);
            } catch (Exception e) {
                throw new Exception("An error occurred while trying to load the epanet library.", e);
            }
        }
    }

    /**
     * Get the reference to the jna native epanet instance.
     * 
     * @return the OmsEpanet native reference.
     */
    public static EpanetNativeFunctions getEpanet() {
        return epanet;
    }

    /**
     * Opens the Toolkit to analyze a particular distribution system.  
     * 
     * @param input name of an EPANET Input file.
     * @param report name of an output Report file.
     * @param outputBin name of an optional binary Output file.
     * @throws IOException
     */
    public void ENopen( String input, String report, String outputBin ) throws EpanetException {
        int errcode = epanet.ENopen(input, report, outputBin);
        checkError(errcode);
    }

    /**
     * Closes down the Toolkit system (including all files being processed). 
     * 
     * <p>ENclose must be called when all processing has been completed, 
     * even if an error condition was encountered.
     * @throws EpanetException 
     */
    public void ENclose() throws EpanetException {
        int errcode = epanet.ENclose();
        checkError(errcode);
    }

    /**
     * Writes all current network input data to 
     * a file using the format of an EPANET input file. 
     * 
     * @param fileName name of the file where data is saved. 
     * @throws EpanetException
     */
    public void ENsaveinpfile( String fileName ) throws EpanetException {
        int err = epanet.ENsaveinpfile(fileName);
        checkError(err);
    }

    /**
     * Runs a complete hydraulic simulation with results for all time periods written to the binary Hydraulics file.
     * 
     * @throws EpanetException 
     */
    public void ENsolveH() throws EpanetException {
        int err = epanet.ENsolveH();
        checkError(err);
    }

    /**
     * Transfers results of a hydraulic simulation from the binary Hydraulics file to the 
     * binary Output file, where results are only reported at uniform reporting intervals. 
     * 
     * @throws EpanetException
     */
    public void ENsaveH() throws EpanetException {
        int err = epanet.ENsaveH();
        checkError(err);
    }

    /**
     * Opens the hydraulics analysis system. 
     * 
     * <p>Call ENopenH prior to running the first hydraulic analysis 
     * using the ENinitH - ENrunH - ENnextH sequence. Multiple analyses 
     * can be made before calling ENcloseH to close the hydraulic 
     * analysis system.  
     * 
     * <p>Do not call this function if ENsolveH is being used to run a 
     * complete hydraulic analysis.  
     * 
     * @throws EpanetException
     */
    public void ENopenH() throws EpanetException {
        int err = epanet.ENopenH();
        checkError(err);
    }

    /**
     * Initializes storage tank levels, link status and settings, and 
     * the simulation clock time prior to running a hydraulic analysis.
     *  
     * @param saveflag 0-1 flag indicating if hydraulic results will be 
     *                  saved to the hydraulics file. 
     * @throws EpanetException 
     */
    public void ENinitH( int saveflag ) throws EpanetException {
        int err = epanet.ENinitH(saveflag);
        checkError(err);
    }

    /**
     * Runs a single period hydraulic analysis, retrieving 
     * the current simulation clock time.
     * 
     * <p>Use {@link #ENrunH(long[])} along with {@link #ENnextH(long[])}
     *  in a do..while loop to 
     * analyze hydraulics in each period of an extended period simulation. 
     * This process automatically updates the simulation clock time so 
     * treat t as a read-only variable.  
     * 
     * <p>{@link #ENinitH(int)} must have been called prior to running the 
     * ENrunH - ENnextH loop.  
     *  
     * @param time current simulation clock time in seconds. This value 
     *              is updated by the method.
     * @throws EpanetException 
     */
    public void ENrunH( long[] time ) throws EpanetException {
        int err = epanet.ENrunH(time);
        checkError(err);
    }

    /**
     * Determines the length of time until the next hydraulic event occurs in 
     * an extended period simulation.  
     * 
     * @param timeStep time (in seconds) until next hydraulic event occurs or 0 
     *                  if at the end of the simulation period. 
     * @throws EpanetException
     */
    public void ENnextH( long[] timeStep ) throws EpanetException {
        int err = epanet.ENnextH(timeStep);
        checkError(err);
    }

    /**
     * Closes the hydraulic analysis system, freeing all allocated memory.  
     * 
     * <p>Call {@link #ENcloseH()} after all hydraulics analyses have been made 
     * using {@link #ENinitH(int)} - {@link #ENrunH(long[])} - {@link #ENnextH(long[])}.
     * Do not call this function if ENsolveH is being used.  
     * 
     * @throws EpanetException
     */
    public void ENcloseH() throws EpanetException {
        int err = epanet.ENcloseH();
        checkError(err);
    }

    /**
     * Saves the current contents of the binary hydraulics file to a file.  
     * 
     * @param filePath name of the file where the hydraulics results should be saved.
     * @throws EpanetException
     */
    public void ENsavehydfile( String filePath ) throws EpanetException {
        int err = epanet.ENsavehydfile(filePath);
        checkError(err);
    }

    /**
     * Uses the contents of the specified file as the current binary hydraulics file.   
     *  
     * @param filePath name of the file containing hydraulic analysis results for the current network.
     * @throws EpanetException
     */
    public void ENusehydfile( String filePath ) throws EpanetException {
        int err = epanet.ENsavehydfile(filePath);
        checkError(err);
    }

    /**
     * @deprecated not implemented yet.
     */
    public int ENsolveQ() {
        return -1;
    }
    /**
     * @deprecated not implemented yet.
     */
    public int ENopenQ() {
        return -1;
    }
    /**
     * @deprecated not implemented yet.
     */
    public int ENinitQ( int int1 ) {
        return -1;
    }
    /**
     * @deprecated not implemented yet.
     */
    public int ENrunQ( Long lPtr1 ) {
        return -1;
    }
    /**
     * @deprecated not implemented yet.
     */
    public int ENnextQ( Long lPtr1 ) {
        return -1;
    }
    /**
     * @deprecated not implemented yet.
     */
    public int ENstepQ( Long lPtr1 ) {
        return -1;
    }
    /**
     * @deprecated not implemented yet.
     */
    public int ENcloseQ() {
        return -1;
    }
    /**
     * @deprecated not implemented yet.
     */
    public int ENwriteline( ByteBuffer charPtr1 ) {
        return -1;
    }

    /**
     * Writes a formatted text report on simulation results to the Report file.  
     * @throws EpanetException 
     */
    public void ENreport() throws EpanetException {
        int err = epanet.ENreport();
        checkError(err);
    }

    /**
     * Clears any report formatting commands that either appeared in the 
     * [REPORT] section of the EPANET Input file or were issued with the 
     * ENsetreport function
     * 
     * @throws EpanetException
     */
    public void ENresetreport() throws EpanetException {
        int err = epanet.ENresetreport();
        checkError(err);
    }

    /**
     * Issues a report formatting command. Formatting commands are the 
     * same as used in the [REPORT] section of the EPANET Input file.  
     * 
     * @param command text of a report formatting command.
     * @throws EpanetException 
     */
    public void ENsetreport( String command ) throws EpanetException {
        int err = epanet.ENsetreport(command);
        checkError(err);
    }

    /**
     * @deprecated not implemented yet
     */
    public int ENgetcontrol( int int1, IntBuffer intPtr1, IntBuffer intPtr2, FloatBuffer floatPtr1, IntBuffer intPtr3,
            FloatBuffer floatPtr2 ) {
        return -1;
    }

    /**
     *  Retrieves the number of network components of a specified type.
     *    
     * @param countcode {@link Components} code .
     * @return number of countcode components in the network.
     * @throws EpanetException 
     */
    public int ENgetcount( Components countcode ) throws EpanetException {
        int[] count = new int[1];
        int error = epanet.ENgetcount(countcode.getCode(), count);
        checkError(error);
        return count[0];
    }

    /**
     * Retrieves the value of a particular analysis option.  
     * 
     * @param optionCode The {@link OptionParameterCodes}.
     * @throws EpanetException 
     */
    public float ENgetoption( OptionParameterCodes optionCode ) throws EpanetException {
        float[] optionValue = new float[1];
        int error = epanet.ENgetoption(optionCode.getCode(), optionValue);
        checkError(error);
        return optionValue[0];
    }

    /**
     *  Retrieves the value of a specific analysis time parameter.  
     *  
     * @param timeParameterCode time parameter code.
     * @return value of time parameter in seconds.
     * @throws EpanetException 
     */
    public long ENgettimeparam( TimeParameterCodes timeParameterCode ) throws EpanetException {
        long[] timeValue = new long[1];
        int error = epanet.ENgettimeparam(timeParameterCode.getCode(), timeValue);
        checkError(error);
        return timeValue[0];
    }
    /**
     * @deprecated not yet implemented.
     */
    public int ENgetflowunits( IntBuffer intPtr1 ) {
        return -1;
    }

    /**
     * Retrieves the index of a particular time pattern.  
     * 
     * @param id pattern ID label.
     * @return the pattern index.
     * @throws EpanetException 
     */
    public int ENgetpatternindex( String id ) throws EpanetException {
        int[] index = new int[1];
        int error = epanet.ENgetpatternindex(id, index);
        checkError(error);
        return index[0];
    }

    /**
     * Retrieves the ID label of a particular time pattern.  
     * 
     * @param index pattern index.
     * @return the id label.
     * @throws EpanetException
     */
    public String ENgetpatternid( int index ) throws EpanetException {
        ByteBuffer bb = ByteBuffer.allocate(64);
        int errcode = epanet.ENgetpatternid(index, bb);
        checkError(errcode);

        String label = byteBuffer2String(bb);
        return label;
    }

    /**
     * @deprecated not yet implemented.
     */
    public int ENgetpatternlen( int int1, IntBuffer intPtr1 ) {
        return -1;
    }

    /**
     * Retrieves the multiplier factor for a specific time period in a time pattern. 
     * 
     * @param index time pattern index.
     * @param period period within time pattern.
     * @return multiplier factor for the period.
     * @throws EpanetException 
     */
    public float ENgetpatternvalue( int index, int period ) throws EpanetException {
        float[] value = new float[1];
        int errcode = epanet.ENgetpatternvalue(index, period, value);
        checkError(errcode);

        return value[0];
    }
    /**
     * @deprecated not yet implemented.
     */
    public int ENgetqualtype( IntBuffer intPtr1, IntBuffer intPtr2 ) {
        return -1;
    }
    /**
     * @deprecated not yet implemented.
     */
    public int ENgeterror( int int1, ByteBuffer charPtr1, int int2 ) {
        return -1;
    }

    /**
     *  Retrieves the index of a node with a specified ID.   
     *  
     * @param id the node id.
     * @return the node index.
     * @throws EpanetException
     */
    public int ENgetnodeindex( String id ) throws EpanetException {
        int[] index = new int[1];
        int error = epanet.ENgetnodeindex(id, index);
        checkError(error);
        return index[0];
    }

    /**
     * Retrieves the ID label of a node with a specified index.  
     * 
     * @param index the node index.
     * @return the node id.
     * @throws EpanetException
     */
    public String ENgetnodeid( int index ) throws EpanetException {
        ByteBuffer bb = ByteBuffer.allocate(64);
        int errcode = epanet.ENgetnodeid(index, bb);
        checkError(errcode);

        String label;
        label = byteBuffer2String(bb);
        return label;
    }

    /**
     * Retrieves the node-type code for a specific node.  
     * 
     * @param index the node index.
     * @return the {@link NodeTypes};
     * @throws EpanetException
     */
    public NodeTypes ENgetnodetype( int index ) throws EpanetException {
        int[] typecode = new int[1];
        int error = epanet.ENgetnodetype(index, typecode);
        checkError(error);

        NodeTypes type = NodeTypes.forCode(typecode[0]);
        return type;
    }

    /**
     * Retrieves the value of a specific link (node?) parameter.  
     * 
     * @param index the node index.
     * @param code the parameter code.
     * @return the value at the node.
     * @throws EpanetException
     */
    public float ENgetnodevalue( int index, NodeParameters code ) throws EpanetException {
        float[] nodeValue = new float[1];
        int error = epanet.ENgetnodevalue(index, code.getCode(), nodeValue);
        checkError(error);
        return nodeValue[0];
    }

    /**
     * Retrieves the index of a link with a specified ID.  
     * 
     * <p>Link indexes are consecutive integers starting from 1.
     * 
     * @param id link ID label.
     * @return the link index.
     * @throws EpanetException 
     */
    public int ENgetlinkindex( String id ) throws EpanetException {
        int[] index = new int[1];
        int error = epanet.ENgetlinkindex(id, index);
        checkError(error);
        return index[0];
    }

    /**
     * Retrieves the ID label of a link with a specified index.
     * 
     * @param index link index.
     * @return the link label.
     * @throws EpanetException 
     */
    public String ENgetlinkid( int index ) throws EpanetException {
        ByteBuffer bb = ByteBuffer.allocate(64);
        int errcode = epanet.ENgetlinkid(index, bb);
        checkError(errcode);

        String label;
        label = byteBuffer2String(bb);
        return label;
    }

    /**
     * Retrieves the link-type code for a specific link.  
     * 
     * @param index link index.
     * @return the {@link LinkTypes}.
     * @throws EpanetException
     */
    public LinkTypes ENgetlinktype( int index ) throws EpanetException {
        int[] typecode = new int[1];
        int error = epanet.ENgetlinktype(index, typecode);
        checkError(error);

        LinkTypes type = LinkTypes.forCode(typecode[0]);
        return type;
    }

    /**
     * Retrieves the indexes of the end nodes of a specified link.
     * 
     * <p> Node and link indexes are consecutive integers starting from 1.  
     * 
     * <p>The From and To nodes are as defined for the link in the 
     * EPANET input file. The actual direction of flow in the link is 
     * not considered.   
     * 
     * @param index the link index.
     * @return the from and to node indexes as a two items array.
     * @throws EpanetException 
     */
    public int[] ENgetlinknodes( int index ) throws EpanetException {
        int[] from = new int[1];
        int[] to = new int[1];
        int error = epanet.ENgetlinknodes(index, from, to);
        checkError(error);
        return new int[]{from[0], to[0]};
    }

    /**
     * Retrieves the value of a specific link parameter. 
     * 
     * @param index link index.
     * @param param {@link LinkParameters}.
     * @return the value.
     * @throws EpanetException
     */
    public float[] ENgetlinkvalue( int index, LinkParameters param ) throws EpanetException {
        float[] value = new float[2];
        int errcode = epanet.ENgetlinkvalue(index, param.getCode(), value);
        checkError(errcode);

        return value;
    }

    /**
     * Get the version of OmsEpanet.
     *
     * @return the version of epanet.
     * @throws EpanetException
     */
    public int ENgetversion() throws EpanetException {
        int[] version = new int[0];
        int errcode = epanet.ENgetversion(version);
        checkError(errcode);
        return version[0];
    }
    /**
     * @deprecated not implemented yet
     */
    public int ENsetcontrol( int int1, int int2, int int3, float float1, int int4, float float2 ) {
        return -1;
    }
    /**
     * Sets the value of a parameter for a specific node.   
     * 
     * @param index node index.
     * @param paramcode parameter code.
     * @param value parameter value.
     * @throws EpanetException 
     */
    public void ENsetnodevalue( int index, NodeParameters nodeParameter, float value ) throws EpanetException {
        int errcode = epanet.ENsetnodevalue(index, nodeParameter.getCode(), value);
        checkError(errcode);
    }
    /**
     * Sets the value of a parameter for a specific link.   
     *  
     * @param index node index.
     * @param paramcode parameter code.
     * @param value parameter value.
     * @throws EpanetException 
     */
    public void ENsetlinkvalue( int index, LinkParameters linkParameter, float value ) throws EpanetException {
        int errcode = epanet.ENsetnodevalue(index, linkParameter.getCode(), value);
        checkError(errcode);
    }

    /**
     * Adds a new time pattern to the network.  
     * 
     * @param id ID label of pattern.
     * @throws EpanetException 
     */
    public void ENaddpattern( String id ) throws EpanetException {
        int errcode = epanet.ENaddpattern(id);
        checkError(errcode);
    }
    /**
     * @deprecated not implemented yet
     */
    public int ENsetpattern( int int1, FloatBuffer floatPtr1, int int2 ) {
        return -1;
    }
    /**
     * @deprecated not implemented yet
     */
    public int ENsetpatternvalue( int int1, int int2, float float1 ) {
        return -1;
    }

    /**
     *  Sets the value of a time parameter.  
     *  
     * @param paramcode the {@link TimeParameterCodes}. 
     * @param timevalue value of time parameter in seconds.
     * @throws EpanetException 
     */
    public void ENsettimeparam( TimeParameterCodes code, Long timevalue ) throws EpanetException {
        int errcode = epanet.ENsettimeparam(code.getCode(), timevalue);
        checkError(errcode);
    }

    /**
     * Sets the value of a particular analysis option.  
     * 
     * @param optionCode the {@link OptionParameterCodes}.
     * @param value the option value.
     * @throws EpanetException 
     */
    public void ENsetoption( OptionParameterCodes optionCode, float value ) throws EpanetException {
        int errcode = epanet.ENsetoption(optionCode.getCode(), value);
        checkError(errcode);
    }

    /**
     * @deprecated not implemented yet
     */
    public int ENsetstatusreport( int int1 ) {
        return -1;
    }
    /**
     * @deprecated not implemented yet
     */
    public int ENsetqualtype( int int1, ByteBuffer charPtr1, ByteBuffer charPtr2, ByteBuffer charPtr3 ) {
        return -1;
    }

    public void checkError( int errcode ) throws EpanetException {
        try {
            EpanetErrors.checkError(errcode);
            warningMessage = EpanetErrors.checkWarning(errcode);
        } catch (EpanetException e) {
            ENclose();
            throw e;
        }
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    private String byteBuffer2String( ByteBuffer bb ) {
        String label;
        StringBuilder sb = new StringBuilder();
        byte[] array = bb.array();
        for( byte b : array ) {
            if (b != 0) {
                sb.append((char) b);
            } else {
                sb.append(' ');
            }
        }
        label = sb.toString().trim();
        return label;
    }

}
