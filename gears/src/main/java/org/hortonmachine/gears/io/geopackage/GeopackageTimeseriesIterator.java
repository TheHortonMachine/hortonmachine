package org.hortonmachine.gears.io.geopackage;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.utils.SqlName;

/**
 * Buffered cursor over a GeoPackage (or any {@link ADb}-backed) two-column
 * table shaped as {@code <timestampCol> INTEGER PRIMARY KEY, <valueCol> REAL}:
 * reads {@code bufferSize} rows at a time into flat {@code long[]}/
 * {@code double[]} buffers - no boxing, no per-row object allocation.
 *
 * <p>
 * Both columns are named explicitly rather than inferred from
 * {@code SELECT *} - a caller that doesn't know the value column name up
 * front (e.g. imported from a CSV header) should discover it via
 * {@code PRAGMA table_info(<table>)} and pass the result in, rather than
 * relying on column order.
 *
 * <p>
 * Generic - not tied to any one GEOframe component's schema. Originally
 * written for WHETGEO-1D's input handler, also used by GEOET's
 * {@code GeoetInputsHandler}; reach for it whenever a component reads one
 * driving timeseries out of a GeoPackage table with this shape.
 *
 * <pre>{@code
 * try (GeopackageTimeseriesIterator it = new GeopackageTimeseriesIterator(db, "timeseries_airT", "timestamp",
 * 		"value_1", 1000)) {
 * 	while (it.next()) {
 * 		long ts = it.timestamp();
 * 		double v = it.value();
 * 	}
 * }
 * }</pre>
 */
public class GeopackageTimeseriesIterator implements AutoCloseable {

	private final IHMConnection conn;
	private final IHMStatement stmt;
	private final IHMResultSet rs;

	private final long[] tsBuffer;
	private final double[] valBuffer;

	private int bufferFill = 0;
	private int bufferPos = -1;

	public GeopackageTimeseriesIterator(ADb db, String tableNameStr, String timestampCol, String valueCol,
			int bufferSize) throws Exception {
		this(db, tableNameStr, timestampCol, valueCol, null, null, bufferSize);
	}

	/**
	 * @param startMillis epoch-millis lower bound (inclusive), or null for no lower
	 *                    limit
	 * @param endMillis   epoch-millis upper bound (inclusive), or null for no upper
	 *                    limit
	 */
	public GeopackageTimeseriesIterator(ADb db, String tableNameStr, String timestampCol, String valueCol,
			Long startMillis, Long endMillis, int bufferSize) throws Exception {
		SqlName tableName = SqlName.m(tableNameStr);
		StringBuilder sql = new StringBuilder("SELECT ").append(timestampCol).append(", ").append(valueCol)
				.append(" FROM ").append(tableName.fixedDoubleName);
		boolean hasWhere = false;
		if (startMillis != null) {
			sql.append(" WHERE ").append(timestampCol).append(" >= ").append(startMillis);
			hasWhere = true;
		}
		if (endMillis != null) {
			sql.append(hasWhere ? " AND " : " WHERE ").append(timestampCol).append(" <= ").append(endMillis);
		}
		sql.append(" ORDER BY ").append(timestampCol).append(" ASC");
		conn = db.getConnectionInternal();
		stmt = conn.createStatement();
		rs = stmt.executeQuery(sql.toString());
		tsBuffer = new long[bufferSize];
		valBuffer = new double[bufferSize];
	}

	/**
	 * Advances to the next row.
	 *
	 * @return {@code true} if a row is available; {@code false} when exhausted
	 */
	public boolean next() throws Exception {
		int nextPos = bufferPos + 1;
		if (nextPos < bufferFill) {
			bufferPos = nextPos;
			return true;
		}
		refill();
		bufferPos = 0;
		return bufferFill > 0;
	}

	/**
	 * Returns the timestamp of the current row (epoch millis). Valid only after a
	 * successful {@link #next()}.
	 */
	public long timestamp() {
		return tsBuffer[bufferPos];
	}

	/** Returns the value of the current row. */
	public double value() {
		return valBuffer[bufferPos];
	}

	/**
	 * Returns the value of the current row wrapped in a fresh single-element
	 * array. Convenience for callers that feed a {@code HashMap<Integer,
	 * double[]>}-typed OMS field (the convention every solver in this codebase
	 * uses), so they don't need to box it themselves.
	 */
	public double[] values() {
		return new double[] { valBuffer[bufferPos] };
	}

	@Override
	public void close() throws Exception {
		rs.close();
		stmt.close();
		conn.close();
	}

	private void refill() throws Exception {
		bufferFill = 0;
		while (bufferFill < tsBuffer.length && rs.next()) {
			tsBuffer[bufferFill] = rs.getLong(1);
			valBuffer[bufferFill] = rs.getDouble(2);
			bufferFill++;
		}
	}
}
