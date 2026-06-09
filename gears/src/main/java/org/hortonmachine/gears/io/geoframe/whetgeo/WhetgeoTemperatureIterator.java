package org.hortonmachine.gears.io.geoframe.whetgeo;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.utils.SqlName;

/**
 * Buffered cursor over a temperature table.
 *
 * <p>Reads {@code bufferSize} rows at a time from the DB. No boxing, no
 * per-row object allocation.
 *
 * <pre>{@code
 * try (TemperatureIterator it = handler.iterateTemperatureBottomInterface(1000)) {
 *     while (it.next()) {
 *         long ts    = it.timestamp();
 *         double[] v = it.values();  // internal array — copy if you need to keep it
 *     }
 * }
 * }</pre>
 */
public class WhetgeoTemperatureIterator implements AutoCloseable {

    private final IHMConnection conn;
    private final IHMStatement stmt;
    private final IHMResultSet rs;
    private final int valueColCount;

    private final long[]     tsBuffer;
    private final double[][] valBuffer;

    private int bufferFill = 0;
    private int bufferPos  = -1;

    WhetgeoTemperatureIterator( ADb db, String tableNameStr, String timestampCol, int bufferSize ) throws Exception {
        SqlName tableName = SqlName.m(tableNameStr);
        String sql = "SELECT * FROM " + tableName.fixedDoubleName
                + " ORDER BY " + timestampCol + " ASC";
        conn = db.getConnectionInternal();
        stmt = conn.createStatement();
        rs   = stmt.executeQuery(sql);
        valueColCount = rs.getMetaData().getColumnCount() - 1;
        tsBuffer  = new long[bufferSize];
        valBuffer = new double[bufferSize][valueColCount];
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
     * Returns the timestamp of the current row (epoch millis).
     * Valid only after a successful {@link #next()}.
     */
    public long timestamp() {
        return tsBuffer[bufferPos];
    }

    /** Returns a copy of the values of the current row. */
    public double[] values() {
        return valBuffer[bufferPos].clone();
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
            for (int i = 0; i < valueColCount; i++) {
                valBuffer[bufferFill][i] = rs.getDouble(i + 2);
            }
            bufferFill++;
        }
    }
}
