package org.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class AntimeridianCuttingTest {

    private GeoJsonConfig config;

    @Before
    public void setUp() {
        // Create a new configuration for each test
        config = new GeoJsonConfig().setCutAntimeridian(true);
    }

    @Test
    public void testLineStringCutting() {
        LineString lineString = new LineString(
                new LngLatAlt(170, 45),
                new LngLatAlt(-170, 45)
        );
        lineString.setConfig(config);

        GeoJsonObject processed = GeoJsonUtils.process(lineString, config);

        assertTrue("Should be a MultiLineString after cutting", processed instanceof MultiLineString);
        MultiLineString multiLineString = (MultiLineString) processed;
        assertEquals("Should have 2 segments", 2, multiLineString.getCoordinates().size());

        List<LngLatAlt> segment1 = multiLineString.getCoordinates().get(0);
        List<LngLatAlt> segment2 = multiLineString.getCoordinates().get(1);

        assertEquals("First segment should end at longitude 180", 180.0, segment1.get(segment1.size() - 1).getLongitude(), 0.001);

        assertEquals("Second segment should start at longitude -180", -180.0, segment2.get(0).getLongitude(), 0.001);
    }

    @Test
    public void testMultipleCrossings() {
        LineString lineString = new LineString(
                new LngLatAlt(170, 45),  // Start in eastern hemisphere
                new LngLatAlt(-170, 45), // Cross to western hemisphere
                new LngLatAlt(-160, 40), // Stay in western
                new LngLatAlt(160, 40)   // Cross back to eastern
        );
        lineString.setConfig(config);

        GeoJsonObject processed = GeoJsonUtils.process(lineString, config);

        assertTrue("Should be a MultiLineString after cutting", processed instanceof MultiLineString);
        MultiLineString multiLineString = (MultiLineString) processed;

        assertTrue("Should have at least 2 segments", multiLineString.getCoordinates().size() >= 2);
    }

    @Test
    public void testPolygonCutting() {
        List<LngLatAlt> ring = Arrays.asList(
                new LngLatAlt(170, 40),
                new LngLatAlt(170, 50),
                new LngLatAlt(-170, 50),
                new LngLatAlt(-170, 40),
                new LngLatAlt(170, 40)  // Close the ring
        );

        Polygon polygon = new Polygon();
        polygon.setConfig(config);
        polygon.add(ring);

        GeoJsonObject processed = GeoJsonUtils.process(polygon, config);

        assertTrue("Should be a MultiPolygon after cutting", processed instanceof MultiPolygon);
        MultiPolygon multiPolygon = (MultiPolygon) processed;

        assertTrue("Should have at least 1 polygon", multiPolygon.getCoordinates().size() >= 1);
    }

    @Test
    public void testPolygonCuttingImproved() {
        // Create a polygon that crosses the antimeridian
        Polygon polygon = new Polygon();
        polygon.setConfig(config);
        polygon.add(Arrays.asList(
                new LngLatAlt(170, 10),
                new LngLatAlt(170, 20),
                new LngLatAlt(-170, 20),
                new LngLatAlt(-170, 10),
                new LngLatAlt(170, 10)  // Close the ring
        ));

        // Cut the polygon at the antimeridian
        GeoJsonObject result = GeoJsonUtils.cutPolygonAtAntimeridian(polygon);

        // Verify the result is a MultiPolygon
        assertTrue("Result should be a MultiPolygon", result instanceof MultiPolygon);
        MultiPolygon multiPolygon = (MultiPolygon) result;

        // Verify we have two polygons (east and west sides)
        assertEquals("Should have 2 polygons after cutting", 2, multiPolygon.getCoordinates().size());

        // Verify each polygon has a valid exterior ring
        for (List<List<LngLatAlt>> polygonRings : multiPolygon.getCoordinates()) {
            assertFalse("Each polygon should have at least one ring", polygonRings.isEmpty());
            List<LngLatAlt> exteriorRing = polygonRings.get(0);
            assertTrue("Exterior ring should have at least 4 points", exteriorRing.size() >= 4);

            // Verify the ring is closed
            LngLatAlt first = exteriorRing.get(0);
            LngLatAlt last = exteriorRing.get(exteriorRing.size() - 1);
            assertEquals("Ring should be closed (first and last points should be the same)",
                    first.getLongitude(), last.getLongitude(), 0.001);
            assertEquals("Ring should be closed (first and last points should be the same)",
                    first.getLatitude(), last.getLatitude(), 0.001);
        }
    }

    @Test
    public void testFeatureCutting() {
        LineString lineString = new LineString(
                new LngLatAlt(170, 45),
                new LngLatAlt(-170, 45)
        );
        lineString.setConfig(config);

        Feature feature = new Feature();
        feature.setConfig(config);
        feature.setGeometry(lineString);
        feature.setProperty("name", "International Date Line Crossing");
        feature.setProperty("length_km", 222.6);

        GeoJsonObject processed = GeoJsonUtils.process(feature, config);

        assertTrue("Should still be a Feature", processed instanceof Feature);
        Feature processedFeature = (Feature) processed;

        assertTrue("Geometry should be a MultiLineString after cutting",
                processedFeature.getGeometry() instanceof MultiLineString);

        assertEquals("Name property should be preserved",
                "International Date Line Crossing", processedFeature.getProperty("name"));
        assertEquals("Length property should be preserved",
                222.6, processedFeature.getProperty("length_km"), 0.1);
    }

    @Test
    public void testFeatureCollectionCutting() {
        LineString lineString = new LineString(
                new LngLatAlt(170, 45),
                new LngLatAlt(-170, 45)
        );
        lineString.setConfig(config);

        List<LngLatAlt> ring = Arrays.asList(
                new LngLatAlt(170, 40),
                new LngLatAlt(170, 50),
                new LngLatAlt(-170, 50),
                new LngLatAlt(-170, 40),
                new LngLatAlt(170, 40)  // Close the ring
        );
        Polygon polygon = new Polygon();
        polygon.setConfig(config);
        polygon.add(ring);

        Point point = new Point(175, 45);
        point.setConfig(config);

        Feature lineFeature = new Feature();
        lineFeature.setConfig(config);
        lineFeature.setGeometry(lineString);
        lineFeature.setProperty("type", "line");

        Feature polygonFeature = new Feature();
        polygonFeature.setConfig(config);
        polygonFeature.setGeometry(polygon);
        polygonFeature.setProperty("type", "polygon");

        Feature pointFeature = new Feature();
        pointFeature.setConfig(config);
        pointFeature.setGeometry(point);
        pointFeature.setProperty("type", "point");

        FeatureCollection featureCollection = new FeatureCollection();
        featureCollection.setConfig(config);
        featureCollection.add(lineFeature);
        featureCollection.add(polygonFeature);
        featureCollection.add(pointFeature);

        GeoJsonObject processed = GeoJsonUtils.process(featureCollection, config);

        assertTrue("Should still be a FeatureCollection", processed instanceof FeatureCollection);
        FeatureCollection processedCollection = (FeatureCollection) processed;

        assertEquals("Should still have 3 features", 3, processedCollection.getFeatures().size());

        int multiLineStrings = 0;
        int multiPolygons = 0;
        int points = 0;

        for (Feature feature : processedCollection) {
            if (feature.getGeometry() instanceof MultiLineString) {
                multiLineStrings++;
                assertEquals("line", feature.getProperty("type"));
            } else if (feature.getGeometry() instanceof MultiPolygon) {
                multiPolygons++;
                assertEquals("polygon", feature.getProperty("type"));
            } else if (feature.getGeometry() instanceof Point) {
                points++;
                assertEquals("point", feature.getProperty("type"));
            }
        }

        assertEquals("Should have 1 MultiLineString", 1, multiLineStrings);
        assertEquals("Should have 1 MultiPolygon", 1, multiPolygons);
        assertEquals("Should have 1 Point", 1, points);
    }

    @Test
    public void testPolygonWithHoleAtAntimeridian() {
        // Create a polygon with a hole that crosses the antimeridian
        Polygon polygon = new Polygon();
        polygon.setConfig(config);

        // Exterior ring crossing the antimeridian
        polygon.add(Arrays.asList(
                new LngLatAlt(160, 0),
                new LngLatAlt(160, 30),
                new LngLatAlt(-160, 30),
                new LngLatAlt(-160, 0),
                new LngLatAlt(160, 0)  // Close the ring
        ));

        // Interior ring (hole) crossing the antimeridian
        polygon.addInteriorRing(
                new LngLatAlt(170, 10),
                new LngLatAlt(170, 20),
                new LngLatAlt(-170, 20),
                new LngLatAlt(-170, 10),
                new LngLatAlt(170, 10)  // Close the ring
        );

        // Cut the polygon at the antimeridian
        GeoJsonObject result = GeoJsonUtils.cutPolygonAtAntimeridian(polygon);

        // Verify the result is a MultiPolygon
        assertTrue("Result should be a MultiPolygon", result instanceof MultiPolygon);
        MultiPolygon multiPolygon = (MultiPolygon) result;

        // Verify we have two polygons (east and west sides)
        assertEquals("Should have 2 polygons after cutting", 2, multiPolygon.getCoordinates().size());

        // Verify each polygon has a valid exterior ring and interior ring
        for (List<List<LngLatAlt>> polygonRings : multiPolygon.getCoordinates()) {
            assertFalse("Each polygon should have at least one ring", polygonRings.isEmpty());

            // Check if this polygon has an interior ring
            if (polygonRings.size() > 1) {
                List<LngLatAlt> interiorRing = polygonRings.get(1);
                assertTrue("Interior ring should have at least 4 points", interiorRing.size() >= 4);

                // Verify the interior ring is closed
                LngLatAlt first = interiorRing.get(0);
                LngLatAlt last = interiorRing.get(interiorRing.size() - 1);
                assertEquals("Ring should be closed (first and last points should be the same)",
                        first.getLongitude(), last.getLongitude(), 0.001);
                assertEquals("Ring should be closed (first and last points should be the same)",
                        first.getLatitude(), last.getLatitude(), 0.001);
            }
        }
    }

    @Test
    public void testGeometryCollectionCutting() {
        LineString lineString = new LineString(
                new LngLatAlt(170, 45),
                new LngLatAlt(-170, 45)
        );
        lineString.setConfig(config);

        List<LngLatAlt> ring = Arrays.asList(
                new LngLatAlt(170, 40),
                new LngLatAlt(170, 50),
                new LngLatAlt(-170, 50),
                new LngLatAlt(-170, 40),
                new LngLatAlt(170, 40)  // Close the ring
        );
        Polygon polygon = new Polygon();
        polygon.setConfig(config);
        polygon.add(ring);

        Point point = new Point(175, 45);
        point.setConfig(config);

        GeometryCollection geometryCollection = new GeometryCollection();
        geometryCollection.setConfig(config);
        geometryCollection.add(lineString);
        geometryCollection.add(polygon);
        geometryCollection.add(point);

        GeoJsonObject processed = GeoJsonUtils.process(geometryCollection, config);

        assertTrue("Should still be a GeometryCollection", processed instanceof GeometryCollection);
        GeometryCollection processedCollection = (GeometryCollection) processed;

        assertEquals("Should still have 3 geometries", 3, processedCollection.getGeometries().size());

        int multiLineStrings = 0;
        int multiPolygons = 0;
        int points = 0;

        for (GeoJsonObject geometry : processedCollection) {
            if (geometry instanceof MultiLineString) {
                multiLineStrings++;
            } else if (geometry instanceof MultiPolygon) {
                multiPolygons++;
            } else if (geometry instanceof Point) {
                points++;
            }
        }

        assertEquals("Should have 1 MultiLineString", 1, multiLineStrings);
        assertEquals("Should have 1 MultiPolygon", 1, multiPolygons);
        assertEquals("Should have 1 Point", 1, points);
    }
}
