/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gui.utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.images.ImageUtilities;

/**
 * A quick image viewer component.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("serial")
public class ImageViewer extends JComponent {
    private BufferedImage image = null;
    private BufferedImage originalImage;
    private int imageWidth;
    private int imageHeight;
    private int currentAngle = 0;

    public ImageViewer( BufferedImage image ) {
        this.image = image;
        this.originalImage = image;
        imageHeight = image.getHeight();
        imageWidth = image.getWidth();

        setPreferredSize(new Dimension(imageWidth, imageHeight));
        addContextMenu();
    }

    @Override
    public void paintComponent( Graphics g ) {
        int newHeight = this.getHeight();
        int newWidth = (imageWidth * newHeight) / imageHeight;
        g.drawImage(image, getWidth() / 2 - newWidth / 2, getHeight() / 2 - newHeight / 2, newWidth, newHeight, null);
    }

    private void addContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(new BevelBorder(BevelBorder.RAISED));
        popupMenu.addPopupMenuListener(new PopupMenuListener(){

            @Override
            public void popupMenuWillBecomeVisible( PopupMenuEvent e ) {
                JMenuItem item = new JMenuItem(new AbstractAction("Rotate clockwise"){
                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        currentAngle += 90;
                        if (currentAngle == 360) {
                            currentAngle = 0;
                        }
                        image = ImageUtilities.rotateImageByDegrees(originalImage, currentAngle);
                        imageWidth = image.getWidth();
                        imageHeight = image.getHeight();
                        repaint();
                    }
                });
                item.setHorizontalTextPosition(JMenuItem.RIGHT);
                popupMenu.add(item);
                JMenuItem item1 = new JMenuItem(new AbstractAction("Rotate counter clockwise"){
                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        currentAngle -= 90;
                        if (currentAngle == 0) {
                            currentAngle = 360;
                        }
                        image = ImageUtilities.rotateImageByDegrees(originalImage, currentAngle);
                        imageWidth = image.getWidth();
                        imageHeight = image.getHeight();
                        repaint();
                    }
                });
                item1.setHorizontalTextPosition(JMenuItem.RIGHT);
                popupMenu.add(item1);
                JMenuItem item2 = new JMenuItem(new AbstractAction("Save image"){
                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        File saveFile = GuiUtilities.showSaveFileDialog(popupMenu, "Save image", PreferencesHandler.getLastFile());
                        String name = saveFile.getName().toLowerCase();
                        String format = "png";
                        if (name.endsWith("jpg") || name.endsWith("jpeg")) {
                            format = "jpg";
                        }

                        try {
                            ImageIO.write(image, format, saveFile);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                            GuiUtilities.showErrorMessage(popupMenu, e1.getLocalizedMessage());
                        }

                    }
                });
                item2.setHorizontalTextPosition(JMenuItem.RIGHT);
                popupMenu.add(item2);
            }

            @Override
            public void popupMenuWillBecomeInvisible( PopupMenuEvent e ) {
                popupMenu.removeAll();
            }

            @Override
            public void popupMenuCanceled( PopupMenuEvent e ) {
                popupMenu.removeAll();
            }
        });

        addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked( MouseEvent e ) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }

            }
        });
    }

    /**
     * Opens a JDialog with the image viewer in it.
     * 
     * @param image the image to show.
     * @param title the title of the dialog.
     * @param modal if <code>true</code>, the dialog is modal.
     */
    public static void show( BufferedImage image, String title, boolean modal ) {
        JDialog f = new JDialog();
        f.add(new ImageViewer(image), BorderLayout.CENTER);
        f.setTitle(title);
        f.setIconImage(ImageCache.getInstance().getBufferedImage(ImageCache.HORTONMACHINE_FRAME_ICON));
        f.setModal(modal);
        f.pack();
        int h = image.getHeight();
        int w = image.getWidth();
        if (h > w) {
            f.setSize(new Dimension(600, 800));
        } else {
            f.setSize(new Dimension(800, 600));
        }
        f.setLocationRelativeTo(null); // Center on screen
        f.setVisible(true);
        f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        f.getRootPane().registerKeyboardAction(e -> {
            f.dispose();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}