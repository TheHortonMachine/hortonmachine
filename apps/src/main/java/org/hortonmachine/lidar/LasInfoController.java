package org.hortonmachine.lidar;

import static org.hortonmachine.gears.utils.math.NumericsUtilities.dEq;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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
import javax.swing.table.DefaultTableModel;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.database.DatabaseViewer;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.Las;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.utils.TransformationUtils;
import org.hortonmachine.gears.utils.colors.ColorInterpolator;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Coordinate;

@SuppressWarnings("unchecked")
public class LasInfoController extends LasInfoView implements IOnCloseListener, KeyListener {
    private ALasReader lasReader;

    private double[] intensityRange = null;
    private int[] impulses = null;
    private int[] classes = null;
    private Integer sampling = null;

    private Double west = null;
    private Double east = null;
    private Double south = null;
    private Double north = null;
    private Double minZ = null;
    private Double maxZ = null;

    public LasInfoController() {
        setPreferredSize(new Dimension(1400, 850));

        init();
    }
    private void init() {
        GuiUtilities.setFileBrowsingOnWidgets(_inputPathField, _loadButton, new String[]{"las", "laz"}, () -> loadNewFile());

        GuiUtilities.setFileBrowsingOnWidgets(_boundsFileField, _boundsLoadButton, new String[]{"shp"},
                () -> loadBoundsFromFile());

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

        _loadPreviewButton.addActionListener(ev -> {
            drawPreview();
        });
    }
    private void drawPreview() {
        int imageWidth = 500;
        int imageHeight = 500;

        try {

            BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = (Graphics2D) image.getGraphics();

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, imageWidth, imageHeight);
            g2d.setColor(Color.RED);

            ILasHeader header = lasReader.getHeader();

            ReferencedEnvelope3D env = header.getDataEnvelope();
            boolean doSampling = false;
            int samp = -1;
            if (sampling != null) {
                doSampling = true;
                samp = sampling;
            }
            int count = 0;

            double minInt = Double.POSITIVE_INFINITY;
            double maxInt = Double.NEGATIVE_INFINITY;
            double minClass = Double.POSITIVE_INFINITY;
            double maxClass = Double.NEGATIVE_INFINITY;
            double minImpulse = Double.POSITIVE_INFINITY;
            double maxImpulse = Double.NEGATIVE_INFINITY;

            List<LasRecord> keepPoints = new ArrayList<>(1000000);
            while( lasReader.hasNextPoint() ) {
                LasRecord lasDot = lasReader.getNextPoint();
                count++;
                final double x = lasDot.x;
                final double y = lasDot.y;
                final double z = lasDot.z;
                final double intensity = lasDot.intensity;
                final int classification = lasDot.classification;
                final double impulse = lasDot.returnNumber;

                boolean takeIt = true;
                if (doSampling && (count % samp != 0)) {
                    takeIt = false;
                }
                if (takeIt && intensityRange != null) {
                    takeIt = false;
                    if (intensity >= intensityRange[0] && intensity <= intensityRange[1]) {
                        takeIt = true;
                    }
                }
                if (takeIt && intensityRange != null) {
                    takeIt = false;
                    if (intensity >= intensityRange[0] && intensity <= intensityRange[1]) {
                        takeIt = true;
                    }
                }
                if (takeIt && impulses != null) {
                    takeIt = false;
                    for( final double imp : impulses ) {
                        if (dEq(impulse, imp)) {
                            takeIt = true;
                            break;
                        }
                    }
                }
                if (takeIt && classes != null) {
                    takeIt = false;
                    for( final double classs : classes ) {
                        if (classification == (int) classs) {
                            takeIt = true;
                            break;
                        }
                    }
                }
                if (takeIt && (west != null && east != null && south != null && north != null)) {
                    takeIt = false;
                    if (x >= west && x <= east && y >= south && y <= north) {
                        takeIt = true;
                    }
                }
                if (takeIt && (minZ != null && maxZ != null)) {
                    takeIt = false;
                    if (z >= minZ && z <= maxZ) {
                        takeIt = true;
                    }
                }

                if (takeIt) {
                    minImpulse = Math.min(minImpulse, impulse);
                    maxImpulse = Math.max(maxImpulse, impulse);
                    minInt = Math.min(minInt, intensity);
                    maxInt = Math.max(maxInt, intensity);
                    minClass = Math.min(minClass, classification);
                    maxClass = Math.max(maxClass, classification);

                    keepPoints.add(lasDot);
                }
            }

            ColorInterpolator colorInterp = null;
            boolean useIntensity = _intensityRadio.isSelected();
            boolean useClass = _classRadio.isSelected();
            boolean useImpulse = _impulseRadio.isSelected();
            boolean useRGB = _ownColorRadio.isSelected();
            if (!useRGB) {
                if (useIntensity) {
                    colorInterp = new ColorInterpolator(EColorTables.rainbow.name(), minInt, maxInt, null);
                } else if (useClass) {
                    colorInterp = new ColorInterpolator(EColorTables.rainbow.name(), minClass, maxClass, null);
                } else if (useImpulse) {
                    colorInterp = new ColorInterpolator(EColorTables.rainbow.name(), minImpulse, maxImpulse, null);
                } else {
                    colorInterp = new ColorInterpolator(EColorTables.elev.name(), env.getMinZ(), env.getMaxZ(), null);
                }
            }

            AffineTransform worldToPixel = TransformationUtils.getWorldToPixel(env, new Rectangle(0, 0, imageWidth, imageHeight));

            ColorInterpolator fcolorInterp = colorInterp;
            Point2D toPoint = new Point2D.Double();
            Point2D fromPoint = new Point2D.Double();
            keepPoints.stream().forEach(lr -> {
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

                g2d.drawLine(xPix, yPix, xPix, yPix);
            });

            g2d.dispose();
            _previewImageLabel.setIcon(new ImageIcon(image));

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

    private void loadBoundsFromFile() {
        String boundsFilePath = _boundsFileField.getText();
        try {
            ReferencedEnvelope env = OmsVectorReader.readEnvelope(boundsFilePath);
            _westField.setText(String.valueOf(env.getMinX()));
            _eastField.setText(String.valueOf(env.getMaxX()));
            _southField.setText(String.valueOf(env.getMinY()));
            _northField.setText(String.valueOf(env.getMaxY()));
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
        if (source == _samplingField) {
            String text = _samplingField.getText();
            try {
                sampling = Integer.parseInt(text.trim());
                if (sampling == 0) {
                    sampling = null;
                }
            } catch (NumberFormatException e1) {
                sampling = null;
            }
        } else if (source == _classesField) {
            final String[] split = _classesField.getText().trim().split(","); //$NON-NLS-1$
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
        } else if (source == _impulsesField) {
            final String[] split = _impulsesField.getText().trim().split(","); //$NON-NLS-1$
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
        } else if (source == _intensityRangeField) {
            final String[] split = _intensityRangeField.getText().trim().split(","); //$NON-NLS-1$
            if (split.length != 2) {
                try {
                    intensityRange = new double[]{Double.parseDouble(split[0]), Double.parseDouble(split[1])};
                } catch (final Exception ex) {
                    intensityRange = null;
                }
            } else {
                intensityRange = null;
            }
        } else if (source == _westField) {
            String text = _westField.getText();
            try {
                west = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                west = null;
            }
        } else if (source == _eastField) {
            String text = _eastField.getText();
            try {
                east = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                east = null;
            }
        } else if (source == _southField) {
            String text = _southField.getText();
            try {
                south = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                south = null;
            }
        } else if (source == _northField) {
            String text = _northField.getText();
            try {
                north = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                north = null;
            }
        } else if (source == _minZField) {
            String text = _minZField.getText();
            try {
                minZ = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                minZ = null;
            }
        } else if (source == _maxZField) {
            String text = _maxZField.getText();
            try {
                maxZ = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                maxZ = null;
            }
        }
    }

}
