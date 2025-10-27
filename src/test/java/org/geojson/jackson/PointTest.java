package org.geojson.jackson;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.util.JsonTestUtils;
import org.geojson.util.TestResourceLoader;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PointTest {

	private final ObjectMapper mapper = new ObjectMapper();

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
	public void itShouldSerializeAPoint() throws Exception {
		Point point = new Point(100, 0);
        String expectedJson = TestResourceLoader.loadJson("json/point/point.json");
        JsonTestUtils.jsonEquals(expectedJson, mapper.writeValueAsString(point));
	}

	@Test
	public void itShouldDeserializeAPoint() throws Exception {
        String json = TestResourceLoader.loadJson("json/point/point_with_coordinates.json");
        GeoJsonObject value = mapper.readValue(json, GeoJsonObject.class);
		assertNotNull(value);
		assertTrue(value instanceof Point);
		Point point = (Point)value;
		assertLngLatAlt(100, 5, Double.NaN, point.getCoordinates());
	}

	@Test
	public void itShouldDeserializeAPointWithAltitude() throws Exception {
        String json = TestResourceLoader.loadJson("json/point/point_with_altitude.json");
        GeoJsonObject value = mapper.readValue(json, GeoJsonObject.class);
		Point point = (Point)value;
		assertLngLatAlt(100, 5, 123, point.getCoordinates());
	}

	@Test
	public void itShouldSerializeAPointWithAltitude() throws Exception {
		Point point = new Point(100, 0, 256);
        String expectedJson = TestResourceLoader.loadJson("json/point/point_with_altitude_serialized.json");
        JsonTestUtils.jsonEquals(expectedJson, mapper.writeValueAsString(point));
	}

	@Test
	public void itShouldDeserializeAPointWithAdditionalAttributes() throws IOException {
        String json = TestResourceLoader.loadJson("json/point/point_with_additional_attributes.json");
        GeoJsonObject value = mapper.readValue(json, GeoJsonObject.class);
		Point point = (Point)value;
		assertLngLatAlt(100, 5, 123, new double[] {456d, 789.2}, point.getCoordinates());
	}

	@Test
    public void itShouldSerializeAPointWithAdditionalAttributes() throws IOException {
		Point point = new Point(100, 0, 256, 345d, 678d);
        String expectedJson = TestResourceLoader.loadJson("json/point/point_with_additional_attributes_serialized.json");
        JsonTestUtils.jsonEquals(expectedJson, mapper.writeValueAsString(point));
	}

	@Test
    public void itShouldSerializeAPointWithAdditionalAttributesAndNull() throws IOException {
		Point point = new Point(100, 0, 256, 345d, 678d);
        String expectedJson = TestResourceLoader.loadJson("json/point/point_with_additional_attributes_serialized.json");
        JsonTestUtils.jsonEquals(expectedJson, mapper.writeValueAsString(point));
	}
}
