package org.hortonmachine.gpextras.camera;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class CameraTest implements CameraListener {
    
    public CameraTest() {
        Camera c = new Camera();
        c.open("/home/hydrologis/TMP/cameratest.jpg");
        c.addListener(this);
    }

    @Override
    public void onPictureTaken( String fileWritten ) {
        System.out.println("Written: " + fileWritten);
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.open(new File(fileWritten));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main( String[] args ) {

        new CameraTest();

    }
}