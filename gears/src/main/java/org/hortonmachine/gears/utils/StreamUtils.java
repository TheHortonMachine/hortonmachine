/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.gears.utils;

import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Utilities for Streams. This more a streams j8 documentation then something to really use.
 *
 * <p>Use .peek(e->sysout) to debug without interference on the stream's items</p>
 * <p>Use primitive streams were possible: {@link IntStream}, {@link LongStream}, {@link DoubleStream}.</p>
 *
 * <p>Intermediate Methods, which produce again streams:
 * <ul>
 *  <li>map</li> 
 *  <li>filter </li>
 *  <li>distinct </li>
 *  <li>sorted </li>
 *  <li>peek </li>
 *  <li>limit </li>
 *  <li>substream </li>
 *  <li>parallel </li>
 *  <li>sequential </li>
 *  <li>unordered</li>
 * <ul>
 * </p>
 * <p>Terminate Methods, after which the stream is consumed and no more operations can be performed on it:
 * <li>forEach</li> 
 * <li>forEachOrdered </li>
 * <li>toArray </li>
 * <li>reduce </li>
 * <li>collect </li>
 * <li>min </li>
 * <li>max </li>
 * <li>count </li>
 * <li>anyMatch </li>
 * <li>allMatch </li>
 * <li>noneMatch </li>
 * <li>findFirst </li>
 * <li>findAny </li>
 * <li>iterator</li>
 * </p>
 * <p>Short-circuit Methods, that stop stream processing once conditions are satisfied.
 * <li>anyMatch</li> 
 * <li>allMatch </li>
 * <li>noneMatch </li>
 * <li>findFirst </li>
 * <li>findAny </li>
 * <li>limit </li>
 * <li>substream</li>
 * </p>
 * 
 * @author Antonello Andrea (www.hydrologis.com)
 */
public class StreamUtils {

    public static <T> Stream<T> fromArray( T[] array ) {
        return Stream.of(array);
    }

    public static <T> Stream<T> fromList( List<T> list ) {
        return list.stream();
    }

    public static <T> Stream<T> fromListParallel( List<T> list ) {
        return list.parallelStream();
    }

    public static <T> Stream<T> fromSupplier( Supplier<T> supplier ) {
        return Stream.generate(supplier);
    }

    public static <T> Stream<T> distinct( Stream<T> stream ) {
        return stream.distinct();
    }

    public static long countStrings( Stream<String> stream, boolean distinct, boolean ignoreCase ) {
        if (ignoreCase && distinct) {
            return stream.map(String::toLowerCase).distinct().count();
        } else if (ignoreCase) {
            return stream.map(String::toLowerCase).count();
        } else if (distinct) {
            return stream.distinct().count();
        } else {
            return stream.count();
        }
    }

    public static String getString( Stream<Object> stream, String separator, String prefix, String suffix ) {
        if (prefix != null || suffix != null) {
            if (prefix == null)
                prefix = "";
            if (suffix == null)
                suffix = "";
            return stream.map(Object::toString).collect(Collectors.joining(separator, prefix, suffix));
        }
        return stream.map(Object::toString).collect(Collectors.joining(separator));
    }

    public static <T> List<T> toList( Stream<T> stream ) {
        return stream.collect(Collectors.toList());
    }

    public static <T> Set<T> toSet( Stream<T> stream ) {
        return stream.collect(Collectors.toSet());
    }

    public static <T> TreeSet<T> toTreeSet( Stream<T> stream ) {
        return stream.collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Collect stream to map.
     * 
     * @param stream the stream to convert.
     * @param keySupplier the supplier for the key, ex. Object::getId()
     * @param valueSupplier the value supplier, ex. Object::getValue() 
     * @return the map.
     */
    public static <T, K, V> Map<K, V> toMap( Stream<T> stream, Function<T, K> keySupplier, Function<T, V> valueSupplier ) {
        return stream.collect(Collectors.toMap(keySupplier, valueSupplier));
    }

    public static <T, K> Map<K, T> toMapWithToStringValue( Stream<T> stream, Function<T, K> keySupplier ) {
        return stream.collect(Collectors.toMap(keySupplier, Function.identity()));
    }

    /**
     * Collect a map by grouping based on the object's field.
     * 
     * @param stream the stream to collect.
     * @param groupingFunction the function supplying the grouping field, ex. Object::getField()
     * @return the map of lists.
     */
    public static <T, K, R> Map<R, List<T>> toMapGroupBy( Stream<T> stream, Function<T, R> groupingFunction ) {
        return stream.collect(Collectors.groupingBy(groupingFunction));
        // to get a set: Collectors.groupingBy(groupingFunction, Collectors.toSet())
    }

    /**
     * Split a stream into a map with true and false.
     * 
     * @param stream the stream to collect.
     * @param predicate the predicate to use to define true or false, ex. e->e.getValue().equals("test")
     * @return the map of lists.
     */
    public static <T> Map<Boolean, List<T>> toMapPartition( Stream<T> stream, Predicate<T> predicate ) {
        return stream.collect(Collectors.partitioningBy(predicate));
    }

    public static <T, R> void printLongStats( Stream<T> stream, ToLongFunction<T> summarizingFunction ) {
        LongSummaryStatistics summary = stream.collect(Collectors.summarizingLong(summarizingFunction));

        System.out.println(summary.getCount());
        System.out.println(summary.getSum());
        System.out.println(summary.getMin());
        System.out.println(summary.getMax());
        System.out.println(summary.getAverage());
    }

    /**
     * Find an element in the stream. 
     * 
     * @param stream the stream to check. This should be parallel.
     * @param predicate the predicate to use.
     * @return the element or null.
     */
    public static <T> T findAny( Stream<T> stream, Predicate<T> predicate ) {
        Optional<T> element = stream.filter(predicate).findAny();
        return element.orElse(null);
    }

    public static <T> Stream<T> findAll( Stream<T> stream, Predicate<T> predicate ) {
        Stream<T> filteredStream = stream.filter(predicate);
        return filteredStream;
    }

}
