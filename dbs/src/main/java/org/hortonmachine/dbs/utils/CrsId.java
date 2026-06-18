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
package org.hortonmachine.dbs.utils;

/**
 * Immutable value type combining a CRS authority name and numeric code.
 *
 * <p>Examples: {@code CrsId.ofEpsg(4326)}, {@code CrsId.of("ESRI:102700")},
 * {@code CrsId.of("4326")} (bare integer defaults to EPSG).
 *
 * <p>Backwards compatibility: all existing code that stored a bare integer SRID
 * assumed EPSG. Use {@link #fromSrid(int)} or {@link #ofEpsg(int)} for those sites.
 */
public final class CrsId {

    public static final String EPSG = "EPSG";

    public final String authority;
    public final int code;

    private CrsId( String authority, int code ) {
        this.authority = authority.toUpperCase().trim();
        this.code = code;
    }

    /** Create an EPSG-authority CrsId from a bare integer code. */
    public static CrsId ofEpsg( int code ) {
        return new CrsId(EPSG, code);
    }

    /**
     * Parse an authority-qualified string such as {@code "EPSG:4326"} or
     * {@code "ESRI:102700"}. A bare integer string (no colon) is treated as EPSG.
     */
    public static CrsId of( String authorityCode ) {
        if (authorityCode == null || authorityCode.isBlank()) {
            throw new IllegalArgumentException("authorityCode must not be null or blank");
        }
        String trimmed = authorityCode.trim();
        int colonIdx = trimmed.indexOf(':');
        if (colonIdx < 0) {
            return new CrsId(EPSG, Integer.parseInt(trimmed));
        }
        String auth = trimmed.substring(0, colonIdx).trim();
        int code = Integer.parseInt(trimmed.substring(colonIdx + 1).trim());
        return new CrsId(auth, code);
    }

    /** Backwards-compatible factory: treats a bare integer SRID as EPSG. */
    public static CrsId fromSrid( int srid ) {
        return ofEpsg(srid);
    }

    /** Returns the full authority-qualified string, e.g. {@code "EPSG:4326"} or {@code "ESRI:102700"}. */
    public String toAuthorityCode() {
        return authority + ":" + code;
    }

    /** Returns only the numeric code part. */
    public int toSrid() {
        return code;
    }

    public boolean isEpsg() {
        return EPSG.equals(authority);
    }

    @Override
    public String toString() {
        return toAuthorityCode();
    }

    @Override
    public boolean equals( Object o ) {
        if (this == o)
            return true;
        if (!(o instanceof CrsId))
            return false;
        CrsId other = (CrsId) o;
        return code == other.code && authority.equals(other.authority);
    }

    @Override
    public int hashCode() {
        return 31 * authority.hashCode() + code;
    }
}
