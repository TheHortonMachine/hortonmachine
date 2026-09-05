/*
 * $Id: WildcardFileFilter.java 15ea6210dfde 2015-01-05 odavid@colostate.edu $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3.ngmf.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.Collection;

/**
 * @author Jason Anderson
 * @version $Revision: 1004077 $ $Date: 2010-10-04 01:58:42 +0100 (Mon, 04 Oct
 * 2010) $
 * @since Commons IO 1.3
 */
public class WildcardFileFilter implements FilenameFilter, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The wildcards that will be used to match filenames.
     */
    private String[] wildcards;
    /**
     * Whether the comparison is case sensitive.
     */
    private IOCase caseSensitivity;


    /**
     * Construct a new case-sensitive wildcard filter for a single wildcard.
     *
     * @param wildcard the wildcard to match
     * @throws IllegalArgumentException if the pattern is null
     */
    public WildcardFileFilter(String wildcard) {
        this(wildcard, IOCase.SENSITIVE);
    }


    /**
     * Construct a new wildcard filter for a single wildcard specifying
     * case-sensitivity.
     *
     * @param wildcard the wildcard to match, not null
     * @param caseSensitivity how to handle case sensitivity, null means
     * case-sensitive
     * @throws IllegalArgumentException if the pattern is null
     */
    public WildcardFileFilter(String wildcard, IOCase caseSensitivity) {
        this(new String[]{wildcard}, caseSensitivity);
    }


    /**
     * Construct a new case-sensitive wildcard filter for an array of wildcards.
     * <p>
     * The array is not cloned, so could be changed after constructing the
     * instance. This would be inadvisable however.
     *
     * @param wildcards the array of wildcards to match
     * @throws IllegalArgumentException if the pattern array is null
     */
    public WildcardFileFilter(String[] wildcards) {
        this(wildcards, IOCase.SENSITIVE);
    }


    /**
     * Construct a new case-sensitive wildcard filter for a list of wildcards.
     *
     * @param wildcards the list of wildcards to match, not null
     * @throws IllegalArgumentException if the pattern list is null
     * @throws ClassCastException if the list does not contain Strings
     */
    public WildcardFileFilter(Collection<String> wildcards) {
        this(wildcards, IOCase.SENSITIVE);
    }


    /**
     * Construct a new wildcard filter for a list of wildcards specifying
     * case-sensitivity.
     *
     * @param wildcards the list of wildcards to match, not null
     * @param caseSensitivity how to handle case sensitivity, null means
     * case-sensitive
     * @throws IllegalArgumentException if the pattern list is null
     * @throws ClassCastException if the list does not contain Strings
     */
    public WildcardFileFilter(Collection<String> wildcards, IOCase caseSensitivity) {
        this(wildcards.toArray(new String[wildcards.size()]), caseSensitivity);
    }


    /**
     * Construct a new wildcard filter for an array of wildcards specifying
     * case-sensitivity.
     * <p>
     * The array is not cloned, so could be changed after constructing the
     * instance. This would be inadvisable however.
     *
     * @param wildcards the array of wildcards to match, not null
     * @param caseSensitivity how to handle case sensitivity, null means
     * case-sensitive
     * @throws IllegalArgumentException if the pattern array is null
     */
    public WildcardFileFilter(String[] wildcards, IOCase caseSensitivity) {
        if (wildcards == null || wildcards.length == 0) {
            throw new IllegalArgumentException("The wildcard array must not be null or empty");
        }
        this.caseSensitivity = caseSensitivity;
        this.wildcards = new String[wildcards.length];
        System.arraycopy(wildcards, 0, this.wildcards, 0, wildcards.length);
    }

    //-----------------------------------------------------------------------

    /**
     * Checks to see if the filename matches one of the wildcards.
     *
     * @param dir the file directory
     * @param name the filename
     * @return true if the filename matches one of the wildcards
     */
    @Override
    public boolean accept(File dir, String name) {
        for (String wildcard : wildcards) {
            if (wildcard == null) {
                throw new RuntimeException("null wildcard.");
            }
            if (FilenameUtils.wildcardMatch(name, wildcard, caseSensitivity)) {
                return true;
            }
        }
        return false;
    }

//    /**
//     * Checks to see if the filename matches one of the wildcards.
//     *
//     * @param file the file to check
//     * @return true if the filename matches one of the wildcards
//     */
//    @Override
//    public boolean accept(File file) {
//        String name = file.getName();
//        for (String wildcard : wildcards) {
//            if (FilenameUtils.wildcardMatch(name, wildcard, caseSensitivity)) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * Provide a String representation of this file filter.
     *
     * @return a String representation
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append("(");
        if (wildcards != null) {
            for (int i = 0; i < wildcards.length; i++) {
                if (i > 0) {
                    buffer.append(",");
                }
                buffer.append(wildcards[i]);
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

}
