package org.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geojson.util.TestResourceLoader;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FeatureTest {

	private final Feature testObject = new Feature();
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void itShouldHaveProperties() throws Exception {
		assertNotNull(testObject.getProperties());
	}

	@Test
	public void itShouldSerializeFeature() throws Exception {
		// http://geojson.org/geojson-spec.html#feature-objects
		// A feature object must have a member with the name "properties".
		// The value of the properties member is an object (any JSON object or a JSON null value).
        String expectedJson = TestResourceLoader.loadJson("json/feature/feature.json");
        assertEquals(expectedJson, mapper.writeValueAsString(testObject));
	}
}
