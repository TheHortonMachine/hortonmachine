/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules.v.vectorreshaper;

import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Documentation;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
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

@Description("Module to reshape a vector.")
@Documentation("VectorReshaper.html")
@Author(name = "Andrea Antonello, Jody Garnett", contact = "http://www.hydrologis.com")
@Keywords("Reshape, Vector, VectorFilter")
@Status(Status.CERTIFIED)
@Label(JGTConstants.VECTORPROCESSING)
@Name("reshape")
@License("General Public License Version 3 (GPLv3)")
@SuppressWarnings("nls")
public class VectorReshaper extends JGTModel {

    @Description("The vector to reshape.")
    @In
    public SimpleFeatureCollection inVector;

    @Description("The ECQL reshape function.")
    @In
    public String pCql = null;

    @Description("The list of fields to remove, comma separated.")
    @In
    public String pRemove = null;

    @Description("The new reshaped vector.")
    @Out
    public SimpleFeatureCollection outVector;

    private SimpleFeature sample = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        checkNull(inVector);

        List<String> removeNames = new ArrayList<String>();
        if (pRemove != null) {
            String[] split = pRemove.split(",");
            for( String string : split ) {
                removeNames.add(string.trim());
            }
        }

        final SimpleFeatureType originalFeatureType = inVector.getSchema();
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

        outVector = FeatureCollections.newCollection();

        final SimpleFeatureBuilder build = new SimpleFeatureBuilder(newFeatureType);
        inVector.accepts(new FeatureVisitor(){
            public void visit( Feature rawFeature ) {
                SimpleFeature feature = (SimpleFeature) rawFeature;
                for( int i = 0; i < expressions.size(); i++ ) {
                    build.add(expressions.get(i).evaluate(feature));
                }
                SimpleFeature created = build.buildFeature(feature.getID());
                outVector.add(created);
            }
        }, null);

    }

    private SimpleFeature getSample() {
        FeatureIterator<SimpleFeature> iterator = inVector.features();
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
