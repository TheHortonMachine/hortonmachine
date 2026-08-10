package org.hortonmachine.gears.io.copernicus;

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

@Description("Downloads, caches and converts to GeoTIFF a day of Copernicus AgERA5 2m temperature data (Kelvin).")
@Author(name = "Antonello Andrea")
@Keywords("Copernicus, CDS, AgERA5, temperature, climate")
@Label("Copernicus")
@Name("copernicusDailyTemperature")
@Status(5)
@License("General Public License Version 3 (GPLv3)")
public class CopernicusDailyTemperatureManager extends CopernicusDailyAgrometeoManagerBase {

    @Description("Daily aggregation to use: MEAN, MIN or MAX.")
    @In
    public String pAggregation = "MEAN";

    @Override
    protected String getVariable() {
        return "2m_temperature";
    }

    @Override
    protected String getStatistic() {
        String agg = pAggregation == null ? "MEAN" : pAggregation.trim().toUpperCase();
        switch( agg ) {
        case "MEAN":
            return "24_hour_mean";
        case "MIN":
            return "24_hour_minimum";
        case "MAX":
            return "24_hour_maximum";
        default:
            throw new ModelsIllegalargumentException("pAggregation must be one of MEAN, MIN, MAX, got: " + pAggregation,
                    this.getClass().getSimpleName(), pm);
        }
    }

    @Execute
    @Override
    public void process() throws Exception {
        super.process();
    }

}
