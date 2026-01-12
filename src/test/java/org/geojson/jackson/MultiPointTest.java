package org.geojson.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import tools.jackson.databind.ObjectMapper;

import org.geojson.LngLatAlt;
import org.geojson.MultiPoint;
import org.junit.Test;

public class MultiPointTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void itShouldSerializeMultiPoint() {
		MultiPoint multiPoint = new MultiPoint(new LngLatAlt(100, 0), new LngLatAlt(101, 1));
		assertEquals("{\"type\":\"MultiPoint\",\"coordinates\":[[100.0,0.0],[101.0,1.0]]}",
				mapper.writeValueAsString(multiPoint));
	}

	@Test
	public void itShouldDeserializeMultiPoint() {
		MultiPoint multiPoint = mapper
				.readValue("{\"type\":\"MultiPoint\",\"coordinates\":[[100.0,0.0],[101.0,1.0]]}",
				MultiPoint.class);
		assertNotNull(multiPoint);
		List<LngLatAlt> coordinates = multiPoint.getCoordinates();
		PointTest.assertLngLatAlt(100, 0, Double.NaN, coordinates.get(0));
		PointTest.assertLngLatAlt(101, 1, Double.NaN, coordinates.get(1));
	}
}
