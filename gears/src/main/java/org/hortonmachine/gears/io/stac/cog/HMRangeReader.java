/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.io.stac.cog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageioimpl.plugins.cog.AbstractRangeReader;

/**
 * A range reader of cloud optimized GeoTIFFs that reads the bytes through a {@link RangeFetcher}, as the
 * authenticated requests of an {@link org.hortonmachine.gears.io.stac.auth.HMStacAccess} (signed S3 requests,
 * tokens, api keys...), so that only the header and the blocks needed are read, as for the public COGs.
 *
 * <p>The headers are not put in the shared cache of the range readers, which is keyed by address only: the
 * bytes read with the credentials of one access are never given to another.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMRangeReader extends AbstractRangeReader {
    /** The parallel range requests of all readers. */
    private static final int THREADS = 8;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(THREADS, r -> {
        Thread thread = new Thread(r, "hm-cog-range-reader");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicLong TOTAL_BYTES_READ = new AtomicLong();

    /**
     * Reads a range of bytes of an object.
     */
    @FunctionalInterface
    public interface RangeFetcher {
        /**
         * @param start the first byte.
         * @param end the last byte, inclusive.
         * @return the bytes, fewer than asked if the object ends before.
         * @throws IOException if the request fails.
         */
        byte[] fetch( long start, long end ) throws IOException;
    }

    private final RangeFetcher fetcher;
    private final AtomicLong bytesRead = new AtomicLong();

    /**
     * @param uri the address of the object, as http(s) url (used only to name it).
     * @param headerLength the length of the header read at first.
     * @param fetcher the reader of the bytes.
     */
    public HMRangeReader( BasicAuthURI uri, int headerLength, RangeFetcher fetcher ) {
        super(uri, headerLength);
        this.fetcher = fetcher;
    }

    /**
     * @return the bytes read by this reader.
     */
    public long getBytesRead() {
        return bytesRead.get();
    }

    /**
     * @return the bytes read by all readers since the start or the last {@link #resetTotalBytesRead()}.
     */
    public static long getTotalBytesRead() {
        return TOTAL_BYTES_READ.get();
    }

    public static void resetTotalBytesRead() {
        TOTAL_BYTES_READ.set(0);
    }

    private byte[] fetch( long start, long end ) {
        try {
            byte[] bytes = fetcher.fetch(start, end);
            bytesRead.addAndGet(bytes.length);
            TOTAL_BYTES_READ.addAndGet(bytes.length);
            return bytes;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read bytes " + start + "-" + end + " of " + uri + ": " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] readHeader() {
        byte[] header = data.get(0L);
        if (header != null)
            return header;
        header = fetch(headerOffset, headerOffset + headerLength - 1L);
        data.put(0L, header);
        return header;
    }

    @Override
    public byte[] fetchHeader() {
        byte[] currentHeader = data.get(0L);
        if (currentHeader != null)
            headerOffset = currentHeader.length;
        byte[] headerBytes = fetch(headerOffset, headerOffset + headerLength - 1L);
        if (headerOffset != 0) {
            byte[] newHeader = new byte[currentHeader.length + headerBytes.length];
            System.arraycopy(currentHeader, 0, newHeader, 0, currentHeader.length);
            System.arraycopy(headerBytes, 0, newHeader, currentHeader.length, headerBytes.length);
            headerBytes = newHeader;
        }
        data.put(0L, headerBytes);
        return headerBytes;
    }

    @Override
    public Map<Long, byte[]> read( Collection<long[]> ranges ) {
        return read(ranges.toArray(new long[][]{}));
    }

    @Override
    public Map<Long, byte[]> read( long[]... ranges ) {
        ranges = reconcileRanges(ranges);
        Map<Long, byte[]> values = new HashMap<>();
        if (ranges.length == 0)
            return values;

        List<long[]> missing = new ArrayList<>();
        for( long[] range : ranges ) {
            byte[] cached = data.get(range[0]);
            if (cached != null)
                values.put(range[0], cached);
            else
                missing.add(range);
        }
        if (missing.size() == 1) {
            long[] range = missing.get(0);
            values.put(range[0], fetch(range[0], range[1]));
        } else if (!missing.isEmpty()) {
            List<Future<byte[]>> futures = new ArrayList<>(missing.size());
            for( long[] range : missing ) {
                futures.add(EXECUTOR.submit(() -> fetch(range[0], range[1])));
            }
            for( int i = 0; i < missing.size(); i++ ) {
                try {
                    values.put(missing.get(i)[0], futures.get(i).get());
                } catch (InterruptedException e) {
                    futures.forEach(f -> f.cancel(true));
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted reading " + uri, e);
                } catch (ExecutionException e) {
                    futures.forEach(f -> f.cancel(true));
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException runtimeException)
                        throw runtimeException;
                    throw new RuntimeException(cause);
                }
            }
        }
        for( long[] range : missing ) {
            data.put(range[0], values.get(range[0]));
        }
        return values;
    }
}
