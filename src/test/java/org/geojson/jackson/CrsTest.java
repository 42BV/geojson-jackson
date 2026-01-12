package org.geojson.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import tools.jackson.databind.ObjectMapper;

import org.geojson.Crs;
import org.geojson.GeoJsonObject;
import org.geojson.Point;
import org.junit.Test;

public class CrsTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void itShouldParseCrsWithLink() {
		GeoJsonObject value = mapper.readValue("{\"crs\": { \"type\": \"link\", \"properties\": "
				+ "{ \"href\": \"http://example.com/crs/42\", \"type\": \"proj4\" }},"
				+ "\"type\":\"Point\",\"coordinates\":[100.0,5.0]}", GeoJsonObject.class);
		assertNotNull(value);
		assertEquals(CrsType.link, value.getCrs().getType());
	}

	@Test
	public void itShouldSerializeCrsWithLink() {
		Point point = new Point();
		Crs crs = new Crs();
		crs.setType(CrsType.link);
		point.setCrs(crs);
		String value = mapper.writeValueAsString(point);
		assertEquals("{\"type\":\"Point\",\"crs\":{\"properties\":{},\"type\":\"link\"}}", value);
	}
}
