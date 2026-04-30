package com.tryright;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/************************************************
 *
 * Author: Kevin Gonzalez
 * Assignment: Program 3
 * Class: CSC 4180
 *
 ************************************************/

/**
 * PointStore implementation for binary-encoded point files.
 *
 * <p>
 * Binary format:
 * Zero or more pairs of 4-byte big-endian, two's-complement integers (x then y)
 * Total file size must be a multiple of 8 bytes.
 * </p>
 *
 * <p>
 * this store uses memory-mapped I/O as required
 * </p>
 */
public final class BinPointStore implements PointStore {
    private static final int INT_SIZE = 4;
    private static final int POINT_SIZE = 8;

    private final RandomAccessFile file;
    private final FileChannel channel;
    private final MappedByteBuffer buffer;
    private final int n;

    /**
     * Construct a BinPointStore from a binary-encoded file.
     *
     * @param filename input file path
     * @throws IOException                       if the file cannot be
     *                                           opened/read/mapped
     * @throws TriangleUtil.InputFormatException if binary format is invalid
     */
    public BinPointStore(final String filename)
            throws IOException, TriangleUtil.InputFormatException {

        this.file = new RandomAccessFile(filename, "r");
        this.channel = file.getChannel();

        final long sizeBytes = channel.size();

        // Must contain 0 or more complete (x,y) pairs.
        if (sizeBytes % POINT_SIZE != 0) {
            closeQuietly();
            throw new TriangleUtil.InputFormatException(
                    "Binary file has incomplete point pair (size not multiple of 8 bytes).");
        }

        final long numPts = sizeBytes / POINT_SIZE;
        if (numPts > Integer.MAX_VALUE) {
            closeQuietly();
            throw new TriangleUtil.InputFormatException("Binary file contains too many points.");
        }

        this.n = (int) numPts;

        // Map the whole file read-only.
        this.buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, sizeBytes);
    }

    @Override
    public int getX(final int idx) {
        checkBounds(idx);
        final int offset = idx * POINT_SIZE;
        return buffer.getInt(offset);
    }

    @Override
    public int getY(final int idx) {
        checkBounds(idx);
        final int offset = idx * POINT_SIZE + INT_SIZE;
        return buffer.getInt(offset);
    }

    // Added checkBounds helper since TEC mentioned that getX() and getY() repeat
    // the same bound check
    private void checkBounds(final int idx) {
        if (idx < 0 || idc >= n) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int numPoints() {
        return n;
    }

    @Override
    public void close() {
        // No official unmap in standard Java;
        closeQuietly();
    }

    private void closeQuietly() {
        try {
            channel.close();
        } catch (Exception ignored) {
        }
        try {
            file.close();
        } catch (Exception ignored) {
        }
    }
}
