/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.io.vrt;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The content of a GDAL virtual raster (<a href="https://gdal.org/en/stable/drivers/raster/vrt.html">VRT</a>)
 * file, as far as HortonMachine uses it: the grid, the CRS and the sources of each band.
 *
 * <p>The parser only reads the file, it does not judge whether its content can be served: that is
 * up to the user of the model, as {@link VrtImageMosaic}. Elements it does not know are skipped.</p>
 */
public class VrtDataset {

    /** The source elements that carry pixel values. */
    public static final Set<String> SOURCE_ELEMENTS = Set.of("SimpleSource", "ComplexSource", "AveragedSource",
            "NoDataFromMaskSource", "KernelFilteredSource");

    /** The children of a source element that change the values read from it. */
    private static final Set<String> VALUE_TRANSFORM_ELEMENTS = Set.of("ScaleOffset", "ScaleRatio", "LUT",
            "ColorTableComponent", "Exponent", "SrcMin", "SrcMax", "DstMin", "DstMax", "Kernel");

    /** The VRT file the dataset was read from. */
    public final File file;
    /** The <code>subClass</code> of the dataset, as VRTWarpedDataset, or <code>null</code> for a plain one. */
    public final String subClass;
    public final int width;
    public final int height;
    /** The SRS text as written in the file, WKT most of the times, or <code>null</code>. */
    public final String srs;
    /** The six GDAL geotransform coefficients, or <code>null</code> if missing. */
    public final double[] geoTransform;
    public final List<Band> bands;

    /**
     * A band of the dataset.
     *
     * @param band       the band number, starting from 1.
     * @param dataType   the GDAL data type name, as Float32.
     * @param subClass   the <code>subClass</code> of the band, as VRTDerivedRasterBand, or <code>null</code>.
     * @param noData     the nodata value of the band, or <code>null</code>.
     * @param sources    the sources of the band, in drawing order.
     */
    public record Band( int band, String dataType, String subClass, Double noData, List<Source> sources ) {
    }

    /**
     * A source of a band.
     *
     * @param kind             the element name, as SimpleSource or ComplexSource.
     * @param path             the path of the source, resolved against the folder of the VRT if
     *                         relative to it. GDAL virtual paths, as /vsicurl/..., are kept as they are.
     * @param sourceBand       the band of the source that is read, starting from 1, or 0 if it is
     *                         the mask band or missing.
     * @param sourceWidth      the width of the source from its SourceProperties, or -1.
     * @param sourceHeight     the height of the source from its SourceProperties, or -1.
     * @param srcRect          the window of the source that is read, as xOff, yOff, xSize, ySize, or <code>null</code>.
     * @param dstRect          the window of the dataset it is drawn into, as xOff, yOff, xSize, ySize, or <code>null</code>.
     * @param noData           the nodata value of the source, or <code>null</code>.
     * @param transformsValues <code>true</code> if the source changes the values it reads, as with a scale or a lookup table.
     */
    public record Source( String kind, String path, int sourceBand, int sourceWidth, int sourceHeight, double[] srcRect,
            double[] dstRect, Double noData, boolean transformsValues ) {

        /**
         * @return <code>true</code> if the path is a GDAL virtual file system path, as /vsicurl/.
         */
        public boolean isVirtualPath() {
            return path.startsWith("/vsi");
        }
    }

    private VrtDataset( File file, String subClass, int width, int height, String srs, double[] geoTransform,
            List<Band> bands ) {
        this.file = file;
        this.subClass = subClass;
        this.width = width;
        this.height = height;
        this.srs = srs;
        this.geoTransform = geoTransform;
        this.bands = Collections.unmodifiableList(bands);
    }

    /**
     * @return <code>true</code> if the geotransform has no rotation terms.
     */
    public boolean isNorthUp() {
        return geoTransform != null && geoTransform[2] == 0.0 && geoTransform[4] == 0.0;
    }

    /**
     * Read a VRT file.
     *
     * @param vrtFile the file.
     * @return the dataset.
     * @throws Exception if the file is not a VRT.
     */
    public static VrtDataset read( File vrtFile ) throws Exception {
        String xml = FileUtilities.readFile(vrtFile);
        return parse(xml, vrtFile);
    }

    /**
     * Parse the text of a VRT file.
     *
     * @param xml     the text.
     * @param vrtFile the file the text comes from, to resolve the sources relative to it.
     * @return the dataset.
     * @throws Exception if the text is not a VRT.
     */
    public static VrtDataset parse( String xml, File vrtFile ) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // no doctype, no external entities
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xml)));
        Element root = document.getDocumentElement();
        if (!"VRTDataset".equals(root.getTagName())) {
            throw new IllegalArgumentException("Not a VRT file, the root element is " + root.getTagName() + ": " + vrtFile);
        }

        String subClass = attribute(root, "subClass");
        int width = Integer.parseInt(root.getAttribute("rasterXSize").trim());
        int height = Integer.parseInt(root.getAttribute("rasterYSize").trim());

        Element srsElement = child(root, "SRS");
        String srs = srsElement != null ? srsElement.getTextContent().trim() : null;
        if (srs != null && srs.isEmpty()) {
            srs = null;
        }

        double[] geoTransform = null;
        Element gtElement = child(root, "GeoTransform");
        if (gtElement != null) {
            String[] split = gtElement.getTextContent().trim().split("\\s*,\\s*");
            if (split.length != 6) {
                throw new IllegalArgumentException("The GeoTransform needs 6 values: " + gtElement.getTextContent());
            }
            geoTransform = new double[6];
            for( int i = 0; i < 6; i++ ) {
                geoTransform[i] = Double.parseDouble(split[i].trim());
            }
        }

        File parentFolder = vrtFile != null ? vrtFile.getAbsoluteFile().getParentFile() : null;
        List<Band> bands = new ArrayList<>();
        for( Element bandElement : children(root, "VRTRasterBand") ) {
            bands.add(parseBand(bandElement, bands.size() + 1, parentFolder));
        }
        return new VrtDataset(vrtFile, subClass, width, height, srs, geoTransform, bands);
    }

    private static Band parseBand( Element bandElement, int defaultNumber, File parentFolder ) {
        String bandStr = attribute(bandElement, "band");
        int number = bandStr != null ? Integer.parseInt(bandStr) : defaultNumber;
        String dataType = attribute(bandElement, "dataType");
        String subClass = attribute(bandElement, "subClass");
        Double noData = number(child(bandElement, "NoDataValue"));

        List<Source> sources = new ArrayList<>();
        NodeList childNodes = bandElement.getChildNodes();
        for( int i = 0; i < childNodes.getLength(); i++ ) {
            Node node = childNodes.item(i);
            if (node instanceof Element element && SOURCE_ELEMENTS.contains(element.getTagName())) {
                sources.add(parseSource(element, parentFolder));
            }
        }
        return new Band(number, dataType, subClass, noData, sources);
    }

    private static Source parseSource( Element sourceElement, File parentFolder ) {
        Element fileElement = child(sourceElement, "SourceFilename");
        if (fileElement == null) {
            throw new IllegalArgumentException("A " + sourceElement.getTagName() + " has no SourceFilename.");
        }
        String name = fileElement.getTextContent().trim();
        boolean relative = "1".equals(attribute(fileElement, "relativeToVRT"));
        String path = name;
        if (relative && !name.startsWith("/vsi") && parentFolder != null) {
            path = new File(parentFolder, name).getAbsolutePath();
        }

        int sourceBand = 0;
        Element bandElement = child(sourceElement, "SourceBand");
        if (bandElement != null) {
            String text = bandElement.getTextContent().trim();
            // mskband:1 is the mask of band 1, it carries no values
            if (!text.startsWith("mask")) {
                sourceBand = Integer.parseInt(text);
            }
        } else {
            sourceBand = 1;
        }

        int sourceWidth = -1;
        int sourceHeight = -1;
        Element propertiesElement = child(sourceElement, "SourceProperties");
        if (propertiesElement != null) {
            String w = attribute(propertiesElement, "RasterXSize");
            String h = attribute(propertiesElement, "RasterYSize");
            if (w != null && h != null) {
                sourceWidth = Integer.parseInt(w);
                sourceHeight = Integer.parseInt(h);
            }
        }

        boolean transformsValues = false;
        NodeList childNodes = sourceElement.getChildNodes();
        for( int i = 0; i < childNodes.getLength(); i++ ) {
            if (childNodes.item(i) instanceof Element element && VALUE_TRANSFORM_ELEMENTS.contains(element.getTagName())) {
                transformsValues = !isIdentityTransform(element);
                if (transformsValues) {
                    break;
                }
            }
        }

        return new Source(sourceElement.getTagName(), path, sourceBand, sourceWidth, sourceHeight,
                rect(child(sourceElement, "SrcRect")), rect(child(sourceElement, "DstRect")),
                number(child(sourceElement, "NODATA")), transformsValues);
    }

    /**
     * GDAL writes ScaleOffset 0 and ScaleRatio 1 at times, which change nothing.
     */
    private static boolean isIdentityTransform( Element element ) {
        String text = element.getTextContent().trim();
        try {
            return switch( element.getTagName() ) {
            case "ScaleOffset" -> Double.parseDouble(text) == 0.0;
            case "ScaleRatio" -> Double.parseDouble(text) == 1.0;
            default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static double[] rect( Element element ) {
        if (element == null) {
            return null;
        }
        return new double[]{Double.parseDouble(element.getAttribute("xOff")), Double.parseDouble(element.getAttribute("yOff")),
                Double.parseDouble(element.getAttribute("xSize")), Double.parseDouble(element.getAttribute("ySize"))};
    }

    private static Double number( Element element ) {
        if (element == null) {
            return null;
        }
        String text = element.getTextContent().trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.equalsIgnoreCase("nan")) {
            return Double.NaN;
        }
        return Double.parseDouble(text);
    }

    private static String attribute( Element element, String name ) {
        return element.hasAttribute(name) ? element.getAttribute(name).trim() : null;
    }

    private static Element child( Element parent, String name ) {
        List<Element> list = children(parent, name);
        return list.isEmpty() ? null : list.get(0);
    }

    private static List<Element> children( Element parent, String name ) {
        List<Element> list = new ArrayList<>();
        NodeList childNodes = parent.getChildNodes();
        for( int i = 0; i < childNodes.getLength(); i++ ) {
            if (childNodes.item(i) instanceof Element element && element.getTagName().equals(name)) {
                list.add(element);
            }
        }
        return list;
    }
}
