package org.hortonmachine.gears.utils.map;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;
import org.geotools.api.style.Style;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.style.HMStyle;

public class TestMapRenderer extends HMTestCase {

    public void testRenderPngAndGif() throws Exception {
        RegionMap region = HMTestMaps.getEnvelopeparams();
        ReferencedEnvelope envelope = new ReferencedEnvelope(region.getWest(), region.getEast(), region.getSouth(),
                region.getNorth(), HMTestMaps.getCrs());

        File tempDirectory = Files.createTempDirectory("hm-maprenderer-test").toFile();
        File pngFile = new File(tempDirectory, "world.png");
        File gifFile = new File(tempDirectory, "anim.gif");

        Style pointStyle = HMStyle.point().type("circle").size(18).fill("#FF0000").stroke("#222222", 1.5).build();
        assertNotNull(pointStyle);

        HMMapRenderer map = new HMMapRenderer();
        map.setWidth(1600);
        map.setHeight(1000);
        map.setBounds(envelope);
        map.addLayer(HMTestMaps.getTestFC(), pointStyle);
        map.setBackgroundColor("white");
        map.render(pngFile);

        assertTrue(pngFile.exists());
        assertTrue(pngFile.length() > 0);

        BufferedImage image = ImageIO.read(pngFile);
        assertEquals(1600, image.getWidth());
        assertEquals(1000, image.getHeight());

        BufferedImage image1 = map.render();
        map.setBackgroundColor("#EEEEFF");
        BufferedImage image2 = map.render();

        HMMapRenderer.renderAnimated(Arrays.asList(image1, image2), gifFile, 1000, true);

        assertTrue(gifFile.exists());
        assertTrue(gifFile.length() > 0);

        MapContent content = map.createMapContent("Renderer Test");
        try {
            assertEquals("Renderer Test", content.getTitle());
            assertEquals(1, content.layers().size());
            assertNotNull(content.getViewport().getBounds());
        } finally {
            content.dispose();
        }
    }
}
