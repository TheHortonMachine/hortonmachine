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

import java.util.Date;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.time.ETimeUtilities;
import org.hortonmachine.geoframe.core.utils.TopologyNode;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

@Description("A module to write water budget simulation results into the database.")
@Author(name = "Andrea Antonello", contact = "https://g-ant.eu")
@Keywords("simulation, writer, basin, value, database")
@Label("WaterBudgetSimulationWriter")
@Name("WaterBudgetSimulationWriter")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class GeoframeWaterBudgetSimulationWriter extends HMModel {

	@Description("The db to use.")
	@In
	public ADb db = null;
	
	@Description("The topologynode carrying the value to store.")
	@In
	public TopologyNode rootNode = null;
	
	@Description("The current time step to write to.")
	@In
	public long currentT;

	
	private IHMPreparedStatement ps;
	private IHMConnection conn;
	
	
	private String tableName = "water_budget_simulation_discharge" + "_" + ETimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.format(new Date());
	
	public void clearTable() throws Exception {
		if(db.hasTable(tableName)) {
			String sql = "DROP TABLE " + tableName + ";";
			db.executeInsertUpdateDeleteSql(sql);
		}
	}

    private void ensureOpen() throws Exception {
    	if(ps != null) {
    		return;
    	}

    	// make sure the output table exists
    	String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" 
    			+ "ts BIGINT NOT NULL, "
				+ "basin_id INT NOT NULL, "
				+ "value DOUBLE, "
				+ "PRIMARY KEY (ts, basin_id) "
				+ ");";
    	db.executeInsertUpdateDeleteSql(sql);
		
    	String insertSql = "INSERT INTO " + tableName + " (ts, basin_id, value) VALUES (?,?,?) ";
    	conn = db.getConnectionInternal();
    	ps = conn.prepareStatement(insertSql);
    	
    }

    @Execute
    public void insert() throws Exception {
        ensureOpen();
        
        conn.enableAutocommit(false);
        rootNode.visitUpstream(node -> {
			try {
				ps.setLong(1, currentT);
				ps.setInt(2, node.basinId);
				ps.setDouble(3, node.accumulatedValue);
				ps.addBatch();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
        ps.executeBatch();
        conn.commit();
        conn.enableAutocommit(true);
    }
    
    @Finalize
    public void close() throws Exception {
        ps.close();
    }
}
