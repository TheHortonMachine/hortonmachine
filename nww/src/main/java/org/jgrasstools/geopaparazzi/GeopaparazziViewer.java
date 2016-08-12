/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.geopaparazzi;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;

import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoGpsLog.GpsLog;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoImages;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.Image;
import org.jgrasstools.gui.utils.DefaultGuiBridgeImpl;
import org.jgrasstools.gui.utils.GuiBridgeHandler;
import org.jgrasstools.gui.utils.GuiUtilities;
import org.jgrasstools.spatialite.SqlTemplatesAndActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The spatialtoolbox view controller.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GeopaparazziViewer extends GeopaparazziController {
    private static final Logger logger = LoggerFactory.getLogger(GeopaparazziController.class);
    private static final long serialVersionUID = 1L;

    private static final int MAX_IMAGE_SIZE = 800;

    public GeopaparazziViewer( GuiBridgeHandler guiBridge ) {
        super(guiBridge);
    }

    private void openImageInDialog( long imageId, String imageName, File dbFile, boolean doOriginalSize ) throws Exception {
        BufferedImage bufferedImage = readImageToBufferedImage(imageId, dbFile, doOriginalSize);

        if (bufferedImage != null) {
            JDialog f = new JDialog();
            f.add(new JLabel(new ImageIcon(bufferedImage)), BorderLayout.CENTER);
            f.setTitle(imageName);
            f.pack();
            f.setSize(bufferedImage.getWidth(), bufferedImage.getHeight());
            f.setLocationRelativeTo(null); // Center on screen
            f.setVisible(true);
            f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        }
    }

    private BufferedImage readImageToBufferedImage( long imageId, File dbFile, boolean doOriginalSize )
            throws Exception, SQLException {
        BufferedImage bufferedImage = null;
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
            byte[] imageData = DaoImages.getImageData(connection, imageId);
            InputStream imageStream = null;
            try {
                imageStream = new ByteArrayInputStream(imageData);
                bufferedImage = createImage(imageStream, doOriginalSize);

            } catch (Exception e) {
                logger.error("error", e);
            }
        }
        return bufferedImage;
    }

    private static BufferedImage createImage( InputStream inputStream, boolean doOriginalSize ) throws Exception {
        BufferedImage image = ImageIO.read(inputStream);

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int width = imageWidth;
        int height = imageHeight;
        if (imageWidth > imageHeight) {
            if (width > MAX_IMAGE_SIZE && !doOriginalSize)
                width = MAX_IMAGE_SIZE;
            height = imageHeight * width / imageWidth;
        } else {
            if (height > MAX_IMAGE_SIZE && !doOriginalSize)
                height = MAX_IMAGE_SIZE;
            width = height * imageWidth / imageHeight;
        }

        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();

        return resizedImage;
    }

    @Override
    protected List<Action> makeGpsLogActions( GpsLog selectedLog ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<Action> makeProjectAction( ProjectInfo project ) {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("serial")
    @Override
    protected List<Action> makeImageAction( Image selectedImage ) {

        List<Action> actions = new ArrayList<>();
        actions.add(new AbstractAction("Open Image"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    openImageInDialog(selectedImage.getId(), selectedImage.getName(), currentSelectedProject.databaseFile, false);
                } catch (Exception ex) {
                    logger.error("Error", ex);
                }
            }
        });
        actions.add(new AbstractAction("Save Image"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    File[] folderFiles = guiBridge.showOpenDirectoryDialog("Save Image", GuiUtilities.getLastFile());
                    if (folderFiles != null && folderFiles.length > 0) {
                        File folderFile = folderFiles[0];
                        File imgFile = new File(folderFile, selectedImage.getName());
                        BufferedImage bufferedImage = readImageToBufferedImage(selectedImage.getId(),
                                currentSelectedProject.databaseFile, true);
                        ImageIO.write(bufferedImage, "jpg", imgFile);
                    }

                } catch (Exception ex) {
                    logger.error("Error", ex);
                }
            }
        });

        return actions;

    }

    public static void main( String[] args ) throws Exception {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        final GeopaparazziViewer controller = new GeopaparazziViewer(gBridge);
        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "Geopaparazzi Projects Viewer");

        Class<GeopaparazziViewer> class1 = GeopaparazziViewer.class;
        ImageIcon icon = new ImageIcon(class1.getResource("/org/jgrasstools/images/geopaparazzi_icon.png"));
        frame.setIconImage(icon.getImage());

        GuiUtilities.addClosingListener(frame, controller);
    }
}
