package org.hortonmachine.hmachine.modules.geomorphology.nabla;
//package org.hortonmachine.hmachine.modules.geomorphology.nabla;
//public class HMNablaOperation extends JGrassAreaOpImage {
//
//    //
//    private static final double DEFAULT_NO_VALUE = JGrassConstants.doubleNovalue;
//
//    // the mode of the operation, mode ==0 return only a mask, 1 if the nabla*nabla absolute value
//    // is less than a threshold value set the sample to 1 otherwise to 0.
//    private int mode = 0;
//    // the threshold value for the mode ==0.
//    private double thNabla = 0;
//
//    public HMNablaOperation( RenderedImage source1, BorderExtender extender, Map config,
//            ImageLayout layout, double dx, double dy, int mode, double threshold, boolean doTile,
//            PrintStream err, PrintStream out ) {
//        super(source1, layout, config, doTile, extender, 1, 1, 1, 1, dx, dy, out, err);
//        // set the value
//
//        this.mode = mode;
//        this.thNabla = threshold;
//
//    }
//
//    protected void computeRect( PlanarImage[] sources, WritableRaster gradientRaster,
//            Rectangle destRect ) {
//        PlanarImage source = sources[0];
//        Rectangle region = new Rectangle(destRect.x + 1, destRect.y + 1, destRect.width - 2,
//                destRect.height - 2);
//        if (mode == 0) {
//            nabla_mask(source.getData(), gradientRaster, region);
//        } else if (mode == 1) {
//            nabla(source.getData(), gradientRaster, region);
//
//        }
//
//    }
//
//    protected void computeRect( Raster[] sources, WritableRaster dest, Rectangle destRect ) {
//        Rectangle region = new Rectangle(destRect.x + 1, destRect.y + 1, destRect.width - 2,
//                destRect.height - 2);
//        if (mode == 0) {
//            nabla_mask(sources[0], dest, region);
//        } else if (mode == 1) {
//            nabla(sources[0], dest, region);
//        }
//
//    }
//
//    /**
//     * Computes the nabla algorithm.
//     * <p>
//     * This is the 0 mode which returns a "mask" so the value of the nablaRaster is equal to 1 of
//     * the nabla*nabla is <=threshold
//     * </p>
//     * 
//     * @param elevationIter holding the elevation data.
//     * @param nablaRaster the to which the Nabla values are written
//     * @param destRect ???
//     */
//    private void nabla_mask( Raster elevationIter, WritableRaster nablaRaster, Rectangle destRect ) {
//        // get rows and cols from the active region
//        // the origin of the Rectangle where there is possible calculate the
//        // slope.
//        int xOrigin = destRect.x;
//        int yOrigin = destRect.y;
//        int nrows = destRect.height;
//        int ncols = destRect.width;
//        int maxNRows = xOrigin + nrows;
//        int maxNCols = yOrigin + ncols;
//
//        int y;
//        double[] z = new double[9];
//        double derivate2;
//        int[][] v = ModelsConstants.DIR;
//
//        // grid contains the dimension of pixels according with flow directions
//        double[] grid = new double[9];
//        grid[0] = 0;
//        grid[1] = grid[5] = xRes;
//        grid[3] = grid[7] = yRes;
//        grid[2] = grid[4] = grid[6] = grid[8] = Math.sqrt(grid[1] * grid[1] + grid[3] * grid[3]);
//
//        
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(out);
//        pm.beginTask(WORKING_ON + "h.nabla", maxNCols - xOrigin);
//        for( int i = xOrigin; i < maxNCols; i++ ) {
//            for( int j = yOrigin; j < maxNRows; j++ ) {
//                z[0] = elevationIter.getSampleDouble(i, j, 0);
//                if (!isNovalue(z[0])) {
//                    y = 1;
//                    // if there is a no value around the current pixel then do nothing.
//                    for( int h = 1; h <= 8; h++ ) {
//                        z[h] = elevationIter.getSampleDouble(i + v[h][0], j + v[h][1], 0);
//                        if (isNovalue(z[h])) {
//                            y = 0;
//                            break;
//                        }
//                    }
//                    if (y == 0) {
//                        nablaRaster.setSample(i, j, 0, 1);
//                    } else {
//                        derivate2 = 0.5 * ((z[1] + z[5] - 2 * z[0]) / (grid[1] * grid[1]) + (z[3]
//                                + z[7] - 2 * z[0])
//                                / (grid[3] * grid[3]));
//                        derivate2 = derivate2 + 0.5
//                                * ((z[2] + z[4] + z[6] + z[8] - 4 * z[0]) / (grid[6] * grid[6]));
//
//                        if (Math.abs(derivate2) <= thNabla || derivate2 > thNabla) {
//                            nablaRaster.setSample(i, j, 0, 0);
//                        } else {
//                            nablaRaster.setSample(i, j, 0, 1);
//                        }
//                    }
//                } else {
//                    nablaRaster.setSample(i, j, 0, DEFAULT_NO_VALUE);
//                }
//            }
//            pm.worked(1);
//        }
//        pm.done();
//    }
//
//    /**
//     * @param elevationIter the elevation matrix
//     * @param nablaRaster the output, the raster with the nabla value.
//     * @param destRect
//     */
//    private void nabla( Raster elevationIter, WritableRaster nablaRaster, Rectangle destRect ) {
//        int y;
//        double nablaT, n;
//        double[] z = new double[9];
//        // the dimension of raster data
//        int height = elevationIter.getHeight();
//        int width = elevationIter.getWidth();
//        // the origin of the x and y axes
//        int xOrigin = destRect.x;
//        int yOrigin = destRect.y;
//        Point p = new Point(xOrigin, yOrigin);
//        // the array which will fill the dataBuffer (a raster data is a
//        // DataBuffer organised in order to a sampleModel.
//        double[] valueArray = new double[height * width];
//        DataBufferDouble buffer = new DataBufferDouble(valueArray, height * width);
//        SampleModel sm = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, width, height,
//                1);
//        WritableRaster segn =  RasterFactory.createWritableRaster(sm,buffer ,p );
//        int nrows = destRect.height;
//        int ncols = destRect.width;
//        int maxNRows = xOrigin + nrows;
//        int maxNCols = yOrigin + ncols;
//
//        int[][] v = ModelsConstants.DIR;
//
//        // grid contains the dimension of pixels according with flow directions
//        double[] grid = new double[9];
//        grid[0] = 0;
//        grid[1] = grid[5] = xRes;
//        grid[3] = grid[7] = yRes;
//        grid[2] = grid[4] = grid[6] = grid[8] = Math.sqrt(grid[1] * grid[1] + grid[3] * grid[3]);
//
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(out);
//        pm.beginTask(WORKING_ON + "h.nabla...", maxNCols - xOrigin + (maxNCols - 1 - xOrigin));
//        for( int i = xOrigin; i < maxNCols; i++ ) {
//            for( int j = yOrigin; j < maxNRows; j++ ) {
//                z[0] = elevationIter.getSampleDouble(i, j, 0);
//                if (!isNovalue((z[0]))) {
//                    y = 1;
//                    for( int h = 1; h <= 8; h++ ) {
//                        z[h] = elevationIter.getSample(i + v[h][0], j + v[h][1], 0);
//                        if (isNovalue(z[h])) {
//                            y = 0;
//                            segn.setSample(i, j, 0, 1);
//                            break;
//                        }
//                    }
//                    if (y == 0) {
//                        nablaRaster.setSample(i, j, 0, DEFAULT_NO_VALUE);
//                    } else {
//                        nablaRaster.setSample(i, j, 0, 0.5 * ((z[1] + z[5] - 2 * z[0])
//                                / (grid[1] * grid[1]) + (z[3] + z[7] - 2 * z[0])
//                                / (grid[3] * grid[3])));
//                        nablaRaster.setSample(i, j, 0, nablaRaster.getSample(i, j, 0) + 0.5
//                                * ((z[2] + z[4] + z[6] + z[8] - 4 * z[0]) / (grid[6] * grid[6])));
//                    }
//                } else {
//                    nablaRaster.setSample(i, j, 0, DEFAULT_NO_VALUE);
//                }
//            }
//            pm.worked(1);
//        }
//
//        for( int i = xOrigin; i < maxNCols - 1; i++ ) {
//            for( int j = yOrigin = 1; j < maxNRows - 1; j++ ) {
//                if (segn.getSampleDouble(i, j, 0) == 1) {
//                    n = 0.0;
//                    nablaT = 0.0;
//                    y = 0;
//                    for( int h = 1; h <= 8; h++ ) {
//                        z[h] = elevationIter.getSampleDouble(i + v[h][0], j + v[h][1], 0);
//                        y = 0;
//                        double nablaSample = nablaRaster.getSampleDouble(i + v[h][0], j + v[h][1],
//                                0);
//                        if (isNovalue(z[h]) || !isNovalue(nablaSample))
//                            y = 1;
//                        if (y == 0) {
//                            n += 1;
//                            nablaT += nablaSample;
//                        }
//                    }
//                    if (n == 0)
//                        n = 1;
//                    nablaRaster.setSample(i, j, 0, nablaT / (float) n);
//                }
//            }
//            pm.worked(1);
//        }
//        pm.done();
//
//    }
//
//}
