/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.utils.time;

import java.io.File;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the files of a folder that have a date in their name, as the monthly or daily rasters of
 * a time series, as <code>prism_ppt_202501.tif</code> or <code>2_0/20180107.tif</code>.
 *
 * <p>The date is read with a pattern of the letters of {@link java.time.format.DateTimeFormatter}:
 * <code>yyyy</code> year, <code>MM</code> month, <code>dd</code> day of the month,
 * <code>DDD</code> day of the year, <code>HH</code> hour and <code>mm</code> minutes. Each run of
 * letters is a group of as many digits, the other characters are taken as they are. The digits
 * of the date can't be part of a longer number, so that <code>yyyyMM</code> does not read the
 * start of <code>20180107</code>.</p>
 *
 * <p>Without <code>/</code> the pattern is looked for in the name of the file, without extension,
 * else in its path from the scanned folder, as <code>yyyy/DDD</code> for
 * <code>2018/045.tif</code>. If more parts of the name match, the last one is used.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class DatedFilesScanner {

    /**
     * The patterns tried when none is given, the most specific first, which wins over the
     * others when they date the same files.
     */
    public static final List<String> AUTO_PATTERNS = List.of("yyyyMMdd", "yyyy-MM-dd", "yyyy_MM_dd", "yyyy.MM.dd", "yyyyMM",
            "yyyy-MM", "yyyy_MM", "yyyy");

    /** The extensions of the raster files read by HortonMachine: GeoTIFF and ESRI ASCII grids. */
    public static final Set<String> RASTER_EXTENSIONS = Set.of("tif", "tiff", "asc");

    /**
     * A file with its date.
     *
     * @param dateTime the date read from the name, at midnight if the pattern has no hour.
     * @param file the file.
     */
    public record DatedFile( LocalDateTime dateTime, File file ) {
    }

    /**
     * The files dated by a pattern.
     *
     * @param pattern the pattern used, also when it was chosen among the {@link #AUTO_PATTERNS},
     *          <code>null</code> if none of them dated any file.
     * @param files the dated files, sorted by date, one per date.
     * @param undated the files the pattern doesn't date.
     * @param duplicates the files of a date that another file already has, left out of the dated
     *          files: of the files of a date, the one nearest to the scanned folder is kept, as
     *          <code>2_0/20180330.tif</code> over <code>2_0/tmp/20180330.tif</code>.
     */
    public record ScanResult( String pattern, List<DatedFile> files, List<File> undated, List<DatedFile> duplicates ) {
        /**
         * @return <code>true</code> if the pattern has an hour, so that the dates are shown with
         *          their time.
         */
        public boolean hasTime() {
            return pattern != null && pattern.contains("H");
        }
    }

    /**
     * A pattern made into a regular expression, with the fields of its groups.
     */
    private record CompiledPattern( String text, Pattern regex, List<Character> fields, boolean onPath ) {
    }

    private DatedFilesScanner() {
    }

    /**
     * List the files of a folder with the given extensions, sorted by their path. The hidden
     * files, starting with a dot, are left out.
     *
     * @param folder the folder.
     * @param recursive if <code>true</code>, also the files of the sub folders.
     * @param extensions the extensions, lower case and without dot, as {@link #RASTER_EXTENSIONS}.
     * @return the files.
     */
    public static List<File> listFiles( File folder, boolean recursive, Set<String> extensions ) {
        List<File> files = new ArrayList<>();
        collect(folder, recursive, extensions, files);
        files.sort(Comparator.comparing(File::getAbsolutePath));
        return files;
    }

    private static void collect( File folder, boolean recursive, Set<String> extensions, List<File> files ) {
        File[] children = folder.listFiles();
        if (children == null) {
            return;
        }
        for( File child : children ) {
            if (child.getName().startsWith(".")) {
                continue;
            }
            if (child.isDirectory()) {
                if (recursive) {
                    collect(child, true, extensions, files);
                }
            } else if (extensions.contains(extension(child.getName()))) {
                files.add(child);
            }
        }
    }

    /**
     * Scan the files of a folder for their dates.
     *
     * @param folder the folder.
     * @param recursive if <code>true</code>, also the files of the sub folders.
     * @param extensions the extensions of the files, as {@link #RASTER_EXTENSIONS}.
     * @param pattern the pattern of the dates, <code>null</code> or blank to choose one of the
     *          {@link #AUTO_PATTERNS}.
     * @return the dated files.
     * @throws IllegalArgumentException if the pattern is not valid.
     */
    public static ScanResult scan( File folder, boolean recursive, Set<String> extensions, String pattern ) {
        return scan(folder, listFiles(folder, recursive, extensions), pattern);
    }

    /**
     * Date files already listed, as with {@link #listFiles(File, boolean, Set)}, so that a pattern
     * can be tried without listing them again.
     *
     * @param folder the folder the files were listed from, the start of their paths for the
     *          patterns with <code>/</code>.
     * @param files the files.
     * @param pattern the pattern of the dates, <code>null</code> or blank to choose the one of
     *          the {@link #AUTO_PATTERNS} that finds the most dates.
     * @return the dated files.
     * @throws IllegalArgumentException if the pattern is not valid.
     */
    public static ScanResult scan( File folder, List<File> files, String pattern ) {
        if (pattern != null && !pattern.isBlank()) {
            return scan(folder, files, compile(pattern.trim()));
        }
        ScanResult best = null;
        for( String autoPattern : AUTO_PATTERNS ) {
            ScanResult result = scan(folder, files, compile(autoPattern));
            // the most dates, not the most files: yyyy dates all the files of a year
            if (best == null || result.files().size() > best.files().size()) {
                best = result;
            }
        }
        if (best == null || best.files().isEmpty()) {
            return new ScanResult(null, List.of(), new ArrayList<>(files), List.of());
        }
        return best;
    }

    private static ScanResult scan( File folder, List<File> files, CompiledPattern pattern ) {
        String folderPath = folder.getAbsolutePath();
        List<DatedFile> dated = new ArrayList<>();
        List<File> undated = new ArrayList<>();
        for( File file : files ) {
            LocalDateTime dateTime = readDate(pattern, matchText(folderPath, file, pattern.onPath()));
            if (dateTime == null) {
                undated.add(file);
            } else {
                dated.add(new DatedFile(dateTime, file));
            }
        }
        // of the files of a date, the nearest to the folder, then the first by path
        dated.sort(Comparator.comparing(DatedFile::dateTime).thenComparingInt(( DatedFile d ) -> depth(d.file()))
                .thenComparing(d -> d.file().getAbsolutePath()));
        List<DatedFile> unique = new ArrayList<>();
        List<DatedFile> duplicates = new ArrayList<>();
        Set<LocalDateTime> seen = new HashSet<>();
        for( DatedFile datedFile : dated ) {
            if (seen.add(datedFile.dateTime())) {
                unique.add(datedFile);
            } else {
                duplicates.add(datedFile);
            }
        }
        return new ScanResult(pattern.text(), unique, undated, duplicates);
    }

    /**
     * Check a pattern.
     *
     * @param pattern the pattern.
     * @return <code>null</code> if it is valid, else what is wrong, for the user.
     */
    public static String validate( String pattern ) {
        try {
            compile(pattern);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /**
     * @param pattern the pattern, as <code>yyyy_DDD</code>.
     * @return the pattern as a regular expression, with a group per field.
     * @throws IllegalArgumentException if the pattern is not valid.
     */
    private static CompiledPattern compile( String pattern ) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("The pattern is empty.");
        }
        Map<Character, Integer> sizes = Map.of('y', 4, 'M', 2, 'd', 2, 'D', 3, 'H', 2, 'm', 2);
        StringBuilder regex = new StringBuilder("(?<!\\d)");
        List<Character> fields = new ArrayList<>();
        Map<Character, Integer> counts = new HashMap<>();
        int i = 0;
        while( i < pattern.length() ) {
            char c = pattern.charAt(i);
            int end = i;
            while( end < pattern.length() && pattern.charAt(end) == c ) {
                end++;
            }
            int run = end - i;
            if (Character.isLetter(c)) {
                Integer size = sizes.get(c);
                if (size == null) {
                    throw new IllegalArgumentException("Unknown letter '" + c + "': use yyyy, MM, dd, DDD, HH and mm.");
                }
                if (run != size) {
                    String letters = String.valueOf(c).repeat(size);
                    throw new IllegalArgumentException("'" + pattern.substring(i, end) + "' needs to be " + letters + ".");
                }
                if (counts.merge(c, 1, Integer::sum) > 1) {
                    throw new IllegalArgumentException("'" + c + "' is in the pattern more than once.");
                }
                regex.append("(\\d{").append(size).append("})");
                fields.add(c);
            } else {
                regex.append(Pattern.quote(pattern.substring(i, end)));
            }
            i = end;
        }
        regex.append("(?!\\d)");
        if (!fields.contains('y')) {
            throw new IllegalArgumentException("The pattern needs the year, as yyyy.");
        }
        if (fields.contains('D') && (fields.contains('M') || fields.contains('d'))) {
            throw new IllegalArgumentException("Use the day of the year DDD or the month and day MM dd, not both.");
        }
        if (fields.contains('d') && !fields.contains('M')) {
            throw new IllegalArgumentException("The day dd needs the month MM.");
        }
        if (fields.contains('m') && !fields.contains('H')) {
            throw new IllegalArgumentException("The minutes mm need the hour HH.");
        }
        return new CompiledPattern(pattern, Pattern.compile(regex.toString()), fields, pattern.contains("/"));
    }

    /**
     * @return the text the pattern is looked for in: the name without extension, or the path from
     *          the folder, with <code>/</code> separators and without extension.
     */
    private static String matchText( String folderPath, File file, boolean onPath ) {
        if (!onPath) {
            return withoutExtension(file.getName());
        }
        String path = file.getAbsolutePath();
        if (path.startsWith(folderPath)) {
            path = path.substring(folderPath.length());
        }
        path = path.replace(File.separatorChar, '/');
        while( path.startsWith("/") ) {
            path = path.substring(1);
        }
        return withoutExtension(path);
    }

    /**
     * @return the date of the last match of the pattern in the text, <code>null</code> if it
     *          doesn't match or the digits are not a date, as a month 13.
     */
    private static LocalDateTime readDate( CompiledPattern pattern, String text ) {
        Matcher matcher = pattern.regex().matcher(text);
        LocalDateTime last = null;
        while( matcher.find() ) {
            LocalDateTime dateTime = toDate(pattern.fields(), matcher);
            if (dateTime != null) {
                last = dateTime;
            }
        }
        return last;
    }

    private static LocalDateTime toDate( List<Character> fields, Matcher matcher ) {
        int year = 0;
        int month = 1;
        int day = 1;
        int dayOfYear = -1;
        int hour = 0;
        int minute = 0;
        for( int f = 0; f < fields.size(); f++ ) {
            int value = Integer.parseInt(matcher.group(f + 1));
            switch( fields.get(f) ) {
            case 'y' -> year = value;
            case 'M' -> month = value;
            case 'd' -> day = value;
            case 'D' -> dayOfYear = value;
            case 'H' -> hour = value;
            case 'm' -> minute = value;
            default -> throw new IllegalStateException();
            }
        }
        try {
            LocalDate date = dayOfYear > 0 ? LocalDate.ofYearDay(year, dayOfYear) : LocalDate.of(year, month, day);
            return date.atTime(hour, minute);
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static int depth( File file ) {
        return file.getAbsolutePath().split(Pattern.quote(File.separator)).length;
    }

    private static String extension( String name ) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String withoutExtension( String name ) {
        int dot = name.lastIndexOf('.');
        int slash = name.lastIndexOf('/');
        return dot > slash ? name.substring(0, dot) : name;
    }
}
