package org.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.geojson.util.PolygonOrientationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GeoJsonMapperTest {

    private GeoJsonMapper legacyMapper;
    private GeoJsonMapper rfc7946Mapper;

    @Before
    public void setUp() {
        // Create legacy configuration
        GeoJsonConfig legacyConfig = GeoJsonConfig.legacy();
        legacyMapper = new GeoJsonMapper(legacyConfig);

        // Create RFC 7946 configuration with auto-fix enabled
        GeoJsonConfig rfc7946Config = GeoJsonConfig.rfc7946();
        rfc7946Config.setAutoFixPolygonOrientation(true);
        rfc7946Mapper = new GeoJsonMapper(rfc7946Config);
    }

    @After
    public void tearDown() {
        // No need to reset global configuration anymore
    }

    @Test
    public void testLegacyMapper() throws IOException {
        // Create a point with CRS
        Point point = new Point(100, 0);
        Crs crs = new Crs();
        crs.getProperties().put("name", "EPSG:4326");
        point.setCrs(crs);

        // Serialize and deserialize with legacy mapper
        String json = legacyMapper.writeValueAsString(point);
        Point deserializedPoint = legacyMapper.readValue(json, Point.class);

        // The CRS should be preserved
        assertNotNull(deserializedPoint.getCrs());
        assertEquals("EPSG:4326", deserializedPoint.getCrs().getProperties().get("name"));
    }

    @Test
    public void testRfc7946Mapper() throws IOException {
        // Create a polygon with counterclockwise exterior ring (valid in RFC 7946)
        Polygon polygon = new Polygon(
                new LngLatAlt(0, 0),
                new LngLatAlt(1, 0),
                new LngLatAlt(1, 1),
                new LngLatAlt(0, 1),
                new LngLatAlt(0, 0)
        );
        polygon.setConfig(rfc7946Mapper.getConfig());

        // Verify the polygon is counterclockwise
        assertTrue(PolygonOrientationUtils.isCounterClockwise(polygon.getExteriorRing()));

        // Serialize and deserialize with RFC 7946 mapper
        String json = rfc7946Mapper.writeValueAsString(polygon);
        Polygon deserializedPolygon = rfc7946Mapper.readValue(json, Polygon.class);

        // The polygon orientation should be preserved
        assertTrue(PolygonOrientationUtils.isCounterClockwise(deserializedPolygon.getExteriorRing()));
    }

    @Test
    public void testProcessMethod() {
        // Create a LineString that crosses the antimeridian
        LineString lineString = new LineString(
                new LngLatAlt(170, 45),
                new LngLatAlt(-170, 45)
        );
        lineString.setConfig(rfc7946Mapper.getConfig());

        // Process the LineString with the RFC 7946 mapper
        GeoJsonObject processed = rfc7946Mapper.process(lineString);

        // The result should be a MultiLineString
        assertTrue(processed instanceof MultiLineString);
        MultiLineString multiLineString = (MultiLineString) processed;

        // It should have two segments
        assertEquals(2, multiLineString.getCoordinates().size());
    }
}
