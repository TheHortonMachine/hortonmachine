package org.jgrasstools.gears.io.disktree;

public interface IDiskTree {
    /**
     * Position of the index address.
     */
    long INDEX_ADDRESS_POSITION = 14;

    /**
     * Byte size of the index address.
     */
    long INDEX_ADDRESS_SIZE = 8;

    /**
     * Byte size of the index length.
     */
    long INDEX_LENGTH_SIZE = 8;
}
