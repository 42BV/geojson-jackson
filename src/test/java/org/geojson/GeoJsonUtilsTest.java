package org.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class GeoJsonUtilsTest {

    @Test
    public void testIsCounterClockwise() {
        // Create a counterclockwise ring (0,0) -> (1,0) -> (1,1) -> (0,1) -> (0,0)
        List<LngLatAlt> ccwRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(1, 0),
                new LngLatAlt(1, 1),
                new LngLatAlt(0, 1),
                new LngLatAlt(0, 0)
        );

        // Create a clockwise ring (0,0) -> (0,1) -> (1,1) -> (1,0) -> (0,0)
        List<LngLatAlt> cwRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(0, 1),
                new LngLatAlt(1, 1),
                new LngLatAlt(1, 0),
                new LngLatAlt(0, 0)
        );

        // Test the counterclockwise ring
        assertTrue("Ring should be counterclockwise", GeoJsonUtils.isCounterClockwise(ccwRing));

        // Test the clockwise ring
        assertFalse("Ring should be clockwise", GeoJsonUtils.isCounterClockwise(cwRing));
    }

    @Test
    public void testIsCounterClockwiseWithInvalidRing() {
        // Test with null ring
        try {
            GeoJsonUtils.isCounterClockwise(null);
            fail("Should have thrown IllegalArgumentException for null ring");
        } catch (IllegalArgumentException e) {
            // Expected exception
        }

        // Test with ring having fewer than 4 points
        try {
            List<LngLatAlt> tooFewPoints = Arrays.asList(
                    new LngLatAlt(0, 0),
                    new LngLatAlt(1, 0),
                    new LngLatAlt(0, 0)  // Only 3 points including closure
            );
            GeoJsonUtils.isCounterClockwise(tooFewPoints);
            fail("Should have thrown IllegalArgumentException for ring with fewer than 4 points");
        } catch (IllegalArgumentException e) {
            // Expected exception
        }
    }

    @Test
    public void testCrossesAntimeridian() {
        // Test case: Points on opposite sides of the antimeridian (should cross)
        LngLatAlt p1 = new LngLatAlt(179, 0);
        LngLatAlt p2 = new LngLatAlt(-179, 0);
        assertTrue("Points at 179 and -179 should cross the antimeridian",
                GeoJsonUtils.crossesAntimeridian(p1, p2));

        // Test case: Points on the same side (should not cross)
        p1 = new LngLatAlt(170, 0);
        p2 = new LngLatAlt(175, 0);
        assertFalse("Points at 170 and 175 should not cross the antimeridian",
                GeoJsonUtils.crossesAntimeridian(p1, p2));

        // Test case: Points exactly at the antimeridian (should not cross)
        p1 = new LngLatAlt(180, 0);
        p2 = new LngLatAlt(180, 10);
        assertFalse("Points at 180 longitude should not cross the antimeridian",
                GeoJsonUtils.crossesAntimeridian(p1, p2));

        // Test case: Points with longitude values outside the standard range
        p1 = new LngLatAlt(190, 0);  // Equivalent to -170
        p2 = new LngLatAlt(-190, 0); // Equivalent to 170
        assertTrue("Points at 190 and -190 should cross the antimeridian",
                GeoJsonUtils.crossesAntimeridian(p1, p2));
    }

    @Test
    public void testInterpolateLatitude() {
        // Test interpolation across the antimeridian (positive to negative)
        LngLatAlt p1 = new LngLatAlt(170, 10);
        LngLatAlt p2 = new LngLatAlt(-170, 20);
        double lat = GeoJsonUtils.interpolateLatitude(p1, p2);
        assertEquals("Latitude should be interpolated correctly", 15.0, lat, 0.001);

        // Test interpolation across the antimeridian (negative to positive)
        p1 = new LngLatAlt(-170, 30);
        p2 = new LngLatAlt(170, 10);
        lat = GeoJsonUtils.interpolateLatitude(p1, p2);
        // The interpolation should be at the midpoint of the latitude values
        assertEquals("Latitude should be interpolated correctly", 20.0, lat, 0.001);

        // Test with points exactly at the antimeridian
        p1 = new LngLatAlt(180, 10);
        p2 = new LngLatAlt(-180, 20);
        lat = GeoJsonUtils.interpolateLatitude(p1, p2);
        assertEquals("Latitude should be interpolated correctly", 15.0, lat, 0.001);
    }
}
