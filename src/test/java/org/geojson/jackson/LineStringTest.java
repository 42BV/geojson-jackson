package org.geojson.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import tools.jackson.databind.ObjectMapper;

import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.MultiPoint;
import org.junit.Test;

public class LineStringTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void itShouldSerializeMultiPoint() {
		MultiPoint lineString = new LineString(new LngLatAlt(100, 0), new LngLatAlt(101, 1));
		assertEquals("{\"type\":\"LineString\",\"coordinates\":[[100.0,0.0],[101.0,1.0]]}",
				mapper.writeValueAsString(lineString));
	}

	@Test
	public void itShouldDeserializeLineString() {
		LineString lineString = mapper.readValue("{\"type\":\"LineString\",\"coordinates\":[[100.0,0.0],[101.0,1.0]]}",
				LineString.class);
		assertNotNull(lineString);
		List<LngLatAlt> coordinates = lineString.getCoordinates();
		PointTest.assertLngLatAlt(100, 0, Double.NaN, coordinates.get(0));
		PointTest.assertLngLatAlt(101, 1, Double.NaN, coordinates.get(1));
	}
}
