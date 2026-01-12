package org.geojson.jackson;

import tools.jackson.databind.ObjectMapper;

import org.geojson.LngLatAlt;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by babbleshack on 27/11/16.
 */
public class LngLatAltDeserializerTest {
    @Test
    public void deserializeMongoLngLatAlt() {
        LngLatAlt lngLatAlt = new LngLatAlt(10D, 15D, 5);
        String lngLatAltJson = new ObjectMapper().writeValueAsString(lngLatAlt);
        LngLatAlt lngLatAlt1 = new ObjectMapper().readValue(lngLatAltJson, LngLatAlt.class);
        Assert.assertEquals(lngLatAlt1, lngLatAlt);
    }
}
