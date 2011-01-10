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
package org.jgrasstools.gears.modules.v.reshape;

import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Reshaper module adapted from uDigs reshape operation by Jody Garnett.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@Description("Module that extends feature collections.")
@Author(name = "Andrea Antonello, Jody Garnett", contact = "www.hydrologis.com")
@Keywords("Reshape, Vector")
@Status(Status.DRAFT)
@Label(JGTConstants.VECTORPROCESSING)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
@SuppressWarnings("nls")
public class FeatureReshaper extends JGTModel {

    @Description("The features to reshape.")
    @In
    public SimpleFeatureCollection inFeatures;

    @Description("The CQL reshape function.")
    @In
    public String pCql = null;

    @Description("List of fields to remove, comma separated.")
    @In
    public String pRemove = null;

    @Description("The filtered features.")
    @Out
    public SimpleFeatureCollection outFeatures;

    private SimpleFeature sample = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outFeatures == null, doReset)) {
            return;
        }
        checkNull(inFeatures);

        List<String> removeNames = new ArrayList<String>();
        if (pRemove != null) {
            String[] split = pRemove.split(",");
            for( String string : split ) {
                removeNames.add(string.trim());
            }
        }

        final SimpleFeatureType originalFeatureType = inFeatures.getSchema();
        List<AttributeDescriptor> attributeDescriptors = originalFeatureType
                .getAttributeDescriptors();
        StringBuilder sB = new StringBuilder();
        for( AttributeDescriptor attributeDescriptor : attributeDescriptors ) {
            String name = attributeDescriptor.getLocalName();
            if (removeNames.contains(name.trim())) {
                continue;
            }
            sB.append(name);
            sB.append("=");
            sB.append(name);
            sB.append("\n");
        }
        if (pCql != null && pCql.length() > 0) {
            sB.append(pCql);
        }

        sample = getSample();

        String expressionString = sB.toString();

        List<String> names = createNameList(expressionString);
        final List<Expression> expressions = createExpressionList(expressionString);
        SimpleFeatureType newFeatureType = createFeatureType(expressionString, originalFeatureType,
                names, expressions);

        outFeatures = FeatureCollections.newCollection();

        final SimpleFeatureBuilder build = new SimpleFeatureBuilder(newFeatureType);
        inFeatures.accepts(new FeatureVisitor(){
            public void visit( Feature rawFeature ) {
                SimpleFeature feature = (SimpleFeature) rawFeature;
                for( int i = 0; i < expressions.size(); i++ ) {
                    build.add(expressions.get(i).evaluate(feature));
                }
                SimpleFeature created = build.buildFeature(feature.getID());
                outFeatures.add(created);
            }
        }, null);

    }

    private SimpleFeature getSample() {
        FeatureIterator<SimpleFeature> iterator = inFeatures.features();
        try {
            if (!iterator.hasNext()) {
                throw new ModelsRuntimeException("Input featurecollection is empty.", this
                        .getClass().getSimpleName());
            }
            return iterator.next();
        } finally {
            iterator.close();
        }
    }

    /**
     * You cannot call this once the dialog is closed, see the okPressed method.
     * @param originalFeatureType 
     * @param expressions 
     * @param names 
     * @return a SimpleFeatureType created based on the contents of Text
     */
    private SimpleFeatureType createFeatureType( String expressionString,
            SimpleFeatureType originalFeatureType, List<String> names, List<Expression> expressions )
            throws SchemaException {

        SimpleFeatureTypeBuilder build = new SimpleFeatureTypeBuilder();

        for( int i = 0; i < names.size(); i++ ) {
            String name = names.get(i);

            Expression expression = expressions.get(i);

            Object value = expression.evaluate(sample);

            // hack because sometimes expression returns null. I think the real bug is with
            // AttributeExpression
            Class< ? > binding = null;
            if (value == null) {
                if (expression instanceof PropertyName) {
                    String path = ((PropertyName) expression).getPropertyName();
                    AttributeType attributeType = sample.getFeatureType().getType(path);
                    if (attributeType == null) {
                        throw new ModelsIllegalargumentException("Attribute type is null", this
                                .getClass().getSimpleName());
                    }
                    binding = attributeType.getClass();
                }
            } else {
                binding = value.getClass();
            }

            if (binding == null) {
                throw new ModelsIllegalargumentException("Binding is null", this.getClass()
                        .getSimpleName());
            }

            if (Geometry.class.isAssignableFrom(binding)) {
                CoordinateReferenceSystem crs;
                AttributeType originalAttributeType = originalFeatureType.getType(name);
                if (originalAttributeType instanceof GeometryType) {
                    crs = ((GeometryType) originalAttributeType).getCoordinateReferenceSystem();
                } else {
                    crs = originalFeatureType.getCoordinateReferenceSystem();
                }
                build.crs(crs);

                build.add(name, binding);
            } else {
                build.add(name, binding);
            }
        }
        build.setName(getNewTypeName(originalFeatureType.getTypeName()));

        return build.buildFeatureType();
    }

    /**
     * You cannot call this once the dialog is closed, see the okPressed method.
     * @param expressionString 
     * 
     * @return a SimpleFeatureType created based on the contents of Text
     */
    private List<String> createNameList( String expressionString ) {
        List<String> list = new ArrayList<String>();

        String definition = expressionString.replaceAll("\r", "\n")
                .replaceAll("[\n\r][\n\r]", "\n");
        for( String line : definition.split("\n") ) {
            int mark = line.indexOf("=");
            if (mark != -1) {
                String name = line.substring(0, mark).trim();
                if (list.contains(name)) {
                    System.out.println("Name already existing");
                    continue;
                }
                list.add(name);
            }
        }
        return list;
    }

    private List<Expression> createExpressionList( String expressionString ) {
        List<Expression> list = new ArrayList<Expression>();

        String definition = expressionString.replaceAll("\r", "\n")
                .replaceAll("[\n\r][\n\r]", "\n");
        for( String line : definition.split("\n") ) {
            int mark = line.indexOf("=");
            if (mark != -1) {
                String expressionDefinition = line.substring(mark + 1).trim();

                Expression expression;
                try {
                    expression = CQL.toExpression(expressionDefinition);
                } catch (CQLException e) {
                    throw new ModelsRuntimeException(e.toString(), this);
                }
                list.add(expression);
            }
        }
        return list;
    }

    static int count = 0;
    public String getNewTypeName( String typeName ) {
        return typeName + (count++);
    }

}
