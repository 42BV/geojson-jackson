package org.geojson.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import tools.jackson.databind.ObjectMapper;

import org.geojson.FeatureCollection;
import org.geojson.GeometryCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.junit.Test;

public class GeometryCollectionTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void itShouldSerialize() {
		GeometryCollection gc = new GeometryCollection();
		gc.add(new Point(100, 0));
		gc.add(new LineString(new LngLatAlt(101, 0), new LngLatAlt(102, 1)));
		assertEquals("{\"type\":\"GeometryCollection\","
						+ "\"geometries\":[{\"type\":\"Point\",\"coordinates\":[100.0,0.0]},"
						+ "{\"type\":\"LineString\",\"coordinates\":[[101.0,0.0],[102.0,1.0]]}]}",
				mapper.writeValueAsString(gc));
	}

	@Test
	public void itShouldDeserialize() {
		GeometryCollection geometryCollection = mapper
				.readValue("{\"type\":\"GeometryCollection\","
								+ "\"geometries\":[{\"type\":\"Point\",\"coordinates\":[100.0,0.0]},"
								+ "{\"type\":\"LineString\",\"coordinates\":[[101.0,0.0],[102.0,1.0]]}]}",
						GeometryCollection.class);
		assertNotNull(geometryCollection);
	}

	@Test
	public void itShouldDeserializeSubtype() {
		FeatureCollection collection = mapper
				.readValue("{\"type\": \"FeatureCollection\","
								+ "  \"features\": ["
								+ "    {"
								+ "      \"type\": \"Feature\","
								+ "      \"geometry\": {"
								+ "        \"type\": \"GeometryCollection\","
								+ "        \"geometries\": ["
								+ "          {"
								+ "            \"type\": \"Point\","
								+ "            \"coordinates\": [100.0, 0.0]"
								+ "          }"
								+ "        ]"
								+ "      }"
								+ "    }"
								+ "  ]"
								+ "}",
						FeatureCollection.class);
		assertNotNull(collection);
		assertTrue(collection.getFeatures().get(0).getGeometry() instanceof GeometryCollection);
	}
}
