package org.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ComplexPolygonTest {

    private GeoJsonConfig config;

    @Before
    public void setUp() {
        // Create a new configuration for each test
        config = new GeoJsonConfig();
    }

    @Test
    public void testComplexPolygonAutoFix() {
        // Configure for RFC 7946 with auto-fix
        config.setValidatePolygonOrientation(true)
                .setAutoFixPolygonOrientation(true);

        // Create a complex polygon with clockwise exterior ring (invalid)
        List<LngLatAlt> exteriorRing = createClockwiseRing(new LngLatAlt(0, 0), new LngLatAlt(0, 10), new LngLatAlt(10, 10), new LngLatAlt(10, 0));

        // Create interior rings with mixed orientations
        List<LngLatAlt> interiorRing1 = createCounterClockwiseRing( // Invalid - should be clockwise
                new LngLatAlt(2, 2), new LngLatAlt(2, 4), new LngLatAlt(4, 4), new LngLatAlt(4, 2));

        List<LngLatAlt> interiorRing2 = createClockwiseRing( // Valid - already clockwise
                new LngLatAlt(6, 6), new LngLatAlt(6, 8), new LngLatAlt(8, 8), new LngLatAlt(8, 6));

        // Create the polygon
        Polygon polygon = new Polygon();
        polygon.setConfig(config);
        polygon.add(exteriorRing);
        polygon.addInteriorRing(interiorRing1);
        polygon.addInteriorRing(interiorRing2);

        // Verify that orientations were fixed
        assertTrue("Exterior ring should be counterclockwise after auto-fix", GeoJsonUtils.isCounterClockwise(polygon.getExteriorRing()));

        assertFalse("Interior ring 1 should be clockwise after auto-fix", GeoJsonUtils.isCounterClockwise(polygon.getInteriorRing(0)));

        assertFalse("Interior ring 2 should be clockwise after auto-fix", GeoJsonUtils.isCounterClockwise(polygon.getInteriorRing(1)));
    }

    @Test
    public void testComplexPolygonValidation() {
        // Configure for RFC 7946 with validation but no auto-fix
        config.setValidatePolygonOrientation(true).setAutoFixPolygonOrientation(false);

        // Create a valid polygon (counterclockwise exterior, clockwise interior)
        List<LngLatAlt> exteriorRing = createCounterClockwiseRing(new LngLatAlt(0, 0), new LngLatAlt(10, 0), new LngLatAlt(10, 10), new LngLatAlt(0, 10),
                new LngLatAlt(0, 0)  // Explicitly close the ring
        );

        List<LngLatAlt> interiorRing = createClockwiseRing(new LngLatAlt(2, 2), new LngLatAlt(4, 2), new LngLatAlt(4, 4), new LngLatAlt(2, 4),
                new LngLatAlt(2, 2)  // Explicitly close the ring
        );

        // Create a temporary config with validation disabled
        GeoJsonConfig tempConfig = new GeoJsonConfig();
        tempConfig.setValidatePolygonOrientation(false);

        // This should not throw an exception
        Polygon validPolygon = new Polygon();
        validPolygon.setConfig(tempConfig);
        validPolygon.add(exteriorRing);
        validPolygon.addInteriorRing(interiorRing);

        // Now try with an invalid polygon
        try {
            // Create an invalid polygon (clockwise exterior ring)
            List<LngLatAlt> invalidExteriorRing = createClockwiseRing(new LngLatAlt(0, 0), new LngLatAlt(0, 10), new LngLatAlt(10, 10), new LngLatAlt(10, 0),
                    new LngLatAlt(0, 0));

            // Validate the ring using the config with validation enabled
            GeoJsonUtils.validatePolygonOrientation(Collections.singletonList(invalidExteriorRing));

            fail("Should have thrown an exception for clockwise exterior ring");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertTrue(e.getMessage().contains("Exterior ring must be counterclockwise"));
        }
    }

    @Test
    public void testPolygonWithDonutHole() {
        // Configure for RFC 7946 with auto-fix
        config
                .setValidatePolygonOrientation(true)
                .setAutoFixPolygonOrientation(true);

        // Create a large exterior ring (counterclockwise)
        List<LngLatAlt> exteriorRing = createCounterClockwiseRing(new LngLatAlt(0, 0), new LngLatAlt(20, 0), new LngLatAlt(20, 20), new LngLatAlt(0, 20));

        // Create a medium-sized hole (clockwise)
        List<LngLatAlt> mediumHole = createClockwiseRing(new LngLatAlt(5, 5), new LngLatAlt(15, 5), new LngLatAlt(15, 15), new LngLatAlt(5, 15));

        // Create a small "island" inside the hole (counterclockwise)
        // This is actually represented as a separate polygon in GeoJSON
        List<LngLatAlt> islandExterior = createCounterClockwiseRing(new LngLatAlt(8, 8), new LngLatAlt(12, 8), new LngLatAlt(12, 12), new LngLatAlt(8, 12));

        // Create the main polygon with its hole
        Polygon mainPolygon = new Polygon();
        mainPolygon.setConfig(config);
        mainPolygon.add(exteriorRing);
        mainPolygon.addInteriorRing(mediumHole);

        // Create the island polygon
        Polygon islandPolygon = new Polygon();
        islandPolygon.setConfig(config);
        islandPolygon.add(islandExterior);

        // Verify orientations
        assertTrue("Exterior ring should be counterclockwise", GeoJsonUtils.isCounterClockwise(mainPolygon.getExteriorRing()));

        assertFalse("Medium hole should be clockwise", GeoJsonUtils.isCounterClockwise(mainPolygon.getInteriorRing(0)));

        assertTrue("Island exterior should be counterclockwise", GeoJsonUtils.isCounterClockwise(islandPolygon.getExteriorRing()));

        // Create a FeatureCollection with both polygons
        FeatureCollection featureCollection = new FeatureCollection();
        featureCollection.setConfig(config);

        Feature mainFeature = new Feature();
        mainFeature.setConfig(config);
        mainFeature.setGeometry(mainPolygon);
        mainFeature.setProperty("name", "Main polygon with hole");

        Feature islandFeature = new Feature();
        islandFeature.setConfig(config);
        islandFeature.setGeometry(islandPolygon);
        islandFeature.setProperty("name", "Island inside hole");

        featureCollection.add(mainFeature);
        featureCollection.add(islandFeature);

        // Process with GeoJsonMapper
        GeoJsonMapper mapper = new GeoJsonMapper(config);
        GeoJsonObject processed = mapper.process(featureCollection);

        // Verify it's still a FeatureCollection
        assertTrue(processed instanceof FeatureCollection);
        assertEquals(2, ((FeatureCollection) processed).getFeatures().size());
    }

    @Test
    public void testPolygonWithManyPoints() {
        // Configure for RFC 7946 with auto-fix
        config
                .setValidatePolygonOrientation(true)
                .setAutoFixPolygonOrientation(true);

        // Create a polygon with 100 points
        List<LngLatAlt> manyPoints = new ArrayList<>();

        // Add points in a clockwise order (will need to be fixed)
        // Note: Using negative angle to ensure clockwise orientation
        for (int i = 0; i < 100; i++) {
            double angle = -2 * Math.PI * i / 100;
            double x = 10 + 5 * Math.cos(angle);
            double y = 10 + 5 * Math.sin(angle);
            manyPoints.add(new LngLatAlt(x, y));
        }

        // Close the ring
        manyPoints.add(manyPoints.get(0));

        // Verify it's clockwise initially
        assertFalse("Ring should be clockwise initially", GeoJsonUtils.isCounterClockwise(manyPoints));

        // Create the polygon
        Polygon polygon = new Polygon();
        polygon.setConfig(config);
        polygon.add(manyPoints);

        // Verify orientation was fixed
        assertTrue("Exterior ring should be counterclockwise after auto-fix", GeoJsonUtils.isCounterClockwise(polygon.getExteriorRing()));
    }

    private List<LngLatAlt> createClockwiseRing(LngLatAlt... points) {
        List<LngLatAlt> ring = new ArrayList<>(Arrays.asList(points));
        // Close the ring if not already closed
        LngLatAlt first = ring.get(0);
        LngLatAlt last = ring.get(ring.size() - 1);
        if (first.getLongitude() != last.getLongitude() || first.getLatitude() != last.getLatitude()) {
            ring.add(new LngLatAlt(first.getLongitude(), first.getLatitude()));
        }
        // Ensure it's clockwise
        if (GeoJsonUtils.isCounterClockwise(ring)) {
            GeoJsonUtils.reverseRing(ring);
        }
        return ring;
    }

    private List<LngLatAlt> createCounterClockwiseRing(LngLatAlt... points) {
        List<LngLatAlt> ring = new ArrayList<>(Arrays.asList(points));
        // Close the ring if not already closed
        LngLatAlt first = ring.get(0);
        LngLatAlt last = ring.get(ring.size() - 1);
        if (first.getLongitude() != last.getLongitude() || first.getLatitude() != last.getLatitude()) {
            ring.add(new LngLatAlt(first.getLongitude(), first.getLatitude()));
        }
        // Ensure it's counterclockwise
        if (!GeoJsonUtils.isCounterClockwise(ring)) {
            GeoJsonUtils.reverseRing(ring);
        }
        return ring;
    }
}
