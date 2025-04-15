package org.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class GeoJsonConfigTest {

    @Test
    public void testRfc7946Mode() {
        // Create RFC 7946 config
        GeoJsonConfig config = GeoJsonConfig.rfc7946();

        // Check that the configuration is set correctly
        assertTrue(config.isValidatePolygonOrientation());
        assertTrue(config.isCutAntimeridian());
    }

    @Test
    public void testLegacyMode() {
        // Create legacy config
        GeoJsonConfig config = GeoJsonConfig.legacy();

        // Check that the configuration is set correctly
        assertFalse(config.isValidatePolygonOrientation());
        assertFalse(config.isCutAntimeridian());
    }

    @Test
    public void testCustomConfiguration() {
        // Configure custom settings
        GeoJsonConfig config = new GeoJsonConfig();
        config.setValidatePolygonOrientation(true)
                .setAutoFixPolygonOrientation(true)
                .setCutAntimeridian(false)
                .setWarnOnCrsUse(false);

        // Check that the configuration is set correctly
        assertTrue(config.isValidatePolygonOrientation());
        assertTrue(config.isAutoFixPolygonOrientation());
        assertFalse(config.isCutAntimeridian());
        assertFalse(config.isWarnOnCrsUse());
    }

    @Test
    public void testPolygonOrientationValidation() {
        // Create a polygon with counterclockwise exterior ring (valid)
        Polygon validPolygon = new Polygon(
                new LngLatAlt(0, 0),
                new LngLatAlt(1, 0),
                new LngLatAlt(1, 1),
                new LngLatAlt(0, 1),
                new LngLatAlt(0, 0)
        );

        // Set RFC 7946 config with validation
        GeoJsonConfig config = GeoJsonConfig.rfc7946();
        config.setAutoFixPolygonOrientation(false);
        validPolygon.setConfig(config);

        // This should not throw an exception
        assertNotNull(validPolygon);

        // Create a polygon with clockwise exterior ring (invalid)
        try {
            // Create a polygon with a clockwise exterior ring (invalid in RFC 7946)
            Polygon invalidPolygon = new Polygon();
            List<LngLatAlt> clockwiseRing = Arrays.asList(
                    new LngLatAlt(0, 0),
                    new LngLatAlt(0, 1),
                    new LngLatAlt(1, 1),
                    new LngLatAlt(1, 0),
                    new LngLatAlt(0, 0)
            );

            // Verify it's actually clockwise
            if (GeoJsonUtils.isCounterClockwise(clockwiseRing)) {
                GeoJsonUtils.reverseRing(clockwiseRing);
            }

            // Set RFC 7946 config with validation
            GeoJsonConfig config2 = GeoJsonConfig.rfc7946();
            config2.setAutoFixPolygonOrientation(false);
            invalidPolygon.setConfig(config2);

            // Now add the ring, which should trigger validation
            invalidPolygon.add(clockwiseRing);

            // If we get here, the validation didn't throw an exception, which is wrong
            fail("Should have thrown an exception for clockwise exterior ring");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertTrue(e.getMessage().contains("Exterior ring must be counterclockwise"));
        }
    }

    @Test
    public void testPolygonOrientationAutoFix() {
        // Create a config with RFC 7946 mode and auto-fix
        GeoJsonConfig config = GeoJsonConfig.rfc7946();
        config.setAutoFixPolygonOrientation(true);

        // Create a polygon with clockwise exterior ring (would be invalid, but will be fixed)
        Polygon polygon = new Polygon();
        polygon.setConfig(config);

        List<LngLatAlt> clockwiseRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(0, 1),
                new LngLatAlt(1, 1),
                new LngLatAlt(1, 0),
                new LngLatAlt(0, 0)
        );

        // Verify it's actually clockwise
        if (GeoJsonUtils.isCounterClockwise(clockwiseRing)) {
            GeoJsonUtils.reverseRing(clockwiseRing);
        }

        // Add the ring, which should trigger auto-fix
        polygon.add(clockwiseRing);

        // The polygon should have been fixed
        assertTrue(GeoJsonUtils.isCounterClockwise(polygon.getExteriorRing()));
    }

    @Test
    public void testCrsWarning() {
        // Create a config with RFC 7946 mode and CRS warnings disabled
        GeoJsonConfig config = GeoJsonConfig.rfc7946();
        config.setWarnOnCrsUse(false);

        Point point = new Point(100, 0);
        point.setConfig(config);

        Crs crs = new Crs();
        crs.getProperties().put("name", "EPSG:4326");
        point.setCrs(crs);

        // The CRS should still be set even in RFC 7946 mode
        assertNotNull(point.getCrs());
        assertEquals("EPSG:4326", point.getCrs().getProperties().get("name"));
    }
}
