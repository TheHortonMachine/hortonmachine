/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.utils.files;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * From: http://vafer.org/blog/20071112204524/
 * 
 * Usage done by overriding:
 * <pre>
 * new FileTraversal() {
 *    public void onFile( final File f ) {
 *       System.out.println(f);
 *   }
 * }.traverse(new File("somedir"));
 * </pre>
 */
public class FileTraversal {
    private FileFilter filter;

    public FileTraversal() {
    }
    
    public FileTraversal( FileFilter filter ) {
        this.filter = filter;
    }

    public final void traverse( final File f ) throws IOException {
        if (f.isDirectory()) {
            onDirectory(f);
            final File[] childs;
            if (filter != null) {
                childs = f.listFiles(filter);
            } else {
                childs = f.listFiles();
            }
            for( File child : childs ) {
                traverse(child);
            }
            return;
        }
        onFile(f);
    }

    public void onDirectory( final File d ) {
    }

    public void onFile( final File f ) {
    }
}
