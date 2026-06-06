package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.spatialite.SpatialiteGeometryColumns;
import org.hortonmachine.dbs.utils.CrsId;
import org.junit.Test;

/**
 * Unit tests for {@link CrsId} and the {@link GeometryColumn#getCrsCode()} helper.
 *
 * These tests cover:
 *   - all factory methods (ofEpsg, of, fromSrid)
 *   - authority normalisation (upper-casing, whitespace trimming)
 *   - bare-integer backwards-compatibility (no colon → defaults to EPSG)
 *   - isEpsg(), toAuthorityCode(), toString(), equals(), hashCode()
 *   - error cases (null, blank, non-numeric code)
 *   - GeometryColumn.getCrsCode() with default and non-EPSG authorities
 */
public class TestCrsId {

    // -----------------------------------------------------------------------
    // ofEpsg / fromSrid
    // -----------------------------------------------------------------------

    @Test
    public void testOfEpsg_authority() {
        CrsId id = CrsId.ofEpsg(4326);
        assertEquals("EPSG", id.authority);
    }

    @Test
    public void testOfEpsg_code() {
        CrsId id = CrsId.ofEpsg(4326);
        assertEquals(4326, id.code);
    }

    @Test
    public void testOfEpsg_isEpsgTrue() {
        assertTrue(CrsId.ofEpsg(4326).isEpsg());
    }

    @Test
    public void testOfEpsg_toAuthorityCode() {
        assertEquals("EPSG:4326", CrsId.ofEpsg(4326).toAuthorityCode());
    }

    @Test
    public void testFromSrid_equalToOfEpsg() {
        assertEquals(CrsId.ofEpsg(32632), CrsId.fromSrid(32632));
    }

    // -----------------------------------------------------------------------
    // of(String) – EPSG-qualified strings
    // -----------------------------------------------------------------------

    @Test
    public void testOf_epsgUppercase() {
        CrsId id = CrsId.of("EPSG:4326");
        assertEquals("EPSG", id.authority);
        assertEquals(4326, id.code);
    }

    @Test
    public void testOf_epsgLowercase_normalisedToUpper() {
        CrsId id = CrsId.of("epsg:4326");
        assertEquals("EPSG", id.authority);
        assertEquals(4326, id.code);
    }

    @Test
    public void testOf_epsgMixedCase_normalisedToUpper() {
        CrsId id = CrsId.of("Epsg:32632");
        assertEquals("EPSG", id.authority);
        assertEquals(32632, id.code);
    }

    @Test
    public void testOf_epsgWithSpaces_trimmed() {
        CrsId id = CrsId.of("  EPSG : 4326  ");
        assertEquals("EPSG", id.authority);
        assertEquals(4326, id.code);
    }

    @Test
    public void testOf_epsgIsEpsgTrue() {
        assertTrue(CrsId.of("EPSG:4326").isEpsg());
    }

    // -----------------------------------------------------------------------
    // of(String) – ESRI-qualified strings
    // -----------------------------------------------------------------------

    @Test
    public void testOf_esriUppercase_authority() {
        assertEquals("ESRI", CrsId.of("ESRI:102700").authority);
    }

    @Test
    public void testOf_esriUppercase_code() {
        assertEquals(102700, CrsId.of("ESRI:102700").code);
    }

    @Test
    public void testOf_esriLowercase_normalisedToUpper() {
        CrsId id = CrsId.of("esri:102700");
        assertEquals("ESRI", id.authority);
        assertEquals(102700, id.code);
    }

    @Test
    public void testOf_esriIsEpsgFalse() {
        assertFalse(CrsId.of("ESRI:102700").isEpsg());
    }

    @Test
    public void testOf_esriToAuthorityCode() {
        assertEquals("ESRI:102700", CrsId.of("ESRI:102700").toAuthorityCode());
    }

    // -----------------------------------------------------------------------
    // of(String) – bare integer (backwards compatibility, defaults to EPSG)
    // -----------------------------------------------------------------------

    @Test
    public void testOf_bareInt_authority() {
        assertEquals("EPSG", CrsId.of("4326").authority);
    }

    @Test
    public void testOf_bareInt_code() {
        assertEquals(4326, CrsId.of("4326").code);
    }

    @Test
    public void testOf_bareInt_isEpsgTrue() {
        assertTrue(CrsId.of("4326").isEpsg());
    }

    @Test
    public void testOf_bareInt_equalToOfEpsg() {
        assertEquals(CrsId.ofEpsg(4326), CrsId.of("4326"));
    }

    @Test
    public void testOf_bareInt32632_equalToOfEpsg() {
        assertEquals(CrsId.ofEpsg(32632), CrsId.of("32632"));
    }

    // -----------------------------------------------------------------------
    // toAuthorityCode / toString / toSrid
    // -----------------------------------------------------------------------

    @Test
    public void testToAuthorityCode_epsg() {
        assertEquals("EPSG:32632", CrsId.ofEpsg(32632).toAuthorityCode());
    }

    @Test
    public void testToAuthorityCode_esri() {
        assertEquals("ESRI:54009", CrsId.of("ESRI:54009").toAuthorityCode());
    }

    @Test
    public void testToString_matchesAuthorityCode() {
        CrsId id = CrsId.of("ESRI:102700");
        assertEquals(id.toAuthorityCode(), id.toString());
    }

    @Test
    public void testToSrid_returnsCode() {
        assertEquals(32632, CrsId.ofEpsg(32632).toSrid());
    }

    // -----------------------------------------------------------------------
    // equals / hashCode
    // -----------------------------------------------------------------------

    @Test
    public void testEquals_sameAuthorityAndCode() {
        assertEquals(CrsId.ofEpsg(4326), CrsId.of("EPSG:4326"));
    }

    @Test
    public void testEquals_sameObject() {
        CrsId id = CrsId.ofEpsg(4326);
        assertEquals(id, id);
    }

    @Test
    public void testNotEquals_differentCode() {
        assertNotEquals(CrsId.ofEpsg(4326), CrsId.ofEpsg(32632));
    }

    @Test
    public void testNotEquals_differentAuthority_sameCode() {
        assertNotEquals(CrsId.ofEpsg(4326), CrsId.of("ESRI:4326"));
    }

    @Test
    public void testNotEquals_null() {
        assertNotEquals(CrsId.ofEpsg(4326), null);
    }

    @Test
    public void testHashCode_equalObjectsHaveEqualHashCodes() {
        assertEquals(CrsId.ofEpsg(4326).hashCode(), CrsId.of("EPSG:4326").hashCode());
    }

    @Test
    public void testHashCode_differentAuthoritiesHaveDifferentHashCodes() {
        assertNotEquals(CrsId.ofEpsg(102700).hashCode(), CrsId.of("ESRI:102700").hashCode());
    }

    // -----------------------------------------------------------------------
    // Error cases
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testOf_null_throwsIllegalArgument() {
        CrsId.of(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOf_blank_throwsIllegalArgument() {
        CrsId.of("   ");
    }

    @Test(expected = NumberFormatException.class)
    public void testOf_nonNumericCode_throwsNumberFormat() {
        CrsId.of("EPSG:WGS84");
    }

    @Test(expected = NumberFormatException.class)
    public void testOf_bareNonNumeric_throwsNumberFormat() {
        CrsId.of("WGS84");
    }

    // -----------------------------------------------------------------------
    // GeometryColumn.getCrsCode()
    // -----------------------------------------------------------------------

    @Test
    public void testGeometryColumn_defaultAuthorityIsEpsg() {
        SpatialiteGeometryColumns col = new SpatialiteGeometryColumns();
        col.srid = 4326;
        assertEquals("EPSG", col.authority);
    }

    @Test
    public void testGeometryColumn_getCrsCode_defaultEpsg() {
        SpatialiteGeometryColumns col = new SpatialiteGeometryColumns();
        col.srid = 4326;
        assertEquals("EPSG:4326", col.getCrsCode());
    }

    @Test
    public void testGeometryColumn_getCrsCode_epsg32632() {
        SpatialiteGeometryColumns col = new SpatialiteGeometryColumns();
        col.srid = 32632;
        assertEquals("EPSG:32632", col.getCrsCode());
    }

    @Test
    public void testGeometryColumn_getCrsCode_esriAuthority() {
        SpatialiteGeometryColumns col = new SpatialiteGeometryColumns();
        col.srid = 102700;
        col.authority = "ESRI";
        assertEquals("ESRI:102700", col.getCrsCode());
    }

    @Test
    public void testGeometryColumn_toString_containsAuthority() {
        SpatialiteGeometryColumns col = new SpatialiteGeometryColumns();
        col.srid = 4326;
        col.authority = "ESRI";
        assertTrue(col.toString().contains("ESRI"));
    }
}
