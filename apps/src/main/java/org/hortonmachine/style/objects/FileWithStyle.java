package org.hortonmachine.style.objects;

import java.io.File;
import java.io.IOException;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

public class FileWithStyle implements IObjectWithStyle {

    private File dataFile;
    private String name;

    public void setDataFile( File dataFile, String name ) {
        this.dataFile = dataFile;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public File getDataFile() {
        return dataFile;
    }

    @Override
    public String getNormalizedPath() {
        return dataFile.getAbsolutePath();
    }
    
    @Override
    public boolean isVector() {
        return HMConstants.isVector(dataFile);
    }

    @Override
    public boolean isRaster() {
        return HMConstants.isRaster(dataFile);
    }

    @Override
    public String getSldString() {
        File styleFile = SldUtilities.getStyleFile(dataFile);
        if (styleFile != null) {
            try {
                return FileUtilities.readFile(styleFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void saveSld( String xml ) throws Exception {
        File sldFile = new File(dataFile.getParentFile(), name + "." + SldUtilities.SLD_EXTENSION);
        FileUtilities.writeFile(xml, sldFile);
    }

}
