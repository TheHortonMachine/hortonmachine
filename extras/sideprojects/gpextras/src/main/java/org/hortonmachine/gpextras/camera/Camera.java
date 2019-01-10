package org.hortonmachine.gpextras.camera;

import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;

/**
 * Central Camera object.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Camera {

    private CameraFrame cameraFrame;

    public void open( String fileToWrite ) {
        if (fileToWrite == null || !new File(fileToWrite).getParentFile().exists()) {
            try {
                File tmpFile = File.createTempFile("gpap_", ".jpg");
                fileToWrite = tmpFile.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        cameraFrame = new CameraFrame(fileToWrite);
        SwingUtilities.invokeLater(cameraFrame);
    }

    public void addListener( CameraListener listener ) {
        cameraFrame.addListener(listener);
    }

    public void removeListener( CameraListener listener ) {
        cameraFrame.removeListener(listener);
    }

}
