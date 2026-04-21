package org.hortonmachine.gears.utils.features;


import java.util.Objects;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;

/**
 * Fluent helper for creating {@link SimpleFeatureType} and {@link SimpleFeature}
 * instances with less boilerplate than the standard GeoTools builders.
 *
 * <p>Typical usage:
 *
 * <pre>
 * HMFeatureBuilder fb = HMFeatureBuilder.type("points")
 *         .crs(crs)
 *         .geom("the_geom", Point.class)
 *         .attr("name", String.class)
 *         .attr("elevation", Double.class);
 *
 * var f1 = fb.add(point1).add("Bolzano").add(262.0).build();
 * var f2 = fb.add(point2).add("Merano").add(325.0).build();
 * </pre>
 */
public class HMFeatureBuilder {

    private final SimpleFeatureTypeBuilder typeBuilder;
    private SimpleFeatureType featureType;
    private SimpleFeatureBuilder featureBuilder;

    private int attributeCount = 0;
    private int currentValueCount = 0;

    private HMFeatureBuilder(String typeName) {
    	Objects.requireNonNull(typeName, "Type name must not be null");
        typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(typeName);
    }

    /**
     * Create a new feature builder for the given feature type name.
     *
     * @param typeName the feature type name.
     * @return a new builder.
     */
    public static HMFeatureBuilder type(String typeName) {
        return new HMFeatureBuilder(typeName);
    }

	/**
	 * Add the geometry attribute to the schema.
	 *
	 * @param name the geometry attribute name, usually {@code the_geom}.
	 * @param geometryClass the geometry class.
	 * @param crs the coordinate reference system.
	 * @return this builder.
	 */
    public HMFeatureBuilder geom(String name, Class<?> geometryClass, CoordinateReferenceSystem crs) throws Exception {
		checkSchemaNotBuilt();
		typeBuilder.setDefaultGeometry(name);
		typeBuilder.setCRS(crs);
		typeBuilder.add(name, geometryClass);
		attributeCount++;
		return this;
	}

    /**
     * Add the geometry attribute to the schema.
     *
     * @param name the geometry attribute name, usually {@code the_geom}.
     * @param geometryClass the geometry class.
     * @param epsgCode the EPSG code for the geometry's CRS.
     * @return this builder.
     */
    public HMFeatureBuilder geom(String name, Class<?> geometryClass, String epsgCode) throws Exception {
		CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs(epsgCode);
		return geom(name, geometryClass, crs);
    }

    /**
     * Add a non-geometry attribute to the schema.
     *
     * @param name the attribute name.
     * @param attributeClass the attribute class.
     * @return this builder.
     */
    public HMFeatureBuilder attr(String name, Class<?> attributeClass) {
        checkSchemaNotBuilt();
        typeBuilder.add(name, attributeClass);
        attributeCount++;
        return this;
    }

    /**
     * Add the next value for the feature currently being assembled.
     *
     * <p>Values must be added in schema order.
     *
     * @param value the next attribute value.
     * @return this builder.
     */
    public HMFeatureBuilder add(Object value) {
        ensureFeatureBuilder();
        if (currentValueCount >= attributeCount) {
            throw new IllegalStateException(
                    "Too many values added. Schema expects " + attributeCount + " values."
            );
        }
        featureBuilder.add(value);
        currentValueCount++;
        return this;
    }

    /**
     * Add all values for the next feature in one call.
     *
     * @param values the values in schema order.
     * @return this builder.
     */
    public HMFeatureBuilder addAll(Object... values) {
    	ensureFeatureBuilder();
        featureBuilder.addAll(values);
        currentValueCount += values.length;
        return this;
    }

    /**
     * Build a feature with a null feature id.
     *
     * @return the built feature.
     */
    public SimpleFeature build() {
        return build(null);
    }

    /**
     * Build a feature with the supplied feature id.
     *
     * @param featureId the feature id, or {@code null}.
     * @return the built feature.
     */
    public SimpleFeature build(String featureId) {
        ensureFeatureBuilder();

        if (currentValueCount != attributeCount) {
            throw new IllegalStateException(
                    "Wrong number of values. Schema expects " + attributeCount
                            + " values, but got " + currentValueCount + "."
            );
        }

        SimpleFeature feature = featureBuilder.buildFeature(featureId);

        // GeoTools resets the underlying builder on buildFeature(...),
        // but we still reset our own counter here.
        currentValueCount = 0;

        return feature;
    }

    /**
     * Reset the current feature assembly without changing the schema.
     *
     * @return this builder.
     */
    public HMFeatureBuilder reset() {
        ensureFeatureBuilder();
        featureBuilder.reset();
        currentValueCount = 0;
        return this;
    }

    /**
     * Get the built feature type.
     *
     * @return the feature type.
     */
    public SimpleFeatureType getFeatureType() {
        ensureFeatureBuilder();
        return featureType;
    }

    private void checkSchemaNotBuilt() {
        if (featureType != null) {
            throw new IllegalStateException(
                    "The feature type has already been built. Schema can no longer be modified."
            );
        }
    }

    private void ensureFeatureBuilder() {
        if (featureType == null) {
            featureType = typeBuilder.buildFeatureType();
            featureBuilder = new SimpleFeatureBuilder(featureType);
        }
    }
}