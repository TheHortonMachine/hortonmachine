package org.hortonmachine.lidar;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.hortonmachine.database.DatabaseViewer;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.Las;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasConstraints;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.houghes.HoughCircles;
import org.hortonmachine.gears.modules.r.houghes.OmsHoughCirclesRaster;
import org.hortonmachine.gears.modules.v.vectorize.OmsVectorizer;
import org.hortonmachine.gears.ui.progress.ProgressUpdate;
import org.hortonmachine.gears.utils.BitMatrix;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.TransformationUtils;
import org.hortonmachine.gears.utils.colors.ColorInterpolator;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.executor.ExecutorIndeterminateGui;
import org.hortonmachine.gui.utils.executor.ExecutorProgressGui;
import org.hortonmachine.gui.utils.monitor.ActionWithProgress;
import org.hortonmachine.gui.utils.monitor.ProgressMonitor;
import org.joda.time.DateTime;
// import org.jzy3d.analysis.AbstractAnalysis;
// import org.jzy3d.analysis.AnalysisLauncher;
// import org.jzy3d.chart.factories.AWTChartComponentFactory;
// import org.jzy3d.maths.Coord3d;
// import org.jzy3d.plot3d.primitives.Scatter;
// import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class LasInfoController extends LasInfoView implements IOnCloseListener, KeyListener {
    private static final DefaultEngineeringCRS DEFAULT_GENERIC = DefaultEngineeringCRS.GENERIC_2D;

    private static final String NO_CRS_MSG = "No CRS is available for the dataset. Please add a proper prj file.";

    private ALasReader lasReader;

    private LasConstraints constraints = new LasConstraints();

    private GridCoverage2D dtm;
    private AffineTransform worldToPixel;
    private AffineTransform pixelToWorld;

    private BufferedImage lastDrawnImage;

    private boolean sliceModeIsOn = false;

    private LinkedHashMap<String, List<LasRecord>> slicesMap;

    public LasInfoController() {
        setPreferredSize(new Dimension(1800, 1100));

        init();
    }
    private void init() {
        _inputPathField.setEditable(false);
        GuiUtilities.setFileBrowsingOnWidgets(_inputPathField, _loadButton, new String[]{"las", "laz"}, () -> {
            new ExecutorIndeterminateGui(){
                @Override
                public void backGroundWork() throws Exception {
                    loadNewFile();
                }
            }.execute();
        });
        _dtmInputPathField.setEditable(false);
        GuiUtilities.setFileBrowsingOnWidgets(_dtmInputPathField, _loadDtmButton, new String[]{"asc", "tiff"}, () -> {
            new ExecutorIndeterminateGui(){
                @Override
                public void backGroundWork() throws Exception {
                    loadDtm();
                }
            }.execute();
        });

        _boundsLoadButton.addActionListener(e -> {
            File[] shpFile = GuiUtilities.showOpenFilesDialog(_boundsLoadButton, "Select bounds file...", false,
                    PreferencesHandler.getLastFile(), new GuiUtilities.ShpFileFilter());
            if (shpFile != null) {
                new ExecutorIndeterminateGui(){
                    @Override
                    public void backGroundWork() throws Exception {
                        loadBoundsFromFile(shpFile[0]);
                    }
                }.execute();
            }
        });

        _samplingField.setText("1000");
        constraints.setSampling(1000);
        _samplingField.addKeyListener(this);
        _classesField.addKeyListener(this);
        _impulsesField.addKeyListener(this);
        _intensityRangeField.addKeyListener(this);
        _westField.addKeyListener(this);
        _eastField.addKeyListener(this);
        _southField.addKeyListener(this);
        _northField.addKeyListener(this);
        _minZField.addKeyListener(this);
        _maxZField.addKeyListener(this);
        _lowerThresField.addKeyListener(this);
        _upperThresField.addKeyListener(this);

        _elevationRadio.setSelected(true);

        _convertButton.addActionListener(e -> {
            if (checkReader()) {
                File saveFile = GuiUtilities.showSaveFileDialog(this, "Save to file", PreferencesHandler.getLastFile());
                if (saveFile != null) {
                    exportAction(saveFile);
                }
            }
        });
        _createOverviewButton.addActionListener(e -> {
            if (checkReader()) {
                File saveFile = GuiUtilities.showSaveFileDialog(this, "Save to file", PreferencesHandler.getLastFile());
                if (saveFile != null) {
                    createOverviewAction(saveFile);
                }
            }
        });

        _previewImageLabel.addMouseListener(new MouseListener(){
            private boolean oneTwo = true;

            @Override
            public void mouseReleased( MouseEvent e ) {
                if (pixelToWorld != null) {
                    int px = e.getX();
                    int py = e.getY();
                    Point2D fromPoint = new Point2D.Double(px, py);
                    Point2D toPoint = new Point2D.Double();
                    pixelToWorld.transform(fromPoint, toPoint);
                    if (SwingUtilities.isMiddleMouseButton(e)) {
                        Envelope env = constraints.getFilteredEnvelope();
                        double expX = env.getWidth() * 0.2 / 2.0;
                        double expY = env.getHeight() * 0.2 / 2.0;
                        Envelope newEnv = new Envelope(env);
                        newEnv.expandBy(expX, expY);
                        constraints.setWest(newEnv.getMinX());
                        constraints.setEast(newEnv.getMaxX());
                        constraints.setSouth(newEnv.getMinY());
                        constraints.setNorth(newEnv.getMaxY());
//                        drawPreview();
                    } else {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            double x = toPoint.getX();
                            double y = toPoint.getY();
                            if (oneTwo) {
                                constraints.setWest(x);
                                constraints.setSouth(y);
                            } else {
                                constraints.setEast(x);
                                constraints.setNorth(y);
                            }
                            oneTwo = !oneTwo;
                        } else if (SwingUtilities.isRightMouseButton(e)) {
                            constraints.setEast(null);
                            constraints.setNorth(null);
                            constraints.setWest(null);
                            constraints.setSouth(null);
                        }
                        Double[] propBounds = constraints.checkBounds();
                        _westField.setText(propBounds[0] != null ? propBounds[0].toString() : "");
                        _eastField.setText(propBounds[1] != null ? propBounds[1].toString() : "");
                        _southField.setText(propBounds[2] != null ? propBounds[2].toString() : "");
                        _northField.setText(propBounds[3] != null ? propBounds[3].toString() : "");
                        drawWithMouseBounds();
                    }
                }
            }

            @Override
            public void mousePressed( MouseEvent e ) {
            }

            @Override
            public void mouseExited( MouseEvent e ) {
            }

            @Override
            public void mouseEntered( MouseEvent e ) {
            }

            @Override
            public void mouseClicked( MouseEvent e ) {
            }
        });

        _loadPreviewButton.addActionListener(e -> {
            if (checkReader()) {
                new ExecutorIndeterminateGui(){
                    @Override
                    public void backGroundWork() throws Exception {
                        constraints.applyConstraints(lasReader, false, null);
                        List<LasRecord> filteredPoints = constraints.getFilteredPoints();
                        drawPreview(filteredPoints);
                    }
                }.execute();
            }
        });
        _loadDataButton.addActionListener(e -> {
            if (checkReader()) {
                loadDataAction();
            }
        });

        _load3DButton.setVisible(false);
//         _load3DButton.addActionListener(e -> {
//             if (checkReader()) {
//                 AbstractAnalysis abstractAnalysis = new AbstractAnalysis(){
//                     @Override
//                     public void init() throws Exception {
//                         constraints.applyConstraints(lasReader, false, null);
//                         List<LasRecord> filteredPoints = constraints.getFilteredPoints();
//                         List<Coord3d> points = new ArrayList<>();
//                         List<org.jzy3d.colors.Color> colors = new ArrayList<>();

//                         for( LasRecord dot : filteredPoints ) {
//                             float x = (float) dot.x;
//                             float y = (float) dot.y;
//                             float z = (float) dot.z;
//                             if (!Double.isNaN(dot.groundElevation)) {
//                                 z = (float) dot.groundElevation;
//                             }
//                             points.add(new Coord3d(x, y, z));
//                             short[] c = dot.color;
//                             colors.add(new org.jzy3d.colors.Color((int) c[0], (int) c[1], (int) c[2]));
//                         }

//                         Coord3d[] coord3ds = points.toArray(new Coord3d[points.size()]);
//                         org.jzy3d.colors.Color[] colorsArray = colors.toArray(new org.jzy3d.colors.Color[colors.size()]);
//                         Scatter scatterLas = new Scatter(coord3ds, colorsArray, 7f);
//                         String text = _pointSizeField.getText();
//                         int pointSize = 1;
//                         try {
//                             pointSize = Integer.parseInt(text.trim());
//                         } catch (Exception e) {
//                             // ignore
//                         }
//                         scatterLas.setWidth(pointSize);

//                         chart = AWTChartComponentFactory.chart(Quality.Fastest, "newt");
//                         chart.getScene().getGraph().add(scatterLas);
// //                    chart.setAxeDisplayed(false);
//                         chart.getView().setSquared(false);

//                     }
//                 };
//                 try {
//                     AnalysisLauncher.open(abstractAnalysis);
//                 } catch (Exception e1) {
//                     GuiUtilities.handleError(_boundsLoadButton, e1);
//                 }
//             }
//         });

        _sliceIntervalField.setText("1");
        _sliceWidthField.setText("0.2");
        _slicingModeCheck.addActionListener(e -> {
            if (checkReader()) {
                sliceModeIsOn = _slicingModeCheck.isSelected();

                _loadSlicedataButton.setEnabled(sliceModeIsOn);
                _slicesCombo.setEnabled(sliceModeIsOn);
            }
        });

        _loadSlicedataButton.addActionListener(e -> {
            if (checkReader()) {
                new ExecutorIndeterminateGui(){
                    @Override
                    public void backGroundWork() throws Exception {
                        loadSliceData();
                    }
                }.execute();
            }
        });

        _slicesCombo.addActionListener(e -> {
            String slice = _slicesCombo.getSelectedItem().toString();
            if (slicesMap != null) {
                List<LasRecord> lrList = slicesMap.get(slice);
                if (lrList != null) {
                    new ExecutorIndeterminateGui(){
                        @Override
                        public void backGroundWork() throws Exception {
                            drawPreview(lrList);
                        }
                    }.execute();
                    return;
                }
            }
            GuiUtilities.showWarningMessage(this, "No slice data available. ");
        });

        _circlesMinCellCountField.setText("50");
        _circlesExtractButton.addActionListener(e -> {
            if (checkReader()) {
                new ExecutorIndeterminateGui(){
                    @Override
                    public void backGroundWork() throws Exception {
                        try {
                            List<Geometry> circles = getCircleGeometries();
                            BufferedImage withCirclesImage = new BufferedImage(lastDrawnImage.getWidth(),
                                    lastDrawnImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                            Graphics2D g2d = (Graphics2D) withCirclesImage.getGraphics();
                            g2d.drawImage(lastDrawnImage, 0, 0, null);
                            GeneralPath gp = new GeneralPath();
                            for( Geometry g : circles ) {
                                Coordinate[] coords = g.getCoordinates();
                                boolean isNew = true;
                                for( Coordinate coordinate : coords ) {
                                    Coordinate newCoord = TransformationUtils.transformCoordinate(worldToPixel, coordinate);
                                    if (isNew) {
                                        gp.moveTo(newCoord.x, newCoord.y);
                                        isNew = false;
                                    } else {
                                        gp.lineTo(newCoord.x, newCoord.y);
                                    }
                                }
                            }
                            g2d.setColor(Color.red);
                            g2d.setStroke(new BasicStroke(3));
                            g2d.draw(gp);
                            g2d.dispose();

                            _previewImageLabel.setIcon(new ImageIcon(withCirclesImage));
                        } catch (Exception e1) {
                            GuiUtilities.showErrorMessage(LasInfoController.this, null, e1.getMessage());
                        }
                    }
                }.execute();
            }
        });
        _circlesSaveShpButton.addActionListener(e -> {
            if (checkReader()) {
                File saveFile = GuiUtilities.showSaveFileDialog(this, "Save circles to shp", PreferencesHandler.getLastFile());
                if (saveFile != null) {
                    new ExecutorIndeterminateGui(){
                        @Override
                        public void backGroundWork() throws Exception {
                            try {
                                List<Geometry> circles = getCircleGeometries();
                                ILasHeader header = lasReader.getHeader();
                                CoordinateReferenceSystem crs = header.getCrs();
                                if (crs == null) {
                                    crs = DEFAULT_GENERIC;
                                }
                                SimpleFeatureCollection fc = FeatureUtilities.featureCollectionFromGeometry(crs,
                                        circles.toArray(new Geometry[0]));
                                OmsVectorWriter.writeVector(saveFile.getAbsolutePath(), fc);
                            } catch (Exception e1) {
                                GuiUtilities.showErrorMessage(LasInfoController.this, null, e1.getMessage());
                            }
                        }
                    }.execute();

                }
            }
        });
    }

    private boolean checkReader() {
        if (lasReader == null) {
            GuiUtilities.showWarningMessage(this, "No data reader seems to be available. Load some data!");
            return false;
        } else {
            return true;
        }
    }
    private List<Geometry> getCircleGeometries() throws Exception {
        ILasHeader header = lasReader.getHeader();
        CoordinateReferenceSystem crs = header.getCrs();
        if (crs == null) {
            crs = DEFAULT_GENERIC;
        }

        String text = _circlesMinCellCountField.getText();
        int minCellCount = (int) Double.parseDouble(text);

        int w = lastDrawnImage.getWidth();
        int h = lastDrawnImage.getHeight();
        Envelope env = constraints.getFilteredEnvelope();
        Envelope fittingEnvelope = TransformationUtils.expandToFitRatio(env, w, h);
        double xRes = fittingEnvelope.getWidth() / w;
        double yRes = fittingEnvelope.getHeight() / h;
        RegionMap rm = CoverageUtilities.makeRegionParamsMap(fittingEnvelope.getMaxY(), fittingEnvelope.getMinY(),
                fittingEnvelope.getMinX(), fittingEnvelope.getMaxX(), xRes, yRes, w, h);
        GridCoverage2D raster = CoverageUtilities.buildCoverage("slice", lastDrawnImage, rm, crs);
        List<Geometry> circles = findCircles(raster, minCellCount);
        return circles;
    }

    @SuppressWarnings("unchecked")
    private void loadSliceData() {
        String text = _sliceIntervalField.getText();
        double interval = Double.parseDouble(text);
        text = _sliceWidthField.getText();
        double width = Double.parseDouble(text);

        double[] stats = constraints.getStats();
        double minElev = stats[0];
        double maxElev = stats[1];

        slicesMap = new LinkedHashMap<>();

        List<LasRecord> filteredPoints = constraints.getFilteredPoints();
        double runningInterval = minElev;
        while( runningInterval < maxElev ) {
            ArrayList<LasRecord> sliceList = new ArrayList<>();
            slicesMap.put(String.valueOf(runningInterval), sliceList);

            double lower = runningInterval - width / 2.0;
            double upper = runningInterval + width / 2.0;
            for( LasRecord lr : filteredPoints ) {
                if (lr.z > lower && lr.z < upper) {
                    sliceList.add(lr);
                }
            }

            runningInterval += interval;
        }

        _slicesCombo.setModel(new DefaultComboBoxModel<>(slicesMap.keySet().toArray(new String[0])));
    }

    private void loadDtm() {
        String dtmFilePath = _dtmInputPathField.getText();
        try {
            if (dtmFilePath.trim().length() > 0) {
                dtm = OmsRasterReader.readRaster(dtmFilePath);
            } else {
                dtm = null;
            }
            constraints.setDtm(dtm);
        } catch (Exception e) {
            GuiUtilities.handleError(_boundsLoadButton, e);
        }
    }

    private void loadDataAction() {
        ILasHeader header = lasReader.getHeader();
        long recordsCount = header.getRecordsCount();
        int work = (int) (recordsCount / 1000);

        new ExecutorProgressGui(work){
            @Override
            public void backGroundWork() throws Exception {
                constraints.applyConstraints(lasReader, true, this);
//                done();
            }
        }.execute();
    }

    private void exportAction( File saveFile ) {
        String outputFilePath = saveFile.getAbsolutePath();

        if (!outputFilePath.toLowerCase().endsWith(".las") && !outputFilePath.toLowerCase().endsWith(".shp")) {
            JOptionPane.showMessageDialog(LasInfoController.this, "Only conversion to las and shp is supported.", "ERROR",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        ILasHeader header = lasReader.getHeader();
        long recordsCount = header.getRecordsCount();
        int work = (int) (recordsCount / 1000);

        new ExecutorProgressGui(work * 2){
            @Override
            public void backGroundWork() throws Exception {
                constraints.applyConstraints(lasReader, true, this);
                List<LasRecord> filteredPoints = constraints.getFilteredPoints();

                publish(new ProgressUpdate("Now writing to file...", work + work / 2));
                if (outputFilePath.toLowerCase().endsWith(".las")) {
                    ALasWriter lasWriter = Las.getWriter(new File(outputFilePath), header.getCrs());
                    Envelope fEnv = constraints.getFilteredEnvelope();
                    double[] stats = constraints.getStats();
                    lasWriter.setBounds(lasReader.getHeader());
                    lasWriter.setBounds(fEnv.getMinX(), fEnv.getMaxX(), fEnv.getMinY(), fEnv.getMaxY(), stats[0], stats[1]);
                    lasWriter.open();
                    filteredPoints.stream().forEach(lr -> {
                        try {
                            lasWriter.addPoint(lr);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    lasWriter.close();
                } else if (outputFilePath.toLowerCase().endsWith(".shp")) {
                    DefaultFeatureCollection fc = new DefaultFeatureCollection();
                    int count = 0;
                    for( LasRecord lr : filteredPoints ) {
                        SimpleFeature feature = LasUtils.tofeature(lr, count++, header.getCrs());
                        fc.add(feature);
                    }
                    OmsVectorWriter.writeVector(outputFilePath, fc);
                }
                done();
            }
        }.execute();
    }

    private void createOverviewAction( File saveFile ) {
        String outputFilePath = saveFile.getAbsolutePath();

        ILasHeader header = lasReader.getHeader();
        long recordsCount = header.getRecordsCount();
        int work = (int) (recordsCount / 1000);

        new ExecutorProgressGui(work * 2){
            @Override
            public void backGroundWork() throws Exception {
                constraints.applyConstraints(lasReader, true, this);

                publish(new ProgressUpdate("Getting bounds...", work + work / 2));
                Envelope filteredEnvelope = constraints.getFilteredEnvelope();
                Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(filteredEnvelope);
                polygon.setUserData("Overview for " + lasReader.getLasFile().getName());
                SimpleFeatureCollection fc = FeatureUtilities.featureCollectionFromGeometry(header.getCrs(), polygon);
                String f = outputFilePath;
                if (!f.toLowerCase().endsWith(".shp"))
                    f = f + ".shp";
                OmsVectorWriter.writeVector(f, fc);
                done();
            }
        }.execute();
    }

    private void loadBoundsFromFile( File shpFile ) {
        String boundsFilePath = shpFile.getAbsolutePath();
        try {
            if (boundsFilePath.trim().length() > 0) {
                ReferencedEnvelope env = OmsVectorReader.readEnvelope(boundsFilePath);
                _westField.setText(String.valueOf(env.getMinX()));
                _eastField.setText(String.valueOf(env.getMaxX()));
                _southField.setText(String.valueOf(env.getMinY()));
                _northField.setText(String.valueOf(env.getMaxY()));
            } else {
                _westField.setText("");
                _eastField.setText("");
                _southField.setText("");
                _northField.setText("");
            }
            checkConstraint(_westField);
            checkConstraint(_eastField);
            checkConstraint(_southField);
            checkConstraint(_northField);
        } catch (Exception e) {
            GuiUtilities.handleError(_boundsLoadButton, e);
        }
    }
    private void loadNewFile() {
        String inputPath = _inputPathField.getText();
        try {
            if (inputPath.trim().length() > 0) {
                lasReader = Las.getReader(new File(inputPath));
                ILasHeader header = lasReader.getHeader();

                List<String[]> headerInfoList = getHeaderInfo(header);
                _headerTable.setModel(
                        new DefaultTableModel(headerInfoList.toArray(new String[0][0]), new String[]{"Property", "Value"}));
                List<String[]> firstPointInfoList = getFirstPointInfo();
                _firstPointTable.setModel(
                        new DefaultTableModel(firstPointInfoList.toArray(new String[0][0]), new String[]{"Property", "Value"}));
            } else {
                lasReader = null;
                _headerTable.setModel(new DefaultTableModel(new String[0][0], new String[]{"Property", "Value"}));
                _firstPointTable.setModel(new DefaultTableModel(new String[0][0], new String[]{"Property", "Value"}));
            }
        } catch (Exception e) {
            GuiUtilities.handleError(_boundsLoadButton, e);
        }
    }
    private List<String[]> getHeaderInfo( ILasHeader header ) {
        List<String[]> headerInfoList = new ArrayList<>();
        headerInfoList.add(new String[]{"File signature", new String(header.getFileSignature())});
        headerInfoList.add(new String[]{"File source ID", "" + header.getFileSourceID()});
        headerInfoList.add(new String[]{"Project ID - data 1", "" + header.getProjectID_GUIDData1()});
        headerInfoList.add(new String[]{"Project ID - data 2", "" + header.getProjectID_GUIDData2()});
        headerInfoList.add(new String[]{"Project ID - data 3", "" + header.getProjectID_GUIDData3()});
        headerInfoList.add(new String[]{"Project ID - data 4", new String(header.getProjectID_GUIDData4())});
        headerInfoList.add(new String[]{"Version", header.getVersion()});
        headerInfoList.add(new String[]{"System identifier", new String(header.getSystemIdentifier())});
        headerInfoList.add(new String[]{"Generating software", header.getGeneratingSoftware()});
        try {
            short fileCreationYear = (short) header.getFileCreationYear();
            short fileCreationDayOfYear = (short) header.getFileCreationDayOfYear();
            String dtString = " - nv - ";
            if (fileCreationYear != 0 && fileCreationDayOfYear != 0) {
                DateTime dateTime = new DateTime();
                dateTime = dateTime.withYear(fileCreationYear).withDayOfYear(fileCreationDayOfYear);
                dtString = dateTime.toString(LasUtils.dateTimeFormatterYYYYMMDD);
            }
            headerInfoList.add(new String[]{"File creation date", dtString});
        } catch (Exception e) {
            e.printStackTrace();
        }
        headerInfoList.add(new String[]{"Header size", "" + header.getHeaderSize()}); //
        headerInfoList.add(new String[]{"Offset to data", "" + header.getOffset()}); //
        headerInfoList.add(new String[]{"Variable length records", "" + header.getNumberOfVariableLengthRecords()}); //
        headerInfoList.add(new String[]{"Point data format ID (0-99 for spec)", "" + header.getPointDataRecordFormat()}); //
        headerInfoList.add(new String[]{"Number of point records", "" + header.getRecordsCount()}); //
        headerInfoList.add(new String[]{"Record length", "" + header.getRecordLength()}); //
        double[] xyzScale = header.getXYZScale();
        headerInfoList.add(new String[]{"Scale", xyzScale[0] + ", " + xyzScale[1] + ", " + xyzScale[2]});
        double[] xyzOffset = header.getXYZOffset();
        headerInfoList.add(new String[]{"Offset", xyzOffset[0] + ", " + xyzOffset[1] + ", " + xyzOffset[2]});
        ReferencedEnvelope3D dataEnvelope3D = header.getDataEnvelope();
        headerInfoList.add(new String[]{"X Range", dataEnvelope3D.getMinX() + ", " + dataEnvelope3D.getMaxX()});
        headerInfoList.add(new String[]{"Y Range", dataEnvelope3D.getMinY() + ", " + dataEnvelope3D.getMaxY()});
        headerInfoList.add(new String[]{"Z Range", dataEnvelope3D.getMinZ() + ", " + dataEnvelope3D.getMaxZ()});
        return headerInfoList;
    }

    private List<String[]> getFirstPointInfo() throws IOException {
        List<String[]> firstPointInfoList = new ArrayList<>();
        if (lasReader.hasNextPoint()) {
            LasRecord lr = lasReader.getNextPoint();
            firstPointInfoList.add(new String[]{"X", "" + lr.x});
            firstPointInfoList.add(new String[]{"Y", "" + lr.y});
            firstPointInfoList.add(new String[]{"Z", "" + lr.z});
            firstPointInfoList.add(new String[]{"Intensity", "" + lr.intensity});
            firstPointInfoList.add(new String[]{"Return", "" + lr.returnNumber});
            firstPointInfoList.add(new String[]{"Number of returns", "" + lr.numberOfReturns});
            firstPointInfoList.add(new String[]{"Classification", "" + lr.classification});
            firstPointInfoList.add(new String[]{"GPS Time", "" + lr.gpsTime});
            firstPointInfoList.add(new String[]{"Color", lr.color[0] + ", " + lr.color[1] + ", " + lr.color[2]});
        }
        lasReader.rewind();
        return firstPointInfoList;
    }

    public JComponent asJComponent() {
        return this;
    }

    @Override
    public void onClose() {
    }

    @Override
    public boolean canCloseWithoutPrompt() {
        return _inputPathField.getText().trim().length() == 0;
    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();

        final LasInfoController controller = new LasInfoController();

        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Las Info Viewer");

        GuiUtilities.setDefaultFrameIcon(frame);

        GuiUtilities.addClosingListener(frame, controller);

    }
    @Override
    public void keyTyped( KeyEvent e ) {
    }
    @Override
    public void keyPressed( KeyEvent e ) {
    }
    @Override
    public void keyReleased( KeyEvent e ) {
        Object source = e.getSource();
        checkConstraint(source);
    }

    private void checkConstraint( Object source ) {
        if (source == _samplingField) {
            String text = _samplingField.getText();
            Integer sampling;
            try {
                sampling = Integer.parseInt(text.trim());
                if (sampling == 0) {
                    sampling = null;
                }
            } catch (NumberFormatException e1) {
                sampling = null;
            }
            constraints.setSampling(sampling);
        } else if (source == _classesField) {
            final String[] split = _classesField.getText().trim().split(","); //$NON-NLS-1$
            int[] classes;
            try {
                if (split.length != 0) {
                    classes = new int[split.length];
                    for( int i = 0; i < split.length; i++ ) {
                        classes[i] = Integer.parseInt(split[i]);
                    }
                } else {
                    classes = null;
                }
            } catch (final Exception ex) {
                classes = null;
            }
            constraints.setClassifications(classes);
        } else if (source == _impulsesField) {
            final String[] split = _impulsesField.getText().trim().split(","); //$NON-NLS-1$
            int[] impulses;
            try {
                if (split.length != 0) {
                    impulses = new int[split.length];
                    for( int i = 0; i < split.length; i++ ) {
                        impulses[i] = Integer.parseInt(split[i]);
                    }
                } else {
                    impulses = null;
                }
            } catch (final Exception ex) {
                impulses = null;
            }
            constraints.setImpulses(impulses);
        } else if (source == _intensityRangeField) {
            final String[] split = _intensityRangeField.getText().trim().split(","); //$NON-NLS-1$
            if (split.length == 2) {
                try {
                    double minInt = Double.parseDouble(split[0].trim());
                    double maxInt = Double.parseDouble(split[1].trim());
                    constraints.setMinIntensity(minInt);
                    constraints.setMaxIntensity(maxInt);
                } catch (final Exception ex) {
                    constraints.setMinIntensity(null);
                    constraints.setMaxIntensity(null);
                }
            } else {
                constraints.setMinIntensity(null);
                constraints.setMaxIntensity(null);
            }
        } else if (source == _westField) {
            String text = _westField.getText();
            Double west;
            try {
                west = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                west = null;
            }
            constraints.setWest(west);
        } else if (source == _eastField) {
            String text = _eastField.getText();
            Double east;
            try {
                east = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                east = null;
            }
            constraints.setEast(east);
        } else if (source == _southField) {
            String text = _southField.getText();
            Double south;
            try {
                south = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                south = null;
            }
            constraints.setSouth(south);
        } else if (source == _northField) {
            String text = _northField.getText();
            Double north;
            try {
                north = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                north = null;
            }
            constraints.setNorth(north);
        } else if (source == _minZField) {
            String text = _minZField.getText();
            Double minZ;
            try {
                minZ = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                minZ = null;
            }
            constraints.setMinZ(minZ);
        } else if (source == _maxZField) {
            String text = _maxZField.getText();
            Double maxZ;
            try {
                maxZ = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                maxZ = null;
            }
            constraints.setMaxZ(maxZ);
        } else if (source == _lowerThresField) {
            String text = _lowerThresField.getText();
            Double lowerThres;
            try {
                lowerThres = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                lowerThres = null;
            }
            constraints.setLowerThres(lowerThres);
        } else if (source == _upperThresField) {
            String text = _upperThresField.getText();
            Double upperThres;
            try {
                upperThres = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                upperThres = null;
            }
            constraints.setUpperThres(upperThres);
        }
    }

    private void drawPreview( List<LasRecord> pointsToDraw ) {
        try {

            ColorInterpolator colorInterp = null;
            boolean useIntensity = _intensityRadio.isSelected();
            boolean useClass = _classRadio.isSelected();
            boolean useImpulse = _impulseRadio.isSelected();
            boolean useRGB = _ownColorRadio.isSelected();
            if (!useRGB) {
                if (useIntensity) {
                    colorInterp = constraints.getIntensityColorInterpolator();
                } else if (useClass) {
                    colorInterp = constraints.getClassificationColorInterpolator();
                } else if (useImpulse) {
                    colorInterp = constraints.getImpulseColorInterpolator();
                } else {
                    colorInterp = constraints.getElevationColorInterpolator();
                }
            }

            String text = _elevHigherThanField.getText();
            double elevHigherThan = Double.NaN;
            try {
                elevHigherThan = Double.parseDouble(text.trim());
            } catch (Exception e) {
                // ignore
            }
            boolean showElevHigherThan = !Double.isNaN(elevHigherThan);
            double felevHigherThan = elevHigherThan;

            text = _intensityHigherThanField.getText();
            double intensityHigherThan = Double.NaN;
            try {
                intensityHigherThan = Double.parseDouble(text.trim());
            } catch (Exception e) {
                // ignore
            }
            boolean showIntensityHigherThan = !Double.isNaN(intensityHigherThan);
            double fintensityHigherThan = intensityHigherThan;

            text = _pointSizeField.getText();
            int pointSize = 1;
            try {
                pointSize = Integer.parseInt(text.trim());
            } catch (Exception e) {
                // ignore
            }
            int fpointSize = pointSize;

            int labelWidth = _previewImageLabel.getWidth();
            int labelHeight = _previewImageLabel.getHeight();

            int imageWidth = 500;
            int imageHeight = 500;
            if (labelWidth > 0 && labelHeight > 0) {
                imageWidth = labelWidth;
                imageHeight = labelHeight;
            }
            if (imageWidth == 0)
                imageWidth = 10;
            if (imageHeight == 0)
                imageHeight = 10;
            Envelope filteredEnvelope = constraints.getFilteredEnvelope();
            Envelope fittingEnvelope = TransformationUtils.expandToFitRatio(filteredEnvelope, imageWidth, imageHeight);

            BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = (Graphics2D) image.getGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, imageWidth, imageHeight);
            g2d.setColor(Color.RED);

            Rectangle pixelRectangle = new Rectangle(0, 0, imageWidth, imageHeight);
            worldToPixel = TransformationUtils.getWorldToPixel(fittingEnvelope, pixelRectangle);
            pixelToWorld = TransformationUtils.getPixelToWorld(pixelRectangle, fittingEnvelope);
            ColorInterpolator fcolorInterp = colorInterp;
            Point2D toPoint = new Point2D.Double();
            Point2D fromPoint = new Point2D.Double();
            BitMatrix bm = new BitMatrix(imageWidth, imageHeight);
            List<int[]> elevHigherThanList = new ArrayList<>();
            List<int[]> intensityHigherThanList = new ArrayList<>();
            pointsToDraw.stream().forEach(lr -> {
                fromPoint.setLocation(lr.x, lr.y);
                worldToPixel.transform(fromPoint, toPoint);

                int xPix = (int) toPoint.getX();
                int yPix = (int) toPoint.getY();

                double theZ = Double.isNaN(lr.groundElevation) ? lr.z : lr.groundElevation;

                if (showElevHigherThan && theZ > felevHigherThan) {
                    elevHigherThanList.add(new int[]{xPix, yPix});
                }
                if (showIntensityHigherThan && lr.intensity > fintensityHigherThan) {
                    intensityHigherThanList.add(new int[]{xPix, yPix});
                }
                if (!bm.isMarked(xPix, yPix)) {
                    Color c;
                    if (useRGB) {
                        try {
                            c = new Color(lr.color[0], lr.color[1], lr.color[2]);
                        } catch (Exception e) {
                            c = Color.RED;
                        }
                    } else {
                        if (useClass) {
                            c = fcolorInterp.getColorFor(lr.classification);
                        } else if (useIntensity) {
                            c = fcolorInterp.getColorFor(lr.intensity);
                        } else if (useImpulse) {
                            c = fcolorInterp.getColorFor(lr.returnNumber);
                        } else {
                            c = fcolorInterp.getColorFor(theZ);
                        }
                    }
                    g2d.setColor(c);

                    if (fpointSize == 1) {
                        g2d.drawLine(xPix, yPix, xPix, yPix);
                    } else {
                        g2d.fillOval(xPix - fpointSize / 2, yPix - fpointSize / 2, fpointSize, fpointSize);
                    }
                }
                bm.mark(xPix, yPix);
            });

            g2d.setColor(Color.black);
            int delta = 5;
            for( int[] xy : elevHigherThanList ) {
                g2d.drawLine(xy[0] - delta, xy[1], xy[0] + delta, xy[1]);
                g2d.drawLine(xy[0], xy[1] - delta, xy[0], xy[1] + delta);
            }
            g2d.setColor(Color.red);
            for( int[] xy : intensityHigherThanList ) {
                g2d.drawLine(xy[0] - delta, xy[1], xy[0] + delta, xy[1]);
                g2d.drawLine(xy[0], xy[1] - delta, xy[0], xy[1] + delta);
            }

            g2d.dispose();

            lastDrawnImage = new BufferedImage(labelWidth, labelHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2dLabel = (Graphics2D) lastDrawnImage.getGraphics();
            int x = labelWidth / 2 - imageWidth / 2;
            int y = labelHeight / 2 - imageHeight / 2;
            g2dLabel.setColor(Color.WHITE);
            g2dLabel.fillRect(0, 0, labelWidth, labelHeight);
            g2dLabel.drawImage(image, x, y, null);
            g2dLabel.dispose();

            drawWithMouseBounds();

            double[] stats = constraints.getStats();
            Envelope fenv = constraints.getFilteredEnvelope();
            StringBuilder toolTip = new StringBuilder("<html>");
            int i = 0;
            toolTip.append("Filtered data stats ").append("<br>");
            toolTip.append("Points count: ").append(pointsToDraw.size()).append("<br>");
            toolTip.append("Min Elevation: ").append(stats[i++]).append("<br>");
            toolTip.append("Max Elevation: ").append(stats[i++]).append("<br>");
            toolTip.append("Min Intensity: ").append(stats[i++]).append("<br>");
            toolTip.append("Max Intensity: ").append(stats[i++]).append("<br>");
            toolTip.append("Min Classification: ").append(stats[i++]).append("<br>");
            toolTip.append("Max Classification: ").append(stats[i++]).append("<br>");
            toolTip.append("Min Impulse: ").append(stats[i++]).append("<br>");
            toolTip.append("Max Impulse: ").append(stats[i++]).append("<br>");
            double maxGroundHeight = stats[i++];
            if (!Double.isInfinite(maxGroundHeight)) {
                toolTip.append("Min ground height: ").append(maxGroundHeight).append("<br>");
                toolTip.append("Max ground height: ").append(stats[i++]).append("<br>");
            }
            toolTip.append("West: ").append(fenv.getMinX()).append("<br>");
            toolTip.append("East: ").append(fenv.getMaxX()).append("<br>");
            toolTip.append("South: ").append(fenv.getMinY()).append("<br>");
            toolTip.append("North: ").append(fenv.getMaxY()).append("<br>");
            toolTip.append("Width: ").append(fenv.getWidth()).append("<br>");
            toolTip.append("Height: ").append(fenv.getHeight()).append("<br>");
            toolTip.append("</html>");
            _previewImageLabel.setToolTipText(toolTip.toString());

        } catch (Exception e1) {
            GuiUtilities.handleError(_boundsLoadButton, e1);
        }
    }

    private void drawWithMouseBounds() {
        Double[] propBounds = constraints.checkBounds();
        BufferedImage withMouseImage = null;
        if (propBounds[0] != null && propBounds[1] != null && propBounds[2] != null && propBounds[3] != null) {
            withMouseImage = new BufferedImage(lastDrawnImage.getWidth(), lastDrawnImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Point2D llFromPoint = new Point2D.Double(propBounds[0], propBounds[2]);
            Point2D urFromPoint = new Point2D.Double(propBounds[1], propBounds[3]);
            Point2D ll = new Point2D.Double();
            Point2D ur = new Point2D.Double();
            worldToPixel.transform(llFromPoint, ll);
            worldToPixel.transform(urFromPoint, ur);

            Graphics2D g2d = (Graphics2D) withMouseImage.getGraphics();

            g2d.drawImage(lastDrawnImage, 0, 0, null);
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(3));

            int x = (int) ll.getX();
            int y = (int) ll.getY();
            int width = (int) (ur.getX() - ll.getX());
            int height = (int) (ll.getY() - ur.getY());
            g2d.drawRect(x, y - height, width, height);
            g2d.dispose();

        } else {
            withMouseImage = lastDrawnImage;
        }
        _previewImageLabel.setIcon(new ImageIcon(withMouseImage));
    }

    public static List<Geometry> findCircles( GridCoverage2D inRaster, int pixelsThreshold ) throws Exception {
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RandomIter rasterIter = CoverageUtilities.getRandomIterator(inRaster);
        WritableRaster[] holder = new WritableRaster[1];
        GridCoverage2D outGC = CoverageUtilities.createCoverageFromTemplate(inRaster, HMConstants.doubleNovalue, holder);
        WritableRandomIter outIter = RandomIterFactory.createWritable(holder[0], null);

        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                double value = rasterIter.getSampleDouble(c, r, 0);
                if (value != 255) {
                    outIter.setSample(c, r, 0, 1);
                } else {
                    outIter.setSample(c, r, 0, -9999.0);
                }
            }
        }

        OmsVectorizer v = new OmsVectorizer();
        v.inRaster = outGC;
        v.doRemoveHoles = false;
        v.pThres = pixelsThreshold;
        v.pValue = 1.0;
        v.process();
        SimpleFeatureCollection outVector = v.outVector;

        List<SimpleFeature> featuresList = FeatureUtilities.featureCollectionToList(outVector).stream().filter(f -> {
            Object attribute = f.getAttribute(v.fDefault);
            if (attribute instanceof Number) {
                double value = ((Number) attribute).doubleValue();
                if (value != -9999.0) {
                    Geometry geom = (Geometry) f.getDefaultGeometry();

                    // assume no centroid intersection
                    Point centroid = geom.getCentroid();
                    Envelope env = geom.getEnvelopeInternal();
                    double buffer = env.getWidth() * 0.01;
                    Geometry centroidB = centroid.buffer(buffer);
                    if (geom.intersects(centroidB)) {
                        return false;
                    }
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());

//        DefaultFeatureCollection fc = new DefaultFeatureCollection();
//        fc.addAll(featuresList);
//        HMMapframe mf = HMMapframe.openFrame(false);
//        mf.addLayer(fc);

        List<Geometry> geomsList = featuresList.stream().map(f -> {
            Geometry geom = (Geometry) f.getDefaultGeometry();
            Envelope env = geom.getEnvelopeInternal();
            Coordinate centre = env.centre();
            Point centerPoint = GeometryUtilities.gf().createPoint(centre);
            double width = env.getWidth();
            double height = env.getHeight();
            double radius = Math.max(width, height) / 2.0;

            Geometry finalBuffer = centerPoint.buffer(radius);
            return finalBuffer;
        }).collect(Collectors.toList());
        return geomsList;
    }
}
