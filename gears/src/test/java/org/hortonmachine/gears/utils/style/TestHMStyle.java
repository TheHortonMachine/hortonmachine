package org.hortonmachine.gears.utils.style;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.api.filter.Filter;
import org.geotools.api.style.Style;
import org.geotools.filter.text.cql2.CQL;
import org.hortonmachine.gears.utils.HMTestCase;

public class TestHMStyle extends HMTestCase {

    public void testPointStyleBuilder() throws Exception {
        Style style = HMStyle.point().type("square").size(10).color("#FF0000").stroke("green", 0.1).opacity(0.5)
                .rotation(45).build();

        StyleWrapper wrapper = new StyleWrapper(style);
        RuleWrapper rule = wrapper.getFirstRule();
        PointSymbolizerWrapper point = rule.getGeometrySymbolizersWrapper().adapt(PointSymbolizerWrapper.class);

        assertEquals("square", point.getMarkName());
        assertEquals("10", point.getSize());
        assertEquals("#FF0000", point.getFillColor());
        assertEquals("0.5", point.getFillOpacity());
        assertEquals("green", point.getStrokeColor());
        assertEquals("0.1", point.getStrokeWidth());
        assertEquals("45", point.getRotation());
    }

    public void testPolygonStyleBuilderWithLabel() throws Exception {
        Map<String, Object> font = new HashMap<>();
        font.put("size", 36);
        font.put("weight", "bold");

        HMStyle.Label label = HMStyle.label("NAME").font(font).anchor(0.5, 0.5).displacement(0, 10).rotation(0)
                .fill("white");
        Style style = HMStyle.polygon().fill("green", 0.2).stroke("green", 1).label(label).halo("green", 3).build();

        StyleWrapper wrapper = new StyleWrapper(style);
        RuleWrapper rule = wrapper.getFirstRule();
        PolygonSymbolizerWrapper polygon = rule.getGeometrySymbolizersWrapper().adapt(PolygonSymbolizerWrapper.class);
        TextSymbolizerWrapper text = wrapper.getFirstTextSymbolizer();

        assertEquals("green", polygon.getFillColor());
        assertEquals("0.2", polygon.getFillOpacity());
        assertEquals("green", polygon.getStrokeColor());
        assertEquals("1", polygon.getStrokeWidth());

        assertEquals("NAME", text.getLabelName());
        assertEquals("36", text.getFontSize());
        assertEquals("bold", text.getFontWeight());
        assertEquals("white", text.getColor());
        assertEquals("green", text.getHaloColor());
        assertEquals("3", text.getHaloRadius());
        assertEquals("0.5", text.getAnchorX());
        assertEquals("0.5", text.getAnchorY());
        assertEquals("0", text.getDisplacementX());
        assertEquals("10", text.getDisplacementY());
    }

    public void testLineStyleBuilderWithLabel() throws Exception {
        HMStyle.Label label = HMStyle.label("name").fill("white").halo("blue", 2);

        Style style = HMStyle.line().stroke("#0000FF", 2).label(label).build();

        StyleWrapper wrapper = new StyleWrapper(style);
        RuleWrapper rule = wrapper.getFirstRule();
        LineSymbolizerWrapper line = rule.getGeometrySymbolizersWrapper().adapt(LineSymbolizerWrapper.class);
        TextSymbolizerWrapper text = wrapper.getFirstTextSymbolizer();

        assertEquals("#0000FF", line.getStrokeColor());
        assertEquals("2", line.getStrokeWidth());

        assertEquals("name", text.getLabelName());
        assertEquals("white", text.getColor());
        assertEquals("blue", text.getHaloColor());
        assertEquals("2", text.getHaloRadius());
    }

    public void testConditionalLineStyleBuilder() throws Exception {
        Filter majorFilter = StyleUtilities.ff.greater(StyleUtilities.ff.property("scalerank"), StyleUtilities.ff.literal(6));

        Style style = HMStyle.line().stroke("#0000FF", 7).where("scalerank <= 6").stroke("#0000FF", 3).where(majorFilter)
                .label(HMStyle.label("name").fill("white")).halo("blue", 2).build();

        StyleWrapper wrapper = new StyleWrapper(style);
        List<FeatureTypeStyleWrapper> featureTypeStyles = wrapper.getFeatureTypeStylesWrapperList();
        assertEquals(1, featureTypeStyles.size());

        List<RuleWrapper> rules = featureTypeStyles.get(0).getRulesWrapperList();
        assertEquals(2, rules.size());

        LineSymbolizerWrapper line1 = rules.get(0).getGeometrySymbolizersWrapper().adapt(LineSymbolizerWrapper.class);
        LineSymbolizerWrapper line2 = rules.get(1).getGeometrySymbolizersWrapper().adapt(LineSymbolizerWrapper.class);
        TextSymbolizerWrapper text1 = rules.get(0).getTextSymbolizersWrapper();
        TextSymbolizerWrapper text2 = rules.get(1).getTextSymbolizersWrapper();

        assertEquals("7", line1.getStrokeWidth());
        assertEquals("3", line2.getStrokeWidth());
        assertEquals("scalerank <= 6", CQL.toCQL(rules.get(0).getRule().getFilter()));
        assertEquals("scalerank > 6", CQL.toCQL(rules.get(1).getRule().getFilter()));

        assertEquals("name", text1.getLabelName());
        assertEquals("white", text1.getColor());
        assertEquals("blue", text1.getHaloColor());
        assertEquals("2", text1.getHaloRadius());

        assertEquals("name", text2.getLabelName());
        assertEquals("white", text2.getColor());
        assertEquals("blue", text2.getHaloColor());
        assertEquals("2", text2.getHaloRadius());
    }
}
