package org.geojson.jackson;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import tools.jackson.databind.ObjectMapper;

import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.junit.Test;

public class PointTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void assertLngLatAlt(double expectedLongitude, double expectedLatitude, double expectedAltitude,
            LngLatAlt point) {
        assertLngLatAlt(expectedLongitude, expectedLatitude, expectedAltitude, new double[0], point);
    }

    public static void assertLngLatAlt(double expectedLongitude, double expectedLatitude, double expectedAltitude,
            double[] expectedAdditionalElements, LngLatAlt point) {
        assertEquals(expectedLongitude, point.getLongitude(), 0.00001);
        assertEquals(expectedLatitude, point.getLatitude(), 0.00001);
        if (Double.isNaN(expectedAltitude)) {
            assertFalse(point.hasAltitude());
        } else {
            assertEquals(expectedAltitude, point.getAltitude(), 0.00001);
            assertArrayEquals(expectedAdditionalElements, point.getAdditionalElements(), 0.0);
        }
    }

    @Test
    public void itShouldSerializeAPoint() {
        Point point = new Point(100, 0);
        assertEquals("{\"type\":\"Point\",\"coordinates\":[100.0,0.0]}",
                mapper.writeValueAsString(point));
    }

    @Test
    public void itShouldDeserializeAPoint() {
        GeoJsonObject value = mapper
                .readValue("{\"type\":\"Point\",\"coordinates\":[100.0,5.0]}", GeoJsonObject.class);
        assertNotNull(value);
        assertTrue(value instanceof Point);
        Point point = (Point) value;
        assertLngLatAlt(100, 5, Double.NaN, point.getCoordinates());
    }

    @Test
    public void itShouldDeserializeAPointWithAltitude() {
        GeoJsonObject value = mapper.readValue("{\"type\":\"Point\",\"coordinates\":[100.0,5.0,123]}",
                GeoJsonObject.class);
        Point point = (Point) value;
        assertLngLatAlt(100, 5, 123, point.getCoordinates());
    }

    @Test
    public void itShouldSerializeAPointWithAltitude() {
        Point point = new Point(100, 0, 256);
        assertEquals("{\"type\":\"Point\",\"coordinates\":[100.0,0.0,256.0]}",
                mapper.writeValueAsString(point));
    }

    @Test
    public void itShouldDeserializeAPointWithAdditionalAttributes() {
        GeoJsonObject value = mapper.readValue("{\"type\":\"Point\",\"coordinates\":[100.0,5.0,123,456,789.2]}",
                GeoJsonObject.class);
        Point point = (Point) value;
        assertLngLatAlt(100, 5, 123, new double[] { 456d, 789.2 }, point.getCoordinates());
    }

    @Test
    public void itShouldSerializeAPointWithAdditionalAttributes() {
        Point point = new Point(100, 0, 256, 345d, 678d);
        assertEquals("{\"type\":\"Point\",\"coordinates\":[100.0,0.0,256.0,345.0,678.0]}",
                mapper.writeValueAsString(point));
    }

    @Test
    public void itShouldSerializeAPointWithAdditionalAttributesAndNull() {
        Point point = new Point(100, 0, 256, 345d, 678d);
        assertEquals("{\"type\":\"Point\",\"coordinates\":[100.0,0.0,256.0,345.0,678.0]}",
                mapper.writeValueAsString(point));
    }
}
