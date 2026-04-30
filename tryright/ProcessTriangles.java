package com.tryright;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/********************************************
 *
 * Author: Kevin Gonzalez
 * Assignemnt: Program 3 (PointStore Update)
 * Class: CSC 4180
 *
 ********************************************/

/**
 * Counts right triangles using multiple Java processes.
 *
 * <p>
 * Parent spltits the outer loop i-range into chunks and starts worker processes.
 * Each worker opens the same input file using PointStore (TextPointStore or BinPointStore)
 * and prints one partial count to stdout.
 * </p>
 */
public final class ProcessTriangles {
    private static final int EXIT_OK = 0;
    private static final int EXIT_USAGE = 1;
    private static final int EXIT_FILE = 2;
    private static final int EXIT_INPUT = 3;
    private static final int EXIT_WORKER = 4;
    private static final int EXIT_INTERNAL = 10;

    public static void main(final String[] args) {
        // Worker process mode:
        // ProcessTriangles --worker <filename> <startI> <endI>
        if (args != null && args.length == 4 && "--worker".equals(args[0])) {
            runWorker(args[1], args[2], args[3]);
            return;
        }

        // Parent mode:
        // ProcessTriangle <inputfile> <numProcesses>
        if (args == null || args.length != 2) {
            System.err.println("Usage: java com.tryright.ProcessTriangles <inputfile> <numProcesses>");
            System.exit(EXIT_USAGE);
            return;
        }

        final String filename = args[0];

        int numProcesses;
        try {
            numProcesses = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: numProcesses must be an integer.");
            System.exit(EXIT_USAGE);
            return;
        }

        if (numProcesses <= 0 ) {
            System.err.println("Error: numProcesses must be positive.");
            System.exit(EXIT_USAGE);
            return;
        }

        // Basic file check
        final File file = new File(filename);
        if (!file.exists()) {
            System.err.println("Error: no such file: " + filename);
            System.exit(EXIT_FILE);
            return;
        }
        if (!file.isFile()) {
            System.err.println("Error: not a regular file " + filename);
            System.exit(EXIT_FILE);
            return;
        }
        if (!file.canRead()) {
            System.err.println("Error: permission denied " + filename);
            System.exit(EXIT_FILE);
            return;
        }

        PointStore ps = null;

        try {
            // Parent opens store once to validate input
            // Changed using shared method from TriangleUtil after optimizations from TEC feedback
            ps = TriangleUtil.openPointStore(filename);
            final int n = ps.numPoints();

            // If fewer than 3 points, answer is 0
            if (n < 3) {
                System.out.println(0);
                System.exit(EXIT_OK);
                return;
            }

            // i values are [0, n-2)
            final int end = n - 2;

            // Don't start more processes than needed
            if (numProcesses > end) {
                numProcesses = end;
                if (numProcesses <= 0) {
                    numProcesses = 1;
                }
            }

            // Split i-range among processes
            final int chunkSize = (end + numProcesses - 1) / numProcesses;

            // Java command
            final String classpath = System.getProperty("java.class.path");
            final String javaHome = System.getProperty("java.home");
            final String javaCommand = javaHome + "/bin/java";

            // Store all child processes so they can run concurrently
            final Process[] processes = new Process[numProcesses];

            // Store outputs and errors from each worker
            final String[] outputs = new String[numProcesses];
            final String[] errors = new String[numProcesses];

            long total = 0;

            // Optimizations from TEC feedback mentioned basically changing the for loop
            // into 2 phases instead of the old one which would wait for a process to start
            // and finish until the next one starts. Now they execute in parallel
            for (int p = 0; p < numProcesses; p++) {
                final int startI = p * chunkSize;
                final int endI = Math.min(startI + chunkSize, end);

                if (startI >= endI) {
                    continue;
                }

                final ProcessBuilder pb = new ProcessBuilder(
                        javaCommand,
                        "-classpath", classpath,
                        "com.tryright.ProcessTriangles",
                        "--worker",
                        filename,
                        Integer.toString(startI),
                        Integer.toString(endI)
                );

                // Don't merge stderr into stdout
                final Process child = pb.start();
            }

            // This is the 2nd phases that collects results from ewach worker
            // Since all processes started in phase 1 everything runs at the same time
            for (int p = 0; p < numProcesses; p++) {
                final Process child = processes[p];
                if (child == null) {
                    continue;
                }

                // Read standard output the partial triangle count
                try (BufferedReader out = new BufferedReader(
                        new InputStreamReader(child.getInputStream()))) {
                    outputs[p] = out.readLine();
                }

                // Read standard error in case the worker failed
                try (BufferedReader err = new BufferedReader(
                        new InputStreamReader(child.getErrorStream()))) {
                    errors[p] = err.readLine();
                }

                // Wait for the process to finish execution
                final int code = child.waitFor();

                // Handle worker failure
                if (code != 0) {
                    if (errors[p] != null && !errors[p].isEmpty()) {
                        System.err.println("Error: worker failed: " + errors[p]);
                    } else {
                        System.err.println("Error: worker exited with code " + code);
                    }
                    System.exit(EXIT_WORKER);
                }

                // Makes sure the worker produced output
                if (outputs[p] == null) {
                    System.err.println("Error: worker returned no output.");
                    System.exit(EXIT_WORKER);
                }

                // Parse and accumulate the partial result
                try {
                    long partial = Long.parseLong(outputs[p].trim());
                    total += partial;
                } catch (Exception e) {
                    System.err.println("Error: worker returned invalid output.");
                    System.exit(EXIT_WORKER);
                }
            }

            // Sucess output
            System.out.println(total);
            System.exit(EXIT_OK);
        } catch (TriangleUtil.InputFormatException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_INPUT);
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Error: file not found: " + filename);
            System.exit(EXIT_FILE);
        } catch (java.io.IOException e) {
            System.err.println("Error: I/O failure: " + e.getMessage());
            System.exit(EXIT_FILE);
        } catch (InterruptedException e) {
            System.err.println("Error: interrupted while waiting for worker processes");
            System.exit(EXIT_INTERNAL);
        } catch (Exception e) {
            System.err.println("Error: unexpected failure: " + e.getMessage());
            System.exit(EXIT_INTERNAL);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Worker process logic: opens file via PointStore, counts triangles for range, prints one number.
     */
    private static void runWorker(final String filename, final String startArg, final String endArg) {

        int startI;
        int endI;

        try {
            startI = Integer.parseInt(startArg);
            endI = Integer.parseInt(endArg);
        } catch (Exception e) {
            System.err.println("Error: invalid worker range arguments.");
            System.exit(EXIT_USAGE);
            return;
        }

        PointStore ps = null;

        try {
            // Changed using shared method from TriangleUtil after optimizations from TEC feedback
            ps = TriangleUtil.openPointStore(filename);

            final long partial = TriangleUtil.countRange(ps, startI, endI);

            // Worker prints exactly one number on stdout.
            System.out.println(partial);
            System.exit(EXIT_OK);
        } catch (TriangleUtil.InputFormatException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_INPUT);
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Error: file not found: " + filename);
            System.exit(EXIT_FILE);
        } catch (java.io.IOException e) {
            System.err.println("Error: I/O failure: " + e.getMessage());
            System.exit(EXIT_FILE);
        } catch (Exception e) {
            System.err.println("Error: unexpected failure: " + e.getMessage());
            System.exit(EXIT_INTERNAL);
        } finally {
            if (ps != null) {
                try { ps.close(); } catch (Exception ignored) {}
            }
        }
    }
}

