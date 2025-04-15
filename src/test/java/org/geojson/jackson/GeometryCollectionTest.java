package org.geojson.jackson;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geojson.FeatureCollection;
import org.geojson.GeometryCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.util.JsonTestUtils;
import org.geojson.util.TestResourceLoader;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GeometryCollectionTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void itShouldSerialize() throws Exception {
		GeometryCollection gc = new GeometryCollection();
		gc.add(new Point(100, 0));
		gc.add(new LineString(new LngLatAlt(101, 0), new LngLatAlt(102, 1)));
		String expectedJson = TestResourceLoader.loadJson("json/geometrycollection/geometry_collection.json");
		JsonTestUtils.jsonEquals(expectedJson, mapper.writeValueAsString(gc));
	}

	@Test
	public void itShouldDeserialize() throws Exception {
		String json = TestResourceLoader.loadJson("json/geometrycollection/geometry_collection.json");
		GeometryCollection geometryCollection = mapper.readValue(json, GeometryCollection.class);
		assertNotNull(geometryCollection);
	}

	@Test
	public void itShouldDeserializeSubtype() throws Exception {
		String json = TestResourceLoader.loadJson("json/geometrycollection/feature_collection_with_geometry_collection.json");
		FeatureCollection collection = mapper.readValue(json, FeatureCollection.class);
		assertNotNull(collection);
		assertTrue(collection.getFeatures().get(0).getGeometry() instanceof GeometryCollection);
	}
}
