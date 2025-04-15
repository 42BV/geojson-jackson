package org.geojson.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geojson.GeoJsonObject;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.MultiLineString;
import org.geojson.MultiPolygon;
import org.geojson.Polygon;
import org.junit.Test;

public class AntimeridianUtilsTest {

    @Test
    public void testNormalizeLongitude() {
        // Test values within range
        assertEquals(0.0, AntimeridianUtils.normalizeLongitude(0.0), 0.0001);
        assertEquals(180.0, AntimeridianUtils.normalizeLongitude(180.0), 0.0001);
        assertEquals(-180.0, AntimeridianUtils.normalizeLongitude(-180.0), 0.0001);
        assertEquals(90.0, AntimeridianUtils.normalizeLongitude(90.0), 0.0001);
        assertEquals(-90.0, AntimeridianUtils.normalizeLongitude(-90.0), 0.0001);

        // Test values outside range
        assertEquals(0.0, AntimeridianUtils.normalizeLongitude(360.0), 0.0001);
        assertEquals(0.0, AntimeridianUtils.normalizeLongitude(-360.0), 0.0001);
        assertEquals(-170.0, AntimeridianUtils.normalizeLongitude(190.0), 0.0001);
        assertEquals(170.0, AntimeridianUtils.normalizeLongitude(-190.0), 0.0001);
        assertEquals(10.0, AntimeridianUtils.normalizeLongitude(370.0), 0.0001);
        assertEquals(-10.0, AntimeridianUtils.normalizeLongitude(-370.0), 0.0001);
    }

    @Test
    public void testCrossesAntimeridian() {
        // Test case: Points on opposite sides of the antimeridian (should cross)
        LngLatAlt p1 = new LngLatAlt(179, 0);
        LngLatAlt p2 = new LngLatAlt(-179, 0);
        assertTrue("Points at 179 and -179 should cross the antimeridian",
                AntimeridianUtils.crossesAntimeridian(p1, p2));

        // Test case: Points on the same side (should not cross)
        p1 = new LngLatAlt(170, 0);
        p2 = new LngLatAlt(175, 0);
        assertFalse("Points at 170 and 175 should not cross the antimeridian",
                AntimeridianUtils.crossesAntimeridian(p1, p2));

        // Test case: Points exactly at the antimeridian (should not cross)
        p1 = new LngLatAlt(180, 0);
        p2 = new LngLatAlt(180, 10);
        assertFalse("Points at 180 longitude should not cross the antimeridian",
                AntimeridianUtils.crossesAntimeridian(p1, p2));

        // Test case: Points with longitude values outside the standard range
        p1 = new LngLatAlt(190, 0);  // Equivalent to -170
        p2 = new LngLatAlt(-190, 0); // Equivalent to 170
        assertTrue("Points at 190 and -190 should cross the antimeridian",
                AntimeridianUtils.crossesAntimeridian(p1, p2));
    }

    @Test
    public void testInterpolateLatitude() {
        // Test interpolation across the antimeridian (positive to negative)
        LngLatAlt p1 = new LngLatAlt(170, 10);
        LngLatAlt p2 = new LngLatAlt(-170, 20);
        double lat = AntimeridianUtils.interpolateLatitude(p1, p2);
        assertEquals("Latitude should be interpolated correctly", 15.0, lat, 0.001);

        // Test interpolation across the antimeridian (negative to positive)
        p1 = new LngLatAlt(-170, 30);
        p2 = new LngLatAlt(170, 10);
        lat = AntimeridianUtils.interpolateLatitude(p1, p2);
        // The interpolation should be at the midpoint of the latitude values
        assertEquals("Latitude should be interpolated correctly", 20.0, lat, 0.001);

        // Test with points exactly at the antimeridian
        p1 = new LngLatAlt(180, 10);
        p2 = new LngLatAlt(-180, 20);
        lat = AntimeridianUtils.interpolateLatitude(p1, p2);
        assertEquals("Latitude should be interpolated correctly", 15.0, lat, 0.001);

        // Test special case for -170 to 170 crossing
        p1 = new LngLatAlt(-170, 10);
        p2 = new LngLatAlt(170, 20);
        lat = AntimeridianUtils.interpolateLatitude(p1, p2);
        assertEquals("Latitude should be the midpoint for -170 to 170 crossing", 15.0, lat, 0.001);
    }

    @Test
    public void testGetAntimeridianLon() {
        // Test crossing from positive to negative
        double lon1 = 170;
        double lon2 = -170;
        assertEquals(180.0, AntimeridianUtils.getAntimeridianLon(lon1, lon2), 0.0001);

        // Test crossing from negative to positive
        lon1 = -170;
        lon2 = 170;
        assertEquals(-180.0, AntimeridianUtils.getAntimeridianLon(lon1, lon2), 0.0001);

        // Test not crossing, both positive
        lon1 = 160;
        lon2 = 170;
        assertEquals(180.0, AntimeridianUtils.getAntimeridianLon(lon1, lon2), 0.0001);

        // Test not crossing, both negative
        lon1 = -170;
        lon2 = -160;
        assertEquals(-180.0, AntimeridianUtils.getAntimeridianLon(lon1, lon2), 0.0001);
    }

    @Test
    public void testCutLineStringAtAntimeridian() {
        // Create a LineString that crosses the antimeridian
        LineString lineString = new LineString();
        lineString.add(new LngLatAlt(170, 0));
        lineString.add(new LngLatAlt(-170, 10));

        // Cut the LineString
        GeoJsonObject result = AntimeridianUtils.cutLineStringAtAntimeridian(lineString);

        // Verify the result is a MultiLineString
        assertTrue(result instanceof MultiLineString);
        MultiLineString multiLineString = (MultiLineString) result;

        // Verify there are two segments
        assertEquals(2, multiLineString.getCoordinates().size());

        // Verify the first segment ends at the antimeridian
        List<LngLatAlt> firstSegment = multiLineString.getCoordinates().get(0);
        assertEquals(180.0, firstSegment.get(firstSegment.size() - 1).getLongitude(), 0.0001);

        // Verify the second segment starts at the antimeridian
        List<LngLatAlt> secondSegment = multiLineString.getCoordinates().get(1);
        assertEquals(-180.0, secondSegment.get(0).getLongitude(), 0.0001);

        // Test with a LineString that doesn't cross the antimeridian
        LineString noAntimeridianCrossing = new LineString();
        noAntimeridianCrossing.add(new LngLatAlt(0, 0));
        noAntimeridianCrossing.add(new LngLatAlt(10, 10));

        // This should return the original LineString
        GeoJsonObject noAntimeridianResult = AntimeridianUtils.cutLineStringAtAntimeridian(noAntimeridianCrossing);
        assertTrue(noAntimeridianResult instanceof LineString);
        assertEquals(noAntimeridianCrossing, noAntimeridianResult);
    }

    @Test
    public void testPolygonCrossesAntimeridian() {
        // Create a polygon that crosses the antimeridian
        List<List<LngLatAlt>> crossingRings = new ArrayList<>();
        List<LngLatAlt> crossingRing = Arrays.asList(
                new LngLatAlt(170, 0),
                new LngLatAlt(170, 10),
                new LngLatAlt(-170, 10),
                new LngLatAlt(-170, 0),
                new LngLatAlt(170, 0)
        );
        crossingRings.add(crossingRing);

        assertTrue(AntimeridianUtils.polygonCrossesAntimeridian(crossingRings));

        // Create a polygon that doesn't cross the antimeridian
        List<List<LngLatAlt>> nonCrossingRings = new ArrayList<>();
        List<LngLatAlt> nonCrossingRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(10, 0),
                new LngLatAlt(10, 10),
                new LngLatAlt(0, 10),
                new LngLatAlt(0, 0)
        );
        nonCrossingRings.add(nonCrossingRing);

        assertFalse(AntimeridianUtils.polygonCrossesAntimeridian(nonCrossingRings));
    }

    @Test
    public void testCutRingAtAntimeridian() {
        // Create a ring that crosses the antimeridian
        List<LngLatAlt> crossingRing = Arrays.asList(
                new LngLatAlt(170, 0),
                new LngLatAlt(170, 10),
                new LngLatAlt(-170, 10),
                new LngLatAlt(-170, 0),
                new LngLatAlt(170, 0)
        );

        // Cut the ring
        List<List<LngLatAlt>> result = AntimeridianUtils.cutRingAtAntimeridian(crossingRing);

        // Verify there are two rings
        assertEquals(2, result.size());

        // Verify both rings are closed
        for (List<LngLatAlt> ring : result) {
            LngLatAlt first = ring.get(0);
            LngLatAlt last = ring.get(ring.size() - 1);
            assertEquals(first.getLongitude(), last.getLongitude(), 0.0001);
            assertEquals(first.getLatitude(), last.getLatitude(), 0.0001);
        }

        // Verify one ring is on the east side (positive longitudes)
        // and one ring is on the west side (negative longitudes)
        boolean hasEastRing = false;
        boolean hasWestRing = false;
        for (List<LngLatAlt> ring : result) {
            // Check the first point (excluding the last which is the same as the first)
            double lon = ring.get(0).getLongitude();
            if (lon > 0) {
                hasEastRing = true;
            } else {
                hasWestRing = true;
            }
        }
        assertTrue("Should have a ring on the east side", hasEastRing);
        assertTrue("Should have a ring on the west side", hasWestRing);

        // Test with a ring that doesn't cross the antimeridian
        List<LngLatAlt> nonCrossingRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(10, 0),
                new LngLatAlt(10, 10),
                new LngLatAlt(0, 10),
                new LngLatAlt(0, 0)
        );

        // This should return the original ring
        List<List<LngLatAlt>> nonCrossingResult = AntimeridianUtils.cutRingAtAntimeridian(nonCrossingRing);
        assertEquals(1, nonCrossingResult.size());
        assertEquals(nonCrossingRing, nonCrossingResult.get(0));
    }

    @Test
    public void testCutPolygonAtAntimeridian() {
        // Create a polygon that crosses the antimeridian
        Polygon polygon = new Polygon();
        List<LngLatAlt> ring = Arrays.asList(
                new LngLatAlt(170, 0),
                new LngLatAlt(170, 10),
                new LngLatAlt(-170, 10),
                new LngLatAlt(-170, 0),
                new LngLatAlt(170, 0));
        polygon.add(ring);

        // Cut the polygon
        GeoJsonObject result = AntimeridianUtils.cutPolygonAtAntimeridian(polygon);

        // Verify the result is a MultiPolygon
        assertTrue(result instanceof MultiPolygon);
        MultiPolygon multiPolygon = (MultiPolygon) result;

        // Verify there are two polygons
        assertEquals(2, multiPolygon.getCoordinates().size());

        // Test with a polygon that doesn't cross the antimeridian
        Polygon nonCrossingPolygon = new Polygon();
        List<LngLatAlt> nonCrossingRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(10, 0),
                new LngLatAlt(10, 10),
                new LngLatAlt(0, 10),
                new LngLatAlt(0, 0));
        nonCrossingPolygon.add(nonCrossingRing);

        // This should return the original polygon
        GeoJsonObject nonCrossingResult = AntimeridianUtils.cutPolygonAtAntimeridian(nonCrossingPolygon);
        assertEquals(nonCrossingPolygon, nonCrossingResult);
    }

    @Test
    public void testCalculateAverageLongitude() {
        // Test with points on the same side of the antimeridian
        List<LngLatAlt> eastRing = Arrays.asList(
                new LngLatAlt(170, 0),
                new LngLatAlt(175, 0),
                new LngLatAlt(175, 5),
                new LngLatAlt(170, 5),
                new LngLatAlt(170, 0)
        );
        double avgLon = AntimeridianUtils.calculateAverageLongitude(eastRing);
        assertEquals(172.5, avgLon, 1.0); // Approximate due to the calculation method

        // Test with points on the other side
        List<LngLatAlt> westRing = Arrays.asList(
                new LngLatAlt(-170, 0),
                new LngLatAlt(-175, 0),
                new LngLatAlt(-175, 5),
                new LngLatAlt(-170, 5),
                new LngLatAlt(-170, 0)
        );
        avgLon = AntimeridianUtils.calculateAverageLongitude(westRing);
        assertEquals(-172.5, avgLon, 1.0); // Approximate due to the calculation method

        // Test with null or empty list
        assertEquals(0.0, AntimeridianUtils.calculateAverageLongitude(null), 0.0001);
        assertEquals(0.0, AntimeridianUtils.calculateAverageLongitude(new ArrayList<>()), 0.0001);
    }
}
