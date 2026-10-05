/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.stone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * The STONE rockfall trajectories simulation for point shaped boulders.
 *
 * <p>Derived from stone.c and points.c of the GRASS r.stone module.</p>
 *
 */
class StoneSimulation {
    /**
     * The value of the sources grid marking cells where boulders stop.
     */
    static final int STOP_CELL = -1;

    /** The gravity acceleration of the original model [m/s2]. */
    private static final double G = 9.805;
    private static final Vector3D GRAVITY = new Vector3D(0, 0, -G);

    /*
     * Fixed parameters, might become user input at some point.
     */
    /** If two successive impact points are closer than this the boulder starts rolling [m]. */
    private static final double FLY_ROLL_DISTANCE = 3.0;
    /** If the boulder velocity falls below this at an impact, the boulder starts rolling [m/s]. */
    private static final double FLY_ROLL_VELOCITY = 5.0;
    /** The maximum number of points of a trajectory between two recordings. */
    private static final int MAX_PATH_POINTS = 100000;
    /** The distance between the points of the flights of the collected trajectories [m]. */
    private static final double TRAJECTORY_FLIGHT_STEP = 1.0;
    /** The longest flight time considered [s]. */
    private static final double MAX_FLIGHT_TIME = 100.;
    /** The global seed of the random numbers. */
    private static final long RANDOM_SEED = 1;

    /**
     * How a flight ended.
     */
    private enum FlightEnd {
        /** The path got too long or the boulder got lost. */
        ABORTED,
        /** The boulder left the current triangle. */
        LEFT_TRIANGLE,
        /** The boulder hit the current triangle. */
        IMPACT,
        /** The boulder reached a stop cell. */
        STOP_CELL,
        /** The boulder can't start flying from where it is, a bounce is needed first. */
        FORCED_BOUNCE
    }

    /**
     * How a roll ended.
     */
    private enum RollEnd {
        /** The path got too long. */
        ABORTED,
        /** The boulder left the current triangle. */
        LEFT_TRIANGLE,
        /** The velocity went under the stop velocity. */
        STOPPED,
        /** The boulder reached a stop cell. */
        STOP_CELL
    }

    /**
     * A point of a trajectory.
     */
    private static class PathPoint {
        Vector3D position;
        Vector3D velocity;
        /** The height over the terrain. */
        double height;
        /** True if the point is too close to the previous one to be recorded. */
        boolean filtered;
        /** True for a point of a flight, false for a point of a roll. */
        boolean flying;
    }

    /**
     * The triangle the boulder is moving on, with its frame.
     */
    private static class Triangle {
        /**
         * True for the lower-left triangle of a square (origin in the SW vertex), false for the
         * upper-right one (origin in the NE vertex).
         */
        boolean lowerLeft;
        /** The origin vertex. */
        Vector3D p0;
        /** The vertex the y axis points towards. */
        Vector3D p2;
        /** The axes of the triangle frame. */
        Vector3D ex, ey, ez;

        /**
         * @param p0 the origin vertex.
         * @param p1 the vertex the x axis points to.
         * @param p2 the vertex the y axis points towards.
         */
        void set( boolean lowerLeft, Vector3D p0, Vector3D p1, Vector3D p2 ) {
            this.lowerLeft = lowerLeft;
            this.p0 = p0;
            this.p2 = p2;
            ex = p1.subtract(p0).normalize();
            Vector3D toP2 = p2.subtract(p0);
            ey = toP2.subtract(toP2.dotProduct(ex), ex).normalize();
            ez = ex.crossProduct(ey);
        }

        /** A vector into the triangle frame. */
        Vector3D toPlane( Vector3D v ) {
            return new Vector3D(v.dotProduct(ex), v.dotProduct(ey), v.dotProduct(ez));
        }

        /** A point into the triangle frame. */
        Vector3D pointToPlane( Vector3D p ) {
            return toPlane(p.subtract(p0));
        }

        /** A vector from the triangle frame. */
        Vector3D fromPlane( Vector3D v ) {
            return new Vector3D(v.getX(), ex, v.getY(), ey, v.getZ(), ez);
        }

        /** A point from the triangle frame. */
        Vector3D pointFromPlane( Vector3D p ) {
            return fromPlane(p).add(p0);
        }

        /** @return true if the position is outside of the triangle (in plan view). */
        boolean isOutside( Vector3D position ) {
            double x = position.getX();
            double y = position.getY();
            if (lowerLeft) {
                double maxY = p2.getY() - (x - p0.getX());
                return x < p0.getX() || y < p0.getY() || y > maxY;
            } else {
                double minY = p2.getY() + (p0.getX() - x);
                return x > p0.getX() || y > p0.getY() || y < minY;
            }
        }
    }

    /** The spatial step of the trajectories [m]. */
    private final double step;
    private final double startVelocity;
    private final double stopVelocity;
    private final int angleDistribution;
    private final int coefficientsDistribution;
    // half widths of the random ranges, the doubled ones are used as standard deviations
    private final double angleRange;
    private final double normalRestitutionRange;
    private final double tangentialRestitutionRange;
    private final double frictionRange;

    private final int paddedRows;
    private final int paddedCols;
    private final int cellsCount;
    private final double cellSize;
    private final double halfCell;
    /**
     * The index offsets of the neighbour cells, clockwise from north. The order matters, it is
     * the one of the directions in {@link #randomStartAngle(int)}.
     */
    private final int[] neighbours = new int[8];

    private double[] elevation;
    private int[] sources;
    private double[] friction;
    private double[] normalRestitution;
    private double[] tangentialRestitution;

    private long[] counter;
    private double[] maxVelocity;
    private double[] maxHeight;

    private final Triangle triangle = new Triangle();
    private final PathPoint[] path = new PathPoint[MAX_PATH_POINTS];
    private int pathSize;
    /** The last cell counted for the current boulder, to count each crossed cell once per pass. */
    private long lastCountedCell;
    /** The last recorded point of the current boulder, null at the start. */
    private Vector3D lastRecordedPosition;
    private double lastRecordedSpeed;
    private double lastRecordedHeight;

    /** The position of the current boulder. */
    private Vector3D position;
    /** The velocity of the current boulder. */
    private Vector3D velocity;

    /** The maximum number of trajectories to collect, about. */
    private long maxTrajectories = 0;
    /** The probability of a boulder to have its trajectory collected. */
    private double trajectoriesFraction = 0;
    private final List<StoneTrajectory> trajectories = new ArrayList<>();
    /** The trajectory of the current boulder, if collected. */
    private StoneTrajectory currentTrajectory;

    /** The random generator of the current source. */
    private SplittableRandom random;
    /** The second value of the last Gaussian pair, if not used yet. */
    private double spareGaussian;
    private boolean hasSpareGaussian;

    /**
     * @param rows the rows of the grid (without the novalue border).
     * @param cols the cols of the grid (without the novalue border).
     * @param cellSize the cell size.
     * @param startVelocity the start velocity [m/s].
     * @param stopVelocity the velocity under which a boulder stops [m/s].
     * @param angleRangePercent the variability of the start angle, as percentage of 45 degrees.
     * @param normalRestitutionRangePercent the variability of the normal restitution, in percentage points.
     * @param tangentialRestitutionRangePercent the variability of the tangential restitution, in percentage points.
     * @param frictionRangePercent the variability of the friction, in hundredths.
     * @param angleDistribution the distribution of the start angle (0 = Gaussian, 1 = Cauchy, 2 = Uniform).
     * @param coefficientsDistribution the distribution of restitutions and friction (0 = Gaussian, else uniform).
     * @param step the spatial step of the trajectories [m]: the tabulation step of flights and rolls
     *          and the minimum distance between two recorded points. It should not be larger than the
     *          cell, else boulders can jump over triangles and cells.
     */
    StoneSimulation( int rows, int cols, double cellSize, double startVelocity, double stopVelocity, int angleRangePercent,
            int normalRestitutionRangePercent, int tangentialRestitutionRangePercent, int frictionRangePercent,
            int angleDistribution, int coefficientsDistribution, double step ) {
        this.step = step;
        this.startVelocity = startVelocity;
        this.stopVelocity = stopVelocity;
        this.angleDistribution = angleDistribution;
        this.coefficientsDistribution = coefficientsDistribution;

        angleRange = angleRangePercent * 0.01 * Math.PI / 4;
        normalRestitutionRange = normalRestitutionRangePercent * 0.01;
        tangentialRestitutionRange = tangentialRestitutionRangePercent * 0.01;
        frictionRange = frictionRangePercent * 0.01;

        paddedCols = cols + 2;
        paddedRows = rows + 2;
        cellsCount = paddedRows * paddedCols;
        this.cellSize = cellSize;
        halfCell = cellSize * 0.5;

        neighbours[0] = -paddedCols; // N
        neighbours[1] = -paddedCols + 1; // NE
        neighbours[2] = 1; // E
        neighbours[3] = paddedCols + 1; // SE
        neighbours[4] = paddedCols; // S
        neighbours[5] = paddedCols - 1; // SW
        neighbours[6] = -1; // W
        neighbours[7] = -paddedCols - 1; // NW
    }

    /**
     * Set the padded input grids.
     *
     * @param elevation the elevation, NaN for novalues.
     * @param sources the number of boulders starting from each cell, {@link #STOP_CELL} for stop cells.
     * @param friction the friction coefficient, NaN for novalues (handled as the maximum friction).
     * @param normalRestitution the normal restitution in percentage, 0 for novalues (stops the boulders).
     * @param tangentialRestitution the tangential restitution in percentage.
     */
    void setInputs( double[] elevation, int[] sources, double[] friction, double[] normalRestitution,
            double[] tangentialRestitution ) {
        this.elevation = elevation;
        this.sources = sources;
        this.friction = friction;
        this.normalRestitution = normalRestitution;
        this.tangentialRestitution = tangentialRestitution;
    }

    /**
     * @return the number of trajectories passing through each cell, -1 where none.
     */
    long[] getCounter() {
        return counter;
    }

    /**
     * @return the maximum velocity in each cell [m/s], NaN where none.
     */
    double[] getMaxVelocity() {
        return maxVelocity;
    }

    /**
     * @return the maximum height of the trajectories over the terrain in each cell [m], NaN where none.
     */
    double[] getMaxHeight() {
        return maxHeight;
    }

    /**
     * Collect the trajectories of a sample of the boulders.
     *
     * <p>The sample is chosen by a hash of the source position and the boulder number, so it is
     * reproducible, spread over all sources and independent of the random numbers of the
     * simulation, which isn't changed by collecting the trajectories.</p>
     *
     * @param maxTrajectories the number of trajectories to collect, about (0 for none).
     */
    void setMaxTrajectories( long maxTrajectories ) {
        this.maxTrajectories = maxTrajectories;
    }

    /**
     * @return the collected trajectories, in local coordinates.
     */
    List<StoneTrajectory> getTrajectories() {
        return trajectories;
    }

    /**
     * Run the simulation.
     */
    void run( IHMProgressMonitor pm ) {
        counter = new long[cellsCount];
        maxVelocity = new double[cellsCount];
        maxHeight = new double[cellsCount];
        Arrays.fill(counter, -1);
        Arrays.fill(maxVelocity, Double.NaN);
        Arrays.fill(maxHeight, Double.NaN);
        for( int i = 0; i < path.length; i++ ) {
            path[i] = new PathPoint();
        }
        trajectories.clear();
        trajectoriesFraction = 0;
        if (maxTrajectories > 0) {
            long boulders = 0;
            for( int cell = 0; cell < cellsCount; ++cell ) {
                if (sources[cell] > 0 && isValidCell(cell)) {
                    boulders += sources[cell];
                }
            }
            trajectoriesFraction = boulders == 0 ? 0 : Math.min(1.0, maxTrajectories / (double) boulders);
        }

        pm.beginTask("Tracking the rockfall trajectories...", paddedRows);
        for( int cell = 0; cell < cellsCount; ++cell ) {
            if (cell % paddedCols == 0) {
                if (pm.isCanceled()) {
                    return;
                }
                pm.worked(1);
            }
            int boulders = sources[cell];
            if (boulders > 0 && isValidCell(cell)) {
                trackSource(cell, boulders);
            }
        }
        pm.done();
    }

    /*
     * Grid access.
     */
    private boolean isInGrid( long cell ) {
        return cell >= 0 && cell < cellsCount;
    }

    private boolean isValidCell( long cell ) {
        return isInGrid(cell) && !Double.isNaN(elevation[(int) cell]);
    }

    private int sourceAt( long cell ) {
        return isInGrid(cell) ? sources[(int) cell] : 0;
    }

    private double normalRestitutionAt( long cell ) {
        return isInGrid(cell) ? normalRestitution[(int) cell] : 0;
    }

    private double tangentialRestitutionAt( long cell ) {
        return isInGrid(cell) ? tangentialRestitution[(int) cell] : 0;
    }

    private double frictionAt( long cell ) {
        double value = isInGrid(cell) ? friction[(int) cell] : Double.NaN;
        return Double.isNaN(value) ? 1.0 : value;
    }

    /**
     * @return the index of the cell containing the local coordinates.
     */
    private long cellAt( double x, double y ) {
        long col = 1 + (long) StrictMath.floor(x / cellSize);
        long row = paddedRows - 2 - (long) StrictMath.floor(y / cellSize);
        if (col < 0 || col >= paddedCols || row < 0 || row >= paddedRows) {
            return -1;
        }
        return row * paddedCols + col;
    }

    private Vector3D cellCenter( int cell ) {
        double x = halfCell + ((cell % paddedCols) - 1) * cellSize;
        double y = halfCell + (paddedRows - 2 - (cell / paddedCols)) * cellSize;
        return new Vector3D(x, y, elevation[cell]);
    }

    /**
     * Round the coordinates to 0.1 mm, as the original code does for the path points.
     */
    private static Vector3D roundTo10thMm( Vector3D p ) {
        return new Vector3D(StrictMath.floor(p.getX() * 10000. + .5) * 0.0001,
                StrictMath.floor(p.getY() * 10000. + .5) * 0.0001, StrictMath.floor(p.getZ() * 10000. + .5) * 0.0001);
    }

    /**
     * Track all the boulders of a source cell.
     */
    private void trackSource( int sourceCell, int boulders ) {
        initRandom(sourceCell);
        Vector3D start = cellCenter(sourceCell);

        // the direction of steepest descent towards the neighbours
        double maxSlope = -100.;
        int direction = 0;
        for( int i = 0; i < 8; ++i ) {
            int neighbour = sourceCell + neighbours[i];
            if (!isValidCell(neighbour)) {
                continue;
            }
            double distance = i % 2 != 0 ? cellSize * StrictMath.sqrt(2) : cellSize;
            double slope = (elevation[sourceCell] - elevation[neighbour]) / distance;
            if (slope > maxSlope) {
                maxSlope = slope;
                direction = i;
            }
        }

        for( int boulder = 1; boulder <= boulders; ++boulder ) {
            if (!buildTriangle(start)) {
                break;
            }
            // horizontal start
            double angle = randomStartAngle(direction);
            position = start;
            velocity = new Vector3D(startVelocity * StrictMath.cos(angle), startVelocity * StrictMath.sin(angle), 0);
            currentTrajectory = isTrajectorySampled(sourceCell, boulder)
                    ? new StoneTrajectory(sourceCell / paddedCols - 1, sourceCell % paddedCols - 1, boulder)
                    : null;
            trackBoulder();
            if (currentTrajectory != null && currentTrajectory.size() > 1) {
                trajectories.add(currentTrajectory);
            }
            currentTrajectory = null;
        }
    }

    /**
     * @return true if the trajectory of the boulder of the source has to be collected.
     */
    private boolean isTrajectorySampled( int sourceCell, int boulder ) {
        if (trajectoriesFraction <= 0) {
            return false;
        }
        if (trajectoriesFraction >= 1) {
            return true;
        }
        long row = sourceCell / paddedCols - 1;
        long col = sourceCell % paddedCols - 1;
        long hash = mix64(mix64(mix64(RANDOM_SEED + 1) ^ (row << 32 | col)) + boulder);
        double uniform = (hash >>> 11) * 0x1.0p-53;
        return uniform < trajectoriesFraction;
    }

    /**
     * Track the current boulder until it stops.
     */
    private void trackBoulder() {
        Vector3D lastImpact = position;
        lastCountedCell = -1;
        lastRecordedPosition = null;
        pathSize = 0;
        boolean flying = true; // we start with a gunshot

        for( ;; ) {
            if (flying) {
                switch( fly() ) {
                case ABORTED:
                    return;
                case LEFT_TRIANGLE:
                    if (!buildTriangle(position)) {
                        recordPath();
                        return;
                    }
                    continue;
                case IMPACT:
                    boolean shortBounce = position.distance(lastImpact) < FLY_ROLL_DISTANCE
                            && velocity.getNorm() < FLY_ROLL_VELOCITY;
                    recordPath();
                    pathSize = 0;
                    if (shortBounce) {
                        flying = false;
                        continue;
                    }
                    if (!bounce()) {
                        return;
                    }
                    lastImpact = position;
                    continue;
                case STOP_CELL:
                    recordPath();
                    return;
                case FORCED_BOUNCE:
                    if (!bounce()) {
                        return;
                    }
                    pathSize = 0;
                    continue;
                }
            } else {
                switch( roll() ) {
                case ABORTED:
                    return;
                case LEFT_TRIANGLE:
                    if (!buildTriangle(position)) {
                        recordPath();
                        return;
                    }
                    if (triangle.pointToPlane(position).getZ() > 0.01) {
                        // the terrain drops away under the boulder, start flying
                        recordPath();
                        pathSize = 0;
                        flying = true;
                    }
                    continue;
                case STOPPED:
                case STOP_CELL:
                    recordPath();
                    return;
                }
            }
        }
    }

    /**
     * Build the triangle under the given position.
     *
     * @return false if the triangle can't be built because it touches novalues.
     */
    private boolean buildTriangle( Vector3D p ) {
        // the center of the cell at the SW corner of the square containing the position
        double x0 = StrictMath.floor((p.getX() - halfCell) / cellSize) * cellSize + halfCell;
        double y0 = StrictMath.floor((p.getY() - halfCell) / cellSize) * cellSize + halfCell;
        long cell = cellAt(x0, y0);
        if (!isInGrid(cell)) {
            return false;
        }

        double dx = p.getX() - x0;
        double dy = p.getY() - y0;
        if (dy <= cellSize - dx) {
            // lower-left triangle, origin SW
            long east = cell + neighbours[2];
            long north = cell + neighbours[0];
            if (!isValidCell(cell) || !isValidCell(east) || !isValidCell(north)) {
                return false;
            }
            triangle.set(true, //
                    new Vector3D(x0, y0, elevation[(int) cell]), //
                    new Vector3D(x0 + cellSize, y0, elevation[(int) east]), //
                    new Vector3D(x0, y0 + cellSize, elevation[(int) north]));
        } else {
            // upper-right triangle, origin NE
            long northEast = cell + neighbours[1];
            long north = northEast + neighbours[6];
            long east = northEast + neighbours[4];
            if (!isValidCell(northEast) || !isValidCell(north) || !isValidCell(east)) {
                return false;
            }
            triangle.set(false, //
                    new Vector3D(x0 + cellSize, y0 + cellSize, elevation[(int) northEast]), //
                    new Vector3D(x0, y0 + cellSize, elevation[(int) north]), //
                    new Vector3D(x0 + cellSize, y0, elevation[(int) east]));
        }
        return true;
    }

    /**
     * Record the current path in the output grids.
     */
    private void recordPath() {
        filterPath();
        // a path is a single flight or roll, so the flight points of a path are on one parabola
        PathPoint previousRecorded = null;
        for( int i = 0; i < pathSize; ++i ) {
            PathPoint point = path[i];
            if (point.filtered) {
                continue;
            }
            Vector3D p = point.position;
            double speed = point.velocity.getNorm();
            if (currentTrajectory != null) {
                if (previousRecorded != null && previousRecorded.flying && point.flying) {
                    addFlightPoints(previousRecorded, point);
                }
                currentTrajectory.add(p.getX(), p.getY(), p.getZ(), speed, point.height);
                previousRecorded = point;
            }
            if (lastRecordedPosition == null) {
                markCell(cellAt(p.getX(), p.getY()), speed, point.height);
            } else {
                markSegment(lastRecordedPosition, lastRecordedSpeed, lastRecordedHeight, p, speed, point.height);
            }
            lastRecordedPosition = p;
            lastRecordedSpeed = speed;
            lastRecordedHeight = point.height;
        }
    }

    /**
     * Add to the current trajectory the points of the parabola between two recorded points of a
     * flight, every {@link #TRAJECTORY_FLIGHT_STEP}, the two points excluded. This only details
     * the drawn trajectory, the simulation doesn't change.
     *
     * <p>In flight only the gravity acts, so from a point with a velocity the boulder follows
     * p(t) = p0 + v0 t + g t^2 / 2 exactly, with constant horizontal velocity.</p>
     */
    private void addFlightPoints( PathPoint from, PathPoint to ) {
        Vector3D p0 = from.position;
        Vector3D v0 = from.velocity;
        double horizontalSpeed = StrictMath.sqrt(v0.getX() * v0.getX() + v0.getY() * v0.getY());
        double dx = to.position.getX() - p0.getX();
        double dy = to.position.getY() - p0.getY();
        double horizontalDistance = StrictMath.sqrt(dx * dx + dy * dy);
        if (horizontalSpeed <= 0 || horizontalDistance <= 0) {
            return;
        }
        double time = horizontalDistance / horizontalSpeed;
        int steps = (int) StrictMath.ceil(p0.distance(to.position) / TRAJECTORY_FLIGHT_STEP);
        for( int k = 1; k < steps; k++ ) {
            double t = time * k / steps;
            Vector3D p = new Vector3D(1, p0, t, v0, t * t * 0.5, GRAVITY);
            Vector3D v = new Vector3D(1, v0, t, GRAVITY);
            double terrain = terrainElevationAt(p.getX(), p.getY());
            double height = Double.isNaN(terrain) ? 0 : Math.max(0, p.getZ() - terrain);
            currentTrajectory.add(p.getX(), p.getY(), p.getZ(), v.getNorm(), height);
        }
    }

    /**
     * @return the elevation of the triangulated terrain at the local coordinates, NaN outside of
     *          the valid data.
     */
    private double terrainElevationAt( double x, double y ) {
        double x0 = StrictMath.floor((x - halfCell) / cellSize) * cellSize + halfCell;
        double y0 = StrictMath.floor((y - halfCell) / cellSize) * cellSize + halfCell;
        long sw = cellAt(x0, y0);
        if (!isInGrid(sw)) {
            return Double.NaN;
        }
        long se = sw + neighbours[2];
        long nw = sw + neighbours[0];
        long ne = sw + neighbours[1];
        double u = (x - x0) / cellSize;
        double v = (y - y0) / cellSize;
        if (v <= 1 - u) {
            // lower-left triangle: sw, se, nw
            if (!isValidCell(sw) || !isValidCell(se) || !isValidCell(nw)) {
                return Double.NaN;
            }
            double zsw = elevation[(int) sw];
            return zsw + u * (elevation[(int) se] - zsw) + v * (elevation[(int) nw] - zsw);
        } else {
            // upper-right triangle: ne, nw, se
            if (!isValidCell(ne) || !isValidCell(se) || !isValidCell(nw)) {
                return Double.NaN;
            }
            double zne = elevation[(int) ne];
            return zne + (1 - u) * (elevation[(int) nw] - zne) + (1 - v) * (elevation[(int) se] - zne);
        }
    }

    /**
     * Mark the cells crossed by the segment between two recorded points, in order. The cell of
     * the start point was marked with the previous point, the cell of the end point gets its
     * values, the cells in between the values interpolated in the middle of the part of the
     * segment inside them.
     */
    private void markSegment( Vector3D from, double fromSpeed, double fromHeight, Vector3D to, double toSpeed,
            double toHeight ) {
        // grid traversal of Amanatides and Woo, in cell units with the origin in the south-west corner
        double x0 = from.getX() / cellSize;
        double y0 = from.getY() / cellSize;
        double x1 = to.getX() / cellSize;
        double y1 = to.getY() / cellSize;
        long i = (long) StrictMath.floor(x0);
        long j = (long) StrictMath.floor(y0);
        long iEnd = (long) StrictMath.floor(x1);
        long jEnd = (long) StrictMath.floor(y1);
        double dx = x1 - x0;
        double dy = y1 - y0;
        int stepI = dx > 0 ? 1 : -1;
        int stepJ = dy > 0 ? 1 : -1;
        double tDeltaX = dx != 0 ? Math.abs(1 / dx) : Double.POSITIVE_INFINITY;
        double tDeltaY = dy != 0 ? Math.abs(1 / dy) : Double.POSITIVE_INFINITY;
        double tMaxX = dx != 0 ? ((dx > 0 ? i + 1 - x0 : x0 - i) * tDeltaX) : Double.POSITIVE_INFINITY;
        double tMaxY = dy != 0 ? ((dy > 0 ? j + 1 - y0 : y0 - j) * tDeltaY) : Double.POSITIVE_INFINITY;
        // the number of cells crossed is known, this also protects from rounding at the end
        long crossings = Math.abs(iEnd - i) + Math.abs(jEnd - j);
        for( long k = 0; k < crossings; k++ ) {
            // the segment enters the next cell at tEnter (0 to 1 along the segment)
            double tEnter = Math.min(tMaxX, tMaxY);
            if (tMaxX < tMaxY) {
                i += stepI;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxX) {
                j += stepJ;
                tMaxY += tDeltaY;
            } else {
                // through a corner, the side cells are only touched in a point
                i += stepI;
                j += stepJ;
                tMaxX += tDeltaX;
                tMaxY += tDeltaY;
                k++;
            }
            if (k >= crossings - 1) {
                markCell(cellIndex(i, j), toSpeed, toHeight);
            } else {
                double tMiddle = (tEnter + Math.min(1, Math.min(tMaxX, tMaxY))) / 2;
                markCell(cellIndex(i, j), fromSpeed + (toSpeed - fromSpeed) * tMiddle,
                        fromHeight + (toHeight - fromHeight) * tMiddle);
            }
        }
        if (crossings == 0) {
            markCell(cellIndex(i, j), toSpeed, toHeight);
        }
    }

    /**
     * @return the index of the cell of col i and row j counted from the south-west corner of the
     *          unpadded grid, -1 if outside of the padded grid.
     */
    private long cellIndex( long i, long j ) {
        long col = 1 + i;
        long row = paddedRows - 2 - j;
        if (col < 0 || col >= paddedCols || row < 0 || row >= paddedRows) {
            return -1;
        }
        return row * paddedCols + col;
    }

    /**
     * Update the outputs for a cell reached by the current boulder.
     */
    private void markCell( long cell, double speed, double height ) {
        if (!isInGrid(cell)) {
            return;
        }
        int index = (int) cell;
        boolean valid = isValidCell(cell);
        if (valid) {
            if (Double.isNaN(maxVelocity[index]) || speed > maxVelocity[index]) {
                maxVelocity[index] = speed;
            }
            if (Double.isNaN(maxHeight[index]) || height > maxHeight[index]) {
                maxHeight[index] = height;
            }
        }
        // also cells with novalues (the border included) end a pass over a cell
        if (cell != lastCountedCell) {
            if (valid) {
                if (counter[index] == -1) {
                    counter[index] = 1;
                } else {
                    counter[index]++;
                }
            }
            lastCountedCell = cell;
        }
    }

    /**
     * Mark the path points closer than {@link #step} to the previous one, keeping the last.
     */
    private void filterPath() {
        int previous = 0;
        for( int i = 1; i < pathSize; ++i ) {
            if (path[previous].position.distance(path[i].position) < step) {
                path[i].filtered = true;
            } else {
                previous = i;
            }
        }
        if (pathSize > 0) {
            path[pathSize - 1].filtered = false;
        }
    }

    /**
     * Store a point at the end of the path.
     *
     * @return the point or null if the path is full.
     */
    private PathPoint addPathPoint( Vector3D position, Vector3D velocity, double height, boolean flying ) {
        if (pathSize >= MAX_PATH_POINTS) {
            return null;
        }
        PathPoint point = path[pathSize++];
        point.position = roundTo10thMm(position);
        point.velocity = velocity;
        point.height = height;
        point.filtered = false;
        point.flying = flying;
        return point;
    }

    /**
     * Init the random generator of a source, seeded from the global seed and the row and col of
     * the source in the (unpadded) grid.
     */
    private void initRandom( int sourceCell ) {
        long row = sourceCell / paddedCols - 1;
        long col = sourceCell % paddedCols - 1;
        random = new SplittableRandom(mix64(mix64(RANDOM_SEED) ^ (row << 32 | col)));
        hasSpareGaussian = false;
    }

    /**
     * The 64 bit finalizer of MurmurHash3, to turn close seeds into unrelated ones.
     */
    private static long mix64( long z ) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    /**
     * A Gaussian random number with the polar method, using {@link StrictMath} to be the same on
     * every platform (the nextGaussian of the JDK generators uses Math).
     */
    private double gaussian( double mean, double standardDeviation ) {
        if (hasSpareGaussian) {
            hasSpareGaussian = false;
            return mean + standardDeviation * spareGaussian;
        }
        double v1, v2, s;
        do {
            v1 = 2 * random.nextDouble() - 1;
            v2 = 2 * random.nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while( s >= 1 || s == 0 );
        double factor = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        spareGaussian = v2 * factor;
        hasSpareGaussian = true;
        return mean + standardDeviation * v1 * factor;
    }

    /**
     * @param direction the index of the neighbour of steepest descent.
     * @return the randomized start angle, counterclockwise from east.
     */
    private double randomStartAngle( int direction ) {
        double angle = ((2 - direction + 8) % 8) * Math.PI / 4;
        switch( angleDistribution ) {
        case 0:
            return gaussian(angle, 2 * angleRange);
        case 1:
            return angle + angleRange * StrictMath.tan(Math.PI * (random.nextDouble() - 0.5));
        case 2:
            // uniform with standard deviation 2 * angleRange
            double width = 2 * angleRange * StrictMath.sqrt(12.0);
            return angle - width / 2 + width * random.nextDouble();
        default:
            return angle - angleRange + 2 * angleRange * random.nextDouble();
        }
    }

    /**
     * @return the coefficient randomized with the coefficients distribution, clamped to [0, 1].
     */
    private double randomCoefficient( double value, double range ) {
        double randomized;
        if (coefficientsDistribution == 0) {
            randomized = gaussian(value, 2 * range);
        } else {
            randomized = value - range + 2 * range * random.nextDouble();
        }
        return Math.max(0., Math.min(1., randomized));
    }

    /**
     * Bounce the current boulder on the current triangle.
     *
     * @return false if the boulder stopped.
     */
    private boolean bounce() {
        long cell = cellAt(position.getX(), position.getY());
        // a normal restitution of zero stops the boulder, as fallen into water
        if (normalRestitutionAt(cell) == 0) {
            return false;
        }
        double normal = randomCoefficient(normalRestitutionAt(cell) * 0.01, normalRestitutionRange);
        double tangential = randomCoefficient(tangentialRestitutionAt(cell) * 0.01, tangentialRestitutionRange);

        Vector3D v = triangle.toPlane(velocity);
        v = new Vector3D(v.getX() * tangential, v.getY() * tangential, -v.getZ() * normal);
        velocity = triangle.fromPlane(v);
        return v.getNorm() >= stopVelocity;
    }

    /**
     * Fly the current boulder along a parabola on the current triangle, storing the points in the
     * path. The boulder is left at the last point.
     */
    private FlightEnd fly() {
        Vector3D v0 = triangle.toPlane(velocity);
        Vector3D p0 = triangle.pointToPlane(position);
        Vector3D g = triangle.toPlane(GRAVITY);

        // cosine of the triangle slope, to turn heights over the triangle into vertical ones
        double cosSlope = triangle.ez.getZ();

        // the time of the impact on the triangle, with back steps if it is in the past
        boolean inside = true;
        double impactTime;
        for( ;; ) {
            double sq = v0.getZ() * v0.getZ() - 2 * g.getZ() * p0.getZ();
            if (sq < 0.) {
                // switching triangle the position can be under the new triangle
                p0 = new Vector3D(p0.getX(), p0.getY(), 0);
                sq = Math.abs(v0.getZ());
            } else {
                sq = StrictMath.sqrt(sq);
            }
            impactTime = (-v0.getZ() - sq) / g.getZ();
            if (!(impactTime <= 0.)) {
                break;
            }
            if (pathSize < 2) {
                return FlightEnd.FORCED_BOUNCE;
            }
            --pathSize;
            position = path[pathSize - 1].position;
            velocity = path[pathSize - 1].velocity;
            p0 = triangle.pointToPlane(position);
            v0 = triangle.toPlane(velocity);
            inside = false;
        }
        impactTime = Math.min(impactTime, MAX_FLIGHT_TIME);

        boolean impact = false;
        int outsideCount = 0;
        double t = 0.;
        PathPoint point;
        for( ;; ) {
            Vector3D pt = new Vector3D(1, p0, t, v0, t * t * 0.5, g);
            Vector3D vt = new Vector3D(1, v0, t, g);
            point = addPathPoint(triangle.pointFromPlane(pt), triangle.fromPlane(vt), pt.getZ() / cosSlope, true);
            if (point == null) {
                return FlightEnd.ABORTED;
            }

            if (sourceAt(cellAt(point.position.getX(), point.position.getY())) == STOP_CELL) {
                return FlightEnd.STOP_CELL;
            }

            boolean outside = triangle.isOutside(point.position);
            if (inside) {
                if (outside) {
                    impact = false;
                    break;
                } else if (impact) {
                    break;
                }
            } else {
                // after a back step, wait to be inside again
                if (!outside) {
                    inside = true;
                } else if (++outsideCount > 5) {
                    return FlightEnd.ABORTED;
                }
            }

            // a time step moving the boulder by about the spatial step, ending exactly at the impact
            t += step / point.velocity.getNorm();
            if (t >= impactTime) {
                t = impactTime;
                impact = true;
            }
        }

        position = point.position;
        velocity = point.velocity;
        return impact ? FlightEnd.IMPACT : FlightEnd.LEFT_TRIANGLE;
    }

    /**
     * Roll the current boulder on the current triangle, storing the points in the path. The
     * boulder is left at the last point.
     */
    private RollEnd roll() {
        // in the triangle frame the movement is in the xy plane
        Vector3D v0 = triangle.toPlane(velocity);
        double vx = v0.getX();
        double vy = v0.getY();
        Vector3D p0 = triangle.pointToPlane(position);
        double px = p0.getX();
        double py = p0.getY();
        Vector3D g = triangle.toPlane(GRAVITY);

        long frictionCell = Long.MIN_VALUE;
        double deceleration = 0;
        // the first step is at t = 0, storing the start point
        double t = 0.;
        Vector3D current = position;

        for( ;; ) {
            long cell = cellAt(current.getX(), current.getY());
            if (cell != frictionCell) {
                double f = randomCoefficient(frictionAt(cell), frictionRange);
                // sin(atan(f)) * G
                deceleration = f / StrictMath.sqrt(1 + f * f) * G;
                frictionCell = cell;
            }

            // the friction is opposite to the velocity, avoid it to act as a spring
            double speed = StrictMath.sqrt(vx * vx + vy * vy);
            double dx = speed > 0 ? vx / speed : 0;
            double dy = speed > 0 ? vy / speed : 0;
            if (speed < deceleration * t) {
                t = speed / deceleration;
            }

            double ax = g.getX() - dx * deceleration;
            double ay = g.getY() - dy * deceleration;
            px += vx * t + ax * t * t * 0.5;
            py += vy * t + ay * t * t * 0.5;
            vx += ax * t;
            vy += ay * t;

            PathPoint point = addPathPoint(triangle.pointFromPlane(new Vector3D(px, py, 0)),
                    triangle.fromPlane(new Vector3D(vx, vy, 0)), 0, false);
            if (point == null) {
                return RollEnd.ABORTED;
            }
            current = point.position;

            // a time step moving the boulder by about the spatial step
            speed = StrictMath.sqrt(vx * vx + vy * vy);
            t = step / speed;

            if (speed < stopVelocity) {
                return RollEnd.STOPPED;
            }
            // as in the original code, the stop cell is checked at the cell of the previous point
            if (sourceAt(cell) == STOP_CELL) {
                return RollEnd.STOP_CELL;
            }
            if (triangle.isOutside(current)) {
                break;
            }
        }

        position = current;
        velocity = path[pathSize - 1].velocity;
        return RollEnd.LEFT_TRIANGLE;
    }
}
