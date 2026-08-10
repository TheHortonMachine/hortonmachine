package org.hortonmachine.gears.io.copernicus;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

/**
 * Generic manager for any Copernicus AgERA5 variable not covered by a dedicated manager
 * (e.g. cloud_cover, vapour_pressure, snow_thickness, 2m_relative_humidity, ...). Use
 * {@link OGCProcessesManager#getProcessDescription(String)} /
 * {@link OGCProcessesManager#getConstraints(String, java.util.Map)} against
 * {@link CopernicusDailyAgrometeoManagerBase#DATASET_ID} to discover legal pVariable
 * values and, per variable, which pStatistic values (if any) are valid.
 *
 */
@Description("Downloads, caches and converts to GeoTIFF a day of an arbitrary Copernicus AgERA5 variable.")
@Author(name = "Antonello Andrea")
@Keywords("Copernicus, CDS, AgERA5, climate")
@Label("Copernicus")
@Name("copernicusDailyGeneric")
@Status(5)
@License("General Public License Version 3 (GPLv3)")
public class CopernicusDailyGenericManager extends CopernicusDailyAgrometeoManagerBase {

    @Description("The CDS 'variable' value to request (see getProcessDescription/getConstraints to discover legal values).")
    @In
    public String pVariable;

    @Description("The CDS 'statistic' value to request, or null if not applicable to pVariable (see getConstraints).")
    @In
    public String pStatistic = null;

    @Override
    protected String getVariable() {
        return pVariable;
    }

    @Override
    protected String getStatistic() {
        return pStatistic;
    }

    @Execute
    @Override
    public void process() throws Exception {
        checkNull(pVariable);
        super.process();
    }

}
