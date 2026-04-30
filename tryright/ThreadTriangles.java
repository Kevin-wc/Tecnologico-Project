package com.tryright;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/************************************************
 *
 * Author: Kevin Gonzalez
 * Assignment: Program 3
 * Class: CSC 4180
 *
 ************************************************/

/**
 * Counts the number of right triangles using multiple threads.
 *
 * <p>This program solves the same problem as {@link Triangles}, but it uses threads
 * instead of processes. Threads share memory, so each thread can write its result
 * into a shared array, and the main thread just adds the results at the end.</p>
 *
 */
public final class ThreadTriangles {

    private static final int EXIT_OK = 0;
    private static final int EXIT_USAGE = 1;
    private static final int EXIT_FILE = 2;
    private static final int EXIT_INPUT = 3;
    private static final int EXIT_WORKER = 4;
    private static final int EXIT_INTERNAL = 10;

    /** Prevent creating instances of this class. */
    private ThreadTriangles() {
    }

    /**
     * Program entry point.
     *
     * <p>Usage:
     * {@code java com.tryright.ThreadTriangles <inputfile> <numThreads>}</p>
     *
     * @param args command-line arguments: input filename and number of threads
     */
    public static void main(final String[] args) {

        // Must have exactly 2 arguments: filename and thread count
        if (args == null || args.length != 2) {
            System.err.println("Usage: java com.tryright.ThreadTriangles <inputfile> <numThreads>");
            System.exit(EXIT_USAGE);
            return;
        }

        final String filename = args[0];

        int numThreads;
        try {
            numThreads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: numThreads must be an integer.");
            System.exit(EXIT_USAGE);
            return;
        }

        if (numThreads <= 0) {
            System.err.println("Error: numThreads must be positive.");
            System.exit(EXIT_USAGE);
            return;
        }

        // Validate input file
        final File file = new File(filename);
        if (!file.exists()) {
            System.err.println("Error: no such file: " + filename);
            System.exit(EXIT_FILE);
            return;
        }
        if (!file.isFile()) {
            System.err.println("Error: not a regular file: " + filename);
            System.exit(EXIT_FILE);
            return;
        }
        if (!file.canRead()) {
            System.err.println("Error: permission denied: " + filename);
            System.exit(EXIT_FILE);
            return;
        }

        PointStore ps = null;

        try {
            // Changed using shared method from TriangleUtil after optimizations from TEC feedback
            ps = TriangleUtil.openPointStore(filename);
            final PointStore store = ps;

            final int n = ps.numPoints();

            // Not enough points to make any triangle
            if (n < 3) {
                System.out.println(0);
                System.exit(EXIT_OK);
                return;
            }

            // Outer loop i in TriangleUtil.countRange goes up to n-3.
            // That means the end for i is (n - 2).
            final int end = n - 2;

            // Starting more threads than work doesn't help, so cap it.
            if (numThreads > end) {
                numThreads = end;
                if (numThreads <= 0) {
                    numThreads = 1;
                }
            }

            // Divide the i-range into numThreads chunks
            final int chunkSize = (end + numThreads - 1) / numThreads;

            // Shared memory: each thread writes its answer into one slot
            final long[] partials = new long[numThreads];
            final Thread[] threads = new Thread[numThreads];

            // Start worker threads
            for (int t = 0; t < numThreads; t++) {
                final int index = t;
                final int startI = t * chunkSize;
                final int endI = Math.min(startI + chunkSize, end);

                threads[t] = new Thread(() -> {
                    // Each thread computes only its slice of i-values
                    if (startI >= endI) {
                        partials[index] = 0;
                    } else {
                        partials[index] = TriangleUtil.countRange(store, startI, endI);
                    }
                });

                threads[t].start();
            }

            // Wait for all threads to finish
            for (Thread thread : threads) {
                thread.join();
            }

            // Combine results
            long total = 0;
            for (long value : partials) {
                total += value;
            }

            // Success output: one number only
            System.out.println(total);
            System.exit(EXIT_OK);

        } catch (TriangleUtil.InputFormatException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_INPUT);
        } catch (FileNotFoundException e) {
            System.err.println("Error: file not found.");
            System.exit(EXIT_FILE);
        } catch (IOException e) {
            System.err.println("Error: I/O failure: " + e.getMessage());
            System.exit(EXIT_FILE);
        } catch (InterruptedException e) {
            System.err.println("Error: interrupted while waiting for threads.");
            System.exit(EXIT_INTERNAL);
        } catch (Exception e) {
            System.err.println("Error: unexpected failure: " + e.getMessage());
            System.exit(EXIT_INTERNAL);
        } finally {
            if (ps != null){
                try { ps.close(); } catch (Exception ignored) {}
            }
        }
    }
}
