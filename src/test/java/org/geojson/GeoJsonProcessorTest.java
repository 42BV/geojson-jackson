package org.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geojson.util.PolygonOrientationUtils;
import org.junit.Before;
import org.junit.Test;

public class GeoJsonProcessorTest {

    private GeoJsonConfig config;

    @Before
    public void setUp() {
        // Create a configuration for testing
        config = new GeoJsonConfig();
        config.setAutoFixPolygonOrientation(true);
        config.setCutAntimeridian(true);
    }

    @Test
    public void testCreatePolygonFromRings() {
        // Create rings
        List<List<LngLatAlt>> rings = new ArrayList<>();

        // Exterior ring
        List<LngLatAlt> exteriorRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(10, 0),
                new LngLatAlt(10, 10),
                new LngLatAlt(0, 10),
                new LngLatAlt(0, 0)
        );

        // Interior ring (hole)
        List<LngLatAlt> interiorRing = Arrays.asList(
                new LngLatAlt(2, 2),
                new LngLatAlt(8, 2),
                new LngLatAlt(8, 8),
                new LngLatAlt(2, 8),
                new LngLatAlt(2, 2)
        );

        rings.add(exteriorRing);
        rings.add(interiorRing);

        // Create polygon from rings
        Polygon polygon = GeoJsonProcessor.createPolygonFromRings(rings);

        // Verify the polygon has the correct rings
        assertEquals(2, polygon.getCoordinates().size());
        assertEquals(exteriorRing, polygon.getExteriorRing());
        assertEquals(interiorRing, polygon.getInteriorRings().get(0));
    }

    @Test
    public void testProcessPolygon() {
        // Create a polygon with incorrect orientation
        Polygon polygon = new Polygon();

        // Add a clockwise exterior ring (incorrect orientation)
        List<LngLatAlt> exteriorRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(0, 10),
                new LngLatAlt(10, 10),
                new LngLatAlt(10, 0),
                new LngLatAlt(0, 0));
        polygon.add(exteriorRing);

        // Process the polygon
        GeoJsonObject processed = GeoJsonProcessor.processPolygon(polygon, config);

        // Verify the result is still a Polygon
        assertTrue(processed instanceof Polygon);
        Polygon processedPolygon = (Polygon) processed;

        // Verify the orientation has been fixed
        assertTrue(PolygonOrientationUtils.isCounterClockwise(processedPolygon.getExteriorRing()));

        // Create a polygon that crosses the antimeridian
        Polygon crossingPolygon = new Polygon();
        List<LngLatAlt> crossingRing = Arrays.asList(
                new LngLatAlt(170, 0),
                new LngLatAlt(170, 10),
                new LngLatAlt(-170, 10),
                new LngLatAlt(-170, 0),
                new LngLatAlt(170, 0));
        crossingPolygon.add(crossingRing);

        // Process the polygon
        GeoJsonObject crossingProcessed = GeoJsonProcessor.processPolygon(crossingPolygon, config);

        // Verify the result is a MultiPolygon
        assertTrue(crossingProcessed instanceof MultiPolygon);
    }

    @Test
    public void testProcessLineString() {
        // Create a LineString that crosses the antimeridian
        LineString lineString = new LineString();
        lineString.add(new LngLatAlt(170, 0));
        lineString.add(new LngLatAlt(-170, 10));

        // Process the LineString
        GeoJsonObject processed = GeoJsonProcessor.processLineString(lineString, config);

        // Verify the result is a MultiLineString
        assertTrue(processed instanceof MultiLineString);

        // Create a LineString that doesn't cross the antimeridian
        LineString nonCrossingLineString = new LineString();
        nonCrossingLineString.add(new LngLatAlt(0, 0));
        nonCrossingLineString.add(new LngLatAlt(10, 10));

        // Process the LineString
        GeoJsonObject nonCrossingProcessed = GeoJsonProcessor.processLineString(nonCrossingLineString, config);

        // Verify the result is still a LineString
        assertTrue(nonCrossingProcessed instanceof LineString);
        assertEquals(nonCrossingLineString, nonCrossingProcessed);
    }

    @Test
    public void testProcessFeature() {
        // Create a Feature with a Polygon geometry
        Polygon polygon = new Polygon();
        List<LngLatAlt> polygonRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(0, 10),
                new LngLatAlt(10, 10),
                new LngLatAlt(10, 0),
                new LngLatAlt(0, 0));
        polygon.add(polygonRing);

        Feature feature = new Feature();
        feature.setGeometry(polygon);

        // Process the Feature
        Feature processed = GeoJsonProcessor.processFeature(feature, config);

        // Verify the geometry has been processed
        assertTrue(processed.getGeometry() instanceof Polygon);
        Polygon processedPolygon = (Polygon) processed.getGeometry();

        // Verify the orientation has been fixed
        assertTrue(PolygonOrientationUtils.isCounterClockwise(processedPolygon.getExteriorRing()));
    }

    @Test
    public void testProcessFeatureCollection() {
        // Create a FeatureCollection with multiple Features
        FeatureCollection featureCollection = new FeatureCollection();

        // Add a Feature with a Polygon
        Polygon polygon = new Polygon();
        List<LngLatAlt> polygonRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(0, 10),
                new LngLatAlt(10, 10),
                new LngLatAlt(10, 0),
                new LngLatAlt(0, 0));
        polygon.add(polygonRing);

        Feature polygonFeature = new Feature();
        polygonFeature.setGeometry(polygon);
        featureCollection.add(polygonFeature);

        // Add a Feature with a LineString
        LineString lineString = new LineString();
        lineString.add(new LngLatAlt(170, 0));
        lineString.add(new LngLatAlt(-170, 10));

        Feature lineStringFeature = new Feature();
        lineStringFeature.setGeometry(lineString);
        featureCollection.add(lineStringFeature);

        // Process the FeatureCollection
        FeatureCollection processed = GeoJsonProcessor.processFeatureCollection(featureCollection, config);

        // Verify the Features have been processed
        assertEquals(2, processed.getFeatures().size());

        // Verify the Polygon Feature
        Feature processedPolygonFeature = processed.getFeatures().get(0);
        assertTrue(processedPolygonFeature.getGeometry() instanceof Polygon);
        Polygon processedPolygon = (Polygon) processedPolygonFeature.getGeometry();
        assertTrue(PolygonOrientationUtils.isCounterClockwise(processedPolygon.getExteriorRing()));

        // Verify the LineString Feature
        Feature processedLineStringFeature = processed.getFeatures().get(1);
        assertTrue(processedLineStringFeature.getGeometry() instanceof MultiLineString);
    }

    @Test
    public void testProcessGeometryCollection() {
        // Create a GeometryCollection with multiple geometries
        GeometryCollection geometryCollection = new GeometryCollection();

        // Add a Polygon
        Polygon polygon = new Polygon();
        List<LngLatAlt> polygonRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(0, 10),
                new LngLatAlt(10, 10),
                new LngLatAlt(10, 0),
                new LngLatAlt(0, 0));
        polygon.add(polygonRing);
        geometryCollection.add(polygon);

        // Add a LineString
        LineString lineString = new LineString();
        lineString.add(new LngLatAlt(170, 0));
        lineString.add(new LngLatAlt(-170, 10));
        geometryCollection.add(lineString);

        // Process the GeometryCollection
        GeometryCollection processed = GeoJsonProcessor.processGeometryCollection(geometryCollection, config);

        // Verify the geometries have been processed
        assertEquals(2, processed.getGeometries().size());

        // Verify the Polygon
        assertTrue(processed.getGeometries().get(0) instanceof Polygon);
        Polygon processedPolygon = (Polygon) processed.getGeometries().get(0);
        assertTrue(PolygonOrientationUtils.isCounterClockwise(processedPolygon.getExteriorRing()));

        // Verify the LineString
        assertTrue(processed.getGeometries().get(1) instanceof MultiLineString);
    }

    @Test
    public void testProcess() {
        // Test with different types of GeoJSON objects

        // Test with a Polygon
        Polygon polygon = new Polygon();
        List<LngLatAlt> polygonRing = Arrays.asList(
                new LngLatAlt(0, 0),
                new LngLatAlt(0, 10),
                new LngLatAlt(10, 10),
                new LngLatAlt(10, 0),
                new LngLatAlt(0, 0));
        polygon.add(polygonRing);

        GeoJsonObject processedPolygon = GeoJsonProcessor.process(polygon, config);
        assertTrue(processedPolygon instanceof Polygon);

        // Test with a LineString
        LineString lineString = new LineString();
        lineString.add(new LngLatAlt(170, 0));
        lineString.add(new LngLatAlt(-170, 10));

        GeoJsonObject processedLineString = GeoJsonProcessor.process(lineString, config);
        assertTrue(processedLineString instanceof MultiLineString);

        // Test with a Point (should not be modified)
        Point point = new Point(0, 0);
        GeoJsonObject processedPoint = GeoJsonProcessor.process(point, config);
        assertEquals(point, processedPoint);

        // Test with null config (should use default config)
        GeoJsonObject processedWithDefaultConfig = GeoJsonProcessor.process(polygon);
        assertNotNull(processedWithDefaultConfig);
    }
}
