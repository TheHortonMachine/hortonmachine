package org.hortonmachine.dbs.utils;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;

/**
 * Buffered cursor over any {@link ADb}-backed table with a timestamp column
 * (epoch millis) and one or more value columns (REAL): reads {@code
 * bufferSize} rows at a time into flat {@code long[]}/{@code double[]}
 * buffers - no boxing, no per-row object allocation.
 *
 * <pre>{@code
 * try (DbTimeseriesIterator it = new DbTimeseriesIterator(db, "timeseries_airT", "timestamp",
 * 		"value_1", 1000)) {
 * 	while (it.next()) {
 * 		long ts = it.timestamp();
 * 		double v = it.value();
 * 	}
 * }
 * }</pre>
 */
public class DbTimeseriesIterator implements AutoCloseable {

	private final IHMStatement stmt;
	private final IHMResultSet rs;

	private final boolean singleValue;
	private final int numValueCols;

	private final long[] tsBuffer;
	private final double[] valBuffer;

	private int bufferFill = 0;
	private int bufferPos = -1;

	public DbTimeseriesIterator(ADb db, String tableNameStr, String timestampCol, String valueCol,
			int bufferSize) throws Exception {
		this(db, tableNameStr, timestampCol, valueCol, null, null, bufferSize);
	}

	/**
	 * @param startMillis epoch-millis lower bound (inclusive), or null for no lower
	 *                    limit
	 * @param endMillis   epoch-millis upper bound (inclusive), or null for no upper
	 *                    limit
	 */
	public DbTimeseriesIterator(ADb db, String tableNameStr, String timestampCol, String valueCol,
			Long startMillis, Long endMillis, int bufferSize) throws Exception {
		singleValue = true;
		numValueCols = 1;
		SqlName tableName = SqlName.m(tableNameStr);
		StringBuilder sql = new StringBuilder("SELECT ").append(timestampCol).append(", ").append(valueCol)
				.append(" FROM ").append(tableName.fixedDoubleName);
		appendWhereOrderBy(sql, timestampCol, startMillis, endMillis);
		IHMConnection conn = db.getConnectionInternal();
		stmt = conn.createStatement();
		rs = stmt.executeQuery(sql.toString());
		tsBuffer = new long[bufferSize];
		valBuffer = new double[bufferSize];
	}

	public DbTimeseriesIterator(ADb db, String tableNameStr, String timestampCol, String[] valueCols,
			int bufferSize) throws Exception {
		this(db, tableNameStr, timestampCol, valueCols, null, null, bufferSize);
	}

	/**
	 * @param valueCols   two or more value columns, read in the given order -
	 *                    {@link #values()} returns them in that same order. For
	 *                    exactly one column, prefer the single-{@code valueCol}
	 *                    constructor instead.
	 * @param startMillis epoch-millis lower bound (inclusive), or null for no lower
	 *                    limit
	 * @param endMillis   epoch-millis upper bound (inclusive), or null for no upper
	 *                    limit
	 */
	public DbTimeseriesIterator(ADb db, String tableNameStr, String timestampCol, String[] valueCols,
			Long startMillis, Long endMillis, int bufferSize) throws Exception {
		singleValue = false;
		numValueCols = valueCols.length;
		SqlName tableName = SqlName.m(tableNameStr);
		StringBuilder sql = new StringBuilder("SELECT ").append(timestampCol);
		for (String valueCol : valueCols) {
			sql.append(", ").append(valueCol);
		}
		sql.append(" FROM ").append(tableName.fixedDoubleName);
		appendWhereOrderBy(sql, timestampCol, startMillis, endMillis);
		IHMConnection conn = db.getConnectionInternal();
		stmt = conn.createStatement();
		rs = stmt.executeQuery(sql.toString());
		tsBuffer = new long[bufferSize];
		valBuffer = new double[bufferSize * numValueCols];
	}

	private static void appendWhereOrderBy(StringBuilder sql, String timestampCol, Long startMillis, Long endMillis) {
		boolean hasWhere = false;
		if (startMillis != null) {
			sql.append(" WHERE ").append(timestampCol).append(" >= ").append(startMillis);
			hasWhere = true;
		}
		if (endMillis != null) {
			sql.append(hasWhere ? " AND " : " WHERE ").append(timestampCol).append(" <= ").append(endMillis);
		}
		sql.append(" ORDER BY ").append(timestampCol).append(" ASC");
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

	/** Returns the first (or only) value column of the current row. */
	public double value() {
		return singleValue ? valBuffer[bufferPos] : valBuffer[bufferPos * numValueCols];
	}

	/**
	 * Returns value column {@code colIndex} (0-based, matching constructor
	 * order) of the current row - no allocation. Only valid on a multi-value
	 * iterator.
	 */
	public double value(int colIndex) {
		return valBuffer[bufferPos * numValueCols + colIndex];
	}

	/**
	 * Returns all value columns of the current row as a fresh array, in
	 * constructor order. Convenience for callers that feed a {@code
	 * HashMap<Integer, double[]>}-typed OMS field.
	 */
	public double[] values() {
		if (singleValue) {
			return new double[] { valBuffer[bufferPos] };
		}
		double[] result = new double[numValueCols];
		System.arraycopy(valBuffer, bufferPos * numValueCols, result, 0, numValueCols);
		return result;
	}

	/**
	 * Closes the {@link IHMStatement}/{@link IHMResultSet} this iterator opened -
	 * not the {@link IHMConnection}, which was handed to it and is owned by
	 * whoever opened the {@link ADb}. Close that separately once you're done
	 * with it.
	 */
	@Override
	public void close() throws Exception {
		rs.close();
		stmt.close();
	}

	private void refill() throws Exception {
		bufferFill = 0;
		if (singleValue) {
			while (bufferFill < tsBuffer.length && rs.next()) {
				tsBuffer[bufferFill] = rs.getLong(1);
				valBuffer[bufferFill] = rs.getDouble(2);
				bufferFill++;
			}
		} else {
			while (bufferFill < tsBuffer.length && rs.next()) {
				tsBuffer[bufferFill] = rs.getLong(1);
				int base = bufferFill * numValueCols;
				for (int c = 0; c < numValueCols; c++) {
					valBuffer[base + c] = rs.getDouble(2 + c);
				}
				bufferFill++;
			}
		}
	}
}
