package org.geojson.jackson;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.geojson.LngLatAlt;
import org.geojson.MultiPoint;
import org.geojson.util.JsonTestUtils;
import org.geojson.util.TestResourceLoader;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MultiPointTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void itShouldSerializeMultiPoint() throws Exception {
		MultiPoint multiPoint = new MultiPoint(new LngLatAlt(100, 0), new LngLatAlt(101, 1));
        String expectedJson = TestResourceLoader.loadJson("json/multipoint/multipoint.json");
		JsonTestUtils.jsonEquals(expectedJson, mapper.writeValueAsString(multiPoint));
	}

	@Test
	public void itShouldDeserializeMultiPoint() throws Exception {
        String json = TestResourceLoader.loadJson("json/multipoint/multipoint.json");
        MultiPoint multiPoint = mapper.readValue(json, MultiPoint.class);
		assertNotNull(multiPoint);
		List<LngLatAlt> coordinates = multiPoint.getCoordinates();
		PointTest.assertLngLatAlt(100, 0, Double.NaN, coordinates.get(0));
		PointTest.assertLngLatAlt(101, 1, Double.NaN, coordinates.get(1));
	}
}
