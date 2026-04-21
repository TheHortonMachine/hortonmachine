package org.hortonmachine.gears.utils.style;

import java.util.List;

import org.geotools.api.filter.Filter;
import org.geotools.api.style.Style;
import org.geotools.filter.text.cql2.CQL;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.colors.ColorUtilities;

public class TestHMStyle extends HMTestCase {

    public void testPointStyleBuilder() throws Exception {
        Style style = HMStyle.point().type("square").size(10).color("#FF0000").stroke("green", 0.1).opacity(0.5)
                .rotation(45).build();

        StyleWrapper wrapper = new StyleWrapper(style);
        RuleWrapper rule = wrapper.getFirstRule();
        PointSymbolizerWrapper point = rule.getGeometrySymbolizersWrapper().adapt(PointSymbolizerWrapper.class);

        assertEquals("square", point.getMarkName());
        assertEquals("10", point.getSize());
        assertEquals(ColorUtilities.asHex("#FF0000"), point.getFillColor());
        assertEquals("0.5", point.getFillOpacity());
        assertEquals(ColorUtilities.asHex("green"), point.getStrokeColor());
        assertEquals("0.1", point.getStrokeWidth());
        assertEquals("45", point.getRotation());
    }

    public void testPolygonStyleBuilderWithLabel() throws Exception {
        HMStyle.FontDef font = HMStyle.font().family("SansSerif").size(36).bold();

        HMStyle.Label label = HMStyle.label("NAME").font(font).anchor(0.5, 0.5).displacement(0, 10).rotation(0)
                .fill("white");
        Style style = HMStyle.polygon().fill("green", 0.2).stroke("green", 1).label(label).halo("green", 3).build();

        StyleWrapper wrapper = new StyleWrapper(style);
        RuleWrapper rule = wrapper.getFirstRule();
        PolygonSymbolizerWrapper polygon = rule.getGeometrySymbolizersWrapper().adapt(PolygonSymbolizerWrapper.class);
        TextSymbolizerWrapper text = wrapper.getFirstTextSymbolizer();

        assertEquals(ColorUtilities.asHex("green"), polygon.getFillColor());
        assertEquals("0.2", polygon.getFillOpacity());
        assertEquals(ColorUtilities.asHex("green"), polygon.getStrokeColor());
        assertEquals("1", polygon.getStrokeWidth());

        assertEquals("NAME", text.getLabelName());
        assertEquals("SansSerif", text.getFontFamily());
        assertEquals("36", text.getFontSize());
        assertEquals("bold", text.getFontWeight());
        assertEquals(ColorUtilities.asHex("white"), text.getColor());
        assertEquals(ColorUtilities.asHex("green"), text.getHaloColor());
        assertEquals("3", text.getHaloRadius());
        assertEquals("0.5", text.getAnchorX());
        assertEquals("0.5", text.getAnchorY());
        assertEquals("0", text.getDisplacementX());
        assertEquals("10", text.getDisplacementY());
    }

    public void testPolygonStyleBuilderWithFontMapStillWorks() throws Exception {
        HMStyle.Label label = HMStyle.label("NAME").font(java.util.Map.of("size", 18, "style", "italic"));
        Style style = HMStyle.polygon().fill("green", 0.2).stroke("green", 1).label(label).build();

        StyleWrapper wrapper = new StyleWrapper(style);
        TextSymbolizerWrapper text = wrapper.getFirstTextSymbolizer();

        assertEquals("18", text.getFontSize());
        assertEquals("italic", text.getFontStyle());
    }

    public void testPolygonStyleOpacityShortcut() throws Exception {
        Style style = HMStyle.polygon().fill("green").opacity(0.2).stroke("green", 1).build();

        StyleWrapper wrapper = new StyleWrapper(style);
        RuleWrapper rule = wrapper.getFirstRule();
        PolygonSymbolizerWrapper polygon = rule.getGeometrySymbolizersWrapper().adapt(PolygonSymbolizerWrapper.class);

        assertEquals(ColorUtilities.asHex("green"), polygon.getFillColor());
        assertEquals("0.2", polygon.getFillOpacity());
        assertEquals(ColorUtilities.asHex("green"), polygon.getStrokeColor());
        assertEquals("1", polygon.getStrokeWidth());
    }

    public void testLineStyleBuilderWithLabel() throws Exception {
        HMStyle.Label label = HMStyle.label("name").fill("white").halo("blue", 2);

        Style style = HMStyle.line().stroke("#0000FF", 2).label(label).build();

        StyleWrapper wrapper = new StyleWrapper(style);
        RuleWrapper rule = wrapper.getFirstRule();
        LineSymbolizerWrapper line = rule.getGeometrySymbolizersWrapper().adapt(LineSymbolizerWrapper.class);
        TextSymbolizerWrapper text = wrapper.getFirstTextSymbolizer();

        assertEquals(ColorUtilities.asHex("#0000FF"), line.getStrokeColor());
        assertEquals("2", line.getStrokeWidth());

        assertEquals("name", text.getLabelName());
        assertEquals(ColorUtilities.asHex("white"), text.getColor());
        assertEquals(ColorUtilities.asHex("blue"), text.getHaloColor());
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
        assertEquals(ColorUtilities.asHex("white"), text1.getColor());
        assertEquals(ColorUtilities.asHex("blue"), text1.getHaloColor());
        assertEquals("2", text1.getHaloRadius());

        assertEquals("name", text2.getLabelName());
        assertEquals(ColorUtilities.asHex("white"), text2.getColor());
        assertEquals(ColorUtilities.asHex("blue"), text2.getHaloColor());
        assertEquals("2", text2.getHaloRadius());
    }

    public void testConditionalPolygonFillAndStrokeShareTheSameWhereClause() throws Exception {
        Style style = HMStyle.polygon().fill("red").stroke("red", 2).where("POP_EST > 80000000").fill("blue")
                .stroke("blue", 2).where("POP_EST > 1000000 and POP_EST <= 80000000").fill("green").stroke("green", 2)
                .where("POP_EST <= 1000000").build();

        StyleWrapper wrapper = new StyleWrapper(style);
        List<RuleWrapper> rules = wrapper.getFeatureTypeStylesWrapperList().get(0).getRulesWrapperList();

        assertEquals(3, rules.size());

        PolygonSymbolizerWrapper polygon1 = rules.get(0).getGeometrySymbolizersWrapper().adapt(PolygonSymbolizerWrapper.class);
        PolygonSymbolizerWrapper polygon2 = rules.get(1).getGeometrySymbolizersWrapper().adapt(PolygonSymbolizerWrapper.class);
        PolygonSymbolizerWrapper polygon3 = rules.get(2).getGeometrySymbolizersWrapper().adapt(PolygonSymbolizerWrapper.class);

        assertEquals("POP_EST > 80000000", CQL.toCQL(rules.get(0).getRule().getFilter()));
        assertEquals(ColorUtilities.asHex("red"), polygon1.getFillColor());
        assertEquals(ColorUtilities.asHex("red"), polygon1.getStrokeColor());
        assertEquals("2", polygon1.getStrokeWidth());

        assertEquals("POP_EST > 1000000 AND POP_EST <= 80000000", CQL.toCQL(rules.get(1).getRule().getFilter()));
        assertEquals(ColorUtilities.asHex("blue"), polygon2.getFillColor());
        assertEquals(ColorUtilities.asHex("blue"), polygon2.getStrokeColor());
        assertEquals("2", polygon2.getStrokeWidth());

        assertEquals("POP_EST <= 1000000", CQL.toCQL(rules.get(2).getRule().getFilter()));
        assertEquals(ColorUtilities.asHex("green"), polygon3.getFillColor());
        assertEquals(ColorUtilities.asHex("green"), polygon3.getStrokeColor());
        assertEquals("2", polygon3.getStrokeWidth());
    }

    public void testConditionalPolygonLabelFollowsTheWhereClause() throws Exception {
        Style style = HMStyle.polygon().fill("red").stroke("red", 2).label(HMStyle.label("NAME").fill("white"))
                .where("POP_EST > 80000000").fill("green").stroke("green", 2).build();

        StyleWrapper wrapper = new StyleWrapper(style);
        List<RuleWrapper> rules = wrapper.getFeatureTypeStylesWrapperList().get(0).getRulesWrapperList();

        assertEquals(1, rules.size());
        assertEquals("POP_EST > 80000000", CQL.toCQL(rules.get(0).getRule().getFilter()));

        TextSymbolizerWrapper text = rules.get(0).getTextSymbolizersWrapper();
        assertEquals("NAME", text.getLabelName());
        assertEquals(ColorUtilities.asHex("white"), text.getColor());
    }
}
