package org.hortonmachine.gears.io.copernicus;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

@Description("Downloads, caches and converts to GeoTIFF a day of Copernicus AgERA5 reference evapotranspiration data (mm/day).")
@Author(name = "Antonello Andrea")
@Keywords("Copernicus, CDS, AgERA5, evapotranspiration, climate")
@Label("Copernicus")
@Name("copernicusDailyEvapotranspiration")
@Status(5)
@License("General Public License Version 3 (GPLv3)")
public class CopernicusDailyEvapotranspirationManager extends CopernicusDailyAgrometeoManagerBase {

    @Override
    protected String getVariable() {
        return "reference_evapotranspiration";
    }

    @Override
    protected String getStatistic() {
        // already a daily-cumulated quantity, no statistic value is valid for it
        return null;
    }

    @Execute
    @Override
    public void process() throws Exception {
        super.process();
    }

}
