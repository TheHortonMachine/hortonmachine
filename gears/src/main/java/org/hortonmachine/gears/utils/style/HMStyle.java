package org.hortonmachine.gears.utils.style;

import static org.hortonmachine.gears.utils.style.StyleUtilities.ff;
import static org.hortonmachine.gears.utils.style.StyleUtilities.sf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.geotools.api.filter.Filter;
import org.geotools.api.style.AnchorPoint;
import org.geotools.api.style.Displacement;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Font;
import org.geotools.api.style.Graphic;
import org.geotools.api.style.Mark;
import org.geotools.api.style.PointPlacement;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.api.style.Symbolizer;
import org.geotools.api.style.TextSymbolizer;
import org.geotools.filter.text.ecql.ECQL;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;

/**
 * Fluent builder for simple vector styles.
 * <p>
 * The builder creates regular GeoTools {@link Style} instances, so it can be used anywhere the
 * existing low level style utilities are already used.
 */
public class HMStyle {

    private static final String DEFAULT_STYLE_NAME = "hmStyle";

    private String name = DEFAULT_STYLE_NAME;
    private EGeometryType geometryType;

    private String pointType = "circle";
    private String pointSize = StyleUtilities.DEFAULT_SIZE;
    private Fill pointFill = new Fill(StyleUtilities.DEFAULT_COLOR, Double.parseDouble(StyleUtilities.DEFAULT_OPACITY));
    private Stroke pointStroke = new Stroke(StyleUtilities.DEFAULT_COLOR, Double.parseDouble(StyleUtilities.DEFAULT_WIDTH));
    private String pointRotation = StyleUtilities.DEFAULT_ROTATION;

    private Fill fill;
    private Stroke stroke;
    private Label label;
    private Halo halo;
    private final List<Stroke> conditionalStrokes = new ArrayList<>();
    private final List<Fill> conditionalFills = new ArrayList<>();
    private final List<Label> conditionalLabels = new ArrayList<>();
    private Fill pendingFill;
    private Stroke pendingStroke;
    private Label pendingLabel;

    public static HMStyle point() {
        return new HMStyle().asPoint();
    }

    public static HMStyle line() {
        return new HMStyle().asLine();
    }

    public static HMStyle polygon() {
        return new HMStyle().asPolygon();
    }

    public static Label label( String fieldName ) {
        return new Label(fieldName);
    }

    public static FontDef font() {
        return new FontDef();
    }

    public static Halo halo( Fill fill, double radius ) {
        return new Halo(fill, radius);
    }

    public HMStyle asPoint() {
        geometryType = EGeometryType.POINT;
        return this;
    }

    public HMStyle asLine() {
        geometryType = EGeometryType.LINESTRING;
        return this;
    }

    public HMStyle asPolygon() {
        geometryType = EGeometryType.POLYGON;
        return this;
    }

    public HMStyle name( String name ) {
        this.name = name;
        return this;
    }

    public HMStyle type( String type ) {
        this.pointType = type;
        return this;
    }

    public HMStyle size( Number size ) {
        this.pointSize = valueOf(size);
        return this;
    }

    public HMStyle color( String color ) {
        this.pointFill = new Fill(color, pointFill.opacity);
        return this;
    }

    public HMStyle fill( Fill fill ) {
        if (fill.hasFilter()) {
            conditionalFills.add(fill);
            pendingFill = null;
        } else if (geometryType == EGeometryType.POINT) {
            this.pointFill = fill;
            pendingFill = fill;
        } else {
            this.fill = fill;
            pendingFill = fill;
        }
        return this;
    }

    public HMStyle fill( String color ) {
        return fill(new Fill(color));
    }

    public HMStyle fill( String color, double opacity ) {
        return fill(new Fill(color, opacity));
    }

    public HMStyle stroke( Stroke stroke ) {
        if (stroke.hasFilter()) {
            conditionalStrokes.add(stroke);
            pendingStroke = null;
        } else if (geometryType == EGeometryType.POINT) {
            this.pointStroke = stroke;
            pendingStroke = stroke;
        } else {
            this.stroke = stroke;
            pendingStroke = stroke;
        }
        return this;
    }

    public HMStyle stroke( String color, double width ) {
        return stroke(new Stroke(color, width));
    }

    public HMStyle opacity( Number opacity ) {
        double opacityValue = opacity.doubleValue();
        if (geometryType == EGeometryType.POINT) {
            Fill previousPointFill = this.pointFill;
            this.pointFill = new Fill(pointFill.color, opacityValue);
            if (pendingFill == previousPointFill) {
                pendingFill = this.pointFill;
            }
        } else if (fill != null) {
            Fill previousFill = this.fill;
            this.fill = new Fill(fill.color, opacityValue);
            if (pendingFill == previousFill) {
                pendingFill = this.fill;
            }
        }
        return this;
    }

    public HMStyle rotation( Number rotation ) {
        this.pointRotation = valueOf(rotation);
        return this;
    }

    public HMStyle label( Label label ) {
        if (label.hasFilter()) {
            conditionalLabels.add(label);
            pendingLabel = null;
        } else {
            this.label = label;
            pendingLabel = label;
        }
        return this;
    }

    public HMStyle halo( Halo halo ) {
        this.halo = halo;
        return this;
    }

    public HMStyle halo( String color, double radius ) {
        return halo(new Halo(color, radius));
    }

    public HMStyle where( String cqlFilter ) {
        return where(parseFilter(cqlFilter));
    }

    public HMStyle where( Filter filter ) {
        if (pendingFill == null && pendingStroke == null && pendingLabel == null) {
            throw new IllegalStateException("where(...) can only be applied right after fill(...), stroke(...) or label(...).");
        }
        if (pendingStroke != null) {
            pendingStroke.where(filter);
            if (this.stroke == pendingStroke) {
                this.stroke = null;
            }
            conditionalStrokes.add(pendingStroke);
        }
        if (pendingFill != null) {
            pendingFill.where(filter);
            if (this.fill == pendingFill) {
                this.fill = null;
            }
            conditionalFills.add(pendingFill);
        }
        if (pendingLabel != null) {
            pendingLabel.where(filter);
            if (this.label == pendingLabel) {
                this.label = null;
            }
            conditionalLabels.add(pendingLabel);
        }
        pendingFill = null;
        pendingStroke = null;
        pendingLabel = null;
        return this;
    }

    public HMStyle add( Object part ) {
        if (part instanceof Fill) {
            return fill((Fill) part);
        }
        if (part instanceof Stroke) {
            return stroke((Stroke) part);
        }
        if (part instanceof Label) {
            return label((Label) part);
        }
        if (part instanceof Halo) {
            return halo((Halo) part);
        }
        if (part instanceof HMStyle) {
            HMStyle other = (HMStyle) part;
            if (other.geometryType != null) {
                geometryType = other.geometryType;
            }
            if (other.fill != null) {
                fill = other.fill;
            }
            if (other.stroke != null) {
                stroke = other.stroke;
            }
            if (other.label != null) {
                label = other.label;
            }
            if (other.halo != null) {
                halo = other.halo;
            }
            conditionalStrokes.addAll(other.conditionalStrokes);
            conditionalFills.addAll(other.conditionalFills);
            conditionalLabels.addAll(other.conditionalLabels);
            pendingFill = null;
            pendingStroke = null;
            pendingLabel = null;
            if (other.geometryType == EGeometryType.POINT) {
                pointType = other.pointType;
                pointSize = other.pointSize;
                pointFill = other.pointFill;
                pointStroke = other.pointStroke;
                pointRotation = other.pointRotation;
            }
            return this;
        }
        throw new IllegalArgumentException("Unsupported style part: " + part);
    }

    public HMStyle plus( Object part ) {
        return add(part);
    }

    public Style build() {
        if (geometryType == null) {
            throw new IllegalStateException("Geometry type not set. Use point(), line(), polygon() or asPoint()/asLine()/asPolygon().");
        }

        Style style = sf.createStyle();
        style.setName(name);

        FeatureTypeStyle featureTypeStyle = sf.createFeatureTypeStyle();
        List<RuleSpec> ruleSpecs = createRuleSpecs();
        for( RuleSpec ruleSpec : ruleSpecs ) {
            Rule rule = sf.createRule();
            rule.setName("New rule");
            rule.setFilter(ruleSpec.filter);
            rule.symbolizers().add(createGeometrySymbolizer(ruleSpec));

            TextSymbolizer textSymbolizer = createTextSymbolizer(ruleSpec.labelOverride != null ? ruleSpec.labelOverride : label);
            if (textSymbolizer != null) {
                rule.symbolizers().add(textSymbolizer);
            }
            featureTypeStyle.rules().add(rule);
        }
        style.featureTypeStyles().add(featureTypeStyle);
        return style;
    }

    private Symbolizer createGeometrySymbolizer( RuleSpec ruleSpec ) {
        switch( geometryType ) {
        case POINT:
            return createPointSymbolizer();
        case LINESTRING:
            return createLineSymbolizer(ruleSpec.strokeOverride != null ? ruleSpec.strokeOverride : stroke);
        case POLYGON:
            return createPolygonSymbolizer(ruleSpec.fillOverride != null ? ruleSpec.fillOverride : fill,
                    ruleSpec.strokeOverride != null ? ruleSpec.strokeOverride : stroke);
        default:
            throw new IllegalArgumentException("Unsupported geometry type: " + geometryType);
        }
    }

    private org.geotools.api.style.PointSymbolizer createPointSymbolizer() {
        Graphic graphic = sf.createDefaultGraphic();
        graphic.graphicalSymbols().clear();

        Mark mark = sf.createMark();
        mark.setWellKnownName(ff.literal(pointType));
        mark.setFill(pointFill.toGtFill());
        mark.setStroke(pointStroke.toGtStroke());
        graphic.graphicalSymbols().add(mark);
        graphic.setSize(ff.literal(pointSize));
        graphic.setRotation(ff.literal(pointRotation));

        org.geotools.api.style.PointSymbolizer pointSymbolizer = sf.createPointSymbolizer();
        pointSymbolizer.setGraphic(graphic);
        return pointSymbolizer;
    }

    private org.geotools.api.style.LineSymbolizer createLineSymbolizer( Stroke strokeToUse ) {
        org.geotools.api.style.LineSymbolizer lineSymbolizer = sf.createLineSymbolizer();
        lineSymbolizer.setStroke((strokeToUse != null ? strokeToUse : new Stroke(StyleUtilities.DEFAULT_COLOR,
                Double.parseDouble(StyleUtilities.DEFAULT_WIDTH))).toGtStroke());
        return lineSymbolizer;
    }

    private org.geotools.api.style.PolygonSymbolizer createPolygonSymbolizer( Fill fillToUse, Stroke strokeToUse ) {
        org.geotools.api.style.PolygonSymbolizer polygonSymbolizer = sf.createPolygonSymbolizer();
        polygonSymbolizer.setFill((fillToUse != null ? fillToUse : new Fill(StyleUtilities.DEFAULT_COLOR,
                Double.parseDouble(StyleUtilities.DEFAULT_OPACITY))).toGtFill());
        polygonSymbolizer.setStroke((strokeToUse != null ? strokeToUse : new Stroke(StyleUtilities.DEFAULT_COLOR,
                Double.parseDouble(StyleUtilities.DEFAULT_WIDTH))).toGtStroke());
        return polygonSymbolizer;
    }

    private TextSymbolizer createTextSymbolizer( Label labelToUse ) {
        if (labelToUse == null) {
            return null;
        }
        TextSymbolizer textSymbolizer = sf.createTextSymbolizer();
        textSymbolizer.setLabel(labelToUse.fromField ? ff.property(labelToUse.fieldName) : ff.literal(labelToUse.fieldName));
        textSymbolizer.setFont(labelToUse.toGtFont());
        textSymbolizer
                .setFill(labelToUse.fill != null ? labelToUse.fill.toGtFill() : new Fill(StyleUtilities.DEFAULT_COLOR).toGtFill());

        Halo effectiveHalo = labelToUse.halo != null ? labelToUse.halo : halo;
        if (effectiveHalo != null) {
            textSymbolizer.setHalo(effectiveHalo.toGtHalo());
        }

        if (geometryType == EGeometryType.POINT || geometryType == EGeometryType.POLYGON) {
            PointPlacement pointPlacement = sf.createPointPlacement(toAnchorPoint(labelToUse.anchor),
                    toDisplacement(labelToUse.displacement), ff.literal(valueOf(labelToUse.rotation)));
            textSymbolizer.setLabelPlacement(pointPlacement);
        }

        return textSymbolizer;
    }

    private AnchorPoint toAnchorPoint( double[] anchor ) {
        double[] safeAnchor = anchor != null ? anchor : new double[]{0.5, 0.5};
        return sf.anchorPoint(ff.literal(valueOf(safeAnchor[0])), ff.literal(valueOf(safeAnchor[1])));
    }

    private Displacement toDisplacement( double[] displacement ) {
        double[] safeDisplacement = displacement != null ? displacement : new double[]{0.0, 0.0};
        return sf.displacement(ff.literal(valueOf(safeDisplacement[0])), ff.literal(valueOf(safeDisplacement[1])));
    }

    private static String valueOf( Number number ) {
        if (number == null) {
            return null;
        }
        double value = number.doubleValue();
        long longValue = (long) value;
        if (value == longValue) {
            return Long.toString(longValue);
        }
        return Double.toString(value);
    }

    private static double[] toDoubleArray( List<Number> values, int expectedSize ) {
        if (values == null || values.size() != expectedSize) {
            throw new IllegalArgumentException("Expected " + expectedSize + " values.");
        }
        double[] array = new double[expectedSize];
        for( int i = 0; i < expectedSize; i++ ) {
            array[i] = values.get(i).doubleValue();
        }
        return array;
    }

    private List<RuleSpec> createRuleSpecs() {
        List<RuleSpec> ruleSpecs = new ArrayList<>();
        if (geometryType == EGeometryType.POINT) {
            ruleSpecs.add(new RuleSpec(null));
            return ruleSpecs;
        }

        for( Stroke conditionalStroke : conditionalStrokes ) {
            getOrCreateRuleSpec(ruleSpecs, conditionalStroke.filter).strokeOverride = conditionalStroke;
        }
        for( Fill conditionalFill : conditionalFills ) {
            getOrCreateRuleSpec(ruleSpecs, conditionalFill.filter).fillOverride = conditionalFill;
        }
        for( Label conditionalLabel : conditionalLabels ) {
            getOrCreateRuleSpec(ruleSpecs, conditionalLabel.filter).labelOverride = conditionalLabel;
        }

        if (ruleSpecs.isEmpty()) {
            ruleSpecs.add(new RuleSpec(null));
        }
        return ruleSpecs;
    }

    private RuleSpec getOrCreateRuleSpec( List<RuleSpec> ruleSpecs, Filter filter ) {
        for( RuleSpec ruleSpec : ruleSpecs ) {
            if (sameFilter(ruleSpec.filter, filter)) {
                return ruleSpec;
            }
        }
        RuleSpec ruleSpec = new RuleSpec(filter);
        ruleSpecs.add(ruleSpec);
        return ruleSpec;
    }

    private boolean sameFilter( Filter filter1, Filter filter2 ) {
        if (filter1 == filter2) {
            return true;
        }
        if (filter1 == null || filter2 == null) {
            return false;
        }
        return filter1.equals(filter2);
    }

    private static Filter parseFilter( String cqlFilter ) {
        try {
            return ECQL.toFilter(cqlFilter);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse filter: " + cqlFilter, e);
        }
    }

    private static class RuleSpec {
        private final Filter filter;
        private Stroke strokeOverride;
        private Fill fillOverride;
        private Label labelOverride;

        private RuleSpec( Filter filter ) {
            this.filter = filter;
        }
    }

    public static class Fill {
        private final String color;
        private final double opacity;
        private Filter filter;

        public Fill( String color ) {
            this(color, Double.parseDouble(StyleUtilities.DEFAULT_OPACITY));
        }

        public Fill( String color, double opacity ) {
            this.color = color;
            this.opacity = opacity;
        }

        public HMStyle plus( Object part ) {
            return new HMStyle().add(this).add(part);
        }

        public Fill where( String cqlFilter ) {
            return where(parseFilter(cqlFilter));
        }

        public Fill where( Filter filter ) {
            this.filter = filter;
            return this;
        }

        private boolean hasFilter() {
            return filter != null;
        }

        private org.geotools.api.style.Fill toGtFill() {
            org.geotools.api.style.Fill gtFill = sf.createFill(ff.literal(ColorUtilities.asHex(color)));
            gtFill.setOpacity(ff.literal(valueOf(opacity)));
            return gtFill;
        }
    }

    public static class Stroke {
        private final String color;
        private final double width;
        private Double opacity;
        private Filter filter;

        public Stroke( String color, double width ) {
            this.color = color;
            this.width = width;
        }

        public Stroke opacity( double opacity ) {
            this.opacity = opacity;
            return this;
        }

        public HMStyle plus( Object part ) {
            return new HMStyle().add(this).add(part);
        }

        public Stroke where( String cqlFilter ) {
            return where(parseFilter(cqlFilter));
        }

        public Stroke where( Filter filter ) {
            this.filter = filter;
            return this;
        }

        private boolean hasFilter() {
            return filter != null;
        }

        private org.geotools.api.style.Stroke toGtStroke() {
            org.geotools.api.style.Stroke gtStroke = sf.createStroke(ff.literal(ColorUtilities.asHex(color)),
                    ff.literal(valueOf(width)));
            if (opacity != null) {
                gtStroke.setOpacity(ff.literal(valueOf(opacity)));
            }
            return gtStroke;
        }
    }

    public static class Halo {
        private final Fill fill;
        private final double radius;

        public Halo( Fill fill, double radius ) {
            this.fill = fill;
            this.radius = radius;
        }

        public Halo( String color, double radius ) {
            this(new Fill(color), radius);
        }

        public HMStyle plus( Object part ) {
            return new HMStyle().add(this).add(part);
        }

        private org.geotools.api.style.Halo toGtHalo() {
            return sf.createHalo(fill.toGtFill(), ff.literal(valueOf(radius)));
        }
    }

    public static class Label {
        private static final String DEFAULT_FONT_FAMILY = "Arial";
        private static final String DEFAULT_FONT_STYLE = "normal";
        private static final String DEFAULT_FONT_WEIGHT = "normal";
        private static final String DEFAULT_FONT_SIZE = "12";

        private final String fieldName;
        private boolean fromField = true;
        private Fill fill;
        private Halo halo;
        private String fontFamily = DEFAULT_FONT_FAMILY;
        private String fontStyle = DEFAULT_FONT_STYLE;
        private String fontWeight = DEFAULT_FONT_WEIGHT;
        private String fontSize = DEFAULT_FONT_SIZE;
        private double[] anchor = new double[]{0.5, 0.5};
        private double[] displacement = new double[]{0.0, 0.0};
        private double rotation = 0.0;
        private Filter filter;

        public Label( String fieldName ) {
            this.fieldName = fieldName;
        }

        public Label literal() {
            fromField = false;
            return this;
        }

        public Label point( List<Number> anchor, List<Number> displacement, Number rotation ) {
            this.anchor = toDoubleArray(anchor, 2);
            this.displacement = toDoubleArray(displacement, 2);
            this.rotation = rotation.doubleValue();
            return this;
        }

        public Label point( Number anchorX, Number anchorY, Number displacementX, Number displacementY,
                Number rotation ) {
            return anchor(anchorX, anchorY).displacement(displacementX, displacementY).rotation(rotation);
        }

        public Label anchor( Number anchorX, Number anchorY ) {
            this.anchor = new double[]{anchorX.doubleValue(), anchorY.doubleValue()};
            return this;
        }

        public Label displacement( Number displacementX, Number displacementY ) {
            this.displacement = new double[]{displacementX.doubleValue(), displacementY.doubleValue()};
            return this;
        }

        public Label rotation( Number rotation ) {
            this.rotation = rotation.doubleValue();
            return this;
        }

        public Label fill( Fill fill ) {
            this.fill = fill;
            return this;
        }

        public Label fill( String color ) {
            return fill(new Fill(color));
        }

        public Label fill( String color, double opacity ) {
            return fill(new Fill(color, opacity));
        }

        public Label halo( Halo halo ) {
            this.halo = halo;
            return this;
        }

        public Label halo( String color, double radius ) {
            return halo(new Halo(color, radius));
        }

        public Label where( String cqlFilter ) {
            return where(parseFilter(cqlFilter));
        }

        public Label where( Filter filter ) {
            this.filter = filter;
            return this;
        }

        public Label font( FontDef fontDefinition ) {
            if (fontDefinition == null) {
                return this;
            }
            return applyFont(fontDefinition.family, fontDefinition.style, fontDefinition.weight, fontDefinition.size);
        }

        public Label font( Map<String, Object> fontDefinition ) {
            Object family = fontDefinition.get("family");
            Object style = fontDefinition.get("style");
            Object weight = fontDefinition.get("weight");
            Object size = fontDefinition.get("size");

            return applyFont(family, style, weight, size);
        }

        public HMStyle plus( Object part ) {
            return new HMStyle().add(this).add(part);
        }

        private Label applyFont( Object family, Object style, Object weight, Object size ) {
            if (family != null) {
                fontFamily = String.valueOf(family);
            }
            if (style != null) {
                fontStyle = String.valueOf(style);
            }
            if (weight != null) {
                fontWeight = String.valueOf(weight);
            }
            if (size != null) {
                if (size instanceof Number) {
                    fontSize = valueOf((Number) size);
                } else {
                    fontSize = String.valueOf(size);
                }
            }
            return this;
        }

        private boolean hasFilter() {
            return filter != null;
        }

        private Font toGtFont() {
            return sf.font(Collections.singletonList(ff.literal(fontFamily)), ff.literal(fontStyle),
                    ff.literal(fontWeight), ff.literal(fontSize));
        }
    }

    public static class FontDef {
        private String family;
        private String style;
        private String weight;
        private Object size;

        public FontDef family( String family ) {
            this.family = family;
            return this;
        }

        public FontDef style( String style ) {
            this.style = style;
            return this;
        }

        public FontDef italic() {
            this.style = "italic";
            return this;
        }

        public FontDef normalStyle() {
            this.style = "normal";
            return this;
        }

        public FontDef weight( String weight ) {
            this.weight = weight;
            return this;
        }

        public FontDef bold() {
            this.weight = "bold";
            return this;
        }

        public FontDef normalWeight() {
            this.weight = "normal";
            return this;
        }

        public FontDef size( Number size ) {
            this.size = size;
            return this;
        }

        public FontDef size( String size ) {
            this.size = size;
            return this;
        }
    }
}
