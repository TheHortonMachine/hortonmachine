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
package org.hortonmachine.gears.io.exif;

import static org.hortonmachine.gears.i18n.GearsMessages.EXIFREADER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.EXIFREADER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.EXIFREADER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.EXIFREADER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.EXIFREADER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.EXIFREADER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.EXIFREADER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.EXIFREADER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.EXIFREADER_OUTTAGS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.EXIFREADER_STATUS;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.hortonmachine.gears.libs.modules.HMModel;
import org.w3c.dom.NodeList;

@Description(EXIFREADER_DESCRIPTION)
@Author(name = EXIFREADER_AUTHORNAMES, contact = EXIFREADER_AUTHORCONTACTS)
@Keywords(EXIFREADER_KEYWORDS)
@Label(EXIFREADER_LABEL)
@Name(EXIFREADER_NAME)
@Status(EXIFREADER_STATUS)
@License(EXIFREADER_LICENSE)
public class ExifReader extends HMModel {

    @Description(EXIFREADER_FILE_DESCRIPTION)
    @In
    public String file = null;

    @Description(EXIFREADER_OUTTAGS_DESCRIPTION)
    @Out
    public HashMap<String, ExifTag> outTags = null;

    @Execute
    public void readExif() throws IOException {
        ImageReader reader = ExifUtil.findReader();
        reader.setInput(new FileImageInputStream(new File(file)));
        IIOMetadata imageMetadata = reader.getImageMetadata(0);

        parseExifMeta(imageMetadata);

    }

    @SuppressWarnings("nls")
    private void parseExifMeta( IIOMetadata exifMeta ) {
        outTags = new HashMap<String, ExifTag>();

        IIOMetadataNode root = (IIOMetadataNode) exifMeta.getAsTree("com_sun_media_imageio_plugins_tiff_image_1.0");

        NodeList imageDirectories = root.getElementsByTagName("TIFFIFD");
        for( int i = 0; i < imageDirectories.getLength(); i++ ) {
            IIOMetadataNode directory = (IIOMetadataNode) imageDirectories.item(i);

            NodeList tiffTags = directory.getElementsByTagName("TIFFField");
            for( int j = 0; j < tiffTags.getLength(); j++ ) {
                IIOMetadataNode tag = (IIOMetadataNode) tiffTags.item(j);

                String tagNumber = tag.getAttribute("number");
                String tagName = tag.getAttribute("name");
                String tagValue;

                StringBuilder tmp = new StringBuilder();
                IIOMetadataNode values = (IIOMetadataNode) tag.getFirstChild();

                if ("TIFFUndefined".equals(values.getNodeName())) {
                    tmp.append(values.getAttribute("value"));
                } else {
                    NodeList tiffNumbers = values.getChildNodes();
                    for( int k = 0; k < tiffNumbers.getLength(); k++ ) {
                        tmp.append(((IIOMetadataNode) tiffNumbers.item(k)).getAttribute("value"));
                        tmp.append(",");
                    }
                    tmp.deleteCharAt(tmp.length() - 1);
                }

                tagValue = tmp.toString();

                ExifTag exifTag = new ExifTag(tagName, tagNumber, tagValue);
                outTags.put(tagName, exifTag);
            }
        }
    }

}
