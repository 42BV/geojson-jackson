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
            PolygonOrientationUtils.fixPolygonOrientation(polygon.getCoordinates());
        }

        // Cut at antimeridian if needed
        if (config.isCutAntimeridian()) {
            return AntimeridianUtils.cutPolygonAtAntimeridian(polygon);
        }

        return polygon;
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
        } else if (object instanceof LineString) {
            return processLineString((LineString) object, config);
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
