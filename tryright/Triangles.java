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
 * Command-line program that counts how many right triangles can be formed
 * from a list of 2D integer points read from an input file.
 */
public final class Triangles {

    private static final int EXIT_OK = 0;
    private static final int EXIT_USAGE = 1;
    private static final int EXIT_FILE = 2;
    private static final int EXIT_INPUT = 3;
    private static final int EXIT_WORKER = 4;
    private static final int EXIT_INTERNAL = 10;

    /**
     * Program entry point.
     *
     * @param args command-line arguments; must contain exactly one value: the input
     *             filename
     */
    public static void main(final String[] args) {
        // Validate command-line arguments
        if (args == null || args.length != 1) {
            printUsage("Wrong number of arguments.");
            System.exit(EXIT_USAGE);
            return;
        }

        final String filename = args[0];
        final File file = new File(filename);

        // Validate File
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
            // Open correct store based on filename
            // Changed using shared method from TriangleUtil after optimizations from TEC feedback
            ps = TriangleUtil.openPointStore(filename);
            // TEC mentioned that Triangles calls countRange(ps, 0, n-2) even when n < 3
            // So adding this guard will make it consistent with other classes.
            // This is my oldest code which makes sense that it lacked this.
            final int n = ps.numPoints();
            if (n < 3) {
                System.out.println(0);
                System.exit(EXIT_OK);
                return;
            }
            final long answer = TriangleUtil.countRange(ps, 0, n - 2);

            // Success output: one number only
            System.out.println(answer);
            System.exit(EXIT_OK);

        } catch (TriangleUtil.InputFormatException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_INPUT);
        } catch (FileNotFoundException e) {
            System.err.println("Error: file not found.");
            System.exit(EXIT_FILE);
        } catch (IOException e) {
            System.err.println("Error: I/O failure: " + e.getMessage());
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
     * Prints a usage message to stderr
     *
     * @param message additional error detail explaining what went wrong
     */
    private static void printUsage(final String message) {
        System.err.println("Error: " + message);
        System.err.println("Usage: java com.tryright.Triangles <inputfile>");
    }
}
