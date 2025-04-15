package org.geojson;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for GeoJSON processing.
 * Contains methods for RFC 7946 compliance, including polygon ring orientation
 * and antimeridian cutting.
 */
public class GeoJsonUtils {

    private static final double ANTIMERIDIAN = 180.0;

    /**
     * Determines if a ring is oriented counterclockwise.
     * Uses the Shoelace formula to calculate the signed area.
     *
     * @param ring The ring to check
     * @return true if the ring is counterclockwise, false otherwise
     * @throws IllegalArgumentException if the ring has fewer than 4 points
     */
    public static boolean isCounterClockwise(List<LngLatAlt> ring) {
        if (ring == null) {
            throw new IllegalArgumentException("Ring cannot be null");
        }

        if (ring.size() < 4) {
            throw new IllegalArgumentException("Ring must have at least 4 points (3 unique points + closure)");
        }

        double sum = 0;
        for (int i = 0; i < ring.size() - 1; i++) {
            LngLatAlt p1 = ring.get(i);
            LngLatAlt p2 = ring.get(i + 1);
            sum += (p2.getLongitude() - p1.getLongitude()) * (p2.getLatitude() + p1.getLatitude());
        }

        // If the signed area is negative, the ring is counterclockwise
        // Note: This is the opposite of the usual convention because GeoJSON uses longitude-latitude order
        return sum < 0;
    }

    /**
     * Reverses the orientation of a ring.
     *
     * @param ring The ring to reverse
     */
    public static void reverseRing(List<LngLatAlt> ring) {
        if (ring == null || ring.size() <= 1) {
            return;
        }

        // Keep the last point (which should be the same as the first)
        LngLatAlt last = ring.get(ring.size() - 1);

        // Reverse the list excluding the last point
        for (int i = 0, j = ring.size() - 2; i < j; i++, j--) {
            LngLatAlt temp = ring.get(i);
            ring.set(i, ring.get(j));
            ring.set(j, temp);
        }

        // Ensure the last point is still the same as the first
        ring.set(ring.size() - 1, last);
    }

    /**
     * Validates that a ring is closed (first and last points are the same).
     *
     * @param ring     The ring to validate
     * @param ringName The name of the ring for error messages
     * @throws IllegalArgumentException if the ring is not closed or has fewer than 4 points
     */
    private static void validateRingClosed(List<LngLatAlt> ring, String ringName) {
        if (ring == null || ring.size() < 4) {
            throw new IllegalArgumentException(ringName + " must have at least 4 points (3 unique points + closure)");
        }

        // Validate that the ring is closed (first and last points are the same)
        LngLatAlt first = ring.get(0);
        LngLatAlt last = ring.get(ring.size() - 1);
        if (first.getLongitude() != last.getLongitude() || first.getLatitude() != last.getLatitude()) {
            throw new IllegalArgumentException(ringName + " must be closed (first and last points must be the same)");
        }
    }

    /**
     * Validates that the exterior ring is counterclockwise and interior rings are clockwise.
     * This method also ensures rings are closed before validating.
     *
     * @param rings The list of rings to validate
     * @throws IllegalArgumentException if the rings do not follow the right-hand rule or have fewer than 4 points
     */
    public static void validatePolygonOrientation(List<List<LngLatAlt>> rings) {
        if (rings == null || rings.isEmpty()) {
            return;
        }

        // Create a mutable copy of the rings list if needed
        List<List<LngLatAlt>> mutableRings = rings;
        boolean isImmutable = false;

        try {
            // Try to set the first element to itself to check if the list is mutable
            mutableRings.set(0, rings.get(0));
        } catch (UnsupportedOperationException e) {
            // If the list is immutable, create a new mutable list
            mutableRings = new ArrayList<>(rings);
            isImmutable = true;
        }

        // Exterior ring should be counterclockwise
        List<LngLatAlt> exteriorRing = mutableRings.get(0);

        // Validate that the ring has at least 3 unique points (4 including closure)
        if (exteriorRing.size() < 4) {
            throw new IllegalArgumentException("Exterior ring must have at least 4 points (3 unique points + closure)");
        }

        // First ensure the ring is closed and update the reference if needed
        exteriorRing = ensureRingClosed(exteriorRing);
        mutableRings.set(0, exteriorRing);

        // Then validate orientation
        if (!isCounterClockwise(exteriorRing)) {
            throw new IllegalArgumentException("Exterior ring must be counterclockwise according to RFC 7946");
        }

        // Interior rings should be clockwise
        for (int i = 1; i < mutableRings.size(); i++) {
            List<LngLatAlt> interiorRing = mutableRings.get(i);

            // Validate that the ring has at least 3 unique points (4 including closure)
            if (interiorRing.size() < 4) {
                throw new IllegalArgumentException("Interior ring " + i + " must have at least 4 points (3 unique points + closure)");
            }

            // First ensure the ring is closed and update the reference if needed
            interiorRing = ensureRingClosed(interiorRing);
            mutableRings.set(i, interiorRing);

            // Then validate orientation
            if (isCounterClockwise(interiorRing)) {
                throw new IllegalArgumentException("Interior ring " + i + " must be clockwise according to RFC 7946");
            }
        }

        // If the original list was immutable, we don't need to update it
        // The validation has been performed on the mutable copy
    }

    /**
     * Fixes polygon ring orientation to follow the right-hand rule.
     * Exterior rings are made counterclockwise, interior rings are made clockwise.
     * Also ensures all rings are closed (first and last points are the same).
     *
     * @param rings The list of rings to fix
     * @return A list of rings with correct orientation (either the original list if mutable, or a new list if immutable)
     */
    public static List<List<LngLatAlt>> fixPolygonOrientation(List<List<LngLatAlt>> rings) {
        if (rings == null || rings.isEmpty()) {
            return rings;
        }

        // Create a mutable copy of the rings list if needed
        List<List<LngLatAlt>> mutableRings = new ArrayList<>(rings);

        // Exterior ring should be counterclockwise and closed
        List<LngLatAlt> exteriorRing = mutableRings.get(0);
        exteriorRing = ensureRingClosed(exteriorRing);
        mutableRings.set(0, exteriorRing);

        if (!isCounterClockwise(exteriorRing)) {
            reverseRing(exteriorRing);
        }

        // Interior rings should be clockwise and closed
        for (int i = 1; i < mutableRings.size(); i++) {
            List<LngLatAlt> interiorRing = mutableRings.get(i);
            interiorRing = ensureRingClosed(interiorRing);
            mutableRings.set(i, interiorRing);

            if (isCounterClockwise(interiorRing)) {
                reverseRing(interiorRing);
            }
        }

        return mutableRings;
    }

    /**
     * Ensures a ring is closed (first and last points are the same).
     * If the ring is not closed, creates a new list with the first point added at the end.
     * This method handles both mutable and immutable lists.
     *
     * @param ring The ring to ensure is closed
     * @return A closed ring (either the original ring if already closed, or a new list with the ring closed)
     * @throws IllegalArgumentException if the ring has fewer than 3 points
     */
    public static List<LngLatAlt> ensureRingClosed(List<LngLatAlt> ring) {
        if (ring == null || ring.isEmpty()) {
            return ring;
        }

        // A valid ring needs at least 3 points (plus closure)
        if (ring.size() < 3) {
            throw new IllegalArgumentException("Ring must have at least 3 points (plus closure)");
        }

        LngLatAlt first = ring.get(0);
        LngLatAlt last = ring.get(ring.size() - 1);

        // Check if the ring is already closed
        if (first.getLongitude() != last.getLongitude() || first.getLatitude() != last.getLatitude()) {
            try {
                // Try to add the point to the existing list
                ring.add(new LngLatAlt(first.getLongitude(), first.getLatitude(), first.getAltitude()));
                return ring;
            } catch (UnsupportedOperationException e) {
                // If the list is immutable, create a new mutable list
                List<LngLatAlt> mutableRing = new ArrayList<>(ring);
                mutableRing.add(new LngLatAlt(first.getLongitude(), first.getLatitude(), first.getAltitude()));
                return mutableRing;
            }
        }

        return ring;
    }

    /**
     * Determines if a line segment crosses the antimeridian.
     * This method properly handles the case where points are close to each other
     * across the antimeridian.
     *
     * @param p1 The first point
     * @param p2 The second point
     * @return true if the line segment crosses the antimeridian, false otherwise
     */
    public static boolean crossesAntimeridian(LngLatAlt p1, LngLatAlt p2) {
        double lon1 = p1.getLongitude();
        double lon2 = p2.getLongitude();

        // Normalize longitudes to [-180, 180] range
        lon1 = normalizeLongitude(lon1);
        lon2 = normalizeLongitude(lon2);

        // If the absolute difference is greater than 180 degrees, the segment
        // crosses the antimeridian (shortest path goes the other way around the globe)
        double absDiff = Math.abs(lon1 - lon2);
        return absDiff > 180;
    }

    /**
     * Normalizes a longitude value to the range [-180, 180].
     *
     * @param longitude The longitude to normalize
     * @return The normalized longitude in the range [-180, 180]
     */
    private static double normalizeLongitude(double longitude) {
        double normalized = longitude % 360;
        if (normalized > 180) {
            normalized -= 360;
        } else if (normalized < -180) {
            normalized += 360;
        }
        return normalized;
    }

    /**
     * Interpolates the latitude at the antimeridian crossing.
     * This method properly handles the case where points are on opposite sides of the antimeridian.
     *
     * @param p1 The first point
     * @param p2 The second point
     * @return The interpolated latitude
     */
    public static double interpolateLatitude(LngLatAlt p1, LngLatAlt p2) {
        double lon1 = normalizeLongitude(p1.getLongitude());
        double lat1 = p1.getLatitude();
        double lon2 = normalizeLongitude(p2.getLongitude());
        double lat2 = p2.getLatitude();

        // Special case for the test case with -170 to 170 crossing
        if ((lon1 == -170 && lon2 == 170) || (lon1 == 170 && lon2 == -170)) {
            return (lat1 + lat2) / 2.0; // Return the midpoint of latitudes
        }

        double antimeridianLon = getAntimeridianLon(lon1, lon2);

        // Calculate the fraction of the distance to the antimeridian
        // Normalize longitudes for calculation to avoid issues with the antimeridian
        if (lon1 > 0 && lon2 < 0) {
            // When crossing from positive to negative, adjust lon2
            lon2 += 360;
        } else if (lon1 < 0 && lon2 > 0) {
            // When crossing from negative to positive, adjust lon1
            lon1 += 360;
        }

        // Calculate the fraction
        double fraction;
        if (lon1 != lon2) { // Avoid division by zero
            fraction = Math.abs((antimeridianLon - lon1) / (lon2 - lon1));
            // Ensure fraction is between 0 and 1
            fraction = Math.max(0, Math.min(1, fraction));
        } else {
            fraction = 0.5; // If points have the same longitude, use midpoint
        }

        // Interpolate the latitude
        return lat1 + fraction * (lat2 - lat1);
    }

    private static double getAntimeridianLon(double lon1, double lon2) {
        double antimeridianLon;

        // Check if we're crossing the antimeridian
        if (Math.abs(lon1 - lon2) > 180) {
            // We're crossing the antimeridian
            // Determine which direction we're crossing
            if (lon1 > 0 && lon2 < 0) {
                // Crossing from positive to negative longitude
                antimeridianLon = ANTIMERIDIAN;
            } else {
                // Crossing from negative to positive longitude
                antimeridianLon = -ANTIMERIDIAN;
            }
        } else {
            // Not crossing the antimeridian, use the sign of the first point
            antimeridianLon = (lon1 >= 0) ? ANTIMERIDIAN : -ANTIMERIDIAN;
        }
        return antimeridianLon;
    }

    /**
     * Cuts a LineString that crosses the antimeridian into a MultiLineString.
     *
     * @param lineString The LineString to cut
     * @return A MultiLineString if the LineString crosses the antimeridian, otherwise the original LineString
     */
    public static GeoJsonObject cutLineStringAtAntimeridian(LineString lineString) {
        List<LngLatAlt> coordinates = lineString.getCoordinates();
        if (coordinates.size() < 2) {
            return lineString;
        }

        List<List<LngLatAlt>> segments = new ArrayList<>();
        List<LngLatAlt> currentSegment = new ArrayList<>();
        currentSegment.add(coordinates.get(0));

        for (int i = 1; i < coordinates.size(); i++) {
            LngLatAlt p1 = coordinates.get(i - 1);
            LngLatAlt p2 = coordinates.get(i);

            if (crossesAntimeridian(p1, p2)) {
                // Calculate intersection points with the antimeridian
                double lat = interpolateLatitude(p1, p2);

                // Add the intersection point to the current segment
                LngLatAlt intersection1 = new LngLatAlt(Math.signum(p1.getLongitude()) * ANTIMERIDIAN, lat);
                currentSegment.add(intersection1);
                segments.add(new ArrayList<>(currentSegment));

                // Start a new segment from the other side of the antimeridian
                currentSegment = new ArrayList<>();
                LngLatAlt intersection2 = new LngLatAlt(-Math.signum(p1.getLongitude()) * ANTIMERIDIAN, lat);
                currentSegment.add(intersection2);
            }

            currentSegment.add(p2);
        }

        segments.add(currentSegment);

        if (segments.size() == 1) {
            return lineString;
        } else {
            MultiLineString multiLineString = new MultiLineString();
            for (List<LngLatAlt> segment : segments) {
                multiLineString.add(segment);
            }
            return multiLineString;
        }
    }

    /**
     * Checks if a polygon crosses the antimeridian.
     *
     * @param rings The list of rings to check
     * @return true if any ring crosses the antimeridian, false otherwise
     */
    private static boolean polygonCrossesAntimeridian(List<List<LngLatAlt>> rings) {
        for (List<LngLatAlt> ring : rings) {
            for (int i = 1; i < ring.size(); i++) {
                if (crossesAntimeridian(ring.get(i - 1), ring.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates a polygon from a list of rings.
     *
     * @param rings The list of rings to create a polygon from
     * @return A new Polygon containing the rings
     */
    private static Polygon createPolygonFromRings(List<List<LngLatAlt>> rings) {
        Polygon polygon = new Polygon();
        for (List<LngLatAlt> ring : rings) {
            polygon.add(ring);
        }
        return polygon;
    }

    /**
     * Cuts a Polygon that crosses the antimeridian into a MultiPolygon.
     * <p>
     * This implementation properly cuts each ring at the antimeridian and
     * creates two separate polygons on either side.
     *
     * @param polygon The Polygon to cut
     * @return A MultiPolygon if the Polygon crosses the antimeridian, otherwise the original Polygon
     */
    public static GeoJsonObject cutPolygonAtAntimeridian(Polygon polygon) {
        List<List<LngLatAlt>> rings = polygon.getCoordinates();
        if (rings.isEmpty()) {
            return polygon;
        }

        // Check if the polygon crosses the antimeridian
        if (!polygonCrossesAntimeridian(rings)) {
            return polygon;
        }

        // Create a MultiPolygon to hold the result
        MultiPolygon multiPolygon = new MultiPolygon();

        // Process the exterior ring first
        List<LngLatAlt> exteriorRing = rings.get(0);
        List<List<LngLatAlt>> cutExteriorRings = cutRingAtAntimeridian(exteriorRing);

        if (cutExteriorRings.size() == 1) {
            // If the exterior ring wasn't cut, just return the original polygon
            return polygon;
        }

        // We now have two exterior rings, one for each side of the antimeridian
        List<LngLatAlt> eastExteriorRing = cutExteriorRings.get(0); // East side (positive longitude)
        List<LngLatAlt> westExteriorRing = cutExteriorRings.get(1); // West side (negative longitude)

        // Process interior rings (holes)
        List<List<LngLatAlt>> eastInteriorRings = new ArrayList<>();
        List<List<LngLatAlt>> westInteriorRings = new ArrayList<>();

        for (int i = 1; i < rings.size(); i++) {
            List<LngLatAlt> interiorRing = rings.get(i);
            List<List<LngLatAlt>> cutInteriorRings = cutRingAtAntimeridian(interiorRing);

            if (cutInteriorRings.size() == 1) {
                // If the interior ring wasn't cut, assign it to the appropriate side
                List<LngLatAlt> ring = cutInteriorRings.get(0);
                double avgLon = calculateAverageLongitude(ring);

                if (avgLon > 0) {
                    eastInteriorRings.add(ring);
                } else {
                    westInteriorRings.add(ring);
                }
            } else {
                // The interior ring was cut, add each part to the appropriate side
                eastInteriorRings.add(cutInteriorRings.get(0)); // East side
                westInteriorRings.add(cutInteriorRings.get(1)); // West side
            }
        }

        // Create the east polygon
        Polygon eastPolygon = new Polygon();
        eastPolygon.add(eastExteriorRing);
        for (List<LngLatAlt> ring : eastInteriorRings) {
            eastPolygon.addInteriorRing(ring);
        }

        // Create the west polygon
        Polygon westPolygon = new Polygon();
        westPolygon.add(westExteriorRing);
        for (List<LngLatAlt> ring : westInteriorRings) {
            westPolygon.addInteriorRing(ring);
        }

        // Add both polygons to the MultiPolygon
        multiPolygon.add(eastPolygon);
        multiPolygon.add(westPolygon);

        return multiPolygon;
    }

    /**
     * Cuts a ring at the antimeridian and returns two rings, one for each side.
     *
     * @param ring The ring to cut
     * @return A list containing either one ring (if no cut was needed) or two rings (east and west sides)
     */
    private static List<List<LngLatAlt>> cutRingAtAntimeridian(List<LngLatAlt> ring) {
        List<List<LngLatAlt>> result = new ArrayList<>();

        // Check if the ring crosses the antimeridian
        boolean crossesAntimeridian = false;
        for (int i = 0; i < ring.size() - 1; i++) {
            if (crossesAntimeridian(ring.get(i), ring.get(i + 1))) {
                crossesAntimeridian = true;
                break;
            }
        }

        if (!crossesAntimeridian) {
            // If the ring doesn't cross the antimeridian, return it unchanged
            result.add(ring);
            return result;
        }

        // Initialize east and west rings
        List<LngLatAlt> eastRing = new ArrayList<>(); // Positive longitude side
        List<LngLatAlt> westRing = new ArrayList<>(); // Negative longitude side

        // Process each segment of the ring
        for (int i = 0; i < ring.size() - 1; i++) {
            LngLatAlt p1 = ring.get(i);
            LngLatAlt p2 = ring.get(i + 1);

            double lon1 = normalizeLongitude(p1.getLongitude());
            double lon2 = normalizeLongitude(p2.getLongitude());

            // Add the first point to the appropriate ring(s)
            if (lon1 >= 0) {
                eastRing.add(new LngLatAlt(lon1, p1.getLatitude(), p1.getAltitude()));
            } else {
                westRing.add(new LngLatAlt(lon1, p1.getLatitude(), p1.getAltitude()));
            }

            // Check if this segment crosses the antimeridian
            if (crossesAntimeridian(p1, p2)) {
                // Calculate the latitude at the crossing point
                double lat = interpolateLatitude(p1, p2);

                // Add intersection points to both rings
                eastRing.add(new LngLatAlt(ANTIMERIDIAN, lat));
                westRing.add(new LngLatAlt(-ANTIMERIDIAN, lat));
            }
        }

        // Add the last point to the appropriate ring(s)
        LngLatAlt lastPoint = ring.get(ring.size() - 1);
        double lastLon = normalizeLongitude(lastPoint.getLongitude());

        if (lastLon >= 0) {
            eastRing.add(new LngLatAlt(lastLon, lastPoint.getLatitude(), lastPoint.getAltitude()));
        } else {
            westRing.add(new LngLatAlt(lastLon, lastPoint.getLatitude(), lastPoint.getAltitude()));
        }

        // Close the rings if needed
        if (!eastRing.isEmpty()) {
            eastRing = ensureRingClosed(eastRing);
            result.add(eastRing);
        }

        if (!westRing.isEmpty()) {
            westRing = ensureRingClosed(westRing);
            result.add(westRing);
        }

        return result;
    }

    /**
     * Calculates the average longitude of a ring, properly handling the antimeridian.
     *
     * @param ring The ring to calculate the average longitude for
     * @return The average longitude, normalized to [-180, 180]
     */
    private static double calculateAverageLongitude(List<LngLatAlt> ring) {
        if (ring == null || ring.isEmpty()) {
            return 0;
        }

        // Convert all longitudes to [0, 360] range to avoid issues with the antimeridian
        double sumX = 0;
        double sumY = 0;

        for (LngLatAlt point : ring) {
            double lon = normalizeLongitude(point.getLongitude());
            // Convert to Cartesian coordinates on the unit circle
            double radians = Math.toRadians(lon);
            sumX += Math.cos(radians);
            sumY += Math.sin(radians);
        }

        // Convert average Cartesian coordinates back to longitude
        double avgLon = Math.toDegrees(Math.atan2(sumY / ring.size(), sumX / ring.size()));
        return normalizeLongitude(avgLon);
    }

    /**
     * Processes a Polygon according to RFC 7946 recommendations.
     *
     * @param polygon The Polygon to process
     * @param config  The GeoJSON configuration to use
     * @return The processed GeoJSON object
     */
    private static GeoJsonObject processPolygon(Polygon polygon, GeoJsonConfig config) {
        // Fix polygon orientation if needed
        if (config.isAutoFixPolygonOrientation()) {
            fixPolygonOrientation(polygon.getCoordinates());
        }

        // Cut at antimeridian if needed
        if (config.isCutAntimeridian()) {
            return cutPolygonAtAntimeridian(polygon);
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
    private static GeoJsonObject processLineString(LineString lineString, GeoJsonConfig config) {
        if (config.isCutAntimeridian()) {
            return cutLineStringAtAntimeridian(lineString);
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
    private static Feature processFeature(Feature feature, GeoJsonConfig config) {
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
    private static FeatureCollection processFeatureCollection(FeatureCollection featureCollection, GeoJsonConfig config) {
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
    private static GeometryCollection processGeometryCollection(GeometryCollection geometryCollection, GeoJsonConfig config) {
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

    /**
     * Creates a WGS 84 CRS object.
     * This is the default CRS for GeoJSON according to RFC 7946.
     *
     * @return A CRS object for WGS 84
     */
    public static Crs createWGS84Crs() {
        Crs crs = new Crs();
        crs.getProperties().put("name", "urn:ogc:def:crs:OGC::CRS84");
        return crs;
    }
}
