package org.geojson.jackson;

import tools.jackson.databind.ObjectMapper;

import org.geojson.LngLatAlt;
import org.junit.Assert;
import org.junit.Test;

public class LngLatAltSerializerTest
{

    @Test
    public void testSerialization()
    {
        LngLatAlt position = new LngLatAlt(49.43245, 52.42345, 120.34626);
        String correctJson = "[49.43245,52.42345,120.34626]";
        String producedJson = new ObjectMapper().writeValueAsString(position);
        Assert.assertEquals(correctJson, producedJson);
    }
}
