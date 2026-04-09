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
            ps = openPointStore(filename);
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

            long total = 0;

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

                // Read worker's one line numeric result
                final String outLine;
                try (BufferedReader out = new BufferedReader(new InputStreamReader(child.getInputStream()))) {
                    outLine = out.readLine();
                }

                // Read stderr (useful message if worker fails)
                final String errLine;
                try (BufferedReader err = new BufferedReader(new InputStreamReader(child.getErrorStream()))) {
                    errLine = err.readLine();
                }

                final int code = child.waitFor();
                if (code != 0) {
                    if (errLine != null && !errLine.isEmpty()) {
                        System.err.println("Error: worker failed: " + errLine);
                    } else {
                        System.err.println("Error: worker exited with code " + code);
                    }
                    System.exit(EXIT_WORKER);
                    return;
                }

                if (outLine == null) {
                    System.err.println("Error: worker returned no output.");
                    System.exit(EXIT_WORKER);
                    return;
                }

                long partial;
                try {
                    partial = Long.parseLong(outLine.trim());
                } catch (Exception e) {
                    System.err.println("Error: worker returned invalid output.");
                    System.exit(EXIT_WORKER);
                    return;
                }
                total += partial;
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
            ps = openPointStore(filename);

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

    /**
     * Chooses BinPointStore for ".dat" files
     */
    private static PointStore openPointStore(final String filename)
        throws Exception {
        if (filename != null && filename.endsWith(".dat")) {
            return new BinPointStore(filename);
        }
        return new TextPointStore(filename);
    }

}

