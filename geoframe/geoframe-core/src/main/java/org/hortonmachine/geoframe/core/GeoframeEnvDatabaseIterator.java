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
package org.hortonmachine.geoframe.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.joda.time.DateTime;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description("A time, basin id and value per timestep iterator, from db.")
@Author(name = "Andrea Antonello", contact = "https://g-ant.eu")
@Keywords("time series, iterator, basin, value, database")
@Label("Time Basin Value Db Iterator")
@Name("TimeBasinValueDbIterator")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class GeoframeEnvDatabaseIterator extends HMModel {

    @Description("The db to use.")
    @In
    public ADb db = null;

    @Description("The parameter id to read to define the measure type to read.")
    @In
    public Integer pParameterId = null;

    @Description("Novalue")
    @In
    public String pDataNovalue = "-9999.0";

    @Description("The optional time at which start to read (format: yyyy-MM-dd HH:mm ).")
    @In
    public String tStart;

    @Description("The optional time at which end to read (format: yyyy-MM-dd HH:mm ).")
    @In
    public String tEnd;

    @Description("The current time read.")
    @Out
    public String tCurrent;

//    @Description("The previous time read.")
//    @Out
//    public String tPrevious;
    
    // TODO the timestep to check data consistency

    @Description("The array of values for each basin (basinid is the index).")
    @Out
    public double[] outData = null;

	private PreparedStatement ps;

	private ResultSet rs;
	
	private boolean initialized = false;

	private long currentT;
	
	private boolean isClosed = false;

	
	
	public GeoframeEnvDatabaseIterator(int maxBasinId) {
		 outData = new double[maxBasinId + 1];
	}

    private void ensureOpen() throws Exception {
    	if(rs != null) {
    		return;
    	}
    	checkNull(pParameterId, db);
    	
    	String sql = "SELECT ts, basin_id, value FROM measurement WHERE parameter_id = ?";
    	
    	if (tStart != null) {
    		// add start time condition
    		long startTs = str2ts(tStart);
    		sql += " AND ts >= " + startTs;
		}
    	if (tEnd != null) {
			// add end time condition
			long endTs = str2ts(tEnd);
			sql += " AND ts <= " + endTs;
    	}
    	sql += " ORDER BY ts, basin_id";
    	Connection connection = db.getJdbcConnection();
                
    	ps = connection.prepareStatement(
	           sql,
	           ResultSet.TYPE_FORWARD_ONLY,
	           ResultSet.CONCUR_READ_ONLY);
    	ps.setInt(1, pParameterId);
    	rs = ps.executeQuery();
    }

    @Execute
    public boolean next() throws Exception {
    	if(isClosed) {
    		return false;
    	}
        ensureOpen();
        // clean the array
        Arrays.fill(outData, Double.NaN);
        
        if (!initialized) {
            if (!rs.next()) {
            	isClosed = true;
                return false;
            }
            initialized = true;
        }
        
        boolean hasNext = false;
        do {
        	long ts = rs.getLong("ts");
        	currentT = ts;
        	tCurrent = ts2str(currentT);
            int basinId  = rs.getInt("basin_id");
            double value = rs.getDouble("value");
            outData[basinId] = value;
            
            hasNext = true;

            if (!rs.next()) {
            	isClosed = true;
                break;
            }

            long nextT = rs.getLong("ts");
            if (nextT != ts) {
                break;
            }
        } while (true);
        
        return hasNext;
    }
    
    public static String ts2str( long millis ) {
        return new DateTime(millis).toString(HMConstants.utcDateFormatterYYYYMMDDHHMMSS);
    }

    public static long str2ts( String isoString ) {
        return HMConstants.utcDateFormatterYYYYMMDDHHMMSS.parseDateTime(isoString).getMillis();
    }


    @Finalize
    public void close() throws Exception {
        rs.close();
        ps.close();
    }
}
