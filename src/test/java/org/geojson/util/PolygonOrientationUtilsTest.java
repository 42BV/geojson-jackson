package org.geojson.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.geojson.LngLatAlt;
import org.junit.Test;

public class PolygonOrientationUtilsTest {

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
        assertTrue("Ring should be counterclockwise", PolygonOrientationUtils.isCounterClockwise(ccwRing));

        // Test the clockwise ring
        assertFalse("Ring should be clockwise", PolygonOrientationUtils.isCounterClockwise(cwRing));
    }

    @Test
    public void testIsCounterClockwiseWithInvalidRing() {
        // Test with null ring
        try {
            PolygonOrientationUtils.isCounterClockwise(null);
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
            PolygonOrientationUtils.isCounterClockwise(tooFewPoints);
            fail("Should have thrown IllegalArgumentException for ring with fewer than 4 points");
        } catch (IllegalArgumentException e) {
            // Expected exception
        }
    }

    @Test
    public void testReverseRing() {
        // Create a counterclockwise ring
        List<LngLatAlt> ring = new ArrayList<>(Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(1, 0),
                new LngLatAlt(1, 1),
                new LngLatAlt(0, 1),
                new LngLatAlt(0, 0)
        ));

        // Verify it's counterclockwise
        assertTrue(PolygonOrientationUtils.isCounterClockwise(ring));

        // Reverse it
        PolygonOrientationUtils.reverseRing(ring);

        // Verify it's now clockwise
        assertFalse(PolygonOrientationUtils.isCounterClockwise(ring));

        // After reversal, the ring should still be closed
        // The first point is now the original second-to-last point (0, 1)
        // The last point should still be (0, 0) to maintain closure
        assertEquals(0.0, ring.get(0).getLongitude(), 0.0);
        assertEquals(1.0, ring.get(0).getLatitude(), 0.0);
        assertEquals(0.0, ring.get(ring.size() - 1).getLongitude(), 0.0);
        assertEquals(0.0, ring.get(ring.size() - 1).getLatitude(), 0.0);
    }

    @Test
    public void testReverseRingWithEdgeCases() {
        // Test with null ring
        PolygonOrientationUtils.reverseRing(null);  // Should not throw exception

        // Test with empty ring
        List<LngLatAlt> emptyRing = new ArrayList<>();
        PolygonOrientationUtils.reverseRing(emptyRing);  // Should not throw exception

        // Test with single point
        List<LngLatAlt> singlePoint = new ArrayList<>(Collections.singletonList(new LngLatAlt(0, 0)));
        PolygonOrientationUtils.reverseRing(singlePoint);  // Should not throw exception
        assertEquals(1, singlePoint.size());
    }

    @Test
    public void testValidateRingClosed() {
        // Create a closed ring
        List<LngLatAlt> closedRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(1, 0),
                new LngLatAlt(1, 1),
                new LngLatAlt(0, 1),
                new LngLatAlt(0, 0)  // Same as first point
        );

        // This should not throw an exception
        PolygonOrientationUtils.validateRingClosed(closedRing, "Test ring");

        // Create an unclosed ring
        List<LngLatAlt> unclosedRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(1, 0),
                new LngLatAlt(1, 1),
                new LngLatAlt(0, 1)  // Missing closure point
        );

        // This should throw an exception
        try {
            PolygonOrientationUtils.validateRingClosed(unclosedRing, "Test ring");
            fail("Should have thrown IllegalArgumentException for unclosed ring");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertTrue(e.getMessage().contains("must be closed"));
        }
    }

    @Test
    public void testEnsureRingClosed() {
        // Create an unclosed ring
        List<LngLatAlt> unclosedRing = new ArrayList<>(Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(1, 0),
                new LngLatAlt(1, 1),
                new LngLatAlt(0, 1)  // Missing closure point
        ));

        // Close the ring
        List<LngLatAlt> closedRing = PolygonOrientationUtils.ensureRingClosed(unclosedRing);

        // Verify the ring is now closed
        assertEquals(5, closedRing.size());
        assertEquals(closedRing.get(0).getLongitude(), closedRing.get(closedRing.size() - 1).getLongitude(), 0.0);
        assertEquals(closedRing.get(0).getLatitude(), closedRing.get(closedRing.size() - 1).getLatitude(), 0.0);

        // Test with already closed ring
        List<LngLatAlt> alreadyClosedRing = new ArrayList<>(Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(1, 0),
                new LngLatAlt(1, 1),
                new LngLatAlt(0, 1),
                new LngLatAlt(0, 0)  // Already closed
        ));

        // This should return the same ring
        List<LngLatAlt> result = PolygonOrientationUtils.ensureRingClosed(alreadyClosedRing);
        assertEquals(5, result.size());
        assertEquals(alreadyClosedRing, result);
    }

    @Test
    public void testEnsureRingClosedWithImmutableList() {
        // Create an unclosed ring with an immutable list
        List<LngLatAlt> unclosedRing = Collections.unmodifiableList(Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(1, 0),
                new LngLatAlt(1, 1),
                new LngLatAlt(0, 1)  // Missing closure point
        ));

        // Close the ring
        List<LngLatAlt> closedRing = PolygonOrientationUtils.ensureRingClosed(unclosedRing);

        // Verify the ring is now closed and a new list was created
        assertEquals(5, closedRing.size());
        assertEquals(closedRing.get(0).getLongitude(), closedRing.get(closedRing.size() - 1).getLongitude(), 0.0);
        assertEquals(closedRing.get(0).getLatitude(), closedRing.get(closedRing.size() - 1).getLatitude(), 0.0);
    }

    @Test
    public void testFixPolygonOrientation() {
        // Create a polygon with clockwise exterior ring (incorrect)
        List<LngLatAlt> exteriorRing = new ArrayList<>(Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(0, 1),
                new LngLatAlt(1, 1),
                new LngLatAlt(1, 0),
                new LngLatAlt(0, 0)
        ));

        // Create a polygon with counterclockwise interior ring (incorrect)
        List<LngLatAlt> interiorRing = new ArrayList<>(Arrays.asList(
                new LngLatAlt(0.2, 0.2),
                new LngLatAlt(0.8, 0.2),
                new LngLatAlt(0.8, 0.8),
                new LngLatAlt(0.2, 0.8),
                new LngLatAlt(0.2, 0.2)
        ));

        // Verify initial orientations
        assertFalse(PolygonOrientationUtils.isCounterClockwise(exteriorRing));
        assertTrue(PolygonOrientationUtils.isCounterClockwise(interiorRing));

        // Create a polygon with these rings
        List<List<LngLatAlt>> rings = new ArrayList<>();
        rings.add(exteriorRing);
        rings.add(interiorRing);

        // Fix the orientation
        List<List<LngLatAlt>> fixedRings = PolygonOrientationUtils.fixPolygonOrientation(rings);

        // Verify the orientations are now correct
        assertTrue(PolygonOrientationUtils.isCounterClockwise(fixedRings.get(0)));  // Exterior should be CCW
        assertFalse(PolygonOrientationUtils.isCounterClockwise(fixedRings.get(1))); // Interior should be CW
    }

    @Test
    public void testValidatePolygonOrientation() {
        // Create a polygon with correct orientations
        List<LngLatAlt> exteriorRing = new ArrayList<>(Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(1, 0),
                new LngLatAlt(1, 1),
                new LngLatAlt(0, 1),
                new LngLatAlt(0, 0)
        ));

        List<LngLatAlt> interiorRing = new ArrayList<>(Arrays.asList(
                new LngLatAlt(0.2, 0.2),
                new LngLatAlt(0.2, 0.8),
                new LngLatAlt(0.8, 0.8),
                new LngLatAlt(0.8, 0.2),
                new LngLatAlt(0.2, 0.2)
        ));

        // Verify orientations
        assertTrue(PolygonOrientationUtils.isCounterClockwise(exteriorRing));
        assertFalse(PolygonOrientationUtils.isCounterClockwise(interiorRing));

        // Create a polygon with these rings
        List<List<LngLatAlt>> correctRings = new ArrayList<>();
        correctRings.add(exteriorRing);
        correctRings.add(interiorRing);

        // This should not throw an exception
        PolygonOrientationUtils.validatePolygonOrientation(correctRings);

        // Create a polygon with incorrect orientations
        List<LngLatAlt> incorrectExteriorRing = new ArrayList<>(Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(0, 1),
                new LngLatAlt(1, 1),
                new LngLatAlt(1, 0),
                new LngLatAlt(0, 0)
        ));

        List<List<LngLatAlt>> incorrectRings = new ArrayList<>();
        incorrectRings.add(incorrectExteriorRing);

        // This should throw an exception
        try {
            PolygonOrientationUtils.validatePolygonOrientation(incorrectRings);
            fail("Should have thrown IllegalArgumentException for incorrect orientation");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertTrue(e.getMessage().contains("must be counterclockwise"));
        }
    }
}
