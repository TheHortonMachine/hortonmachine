//package org.hortonmachine.gps;
//
//import static java.lang.Math.atan;
//import static java.lang.Math.toDegrees;
//
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.Dimension;
//import java.io.File;
//import java.util.Arrays;
//import java.util.List;
//
//import javax.swing.DefaultComboBoxModel;
//import javax.swing.ImageIcon;
//import javax.swing.JComponent;
//import javax.swing.JFrame;
//import javax.swing.SwingUtilities;
//
//import org.hortonmachine.database.DatabaseViewer;
//import org.hortonmachine.gears.libs.modules.HMConstants;
//import org.hortonmachine.gps.nmea.ANmeaGps;
//import org.hortonmachine.gps.nmea.FakeNmeaGps;
//import org.hortonmachine.gps.nmea.NmeaGpsListener;
//import org.hortonmachine.gps.nmea.SerialNmeaGps;
//import org.hortonmachine.gps.utils.CurrentGpsInfo;
//import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
//import org.hortonmachine.gui.utils.GuiUtilities;
//import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
//import org.hortonmachine.nww.gui.LayersPanelController;
//import org.hortonmachine.nww.gui.NwwPanel;
//import org.hortonmachine.nww.layers.defaults.other.CurrentGpsPointLayer;
//import org.joda.time.DateTime;
//import org.locationtech.jts.geom.Coordinate;
//
//import gov.nasa.worldwind.WorldWindow;
//import net.sf.marineapi.nmea.util.Date;
//import net.sf.marineapi.nmea.util.GpsFixQuality;
//import net.sf.marineapi.nmea.util.Position;
//import net.sf.marineapi.nmea.util.SatelliteInfo;
//import net.sf.marineapi.nmea.util.Time;
//
//public class GpsInfoController extends GpsInfoView implements NmeaGpsListener, IOnCloseListener {
//    private static final String LAST_USED_GPSPORT = "LAST_USED_GPSPORT";
//    private NwwPanel wwjPanel;
//    private CurrentGpsPointLayer gpsPointLayer = new CurrentGpsPointLayer("GPS position", Color.RED, null, null, 20.0);
//
//    private ANmeaGps gps = null;
//    private boolean useFakeGps;
//    private Coordinate previousCoordinate;
//
//    private boolean isFirstEvent = true;
//    private File logFile;
//
//    /**
//     * Default constructor
//     */
//    @SuppressWarnings({"serial", "unchecked"})
//    public GpsInfoController() {
//        setPreferredSize(new Dimension(1200, 800));
//
//        String lastUsedPort = GuiUtilities.getPreference(LAST_USED_GPSPORT, (String) null);
//        String[] availablePorts = SerialNmeaGps.getAvailablePortNames();
//
//        _portsCombo.setModel(new DefaultComboBoxModel<>(availablePorts));
//        if (lastUsedPort != null)
//            _portsCombo.setSelectedItem(lastUsedPort);
//
//        boolean doRecordNmea = _recordNmeaCheck.isSelected();
//        _recordNmeaCheck.addActionListener(e -> {
//            boolean doRecordNmeaTmp = _recordNmeaCheck.isSelected();
//            if (doRecordNmeaTmp) {
//                enableLogFile();
//            } else {
//                logFile = null;
//            }
//        });
//        if (doRecordNmea) {
//            enableLogFile();
//        }
//
//        useFakeGps = _useFakeGpsCheck.isSelected();
//        _useFakeGpsCheck.addActionListener(e -> {
//            useFakeGps = _useFakeGpsCheck.isSelected();
//            _portsCombo.setEnabled(!useFakeGps);
//            _gpsConnectButton.setEnabled(!useFakeGps);
//            if (useFakeGps) {
//                wwjPanel.addLayer(gpsPointLayer);
//                new Thread(new Runnable(){
//                    public void run() {
//                        gps = new FakeNmeaGps(null);
//                        gps.addListener(GpsInfoController.this);
//                        try {
//                            gps.start();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }, "GpsInfoController -> Fake GPS thread").start();
//            } else {
//                if (gps != null) {
//                    try {
//                        wwjPanel.removeLayer(gpsPointLayer);
//                        gps.stop();
//                        gps = null;
//                        resetTextfields();
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
//            }
//        });
//
//        _gpsConnectButton.addActionListener(e -> {
//            if (_gpsConnectButton.isSelected()) {
//                String port = _portsCombo.getSelectedItem().toString();
//
//                GuiUtilities.setPreference(LAST_USED_GPSPORT, port);
//                _gpsConnectButton.setText("disconnect");
//                wwjPanel.addLayer(gpsPointLayer);
//                new Thread(new Runnable(){
//                    public void run() {
//                        gps = new SerialNmeaGps(port);
//                        try {
//                            ((SerialNmeaGps) gps).logToFile(logFile);
//                            gps.addListener(GpsInfoController.this);
//                            gps.start();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }, "GpsInfoController -> GPS thread").start();
//            } else {
//                if (gps != null) {
//                    try {
//                        wwjPanel.removeLayer(gpsPointLayer);
//                        gps.stop();
//                        gps = null;
//                        resetTextfields();
//                        _gpsConnectButton.setText("connect");
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
//            }
//        });
//
//        wwjPanel = (NwwPanel) NwwPanel.createNwwPanel(false, true, false);
//        _mapPanel.setLayout(new BorderLayout());
//        _mapPanel.add(wwjPanel, BorderLayout.CENTER);
//
//        LayersPanelController lp = new LayersPanelController(wwjPanel);
//        _layersPanel.setLayout(new BorderLayout());
//        _layersPanel.add(lp, BorderLayout.CENTER);
//
//    }
//
//    private void enableLogFile() {
//        String userHome = System.getProperty("user.home");
//        String ts = DateTime.now().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSScompact);
//        logFile = new File(userHome + File.separator + ts + "_hortonmachine.nmea");
//    }
//
//    @Override
//    public void onGpsEvent( CurrentGpsInfo gpsInfo ) {
//        SwingUtilities.invokeLater(new Runnable(){
//            public void run() {
//                _pointCountText.setText(gpsInfo.getCount() + "");
//                _gpsFixStatusText.setText(gpsInfo.getGpsFixStatus().name());
//                GpsFixQuality gpsFixQuality = gpsInfo.getGpsFixQuality();
//                if (gpsFixQuality != null)
//                    _gpsFixQualityText.setText(gpsFixQuality.name());
//                _dataStatusText.setText(gpsInfo.getDataStatus().name());
//                _faaModeText.setText(gpsInfo.getFaaMode().name());
//                _hdopText.setText("" + gpsInfo.getHorizontalPrecision());
//                _vdopText.setText("" + gpsInfo.getVerticalPrecision());
//                _pdopText.setText("" + gpsInfo.getPositionPrecision());
//                Date date = gpsInfo.getDate();
//                Time time = gpsInfo.getTime();
//                if (date != null && time != null)
//                    _tsText.setText(date.toISO8601(time));
//                _speedText.setText(gpsInfo.getKmHSpeed() + "");
//
//                String[] satelliteIds = gpsInfo.getSatelliteIds();
//                if (satelliteIds != null) {
//                    _satCountText.setText(satelliteIds.length + "");
//                    _satIdsText.setText(Arrays.toString(satelliteIds));
//                }
//                List<SatelliteInfo> satelliteInfo = gpsInfo.getSatelliteInfo();
//                if (satelliteInfo != null) {
//                    _satInfoCountText.setText(satelliteInfo.size() + "");
//                }
//
//                if (gpsInfo.isHmGpsInfo()) {
//                    double humidityPerc = gpsInfo.getHumidityPerc();
//                    _humidityText.setText(humidityPerc + "");
//                    double tempC = gpsInfo.getTempC();
//                    _temperatureText.setText(tempC + "");
//                }
//
//                Position position = gpsInfo.getPosition();
//                if (position != null) {
//                    double latitude = position.getLatitude();
//                    double longitude = position.getLongitude();
//                    Coordinate current = new Coordinate(longitude, latitude);
//                    _latText.setText(latitude + " " + position.getLatitudeHemisphere());
//                    _lonText.setText(longitude + " " + position.getLongitudeHemisphere());
//                    gpsPointLayer.updatePosition(latitude, longitude);
//
//                    if (_followGPSCheck.isSelected()) {
//                        Double azimuth = null;
//                        if (previousCoordinate != null) {
//                            double azimuthTmp = azimuth(previousCoordinate, current);
//                            if (!Double.isNaN(azimuthTmp)) {
//                                azimuth = azimuthTmp;
//                            }
//
//                        }
//
//                        double elev = 1000.0;
//                        if (!isFirstEvent) {
//                            WorldWindow wwd = wwjPanel.getWwd();
//                            elev = wwd.getView().getCurrentEyePosition().getAltitude();
//                        }
//                        isFirstEvent = false;
//
//                        wwjPanel.goTo(longitude, latitude, elev, azimuth, false);
//                    }
//                }
//
//            }
//        });
//
//    }
//
//    public void resetTextfields() {
//        _pointCountText.setText("");
//        _latText.setText("");
//        _lonText.setText("");
//        _gpsFixStatusText.setText("");
//        _gpsFixQualityText.setText("");
//        _dataStatusText.setText("");
//        _faaModeText.setText("");
//        _hdopText.setText("");
//        _vdopText.setText("");
//        _pdopText.setText("");
//        _tsText.setText("");
//        _speedText.setText("");
//        _satCountText.setText("");
//        _satIdsText.setText("");
//        _satInfoCountText.setText("");
//        _humidityText.setText("");
//        _temperatureText.setText("");
//    }
//
//    private JComponent asJComponent() {
//        return this;
//    }
//
//    @Override
//    public void onClose() {
//
//    }
//
//    /**
//     * Calculates the azimuth in degrees given two {@link Coordinate} composing a
//     * line.
//     * 
//     * Note that the coords order is important and will differ of 180.
//     * 
//     * @param c1 first coordinate (used as origin).
//     * @param c2 second coordinate.
//     * @return the azimuth angle.
//     */
//    public static double azimuth( Coordinate c1, Coordinate c2 ) {
//        // vertical
//        if (c1.x == c2.x) {
//            if (c1.y == c2.y) {
//                // same point
//                return Double.NaN;
//            } else if (c1.y < c2.y) {
//                return 0.0;
//            } else if (c1.y > c2.y) {
//                return 180.0;
//            }
//        }
//        // horiz
//        if (c1.y == c2.y) {
//            if (c1.x < c2.x) {
//                return 90.0;
//            } else if (c1.x > c2.x) {
//                return 270.0;
//            }
//        }
//        // -> /
//        if (c1.x < c2.x && c1.y < c2.y) {
//            double tanA = (c2.x - c1.x) / (c2.y - c1.y);
//            double atan = atan(tanA);
//            return toDegrees(atan);
//        }
//        // -> \
//        if (c1.x < c2.x && c1.y > c2.y) {
//            double tanA = (c1.y - c2.y) / (c2.x - c1.x);
//            double atan = atan(tanA);
//            return toDegrees(atan) + 90.0;
//        }
//        // <- /
//        if (c1.x > c2.x && c1.y > c2.y) {
//            double tanA = (c1.x - c2.x) / (c1.y - c2.y);
//            double atan = atan(tanA);
//            return toDegrees(atan) + 180;
//        }
//        // <- \
//        if (c1.x > c2.x && c1.y < c2.y) {
//            double tanA = (c2.y - c1.y) / (c1.x - c2.x);
//            double atan = atan(tanA);
//            return toDegrees(atan) + 270;
//        }
//
//        return Double.NaN;
//    }
//
//    public boolean canCloseWithoutPrompt() {
//        return false;
//    }
//    
//    public static void main( String[] args ) {
//
//        GuiUtilities.setDefaultLookAndFeel();
//
//        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
//        final GpsInfoController controller = new GpsInfoController();
//        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Database Viewer");
//
//        Class<DatabaseViewer> class1 = DatabaseViewer.class;
//        ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));
//        frame.setIconImage(icon.getImage());
//
//        GuiUtilities.addClosingListener(frame, controller);
//
//    }
//
//}
