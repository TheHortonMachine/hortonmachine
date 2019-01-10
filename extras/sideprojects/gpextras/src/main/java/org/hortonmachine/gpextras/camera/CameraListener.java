package org.hortonmachine.gpextras.camera;

/**
 * Listener to pictures taken by {@link Camera}.
 * 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface CameraListener {
    void onPictureTaken( String fileWritten );
}
