package org.hortonmachine.geoframe.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.DynamicDoubleArray;
import org.hortonmachine.gears.utils.chart.TimeSeries;
import org.hortonmachine.geoframe.calibration.WaterBudgetParameters;
import org.hortonmachine.geoframe.core.TopologyNode;
import org.hortonmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.utils.GeoframeUtils;
import org.jfree.chart.ChartPanel;
import org.locationtech.jts.geom.Geometry;

/**
 * An interface representing a complete execution of the
 * Geoframe Water Budget simulation.
 */
public interface IWaterBudgetSimulationRunner {

	/**
	 * Configure the runner with the necessary parameters.
	 * 
	 * @param timeStepMinutes the minutes corresponding to the time step used.
	 * @param maxBasinId the maximum basin id (to size output and state arrays).
	 * @param rootNode the root of the topological basin network.
	 * @param basinAreas array of basin areas indexed by basin id.
	 * @param doParallel whether to run the simulation in parallel mode over the basins.
	 * @param doTopologicallyOrdered whether to process nodes in topological order.
	 * @param writeState whether to write the model state to the output database.
	 * @param outputDb output database for writing simulation results (may be null).
	 * @param pm progress monitor for messages and progress tracking.
	 */
	void configure(
	    int timeStepMinutes,
	    int maxBasinId,
	    TopologyNode rootNode,
	    double[] basinAreas,
	    boolean doParallel,
	    boolean doTopologicallyOrdered,
	    boolean writeState, 
	    ADb outputDb,
	    IHMProgressMonitor pm
		);
	
    /**
     * Executes a water budget simulation for the given time interval and model parameters.
     *
     * @param wbParams all paramters required by the water budget model.
     * @param lai Leaf Area Index used by the canopy model.
     * @param precipReader Iterator providing precipitation input data.
     * @param tempReader Iterator providing temperature input data.
     * @param etpReader Iterator providing evapotranspiration input data.
     * @param iterationInfo String containing information about the current iteration (for logging purposes).
     * @return Array containing the simulated discharge values at the root node over time.
     * @throws Exception
     */
    double[] run(
        WaterBudgetParameters wbParams,
        double lai,
        GeoframeEnvDatabaseIterator precipReader,
        GeoframeEnvDatabaseIterator tempReader,
        GeoframeEnvDatabaseIterator etpReader,
        String iterationInfo
    ) throws Exception;
    
    static double[] getObservedDischarge(ADb envDb, String fromTS, String toTS) throws Exception {
		long from = GeoframeEnvDatabaseIterator.str2ts(fromTS);
		long to = GeoframeEnvDatabaseIterator.str2ts(toTS);
		String sql = "select ts, value from observed_discharge where ts >= " + from + " " + "and ts <= " + to
				+ " order by ts asc";
		QueryResult qr = envDb.getTableRecordsMapFromRawSql(sql, -1);
		DynamicDoubleArray dda = new DynamicDoubleArray(10000, 10000);
		int valueIndex = qr.names.indexOf("value");
		for (Object[] row : qr.data) {
			double value = ((Number) row[valueIndex]).doubleValue();
			dda.addValue(value);
		}
		return dda.getTrimmedInternalArray();
	}
    
	static double[] getBasinAreas(ASpatialDb db, int maxBasinId) throws Exception {
		QueryResult queryResult = db.getTableRecordsMapIn(GeoframeUtils.GEOFRAME_BASIN_TABLE, null, -1, -1, null);
		double[] basinAreas = new double[maxBasinId + 1];
		int idIndex = queryResult.names.indexOf("basinid");
		for (int i = 0; i < queryResult.data.size(); i++) {
			Object[] row = queryResult.data.get(i);
			int basinId = (int) row[idIndex];
			Geometry basinGeom = (Geometry) row[queryResult.geometryIndex];
			double area = basinGeom.getArea() / 1_000_000.0; // in km2
			basinAreas[basinId] = area;
		}
		return basinAreas;
	}
	
	static int getMaxBasinId(ADb db) throws Exception {
		int maxBasinId = db.getLong("select max(basinid) from " + GeoframeUtils.GEOFRAME_BASIN_TABLE).intValue();
		return maxBasinId;
	}

	static void quickChartResult(String title, double[] simQ, double[] observedDischarge, int timeStepMinutes,
			String fromTS, Integer spinupTimesteps) {
		String xLabel = "time";
		String yLabel = "Q [m3]";
		int width = 2600;
		int height = 1400;

		List<String> series = new ArrayList<>();
		series.add("Simulated Discharge");
		series.add("Observed Discharge");
		List<Boolean> doLines = new ArrayList<>();
		doLines.add(true);
		doLines.add(true);

		long startTS = GeoframeEnvDatabaseIterator.str2ts(fromTS);

		List<double[]> allValuesList = new ArrayList<>();
		List<long[]> allTimesList = new ArrayList<>();
		// simulated
		DynamicDoubleArray simValues = new DynamicDoubleArray(simQ.length, simQ.length);
		DynamicDoubleArray obsValues = new DynamicDoubleArray(observedDischarge.length, simQ.length);
		DynamicDoubleArray simTimes1 = new DynamicDoubleArray(simQ.length, simQ.length);
		DynamicDoubleArray simTimes2 = new DynamicDoubleArray(simQ.length, simQ.length);
		
		int startIndex = 0;
		if (spinupTimesteps != null && spinupTimesteps > 0) {
			startIndex = spinupTimesteps;
		}
		for (int i = startIndex; i < simQ.length; i++) {
			if (!HMConstants.isNovalue(simQ[i])) {
				simValues.addValue(simQ[i]);
				simTimes1.addValue(startTS + i * timeStepMinutes * 60 * 1000L);
			}

			if (!HMConstants.isNovalue(observedDischarge[i])) {
				obsValues.addValue(observedDischarge[i]);
				simTimes2.addValue(startTS + i * timeStepMinutes * 60 * 1000L);
			}
		}
		
		double[] simValuesArr = simValues.getTrimmedInternalArray();
		long[] simTimesArr1 = simTimes1.getTrimmedInternalArrayLong();

		double[] obsValuesArr = obsValues.getTrimmedInternalArray();
		long[] simTimesArr2 = simTimes2.getTrimmedInternalArrayLong();

		allValuesList.add(simValuesArr);
		allTimesList.add(simTimesArr1);

		allValuesList.add(obsValuesArr);
		allTimesList.add(simTimesArr2);

		TimeSeries timeseriesChart = new TimeSeries(title, series, allTimesList, allValuesList);
		timeseriesChart.setXLabel(xLabel);
		timeseriesChart.setYLabel(yLabel);
		timeseriesChart.setShowLines(doLines);

		timeseriesChart.setColors(new Color[] { Color.BLUE, Color.RED });
//	        if (doShapes != null)
//	            timeseriesChart.setShowShapes(doShapes);

		ChartPanel chartPanel = new ChartPanel(timeseriesChart.getChart(), true);
		Dimension preferredSize = new Dimension(width, height);
		chartPanel.setPreferredSize(preferredSize);

//	        GuiUtilities.openDialogWithPanel(chartPanel, "HM Chart Window", preferredSize, false);
		JDialog f = new JDialog();
		f.add(chartPanel, BorderLayout.CENTER);
		f.setTitle(title);
		f.setModal(false);
		f.pack();
//	        if (dimension != null)
//	            f.setSize(dimension);
		f.setLocationRelativeTo(null); // Center on screen
		f.setVisible(true);
		f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		f.getRootPane().registerKeyboardAction(e -> {
			f.dispose();
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

	}
}

