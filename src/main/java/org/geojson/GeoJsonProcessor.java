package org.geojson;

import java.util.ArrayList;
import java.util.List;

import org.geojson.util.AntimeridianUtils;
import org.geojson.util.PolygonOrientationUtils;

/**
 * Utility class for processing GeoJSON objects according to RFC 7946.
 * Contains methods for processing different GeoJSON objects.
 */
public class GeoJsonProcessor {

    /**
     * Creates a polygon from a list of rings.
     *
     * @param rings The list of rings to create a polygon from
     * @return A new Polygon containing the rings
     */
    public static Polygon createPolygonFromRings(List<List<LngLatAlt>> rings) {
        Polygon polygon = new Polygon();
        for (List<LngLatAlt> ring : rings) {
            polygon.add(ring);
        }
        return polygon;
    }

    /**
     * Processes a Polygon according to RFC 7946 recommendations.
     *
     * @param polygon The Polygon to process
     * @param config  The GeoJSON configuration to use
     * @return The processed GeoJSON object
     */
    public static GeoJsonObject processPolygon(Polygon polygon, GeoJsonConfig config) {
        // Fix polygon orientation if needed
        if (config.isAutoFixPolygonOrientation()) {
            polygon.setCoordinates(PolygonOrientationUtils.fixPolygonOrientation(polygon.getCoordinates()));
        }

        // Cut at antimeridian if needed
        if (config.isCutAntimeridian()) {
            GeoJsonObject cut = AntimeridianUtils.cutPolygonAtAntimeridian(polygon);
            // Cutting preserves traversal direction, but re-fix the resulting parts to be safe
            if (cut instanceof MultiPolygon && config.isAutoFixPolygonOrientation()) {
                fixMultiPolygonOrientation((MultiPolygon) cut);
            }
            return cut;
        }

        return polygon;
    }

    /**
     * Processes a MultiPolygon according to RFC 7946 recommendations.
     * Polygons that cross the antimeridian are cut, so the result may contain
     * more polygons than the input.
     *
     * @param multiPolygon The MultiPolygon to process
     * @param config       The GeoJSON configuration to use
     * @return The processed MultiPolygon
     */
    public static MultiPolygon processMultiPolygon(MultiPolygon multiPolygon, GeoJsonConfig config) {
        List<List<List<LngLatAlt>>> processed = new ArrayList<>();
        for (List<List<LngLatAlt>> rings : multiPolygon.getCoordinates()) {
            GeoJsonObject result = processPolygon(createPolygonFromRings(rings), config);
            if (result instanceof MultiPolygon) {
                processed.addAll(((MultiPolygon) result).getCoordinates());
            } else {
                processed.add(((Polygon) result).getCoordinates());
            }
        }
        multiPolygon.setCoordinates(processed);
        return multiPolygon;
    }

    /**
     * Processes a LineString according to RFC 7946 recommendations.
     *
     * @param lineString The LineString to process
     * @param config     The GeoJSON configuration to use
     * @return The processed GeoJSON object
     */
    public static GeoJsonObject processLineString(LineString lineString, GeoJsonConfig config) {
        if (config.isCutAntimeridian()) {
            return AntimeridianUtils.cutLineStringAtAntimeridian(lineString);
        }
        return lineString;
    }

    /**
     * Processes a MultiLineString according to RFC 7946 recommendations.
     * Lines that cross the antimeridian are cut, so the result may contain
     * more lines than the input.
     *
     * @param multiLineString The MultiLineString to process
     * @param config          The GeoJSON configuration to use
     * @return The processed MultiLineString
     */
    public static MultiLineString processMultiLineString(MultiLineString multiLineString, GeoJsonConfig config) {
        if (!config.isCutAntimeridian()) {
            return multiLineString;
        }
        List<List<LngLatAlt>> processed = new ArrayList<>();
        for (List<LngLatAlt> line : multiLineString.getCoordinates()) {
            processed.addAll(AntimeridianUtils.cutLineAtAntimeridian(line));
        }
        multiLineString.setCoordinates(processed);
        return multiLineString;
    }

    private static void fixMultiPolygonOrientation(MultiPolygon multiPolygon) {
        List<List<List<LngLatAlt>>> fixed = new ArrayList<>();
        for (List<List<LngLatAlt>> rings : multiPolygon.getCoordinates()) {
            fixed.add(PolygonOrientationUtils.fixPolygonOrientation(rings));
        }
        multiPolygon.setCoordinates(fixed);
    }

    /**
     * Processes a Feature according to RFC 7946 recommendations.
     *
     * @param feature The Feature to process
     * @param config  The GeoJSON configuration to use
     * @return The processed Feature
     */
    public static Feature processFeature(Feature feature, GeoJsonConfig config) {
        GeoJsonObject geometry = feature.getGeometry();
        if (geometry != null) {
            feature.setGeometry(process(geometry, config));
        }
        return feature;
    }

    /**
     * Processes a FeatureCollection according to RFC 7946 recommendations.
     *
     * @param featureCollection The FeatureCollection to process
     * @param config            The GeoJSON configuration to use
     * @return The processed FeatureCollection
     */
    public static FeatureCollection processFeatureCollection(FeatureCollection featureCollection, GeoJsonConfig config) {
        for (Feature feature : featureCollection) {
            processFeature(feature, config);
        }
        return featureCollection;
    }

    /**
     * Processes a GeometryCollection according to RFC 7946 recommendations.
     *
     * @param geometryCollection The GeometryCollection to process
     * @param config             The GeoJSON configuration to use
     * @return The processed GeometryCollection
     */
    public static GeometryCollection processGeometryCollection(GeometryCollection geometryCollection, GeoJsonConfig config) {
        List<GeoJsonObject> processed = new ArrayList<>();
        for (GeoJsonObject geometry : geometryCollection.getGeometries()) {
            processed.add(process(geometry, config));
        }
        geometryCollection.setGeometries(processed);
        return geometryCollection;
    }

    /**
     * Processes a GeoJSON object according to RFC 7946 recommendations.
     * This includes:
     * - Cutting geometries that cross the antimeridian
     * - Fixing polygon ring orientation
     *
     * @param object The GeoJSON object to process
     * @param config The GeoJSON configuration to use
     * @return The processed GeoJSON object
     */
    public static GeoJsonObject process(GeoJsonObject object, GeoJsonConfig config) {
        if (object instanceof Polygon) {
            return processPolygon((Polygon) object, config);
        } else if (object instanceof MultiPolygon) {
            return processMultiPolygon((MultiPolygon) object, config);
        } else if (object instanceof LineString) {
            return processLineString((LineString) object, config);
        } else if (object instanceof MultiLineString) {
            return processMultiLineString((MultiLineString) object, config);
        } else if (object instanceof Feature) {
            return processFeature((Feature) object, config);
        } else if (object instanceof FeatureCollection) {
            return processFeatureCollection((FeatureCollection) object, config);
        } else if (object instanceof GeometryCollection) {
            return processGeometryCollection((GeometryCollection) object, config);
        }

        return object;
    }

    /**
     * Processes a GeoJSON object according to RFC 7946 recommendations using the global configuration.
     * This method is provided for backward compatibility.
     *
     * @param object The GeoJSON object to process
     * @return The processed GeoJSON object
     * @deprecated Use {@link #process(GeoJsonObject, GeoJsonConfig)} instead
     */
    @Deprecated
    public static GeoJsonObject process(GeoJsonObject object) {
        GeoJsonConfig config = new GeoJsonConfig();
        return process(object, config);
    }
}
