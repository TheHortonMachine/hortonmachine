package org.hortonmachine.lidar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import javax.swing.table.DefaultTableModel;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.database.DatabaseViewer;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.Las;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasConstraints;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.utils.TransformationUtils;
import org.hortonmachine.gears.utils.colors.ColorInterpolator;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.monitor.ActionWithProgress;
import org.hortonmachine.gui.utils.monitor.ProgressMonitor;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Envelope;

public class LasInfoController extends LasInfoView implements IOnCloseListener, KeyListener {
    private ALasReader lasReader;

    private LasConstraints constraints = new LasConstraints();

    public LasInfoController() {
        setPreferredSize(new Dimension(1400, 850));

        init();
    }
    private void init() {
        GuiUtilities.setFileBrowsingOnWidgets(_inputPathField, _loadButton, new String[]{"las", "laz"}, () -> loadNewFile());

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

        _elevationRadio.setSelected(true);

        _outputSaveButton.addActionListener(e -> {
            File saveFile = GuiUtilities.showSaveFileDialog(this, "Save to geotiff", GuiUtilities.getLastFile());
            if (saveFile != null) {
                _outputFileField.setText(saveFile.getAbsolutePath());
            }
        });

        updateLoadPreviewAction();
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
        ActionWithProgress uploadAction = new ActionWithProgress(this, "Filtering data and loading preview... ", work, false){
            private List<LasRecord> filteredPoints;
            private String errorMessage;
            @Override
            public void onError( Exception e ) {
                JOptionPane.showMessageDialog(LasInfoController.this, "An error occurred: " + e.getMessage(), "ERROR",
                        JOptionPane.ERROR_MESSAGE);
            }
            @Override
            public void backGroundWork( ProgressMonitor monitor ) {
                try {
                    constraints.applyConstraints(lasReader, monitor);
                    filteredPoints = constraints.getFilteredPoints();
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

                    String text = _pointSizeField.getText();
                    int pointSize = 1;
                    try {
                        pointSize = Integer.parseInt(text.trim());
                    } catch (Exception e) {
                        // ignore
                    }
                    int fpointSize = pointSize;

                    int imageWidth = 500;
                    int imageHeight = 500;
                    Envelope filteredEnvelope = constraints.getFilteredEnvelope();
                    if (filteredEnvelope.getWidth() > filteredEnvelope.getHeight()) {
                        Envelope scaledToWidth = TransformationUtils.scaleToWidth(filteredEnvelope, imageWidth);
                        imageHeight = (int) scaledToWidth.getHeight();
                    } else {
                        Envelope scaledToHeight = TransformationUtils.scaleToHeight(filteredEnvelope, imageHeight);
                        imageWidth = (int) scaledToHeight.getWidth();
                    }

                    BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = (Graphics2D) image.getGraphics();
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, imageWidth, imageHeight);
                    g2d.setColor(Color.RED);

                    AffineTransform worldToPixel = TransformationUtils.getWorldToPixel(filteredEnvelope,
                            new Rectangle(0, 0, imageWidth, imageHeight));
                    ColorInterpolator fcolorInterp = colorInterp;
                    Point2D toPoint = new Point2D.Double();
                    Point2D fromPoint = new Point2D.Double();
                    filteredPoints.stream().forEach(lr -> {
                        fromPoint.setLocation(lr.x, lr.y);
                        worldToPixel.transform(fromPoint, toPoint);

                        int xPix = (int) toPoint.getX();
                        int yPix = (int) toPoint.getY();

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
                                c = fcolorInterp.getColorFor(lr.z);
                            }
                        }
                        g2d.setColor(c);

                        if (fpointSize == 1) {
                            g2d.drawLine(xPix, yPix, xPix, yPix);
                        } else {
                            g2d.fillOval(xPix - fpointSize / 2, yPix - fpointSize / 2, fpointSize, fpointSize);
                        }
                    });

                    g2d.dispose();
                    _previewImageLabel.setIcon(new ImageIcon(image));

                    double[] stats = constraints.getStats();
                    StringBuilder toolTip = new StringBuilder("<html>");
                    int i = 0;
                    toolTip.append("Filtered data stats ").append("<br>");
                    toolTip.append("\tMin Elevation: ").append(stats[i++]).append("<br>");
                    toolTip.append("Max Elevation: ").append(stats[i++]).append("<br>");
                    toolTip.append("Min Intensity: ").append(stats[i++]).append("<br>");
                    toolTip.append("Max Intensity: ").append(stats[i++]).append("<br>");
                    toolTip.append("Min Classification: ").append(stats[i++]).append("<br>");
                    toolTip.append("Max Classification: ").append(stats[i++]).append("<br>");
                    toolTip.append("Min Impulse: ").append(stats[i++]).append("<br>");
                    toolTip.append("Max Impulse: ").append(stats[i++]).append("<br>");
                    toolTip.append("</html>");
                    _previewImageLabel.setToolTipText(toolTip.toString());

                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } finally {
                    try {
                        lasReader.rewind();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        _loadPreviewButton.setAction(uploadAction);
        _loadPreviewButton.setText("Load Preview");

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
            lasReader = Las.getReader(new File(inputPath));
            ILasHeader header = lasReader.getHeader();

            List<String[]> headerInfoList = getHeaderInfo(header);
            _headerTable
                    .setModel(new DefaultTableModel(headerInfoList.toArray(new String[0][0]), new String[]{"Property", "Value"}));
            List<String[]> firstPointInfoList = getFirstPointInfo();
            _firstPointTable.setModel(
                    new DefaultTableModel(firstPointInfoList.toArray(new String[0][0]), new String[]{"Property", "Value"}));

            updateLoadPreviewAction();
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
        }
    }

}
