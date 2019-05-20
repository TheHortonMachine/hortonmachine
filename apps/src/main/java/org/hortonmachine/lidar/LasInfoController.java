package org.hortonmachine.lidar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
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
import org.hortonmachine.gears.utils.BitMatrix;
import org.hortonmachine.gears.utils.TransformationUtils;
import org.hortonmachine.gears.utils.colors.ColorInterpolator;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.monitor.ActionWithProgress;
import org.hortonmachine.gui.utils.monitor.ProgressMonitor;
import org.joda.time.DateTime;
import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;

public class LasInfoController extends LasInfoView implements IOnCloseListener, KeyListener {
    private static final String NO_CRS_MSG = "No CRS is available for the dataset. Please add a proper prj file.";

    private ALasReader lasReader;

    private LasConstraints constraints = new LasConstraints();

    private GridCoverage2D dtm;
    private AffineTransform worldToPixel;
    private AffineTransform pixelToWorld;

    private BufferedImage lastDrawnImage;

    public LasInfoController() {
        setPreferredSize(new Dimension(1800, 1000));

        init();
    }
    private void init() {
        _inputPathField.setEditable(false);
        GuiUtilities.setFileBrowsingOnWidgets(_inputPathField, _loadButton, new String[]{"las", "laz"}, () -> loadNewFile());
        _dtmInputPathField.setEditable(false);
        GuiUtilities.setFileBrowsingOnWidgets(_dtmInputPathField, _loadDtmButton, new String[]{"asc", "tiff"}, () -> loadDtm());

        _boundsFileField.setEditable(false);
        GuiUtilities.setFileBrowsingOnWidgets(_boundsFileField, _boundsLoadButton, new String[]{"shp"},
                () -> loadBoundsFromFile());

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

        _outputFileField.setEditable(false);
        _outputSaveButton.addActionListener(e -> {
            File saveFile = GuiUtilities.showSaveFileDialog(this, "Save to file", GuiUtilities.getLastFile());
            if (saveFile != null) {
                _outputFileField.setText(saveFile.getAbsolutePath());
                updateExportAction();
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
                        drawPreview();
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
                            _boundsFileField.setText("");
                        }
                        Double[] propBounds = constraints.checkBounds();
                        _westField.setText(propBounds[0] != null ? propBounds[0].toString() : "");
                        _eastField.setText(propBounds[1] != null ? propBounds[1].toString() : "");
                        _southField.setText(propBounds[2] != null ? propBounds[2].toString() : "");
                        _northField.setText(propBounds[3] != null ? propBounds[3].toString() : "");
                        drawWithMouseMouse();
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

        updateLoadPreviewAction();
        updateLoadDataAction();
        updateExportAction();

        _load3DButton.addActionListener(e -> {
            AbstractAnalysis abstractAnalysis = new AbstractAnalysis(){
                @Override
                public void init() throws Exception {
                    constraints.applyConstraints(lasReader, false, null);
                    List<LasRecord> filteredPoints = constraints.getFilteredPoints();
                    List<Coord3d> points = new ArrayList<>();
                    List<org.jzy3d.colors.Color> colors = new ArrayList<>();

                    for( LasRecord dot : filteredPoints ) {
                        float x = (float) dot.x;
                        float y = (float) dot.y;
                        float z = (float) dot.z;
                        if (!Double.isNaN(dot.groundElevation)) {
                            z = (float) dot.groundElevation;
                        }
                        points.add(new Coord3d(x, y, z));
                        short[] c = dot.color;
                        colors.add(new org.jzy3d.colors.Color((int) c[0], (int) c[1], (int) c[2]));
                    }

                    Coord3d[] coord3ds = points.toArray(new Coord3d[points.size()]);
                    org.jzy3d.colors.Color[] colorsArray = colors.toArray(new org.jzy3d.colors.Color[colors.size()]);
                    Scatter scatterLas = new Scatter(coord3ds, colorsArray, 7f);
                    String text = _pointSizeField.getText();
                    int pointSize = 1;
                    try {
                        pointSize = Integer.parseInt(text.trim());
                    } catch (Exception e) {
                        // ignore
                    }
                    scatterLas.setWidth(pointSize);

                    chart = AWTChartComponentFactory.chart(Quality.Fastest, "newt");
                    chart.getScene().getGraph().add(scatterLas);
                    chart.setAxeDisplayed(false);
                    chart.getView().setSquared(false);

                }
            };
            try {
                AnalysisLauncher.open(abstractAnalysis);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
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
            e.printStackTrace();
        }
    }

    @SuppressWarnings("serial")
    private void updateLoadPreviewAction() {
        if (lasReader == null) {
            _loadPreviewButton.setEnabled(false);
            return;
        } else {
            _loadPreviewButton.setEnabled(true);
        }
        ILasHeader header = lasReader.getHeader();
        long recordsCount = header.getRecordsCount();
        int work = (int) (recordsCount / 1000);
        ActionWithProgress previewAction = new ActionWithProgress(this, "Drawing data... ", work, false){
            private String errorMessage;

            @Override
            public void onError( Exception e ) {
                JOptionPane.showMessageDialog(LasInfoController.this, "An error occurred: " + e.getMessage(), "ERROR",
                        JOptionPane.ERROR_MESSAGE);
            }
            @Override
            public void backGroundWork( ProgressMonitor monitor ) {
                try {
                    constraints.applyConstraints(lasReader, false, monitor);
                    monitor.done();
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    if (errorMessage == null) {
                        errorMessage = "An undefined error was thrown.";
                        errorMessage += ExceptionUtils.getStackTrace(e);
                    }
                }
            }
            @Override
            public void postWork() throws Exception {
                if (errorMessage != null) {
                    JOptionPane.showMessageDialog(LasInfoController.this, errorMessage, "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                drawPreview();
            }
        };

        _loadPreviewButton.setAction(previewAction);
        _loadPreviewButton.setText("Draw Data");

    }

    private void updateLoadDataAction() {
        if (lasReader == null) {
            _loadDataButton.setEnabled(false);
            return;
        } else {
            _loadDataButton.setEnabled(true);
        }
        ILasHeader header = lasReader.getHeader();
        long recordsCount = header.getRecordsCount();
        int work = (int) (recordsCount / 1000);
        ActionWithProgress previewAction = new ActionWithProgress(this, "Loading data using filters... ", work, false){
            private String errorMessage;

            @Override
            public void onError( Exception e ) {
                JOptionPane.showMessageDialog(LasInfoController.this, "An error occurred: " + e.getMessage(), "ERROR",
                        JOptionPane.ERROR_MESSAGE);
            }
            @Override
            public void backGroundWork( ProgressMonitor monitor ) {
                try {
                    constraints.applyConstraints(lasReader, true, monitor);
                    monitor.done();
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    if (errorMessage == null) {
                        errorMessage = "An undefined error was thrown.";
                        errorMessage += ExceptionUtils.getStackTrace(e);
                    }
                }
            }
            @Override
            public void postWork() throws Exception {
                if (errorMessage != null) {
                    JOptionPane.showMessageDialog(LasInfoController.this, errorMessage, "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        };

        _loadDataButton.setAction(previewAction);
        _loadDataButton.setText("Load Data");

    }

    @SuppressWarnings("serial")
    private void updateExportAction() {
        if (lasReader == null) {
            _convertButton.setEnabled(false);
            _createOverviewButton.setEnabled(false);
            return;
        } else {
            _convertButton.setEnabled(true);
            _createOverviewButton.setEnabled(true);
        }
        String outputFilePath = _outputFileField.getText();
        if (outputFilePath.trim().length() == 0) {
            return;
        }
        if (!outputFilePath.toLowerCase().endsWith(".las") && !outputFilePath.toLowerCase().endsWith(".shp")) {
            JOptionPane.showMessageDialog(LasInfoController.this, "Only conversion to las and shp is supported.", "ERROR",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        ILasHeader header = lasReader.getHeader();
        long recordsCount = header.getRecordsCount();
        int work = (int) (recordsCount / 1000);
        ActionWithProgress exportAction = new ActionWithProgress(this, "Exporting data... ", work * 2, false){
            private String errorMessage;

            @Override
            public void onError( Exception e ) {
                GuiUtilities.showErrorMessage(parent, "An error occurred: " + e.getMessage());
            }
            @Override
            public void backGroundWork( ProgressMonitor monitor ) {
                try {
                    constraints.applyConstraints(lasReader, true, monitor);
                    List<LasRecord> filteredPoints = constraints.getFilteredPoints();

                    monitor.setCurrent("Now writing to file...", work + work / 2);
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
                    monitor.done();
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    if (errorMessage == null) {
                        errorMessage = "An undefined error was thrown.";
                        errorMessage += ExceptionUtils.getStackTrace(e);
                    }
                }
            }
            @Override
            public void postWork() throws Exception {
                if (errorMessage != null) {
                    GuiUtilities.showErrorMessage(parent, errorMessage);
                }
            }
        };

        ActionWithProgress createOverviewAction = new ActionWithProgress(this, "Exporting data... ", 2, false){
            private String errorMessage;

            @Override
            public void onError( Exception e ) {
                GuiUtilities.showErrorMessage(parent, "An error occurred: " + e.getMessage());
            }
            @Override
            public void backGroundWork( ProgressMonitor monitor ) {
                try {
                    if (header.getCrs() == null) {
                        throw new Exception(NO_CRS_MSG);
                    }

                    constraints.applyConstraints(lasReader, true, monitor);
                    monitor.setCurrent("Getting bounds...", 0);
                    Envelope filteredEnvelope = constraints.getFilteredEnvelope();
                    Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(filteredEnvelope);
                    polygon.setUserData("Overview for " + lasReader.getLasFile().getName());
                    SimpleFeatureCollection fc = FeatureUtilities.featureCollectionFromGeometry(header.getCrs(), polygon);

                    monitor.setCurrent("Writing to file...", 12);
                    String f = outputFilePath;
                    if (!f.toLowerCase().endsWith(".shp"))
                        f = f + ".shp";
                    OmsVectorWriter.writeVector(f, fc);

                    monitor.done();
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    if (errorMessage == null) {
                        errorMessage = "An undefined error was thrown.";
                        errorMessage += ExceptionUtils.getStackTrace(e);
                    }
                }
            }
            @Override
            public void postWork() throws Exception {
                if (errorMessage != null) {
                    GuiUtilities.showErrorMessage(parent, errorMessage);
                }
            }
        };

        _convertButton.setAction(exportAction);
        _convertButton.setText("Convert");
        _createOverviewButton.setAction(createOverviewAction);
        _createOverviewButton.setText("Create overview shp");

    }

    private void loadBoundsFromFile() {
        String boundsFilePath = _boundsFileField.getText();
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
        } catch (IOException e) {
            e.printStackTrace();
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
            updateLoadPreviewAction();
            updateLoadDataAction();
            updateExportAction();
        } catch (Exception e) {
            e.printStackTrace();
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

        Class<DatabaseViewer> class1 = DatabaseViewer.class;
        ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));
        frame.setIconImage(icon.getImage());

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

    private void drawPreview() {
        try {
            List<LasRecord> filteredPoints = constraints.getFilteredPoints();
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
            filteredPoints.stream().forEach(lr -> {
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
                    lr.color[0] = (short) c.getRed();
                    lr.color[1] = (short) c.getGreen();
                    lr.color[2] = (short) c.getBlue();

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

            drawWithMouseMouse();
//            _previewImageLabel.setIcon(new ImageIcon(lastDrawnImage));

            double[] stats = constraints.getStats();
            Envelope fenv = constraints.getFilteredEnvelope();
            StringBuilder toolTip = new StringBuilder("<html>");
            int i = 0;
            toolTip.append("Filtered data stats ").append("<br>");
            toolTip.append("Points count: ").append(filteredPoints.size()).append("<br>");
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
            toolTip.append("</html>");
            _previewImageLabel.setToolTipText(toolTip.toString());

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void drawWithMouseMouse() {
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
            g2d.setColor(Color.BLACK);

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
}
