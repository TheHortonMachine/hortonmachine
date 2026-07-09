/**
 * This package provides a possible workflow to prepare the data for
 * and execute ERM calibrations and simulations.
 * 
 * Steps:
 * 
 * <ol>
 * <li> {@link org.hortonmachine.hmachine.geoframe.ermworkflow.ErmDataPreparator}: prepare the geospatial data starting form the dtm 
 * and the stream gauges. This results in the creation of the basins and network topology inside a db.</li>
 * <li> {@link org.hortonmachine.hmachine.geoframe.ermworkflow.ErmStationDataImporter}: import the hydro-meteorological data from the stations and stream gauges.</li>
 * <li> {@link org.hortonmachine.hmachine.geoframe.ermworkflow.ErmKriging}: perform the kriging interpolation of the temperature and precipitation data to the basins.</li>
 * <li> {@link org.hortonmachine.hmachine.geoframe.ermworkflow.ErmRadiation}: compute the radiation for each basin.</li>
 * <li> {@link org.hortonmachine.hmachine.geoframe.ermworkflow.ErmPrestleyEt}: compute the potential evapotranspiration for each basin.</li>
 * 
 */
package org.hortonmachine.hmachine.geoframe.ermworkflow;

