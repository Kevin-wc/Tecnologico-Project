package com.tryright;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/************************************************
 *
 * Author: Kevin Gonzalez
 * Assignment: Program 3
 * Class: CSC 4180
 *
 ************************************************/

/**
 * PointStore implementation for Program 0 text-encoded files
 *
 * <p>Format:
 * First integer is N, followed by N pairs (x y).</p>
 */
public final class TextPointStore implements PointStore {
    private final int[] x;
    private final int[] y;

    /**
     * Construct a TextPointStore from a text-encoded file.
     *
     * @param filename input file path
     * @throws FileNotFoundException if file cannot be opened
     * @throws TriangleUtil.InputFormatException if format is invalid
     */
    public TextPointStore(final String filename)
        throws FileNotFoundException, TriangleUtil.InputFormatException {
        final File file = new File(filename);
        final Scanner scanner = new Scanner(file);

        if (!scanner.hasNextInt()) {
            scanner.close();
            throw new TriangleUtil.InputFormatException("Missing number of points.");
        }

        final int n = scanner.nextInt();
        if (n < 0) {
            scanner.close();
            throw new TriangleUtil.InputFormatException("Number of points cannot be negative");
        }

        final int[] tx = new int[n];
        final int[] ty = new int[n];

        for (int i = 0; i < n; i++) {
            if (!scanner.hasNextInt()) {
                scanner.close();
                throw new TriangleUtil.InputFormatException("Missing x coordinate for point " + i);
            }
            tx[i] = scanner.nextInt();

            if (!scanner.hasNextInt())  {
                scanner.close();
                throw new TriangleUtil.InputFormatException("Missing y coordinate for point " + i);
            }
            ty[i] = scanner.nextInt();
        }
        scanner.close();

        this.x = tx;
        this.y = ty;
    }

    @Override
    public int getX(final int idx) {
        if (idx < 0 || idx >= x.length){
            throw new IndexOutOfBoundsException();
        }
        return x[idx];
    }

    @Override
    public int getY(final int idx) {
        if (idx < 0 || idx >= x.length){
            throw new IndexOutOfBoundsException();
        }
        return y[idx];
    }

    @Override
    public int numPoints()  {
        return x.length;
    }

    @Override
    public void close()  {
        //Points are stored in arrays.
    }
}
