package com.tryright;

/************************************************
 *
 * Author: Kevin Gonzalez
 * Assignment: Program 3
 * Class: CSC 4180
 *
 ************************************************/

/**
 * Utility class containing the shared logic for counting right triangles, and avoid repetition as
 * instructed.
 *
 * <p>This version accesses points ONLY through the PointStore interface.</p>
 */
public final class TriangleUtil {

    private TriangleUtil(){}

    /**
     * Counts right triangles using only PointStore access in O(n^3).
     *
     * <p>
     *     For each point in ( the right angle corner), we cound direction vectors to every other point.
     *     Two vectors form a right triangle if one is perpendicular to the other.
     *
     * @param ps points store (must not be null)
     * @param startI starting index for i
     * @param endI ending index for i
     * @return number of right triangles in this range
     * </p>
     */
    public static long countRange(
            final PointStore ps,
            final int startI,
            final int endI) {

        if (ps == null) {
            return 0;
        }

        final int n = ps.numPoints();
        if (n < 3) {
            return 0;
        }

        // Clamp bounds so we never ask the store for invalid indices.
        final int safeStart = Math.max(0, startI);
        final int safeEnd = Math.min(endI, n - 2);

        final int[] x = new int[n];
        final int[] y = new int[n];
        for (int p = 0; p < n; p++) {
            x[p] = ps.getX(p);
            y[p] = ps.getY(p);
        }

        long count = 0;

        for (int i = safeStart; i < safeEnd; i++) {

            final int xi = x[i];
            final int yi = y[i];

            for (int j = i + 1; j < n - 1; j++){

                final int xj = x[j];
                final int yj = y[j];

                final long dxij = (long) xj - (long) xi;
                final long dyij = (long) yj - (long) yi;
                final long ij2 = dxij * dxij + dyij * dyij;

                for (int k = j + 1; k < n; k++) {

                    final int xk = x[k];
                    final int yk = y[k];

                    final long dxik = (long) xk - (long) xi;
                    final long dyik = (long) yk - (long) yi;
                    final long ik2 = dxik * dxik + dyik * dyik;

                    final long dxjk = (long) xk - (long) xj;
                    final long dyjk = (long) yk - (long) yj;
                    final long jk2 = dxjk * dxjk + dyjk * dyjk;

                    // Skip cases where two points are the same
                    if (ij2 == 0 || ik2 == 0 || jk2 == 0) {
                        continue;
                    }
                    // A triangle is right if the two smaller squared sides sum to the largest.
                    if (ij2 + ik2 == jk2 ||
                        ij2 + jk2 == ik2 ||
                        ik2 + jk2 == ij2) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Exception used for input format errors.
     *
     * <p>Kept here because other classes (PointStores) may use it for reporting bad inputs.</p>
     */
    public static final class InputFormatException extends Exception {
        public InputFormatException(final String message) {
            super(message);
        }
    }
}
