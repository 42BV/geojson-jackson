package org.geojson.jackson;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.MultiPoint;
import org.geojson.util.JsonTestUtils;
import org.geojson.util.TestResourceLoader;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LineStringTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void itShouldSerializeMultiPoint() throws Exception {
		MultiPoint lineString = new LineString(new LngLatAlt(100, 0), new LngLatAlt(101, 1));
        String expectedJson = TestResourceLoader.loadJson("json/linestring/linestring.json");
        JsonTestUtils.jsonEquals(expectedJson, mapper.writeValueAsString(lineString));
	}

	@Test
	public void itShouldDeserializeLineString() throws Exception {
        String json = TestResourceLoader.loadJson("json/linestring/linestring.json");
        LineString lineString = mapper.readValue(json, LineString.class);
		assertNotNull(lineString);
		List<LngLatAlt> coordinates = lineString.getCoordinates();
		PointTest.assertLngLatAlt(100, 0, Double.NaN, coordinates.get(0));
		PointTest.assertLngLatAlt(101, 1, Double.NaN, coordinates.get(1));
	}
}
