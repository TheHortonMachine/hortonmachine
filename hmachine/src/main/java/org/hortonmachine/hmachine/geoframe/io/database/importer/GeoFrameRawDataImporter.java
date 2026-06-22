/**
 * TODO 
 *This class provides a temporary solution for importing data into the database. 
 *The prerequisite is that the input data must be in the standard GeoFrame CSV format. 
 *
 *
 *nb this is related to the OMSGeoframeInputbuilder where create the stream gauge table.
 *
 *
 *
*/

package org.hortonmachine.hmachine.geoframe.io.database.importer;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMConstants;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("Populate the db with raw data e.g. meteo srtation data, stream gauge data etc.")
@Author(name = "Daniele Andreis")
@Keywords("time series, iterator, basin, value, database")
@Name("GeoFrameRawDataImporter")
@Status(40)
@UI(HMConstants.HIDE_UI_HINT)
@License("General Public License Version 3 (GPLv3)")
public class GeoFrameRawDataImporter {

	@Description("A colum with the measurement point id")
	@In
	public String inMeasurementPointIdFieldName = null;

	@Description("Input station/measured location vector point.")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inMeasurementsPointFilePath = null;

	@Description("Input csv file with data to import.")
	@UI(HMConstants.FILEIN_UI_HINT_CSV)
	@In
	public String inMeasurementDataFilePath = null;

	@Description("Input database path")
	@UI(HMConstants.FILEIN_UI_HINT_DBF)
	@In
	public String inGeoframeDBPath = null;

	public boolean doOverWrite = true;

	public void process() {

		if (inGeoframeDBPath == null) {
			throw new IllegalArgumentException();
		}
		try {
			ADb inGeoframeDb = EDb.SQLITE.getDb();
			inGeoframeDb.open(inGeoframeDBPath);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
