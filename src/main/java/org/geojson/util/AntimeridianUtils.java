package org.geojson.util;

import java.util.ArrayList;
import java.util.List;

import org.geojson.GeoJsonObject;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.MultiLineString;
import org.geojson.MultiPolygon;
import org.geojson.Polygon;

/**
 * Utility class for handling geometries that cross the antimeridian.
 * Contains methods for detecting and handling geometries that cross the antimeridian according to RFC 7946.
 */
public class AntimeridianUtils {

    private static final double ANTIMERIDIAN = 180.0;

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
    public static double normalizeLongitude(double longitude) {
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
        double fraction = crossingFraction(p1, p2);
        return p1.getLatitude() + fraction * (p2.getLatitude() - p1.getLatitude());
    }

    /**
     * Calculates the fraction along the segment from p1 to p2 (crossing the antimeridian)
     * at which the antimeridian is reached.
     *
     * @param p1 The first point
     * @param p2 The second point
     * @return The fraction in the range [0, 1]
     */
    private static double crossingFraction(LngLatAlt p1, LngLatAlt p2) {
        double lon1 = normalizeLongitude(p1.getLongitude());
        double lon2 = normalizeLongitude(p2.getLongitude());

        // Unwrap the second longitude so the segment is continuous in longitude
        // and passes through the antimeridian within the [lon1, lon2] interval.
        double crossingLon;
        if (lon1 > lon2) {
            // Eastward crossing (positive to negative side): shift lon2 up past 180
            lon2 += 360;
            crossingLon = ANTIMERIDIAN;
        } else {
            // Westward crossing (negative to positive side): shift lon2 down past -180
            lon2 -= 360;
            crossingLon = -ANTIMERIDIAN;
        }

        if (lon1 == lon2) {
            return 0.5; // Degenerate segment (e.g. 180 to -180), use midpoint
        }

        double fraction = (crossingLon - lon1) / (lon2 - lon1);
        return Math.max(0, Math.min(1, fraction));
    }

    /**
     * Creates the intersection point of a segment with the antimeridian, at the given
     * longitude (180 for the east side, -180 for the west side). Latitude (and altitude,
     * when both points have one) is interpolated linearly along the segment.
     *
     * @param p1        The first point of the segment
     * @param p2        The second point of the segment
     * @param longitude The longitude to place the intersection point at (180 or -180)
     * @return The intersection point
     */
    private static LngLatAlt intersectionPoint(LngLatAlt p1, LngLatAlt p2, double longitude) {
        double fraction = crossingFraction(p1, p2);
        double latitude = p1.getLatitude() + fraction * (p2.getLatitude() - p1.getLatitude());
        if (p1.hasAltitude() && p2.hasAltitude()) {
            double altitude = p1.getAltitude() + fraction * (p2.getAltitude() - p1.getAltitude());
            return new LngLatAlt(longitude, latitude, altitude);
        }
        return new LngLatAlt(longitude, latitude);
    }

    /**
     * Gets the longitude of the antimeridian crossing.
     *
     * @param lon1 The first longitude
     * @param lon2 The second longitude
     * @return The longitude of the antimeridian crossing
     */
    public static double getAntimeridianLon(double lon1, double lon2) {
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
        List<List<LngLatAlt>> segments = cutLineAtAntimeridian(lineString.getCoordinates());

        if (segments.size() <= 1) {
            return lineString;
        }
        MultiLineString multiLineString = new MultiLineString();
        for (List<LngLatAlt> segment : segments) {
            multiLineString.add(segment);
        }
        return multiLineString;
    }

    /**
     * Cuts a line (a list of points) at the antimeridian.
     *
     * @param coordinates The points of the line
     * @return The segments of the line after cutting; a single segment if the line does not
     * cross the antimeridian
     */
    public static List<List<LngLatAlt>> cutLineAtAntimeridian(List<LngLatAlt> coordinates) {
        List<List<LngLatAlt>> segments = new ArrayList<>();
        if (coordinates.size() < 2) {
            segments.add(coordinates);
            return segments;
        }

        List<LngLatAlt> currentSegment = new ArrayList<>();
        currentSegment.add(coordinates.get(0));

        for (int i = 1; i < coordinates.size(); i++) {
            LngLatAlt p1 = coordinates.get(i - 1);
            LngLatAlt p2 = coordinates.get(i);

            if (crossesAntimeridian(p1, p2)) {
                // End the current segment at the antimeridian, on the side p1 is on
                double side = normalizeLongitude(p1.getLongitude()) >= 0 ? ANTIMERIDIAN : -ANTIMERIDIAN;
                currentSegment.add(intersectionPoint(p1, p2, side));
                segments.add(currentSegment);

                // Start a new segment from the other side of the antimeridian
                currentSegment = new ArrayList<>();
                currentSegment.add(intersectionPoint(p1, p2, -side));
            }

            currentSegment.add(p2);
        }

        segments.add(currentSegment);
        return segments;
    }

    /**
     * Checks if a polygon crosses the antimeridian.
     *
     * @param rings The list of rings to check
     * @return true if any ring crosses the antimeridian, false otherwise
     */
    public static boolean polygonCrossesAntimeridian(List<List<LngLatAlt>> rings) {
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
     * <p>
     * Note: rings that cross the antimeridian more than twice (multiple lobes on either side)
     * are merged into a single ring per side, which can produce a self-intersecting ring.
     *
     * @param ring The ring to cut
     * @return A list containing either one ring (if no cut was needed) or two rings (east and west sides)
     */
    public static List<List<LngLatAlt>> cutRingAtAntimeridian(List<LngLatAlt> ring) {
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
                // Add intersection points to both rings
                eastRing.add(intersectionPoint(p1, p2, ANTIMERIDIAN));
                westRing.add(intersectionPoint(p1, p2, -ANTIMERIDIAN));
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
            eastRing = PolygonOrientationUtils.ensureRingClosed(eastRing);
            result.add(eastRing);
        }

        if (!westRing.isEmpty()) {
            westRing = PolygonOrientationUtils.ensureRingClosed(westRing);
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
    public static double calculateAverageLongitude(List<LngLatAlt> ring) {
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
}
