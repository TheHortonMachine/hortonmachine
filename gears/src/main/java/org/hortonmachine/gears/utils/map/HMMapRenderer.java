package org.hortonmachine.gears.utils.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.WindowConstants;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.swing.JMapFrame;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.style.HMStyle;

/**
 * Simple utility for rendering map images from vector layers.
 * <p>
 * The class is bean-friendly so it can be instantiated from Groovy like:
 * {@code new HMMapRenderer(width: 1600, height: 1000)}.
 */
public class HMMapRenderer {

    private int width = 800;
    private int height = 600;
    private ReferencedEnvelope bounds;
    private Color backgroundColor = Color.WHITE;
    private final List<VectorLayerSpec> layers = new ArrayList<>();

    public HMMapRenderer() {
    }

    public HMMapRenderer( int width, int height ) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth( int width ) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight( int height ) {
        this.height = height;
    }

    public ReferencedEnvelope getBounds() {
        return bounds;
    }

    public void setBounds( ReferencedEnvelope bounds ) {
        this.bounds = bounds != null ? new ReferencedEnvelope(bounds) : null;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor( Color backgroundColor ) {
        this.backgroundColor = backgroundColor != null ? backgroundColor : Color.WHITE;
    }

    public void setBackgroundColor( String color ) {
        setBackgroundColor(parseColor(color));
    }

    public HMMapRenderer addLayer( SimpleFeatureCollection featureCollection, Style style ) {
        Style effectiveStyle = style != null ? style : SLD.createSimpleStyle(featureCollection.getSchema());
        layers.add(new VectorLayerSpec(featureCollection, effectiveStyle));
        if (bounds == null) {
            expandBounds(featureCollection.getBounds());
        }
        return this;
    }

    public HMMapRenderer addLayer( SimpleFeatureCollection featureCollection, HMStyle style ) {
        Style builtStyle = style != null ? style.build() : null;
        return addLayer(featureCollection, builtStyle);
    }

    public HMMapRenderer addLayer( SimpleFeatureSource featureSource, Style style ) throws IOException {
        return addLayer(featureSource.getFeatures(), style);
    }

    public HMMapRenderer addLayer( SimpleFeatureSource featureSource, HMStyle style ) throws IOException {
        return addLayer(featureSource.getFeatures(), style);
    }

    public MapContent createMapContent() {
        return createMapContent("MapRenderer");
    }

    public MapContent createMapContent( String title ) {
        ReferencedEnvelope renderBounds = resolveBounds();

        MapContent content = new MapContent();
        content.setTitle(title != null ? title : "MapRenderer");
        for( VectorLayerSpec layerSpec : layers ) {
            content.addLayer(layerSpec.toLayer());
        }
        content.getViewport().setBounds(fitToImageRatio(renderBounds, width, height));
        return content;
    }

    public JMapFrame display() {
        return display("MapRenderer");
    }

    public JMapFrame display( String title ) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Map display is not available in a headless environment.");
        }

        MapContent content = createMapContent(title);
        JMapFrame frame = new JMapFrame(content);
        frame.setSize(width, height);
        frame.enableStatusBar(true);
        frame.enableToolBar(true);
        frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN, JMapFrame.Tool.RESET);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter(){
            @Override
            public void windowClosing( java.awt.event.WindowEvent e ) {
                cleanupFrame(frame, content);
            }

            @Override
            public void windowClosed( java.awt.event.WindowEvent e ) {
                cleanupFrame(frame, content);
            }
        });
        frame.setVisible(true);

        while( frame.isDisplayable() ) {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return frame;
    }

    private static void cleanupFrame( JMapFrame frame, MapContent content ) {
        if (frame == null) {
            return;
        }

        if (frame.getMapPane() != null) {
            frame.getMapPane().setRenderingExecutor(null);
            frame.getMapPane().setMapContent(null);
            shutdownPaneTaskExecutor(frame);
        }
        if (content != null) {
            content.dispose();
        }
    }

    private static void shutdownPaneTaskExecutor( JMapFrame frame ) {
        try {
            Field paneTaskExecutorField = frame.getMapPane().getClass().getSuperclass().getDeclaredField("paneTaskExecutor");
            paneTaskExecutorField.setAccessible(true);
            Object executor = paneTaskExecutorField.get(frame.getMapPane());
            if (executor instanceof ScheduledExecutorService) {
                ((ScheduledExecutorService) executor).shutdownNow();
            }
        } catch (Exception e) {
            // Best-effort cleanup: older/newer GeoTools versions may expose this differently.
        }
    }

    public BufferedImage render() {
        MapContent content = createMapContent();
        try {
            StreamingRenderer renderer = new StreamingRenderer();
            renderer.setMapContent(content);
            ReferencedEnvelope renderBounds = content.getViewport().getBounds();

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(backgroundColor);
                graphics.fillRect(0, 0, width, height);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                renderer.paint(graphics, new Rectangle(0, 0, width, height), renderBounds);
            } finally {
                graphics.dispose();
            }
            return image;
        } finally {
            content.dispose();
        }
    }

    public void render( String path ) throws IOException {
        render(new File(path));
    }

    public void render( File file ) throws IOException {
        BufferedImage image = render();
        String format = imageFormat(file);
        ImageIO.write(image, format, file);
    }

    public static void renderAnimated( List<BufferedImage> images, File outFile, int timeIntervalMilliseconds, boolean loop )
            throws IOException {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("No images supplied for GIF rendering.");
        }

        try (ImageOutputStream outputStream = new FileImageOutputStream(outFile);
                GifSequenceWriter writer = new GifSequenceWriter(outputStream, images.get(0).getType(),
                        timeIntervalMilliseconds, loop)) {
            for( BufferedImage image : images ) {
                writer.writeToSequence(image);
            }
        }
    }

    private void expandBounds( ReferencedEnvelope envelope ) {
        if (envelope == null || envelope.isEmpty()) {
            return;
        }
        if (bounds == null) {
            bounds = new ReferencedEnvelope(envelope);
        } else {
            bounds.expandToInclude(envelope);
        }
    }

    private ReferencedEnvelope inferBounds() {
        ReferencedEnvelope inferred = null;
        for( VectorLayerSpec layer : layers ) {
            ReferencedEnvelope layerBounds = toReferencedEnvelope(layer.bounds);
            if (layerBounds == null || layerBounds.isEmpty()) {
                continue;
            }
            if (inferred == null) {
                inferred = new ReferencedEnvelope(layerBounds);
            } else {
                inferred.expandToInclude(layerBounds);
            }
        }
        return inferred;
    }

    private ReferencedEnvelope resolveBounds() {
        if (layers.isEmpty()) {
            throw new IllegalStateException("No layers available for rendering.");
        }
        ReferencedEnvelope renderBounds = bounds != null ? new ReferencedEnvelope(bounds) : inferBounds();
        if (renderBounds == null || renderBounds.isEmpty()) {
            throw new IllegalStateException("Unable to determine map bounds.");
        }
        return renderBounds;
    }

    private static class VectorLayerSpec {
        private final SimpleFeatureCollection featureCollection;
        private final Style style;
        private final ReferencedEnvelope bounds;

        private VectorLayerSpec( SimpleFeatureCollection featureCollection, Style style ) {
            this.featureCollection = featureCollection;
            this.style = style;
            this.bounds = featureCollection != null ? featureCollection.getBounds() : null;
        }

        private FeatureLayer toLayer() {
            FeatureLayer layer = new FeatureLayer(featureCollection, style);
            layer.setStyle(style);
            return layer;
        }
    }

    private ReferencedEnvelope toReferencedEnvelope( org.geotools.api.geometry.Bounds layerBounds ) {
        if (layerBounds == null) {
            return null;
        }
        if (layerBounds instanceof ReferencedEnvelope) {
            return new ReferencedEnvelope((ReferencedEnvelope) layerBounds);
        }
        if (layerBounds.getCoordinateReferenceSystem() == null) {
            return null;
        }
        ReferencedEnvelope envelope = new ReferencedEnvelope(layerBounds.getCoordinateReferenceSystem());
        var lower = layerBounds.getLowerCorner().getCoordinate();
        var upper = layerBounds.getUpperCorner().getCoordinate();
        envelope.expandToInclude(lower[0], lower[1]);
        envelope.expandToInclude(upper[0], upper[1]);
        return envelope;
    }

    private ReferencedEnvelope fitToImageRatio( ReferencedEnvelope envelope, int imageWidth, int imageHeight ) {
        Rectangle2D mapRect = new Rectangle2D.Double(envelope.getMinX(), envelope.getMinY(), envelope.getWidth(),
                envelope.getHeight());
        Rectangle2D imageRect = new Rectangle2D.Double(0, 0, imageWidth, imageHeight);
        GeometryUtilities.scaleToRatio(imageRect, mapRect, false);
        return new ReferencedEnvelope(mapRect, envelope.getCoordinateReferenceSystem());
    }

    private String imageFormat( File file ) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "jpg";
        }
        if (name.endsWith(".gif")) {
            return "gif";
        }
        return "png";
    }

    private Color parseColor( String colorString ) {
        if (colorString == null) {
            return Color.WHITE;
        }
        return ColorUtilities.fromString(colorString);
    }

    private static class GifSequenceWriter implements Closeable {
        private final ImageWriter gifWriter;
        private final ImageWriteParam imageWriteParam;
        private final IIOMetadata imageMetadata;

        private GifSequenceWriter( ImageOutputStream outputStream, int imageType, int delayMs, boolean loop )
                throws IOException {
            gifWriter = gifWriter();
            imageWriteParam = gifWriter.getDefaultWriteParam();
            ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
            imageMetadata = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);

            String metaFormatName = imageMetadata.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) imageMetadata.getAsTree(metaFormatName);

            IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
            graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
            graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
            graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
            graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(Math.max(1, delayMs / 10)));
            graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

            IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
            IIOMetadataNode appExtensionNode = new IIOMetadataNode("ApplicationExtension");
            appExtensionNode.setAttribute("applicationID", "NETSCAPE");
            appExtensionNode.setAttribute("authenticationCode", "2.0");

            int loopCount = loop ? 0 : 1;
            appExtensionNode.setUserObject(new byte[]{0x1, (byte) (loopCount & 0xFF), (byte) ((loopCount >> 8) & 0xFF)});
            appExtensionsNode.appendChild(appExtensionNode);

            imageMetadata.setFromTree(metaFormatName, root);

            gifWriter.setOutput(outputStream);
            gifWriter.prepareWriteSequence(null);
        }

        private void writeToSequence( BufferedImage image ) throws IOException {
            gifWriter.writeToSequence(new IIOImage(image, null, imageMetadata), imageWriteParam);
        }

        @Override
        public void close() throws IOException {
            gifWriter.endWriteSequence();
        }

        private static ImageWriter gifWriter() {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("gif");
            if (!writers.hasNext()) {
                throw new IllegalStateException("No GIF ImageWriter available.");
            }
            return writers.next();
        }

        private static IIOMetadataNode getNode( IIOMetadataNode rootNode, String nodeName ) {
            for( int i = 0; i < rootNode.getLength(); i++ ) {
                if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                    return (IIOMetadataNode) rootNode.item(i);
                }
            }
            IIOMetadataNode node = new IIOMetadataNode(nodeName);
            rootNode.appendChild(node);
            return node;
        }
    }
}
