package org.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import tools.jackson.databind.ObjectMapper;

import org.junit.Test;

public class FeatureTest {

	private static final Feature testObject = new Feature();
	private static final tools.jackson.databind.ObjectMapper mapper = new ObjectMapper();

	@Test
	public void itShouldHaveProperties() {
		assertNotNull(testObject.getProperties());
	}

	@Test
	public void itShouldSerializeFeature() {
		// http://geojson.org/geojson-spec.html#feature-objects
		// A feature object must have a member with the name "properties".
		// The value of the properties member is an object (any JSON object or a JSON null value).
		assertEquals("{\"type\":\"Feature\",\"geometry\":null,\"properties\":{}}",
				mapper.writeValueAsString(testObject));
	}
}