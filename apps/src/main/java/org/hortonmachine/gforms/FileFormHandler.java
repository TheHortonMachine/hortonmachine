package org.hortonmachine.gforms;

import java.io.File;
import java.util.List;

import org.hortonmachine.gears.utils.files.FileUtilities;

public class FileFormHandler implements IFormHandler {

    private File file;

    public FileFormHandler( File file ) {
        this.file = file;
    }

    @Override
    public boolean isFileBased() {
        return true;
    }

    @Override
    public boolean exists() {
        return file != null && file.exists();
    }

    @Override
    public String getForm() throws Exception {
        if (file == null)
            return null;
        return FileUtilities.readFile(file);
    }

    @Override
    public void saveForm( String form ) throws Exception {
        if (file != null)
            FileUtilities.writeFile(form, file);
    }

    @Override
    public String getLabel() {
        if (file == null)
            return null;
        return file.getAbsolutePath();
    }

    @Override
    public List<String> getFormKeys() {
        return null;
    }
}
