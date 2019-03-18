package org.hortonmachine.gears.utils.style;

import static org.hortonmachine.gears.utils.style.StyleUtilities.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.geotools.filter.function.FilterFunction_endPoint;
import org.geotools.filter.function.FilterFunction_startPoint;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Symbolizer;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.style.GraphicalSymbol;

/**
 * A wrapper for a {@link LineSymbolizer} to ease interaction with gui.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LineSymbolizerWrapper extends SymbolizerWrapper {

    protected String strokeColor = DEFAULT_COLOR;
    protected String strokeOpacity = DEFAULT_OPACITY;
    protected String strokeWidth = DEFAULT_WIDTH;

    protected Stroke stroke = StyleUtilities.createDefaultStroke();
    protected Graphic strokeGraphicStroke;
    protected String dash = "";
    protected String dashOffset = "0";
    protected String lineCap = "round";
    protected String lineJoin = "round";

    protected PointSymbolizerWrapper endPointStyle;
    protected PointSymbolizerWrapper startPointStyle;

    public LineSymbolizerWrapper( PolygonSymbolizer polygonSymbolizer, RuleWrapper parent ) {
        super(polygonSymbolizer, parent);
        initEndPointSymbolizers();
    }

    public LineSymbolizerWrapper( Symbolizer symbolizer, RuleWrapper parent ) {
        super(symbolizer, parent);

        initEndPointSymbolizers();

        LineSymbolizer lineSymbolizer = (LineSymbolizer) symbolizer;

        // offset
        Point2D offset = getOffset(lineSymbolizer);
        if (offset != null) {
            xOffset = String.valueOf(offset.getX());
            yOffset = String.valueOf(offset.getY());
        } else {
            xOffset = DEFAULT_OFFSET;
            yOffset = DEFAULT_OFFSET;
        }

        Stroke strokeTmp = lineSymbolizer.getStroke();
        if (strokeTmp != null) {
            stroke = strokeTmp;
        } else {
            lineSymbolizer.setStroke(stroke);
        }
        Expression color = stroke.getColor();
        if (color != null) {
            strokeColor = expressionToString(color);
        } else {
            stroke.setColor(ff.literal(strokeColor));
        }
        Expression width = stroke.getWidth();
        if (width != null) {
            strokeWidth = expressionToString(width);
        } else {
            stroke.setWidth(ff.literal(strokeWidth));
        }
        Expression opacity = stroke.getOpacity();
        if (opacity != null) {
            strokeOpacity = expressionToString(opacity);
        } else {
            stroke.setOpacity(ff.literal(strokeOpacity));
        }

        strokeGraphicStroke = stroke.getGraphicStroke();
        if (strokeGraphicStroke != null) {
            List<GraphicalSymbol> graphicalSymbolsList = strokeGraphicStroke.graphicalSymbols();
            if (graphicalSymbolsList.size() > 0) {
                GraphicalSymbol graphicalSymbol = graphicalSymbolsList.get(0);
                if (graphicalSymbol instanceof ExternalGraphic) {
                    strokeExternalGraphicStroke = (ExternalGraphic) graphicalSymbol;
                }
            }
        }

        // dash

        float[] dashArray = getDashArrayFloats();

        if (dashArray.length > 0) {
            dash = getDashString(dashArray);
        } else {
            dash = ""; //$NON-NLS-1$
        }
        // dashoffset
        dashOffset = stroke.getDashOffset().evaluate(null, String.class);
        // line cap
        lineCap = stroke.getLineCap().evaluate(null, String.class);
        // line join
        lineJoin = stroke.getLineJoin().evaluate(null, String.class);

    }

    private void initEndPointSymbolizers() {
        for( Symbolizer x : super.getParent().getRule().getSymbolizers() ) {
            if (x instanceof PointSymbolizer) {
                PointSymbolizer pnt = (PointSymbolizer) x;
                Expression ex = pnt.getGeometry();
                boolean endpnt = ex instanceof FilterFunction_endPoint;
                boolean startpnt = ex instanceof FilterFunction_startPoint;
                if (endpnt || startpnt) {
                    GraphicalSymbol gs = pnt.getGraphic().graphicalSymbols().get(0);
                    if (gs instanceof Mark) {
                        String name = ((Mark) gs).getWellKnownName().evaluate(null, String.class);
                        if (StyleUtilities.lineEndStyles.values().contains(name)) {
                            if (endpnt) {
                                endPointStyle = new PointSymbolizerWrapper(pnt, super.getParent());
                            } else if (startpnt) {
                                startPointStyle = new PointSymbolizerWrapper(pnt, super.getParent());
                            }
                        }
                    }
                }
            }
        }
    }

    public PointSymbolizerWrapper getEndPointStyle() {
        return endPointStyle;
    }

    public PointSymbolizerWrapper getStartPointStyle() {
        return startPointStyle;
    }

    public void setEndPointStyle( String geomName, String wkgName, String size, String color ) {
        endPointStyle = updateEndpointStyle(geomName, endPointStyle, wkgName, size, color, false);
    }

    public void setStartPointStyle( String geomName, String wkgName, String size, String color ) {
        startPointStyle = updateEndpointStyle(geomName, startPointStyle, wkgName, size, color, true);
    }

    private PointSymbolizerWrapper updateEndpointStyle( String geomName, PointSymbolizerWrapper wrapper, String wkgName,
            String size, String color, boolean isStart ) {
        if (wkgName == null || wkgName.length() == 0) {
            if (wrapper != null) {
                getParent().getRule().symbolizers().remove(wrapper.getSymbolizer());
                return null;
            }
        }
        if (wrapper == null) {
            PointSymbolizer p = sf.createPointSymbolizer();
            if (isStart) {
                p.setGeometry(ff.function("startPoint", ff.property(geomName))); //$NON-NLS-1$
                p.getGraphic().setRotation(ff.add(ff.function("startAngle", ff.property(geomName)), ff.literal(-180))); // rotate //$NON-NLS-1$
                                                                                                                        // start
                                                                                                                        // 180
                                                                                                                        // degrees
            } else {
                p.setGeometry(ff.function("endPoint", ff.property(geomName))); //$NON-NLS-1$
                p.getGraphic().setRotation(ff.function("endAngle", ff.property(geomName))); //$NON-NLS-1$
            }
            wrapper = new PointSymbolizerWrapper(p, getParent());

            getParent().getRule().symbolizers().add(wrapper.getSymbolizer());
        }
        wrapper.setMarkName(wkgName);
        wrapper.setStrokeColor(color);
        wrapper.setFillColor(color);
        wrapper.setSize(size, false);
        return wrapper;
    }

    public void clearGraphicStroke() {
        if (stroke == null)
            return;
        stroke.setGraphicStroke(null);
        strokeGraphicStroke = null;
    }

    public Graphic getStrokeGraphicStroke() {
        return strokeGraphicStroke;
    }

    public void setStrokeGraphicStroke( Graphic strokeGraphicStroke ) {
        this.strokeGraphicStroke = strokeGraphicStroke;
        stroke.setGraphicStroke(strokeGraphicStroke);
    }

    public void setStrokeWidth( String strokeWidth, boolean isProperty ) {
        this.strokeWidth = strokeWidth;
        if (isProperty) {
            stroke.setWidth(ff.property(strokeWidth));
        } else {
            stroke.setWidth(ff.literal(strokeWidth));
        }
    }

    public void setStrokeColor( String strokeColor, boolean isProperty ) {
        this.strokeColor = strokeColor;
        if (isProperty) {
            stroke.setColor(ff.property(strokeColor));
        } else {
            if (strokeColor != null) {
                stroke.setColor(ff.literal(strokeColor));
            }
        }
    }

    public void setStrokeOpacity( String strokeOpacity, boolean isProperty ) {
        this.strokeOpacity = strokeOpacity;
        if (isProperty) {
            stroke.setOpacity(ff.property(strokeOpacity));
        } else {
            stroke.setOpacity(ff.literal(strokeOpacity));
        }

        // update end point styles if applicable
        if (endPointStyle != null) {
            endPointStyle.setStrokeOpacity(strokeOpacity, isProperty);
            endPointStyle.setFillOpacity(strokeOpacity, isProperty);
        }
        if (startPointStyle != null) {
            startPointStyle.setStrokeOpacity(strokeOpacity, isProperty);
            startPointStyle.setFillOpacity(strokeOpacity, isProperty);
        }
    }

    public void setDash( String dash ) {
        this.dash = dash;
        float[] dashArray = StyleUtilities.getDash(dash);
        if (dashArray == null) {
            stroke.dashArray().clear();
        } else {
            List<Expression> dashArrayExpr = new ArrayList<>();
            for( float value : dashArray ) {
                dashArrayExpr.add(ff.literal(value));
            }
            stroke.setDashArray(dashArrayExpr);
        }
    }

    private float[] getDashArrayFloats() {
        List<Expression> dashArrayExpr = stroke.dashArray();
        if (dashArrayExpr == null) {
            return new float[0];
        }
        float[] dashArray = new float[dashArrayExpr.size()];
        int index = 0;
        for( Expression expression : dashArrayExpr ) {
            if (expression instanceof Literal) {
                Literal literal = (Literal) expression;
                dashArray[index] = literal.evaluate(null, Float.class);
            } else {
                throw new RuntimeException("Dash array is not literal: '" + expression + "'.");
            }
            index++;
        }
        return dashArray;
    }

    public void setDashOffset( String dashOffset ) {
        this.dashOffset = dashOffset;
        if (dashOffset != null && dashOffset.length() > 0) {
            stroke.setDashOffset(ff.literal(dashOffset));
        }
    }

    public void setLineCap( String lineCap ) {
        this.lineCap = lineCap;
        stroke.setLineCap(ff.literal(lineCap));
    }

    public void setLineJoin( String lineJoin ) {
        this.lineJoin = lineJoin;
        stroke.setLineJoin(ff.literal(lineJoin));
    }

    // getters
    public String getStrokeColor() {
        return strokeColor;
    }

    public String getStrokeOpacity() {
        return strokeOpacity;
    }

    public String getStrokeWidth() {
        return strokeWidth;
    }

    public String getDash() {
        return dash;
    }

    public String getDashOffset() {
        return dashOffset;
    }

    public String getLineCap() {
        return lineCap;
    }

    public String getLineJoin() {
        return lineJoin;
    }

}
