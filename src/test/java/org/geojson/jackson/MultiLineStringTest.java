package org.geojson.jackson;

import java.util.Arrays;

import org.geojson.LngLatAlt;
import org.geojson.MultiLineString;
import org.geojson.util.JsonTestUtils;
import org.geojson.util.TestResourceLoader;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MultiLineStringTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void itShouldSerialize() throws Exception {
		MultiLineString multiLineString = new MultiLineString();
		multiLineString.add(Arrays.asList(new LngLatAlt(100, 0), new LngLatAlt(101, 1)));
		multiLineString.add(Arrays.asList(new LngLatAlt(102, 2), new LngLatAlt(103, 3)));
        String expectedJson = TestResourceLoader.loadJson("json/multilinestring/multilinestring.json");
		JsonTestUtils.jsonEquals(expectedJson, mapper.writeValueAsString(multiLineString));
	}
}
