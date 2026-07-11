package org.hortonmachine.hmachine.geoframe.ermworkflow;

import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.TimeResolution;

/**
 * Helper class to centralize common data to execute ERM.
 */
public class ErmCommonData {
	
	public static String START_TIMESTAMP = "2015-10-01 01:00";
	public static String END_TIMESTAMP = "2023-10-01 01:00";
	public static TimeResolution TIME_RESOLUTION = TimeResolution.HOURLY;

}
