package org.geojson.jackson;

import static org.junit.Assert.assertEquals;

import org.geojson.LngLatAlt;
import org.geojson.MultiPolygon;
import org.geojson.Polygon;
import org.geojson.util.TestResourceLoader;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MultiPoligonTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void itShouldSerialize() throws Exception {
		MultiPolygon multiPolygon = new MultiPolygon();
		multiPolygon.add(new Polygon(new LngLatAlt(102, 2), new LngLatAlt(103, 2), new LngLatAlt(103, 3),
				new LngLatAlt(102, 3), new LngLatAlt(102, 2)));
		Polygon polygon = new Polygon(MockData.EXTERNAL);
		polygon.addInteriorRing(MockData.INTERNAL);
		multiPolygon.add(polygon);
        String expectedJson = TestResourceLoader.loadJson("json/multipolygon/multipolygon.json");
        assertEquals(expectedJson, mapper.writeValueAsString(multiPolygon));
	}

	@Test
	public void itShouldDeserialize() throws Exception {
        String json = TestResourceLoader.loadJson("json/multipolygon/multipolygon.json");
        MultiPolygon multiPolygon = mapper.readValue(json, MultiPolygon.class);
		assertEquals(2, multiPolygon.getCoordinates().size());
	}
}
