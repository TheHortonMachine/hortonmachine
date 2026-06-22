/**
 * TODO 
 *This class provides a temporary solution for importing data into the database. 
 *The prerequisite is that the input data must be in the standard GeoFrame CSV format. 
*/

package org.hortonmachine.hmachine.geoframe.io.database.importer;

import org.hortonmachine.gears.libs.modules.HMConstants;

import oms3.annotations.Author;
import oms3.annotations.Description;
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

}
