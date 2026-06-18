package org.hortonmachine.hmachine.modules.statistics.kriging.interpolationdata;

import java.util.LinkedHashMap;

import org.locationtech.jts.geom.Coordinate;

/**
 * The InterpolationDataProvider interface defines the contract for providing
 * spatial data required for kriging interpolation. Implementations of this
 * interface should supply a collection of coordinates (each uniquely identified
 * by an Integer key) and a mechanism to retrieve the corresponding z value,
 * which typically represents the attribute (e.g., elevation) used in
 * interpolation.
 *
 * <p>
 * <strong>Improvements and Considerations:</strong>
 * </p>
 * <ul>
 * <li><em>Generics:</em> If flexibility for key types is desired, consider
 * using generics (e.g., <code>LinkedHashMap<K, Coordinate></code>) instead of
 * hardcoding <code>Integer</code>.</li>
 * <li><em>Error Handling:</em> Implementations should not return
 * <code>null</code> in case of an error. Instead, they could throw a custom
 * unchecked exception (e.g., <code>InterpolationDataException</code>) to ensure
 * the error is properly handled by the caller.</li>
 * <li><em>Validation:</em> In the <code>getValueAt</code> method, it is
 * advisable to validate the input <code>Coordinate</code> and ensure that its z
 * value is within expected bounds. If not, throwing an
 * <code>IllegalArgumentException</code> or a custom exception might be
 * appropriate.</li>
 * </ul>
 */
public interface InterpolationDataProvider {
	/**
	 * Returns a map with unique identifiers as keys and their corresponding
	 * coordinates as values.
	 *
	 * <p>
	 * The map should preserve the insertion order (for example, by using a
	 * LinkedHashMap) to maintain a consistent ordering of coordinates.
	 * </p>
	 *
	 * <p>
	 * <strong>Possible Improvement:</strong> Instead of returning <code>null</code>
	 * when an error occurs, the implementation could throw a custom exception
	 * (e.g., <code>InterpolationDataException</code>).
	 * </p>
	 *
	 * @return A LinkedHashMap mapping unique Integer keys to Coordinate objects.
	 */
	LinkedHashMap<Integer, Coordinate> getCoordinates();

	/**
	 * Returns the z value from the given Coordinate.
	 *
	 * <p>
	 * This method assumes that the <code>z</code> field of the Coordinate contains
	 * the value necessary for interpolation (e.g., elevation).
	 * </p>
	 *
	 * <p>
	 * <strong>Possible Improvement:</strong> Validate the input
	 * <code>Coordinate</code> (e.g., ensure it is not <code>null</code> and that
	 * the <code>z</code> value is valid). If validation fails, consider throwing an
	 * <code>IllegalArgumentException</code> or a custom exception.
	 * </p>
	 *
	 * @param coordinate The coordinate from which to retrieve the z value.
	 * @return The z value from the coordinate.
	 */

	double getValueAt(Coordinate coordinate);
}
