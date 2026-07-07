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
     * Each ring is cut at the antimeridian and the resulting pieces are stitched into
     * simple rings. A polygon crossing the antimeridian more than twice produces more
     * than two polygons. Cut and uncut holes are assigned to the exterior piece that
     * contains them.
     * <p>
     * Limitation: rings that cross the antimeridian an odd number of times enclose a pole
     * and cannot be cut without pole handling; such polygons are returned unchanged.
     *
     * @param polygon The Polygon to cut
     * @return A MultiPolygon if the Polygon crosses the antimeridian, otherwise the original Polygon
     */
    public static GeoJsonObject cutPolygonAtAntimeridian(Polygon polygon) {
        List<List<LngLatAlt>> rings = polygon.getCoordinates();
        if (rings.isEmpty()) {
            return polygon;
        }

        List<List<LngLatAlt>> exteriorPieces = cutRingAtAntimeridian(rings.get(0));
        if (exteriorPieces.size() == 1) {
            // The exterior ring wasn't cut, return the original polygon
            return polygon;
        }

        List<Polygon> pieces = new ArrayList<>();
        for (List<LngLatAlt> exteriorPiece : exteriorPieces) {
            pieces.add(new Polygon(exteriorPiece));
        }

        // Assign each (possibly cut) interior ring to the exterior piece containing it
        for (int i = 1; i < rings.size(); i++) {
            for (List<LngLatAlt> holePiece : cutRingAtAntimeridian(rings.get(i))) {
                pieces.get(findContainingPiece(holePiece, exteriorPieces)).addInteriorRing(holePiece);
            }
        }

        MultiPolygon multiPolygon = new MultiPolygon();
        for (Polygon piece : pieces) {
            multiPolygon.add(piece);
        }
        return multiPolygon;
    }

    /**
     * Cuts a ring at the antimeridian into one or more simple rings.
     * <p>
     * The ring is split into arcs at each antimeridian crossing. Arcs are stitched back
     * together with segments along the antimeridian, pairing each arc's end with the
     * correct start based on the ring's orientation, so that a ring crossing the
     * antimeridian multiple times produces one ring per lobe instead of a
     * self-intersecting ring. The traversal direction of the input is preserved.
     * <p>
     * Limitation: a ring that crosses the antimeridian an odd number of times encloses a
     * pole and cannot be cut this way; it is returned unchanged.
     *
     * @param ring The ring to cut
     * @return A list containing the original ring (if no cut was possible or needed) or the cut rings
     */
    public static List<List<LngLatAlt>> cutRingAtAntimeridian(List<LngLatAlt> ring) {
        List<List<LngLatAlt>> result = new ArrayList<>();
        if (ring == null || ring.size() < 2) {
            result.add(ring);
            return result;
        }

        List<LngLatAlt> closedRing = PolygonOrientationUtils.ensureRingClosed(ring);
        int uniquePoints = closedRing.size() - 1;

        // Find the segments that cross the antimeridian; segment i runs from vertex i to vertex i + 1
        List<Integer> crossingSegments = new ArrayList<>();
        for (int i = 0; i < uniquePoints; i++) {
            if (crossesAntimeridian(closedRing.get(i), closedRing.get((i + 1) % uniquePoints))) {
                crossingSegments.add(i);
            }
        }

        if (crossingSegments.isEmpty() || crossingSegments.size() % 2 != 0) {
            // No crossings, or an odd number of crossings (ring encloses a pole): return uncut
            result.add(ring);
            return result;
        }

        boolean ccw = isCounterClockwiseUnwrapped(closedRing);
        List<Arc> arcs = buildArcs(closedRing, uniquePoints, crossingSegments);
        return stitchArcs(arcs, ccw);
    }

    /**
     * A piece of a ring between two antimeridian crossings. The points start with the
     * entry intersection on the antimeridian, followed by the original vertices, and end
     * with the exit intersection on the antimeridian.
     */
    private static class Arc {

        final List<LngLatAlt> points = new ArrayList<>();
        double entryLon;
        double entryLat;
        double exitLon;
        double exitLat;
    }

    /**
     * Splits a closed ring into arcs at its antimeridian crossings, walking the ring in
     * its original direction starting just after the first crossing.
     */
    private static List<Arc> buildArcs(List<LngLatAlt> ring, int uniquePoints, List<Integer> crossingSegments) {
        List<Arc> arcs = new ArrayList<>();
        int firstCrossing = crossingSegments.get(0);
        boolean[] crossesAtSegment = new boolean[uniquePoints];
        for (int segment : crossingSegments) {
            crossesAtSegment[segment] = true;
        }

        Arc current = startArc(ring.get(firstCrossing), ring.get((firstCrossing + 1) % uniquePoints));
        for (int k = 1; k <= uniquePoints; k++) {
            int index = (firstCrossing + k) % uniquePoints;
            current.points.add(normalizedPoint(ring.get(index)));

            if (crossesAtSegment[index]) {
                LngLatAlt p1 = ring.get(index);
                LngLatAlt p2 = ring.get((index + 1) % uniquePoints);
                // The exit is on the antimeridian side the segment leaves from
                double exitLon = normalizeLongitude(p1.getLongitude()) >= 0 ? ANTIMERIDIAN : -ANTIMERIDIAN;
                LngLatAlt exitPoint = intersectionPoint(p1, p2, exitLon);
                current.points.add(exitPoint);
                current.exitLon = exitLon;
                current.exitLat = exitPoint.getLatitude();
                arcs.add(current);

                if (k < uniquePoints) {
                    current = startArc(p1, p2);
                }
            }
        }
        return arcs;
    }

    /**
     * Starts a new arc at the entry intersection of the given crossing segment.
     */
    private static Arc startArc(LngLatAlt p1, LngLatAlt p2) {
        // The entry is on the opposite antimeridian side from the one the segment leaves
        double entryLon = normalizeLongitude(p1.getLongitude()) >= 0 ? -ANTIMERIDIAN : ANTIMERIDIAN;
        Arc arc = new Arc();
        LngLatAlt entryPoint = intersectionPoint(p1, p2, entryLon);
        arc.points.add(entryPoint);
        arc.entryLon = entryLon;
        arc.entryLat = entryPoint.getLatitude();
        return arc;
    }

    /**
     * Stitches arcs into closed rings. Each arc's exit connects along the antimeridian to
     * the nearest arc entry on the same side, searching north or south depending on the
     * ring orientation, so that connecting segments bound the polygon interior.
     */
    private static List<List<LngLatAlt>> stitchArcs(List<Arc> arcs, boolean ccw) {
        List<List<LngLatAlt>> rings = new ArrayList<>();
        boolean[] visited = new boolean[arcs.size()];

        for (int start = 0; start < arcs.size(); start++) {
            if (visited[start]) {
                continue;
            }
            visited[start] = true;

            List<LngLatAlt> ring = new ArrayList<>();
            int current = start;
            while (true) {
                ring.addAll(arcs.get(current).points);
                int next = findConnectedArc(arcs, visited, start, arcs.get(current), ccw);
                if (next == -1 || next == start) {
                    break;
                }
                visited[next] = true;
                current = next;
            }

            // Close the ring back to the entry point of the first arc
            LngLatAlt first = ring.get(0);
            ring.add(new LngLatAlt(first.getLongitude(), first.getLatitude()));
            rings.add(ring);
        }
        return rings;
    }

    /**
     * Finds the arc whose entry point connects to the given arc's exit point along the
     * antimeridian. For a counterclockwise ring, exits on the east side (180) connect
     * northward and exits on the west side (-180) connect southward; for a clockwise
     * ring the directions are mirrored.
     */
    private static int findConnectedArc(List<Arc> arcs, boolean[] visited, int start, Arc from, boolean ccw) {
        boolean searchNorth = (from.exitLon > 0) == ccw;
        int best = -1;
        double bestDelta = Double.MAX_VALUE;
        for (int i = 0; i < arcs.size(); i++) {
            if (visited[i] && i != start) {
                continue;
            }
            Arc candidate = arcs.get(i);
            if (candidate.entryLon != from.exitLon) {
                continue;
            }
            double delta = searchNorth ? candidate.entryLat - from.exitLat : from.exitLat - candidate.entryLat;
            if (delta >= 0 && delta < bestDelta) {
                bestDelta = delta;
                best = i;
            }
        }
        return best;
    }

    /**
     * Determines ring orientation using the shoelace formula on unwrapped longitudes, so
     * that rings crossing the antimeridian are measured as the continuous shape they
     * describe rather than as a shape spanning the world the other way around.
     */
    private static boolean isCounterClockwiseUnwrapped(List<LngLatAlt> ring) {
        double sum = 0;
        double previousLon = normalizeLongitude(ring.get(0).getLongitude());
        double previousUnwrapped = previousLon;
        for (int i = 1; i < ring.size(); i++) {
            double lon = normalizeLongitude(ring.get(i).getLongitude());
            double unwrapped = previousUnwrapped + normalizeLongitude(lon - previousLon);
            sum += (unwrapped - previousUnwrapped) * (ring.get(i).getLatitude() + ring.get(i - 1).getLatitude());
            previousLon = lon;
            previousUnwrapped = unwrapped;
        }
        return sum < 0;
    }

    /**
     * Returns the point with its longitude normalized to [-180, 180], reusing the
     * original point when it is already normalized.
     */
    private static LngLatAlt normalizedPoint(LngLatAlt point) {
        double lon = normalizeLongitude(point.getLongitude());
        if (lon == point.getLongitude()) {
            return point;
        }
        if (point.hasAltitude()) {
            return new LngLatAlt(lon, point.getLatitude(), point.getAltitude());
        }
        return new LngLatAlt(lon, point.getLatitude());
    }

    /**
     * Finds the index of the exterior piece containing the given hole piece, preferring a
     * point-in-ring test on a representative point and falling back to the closest
     * average longitude.
     */
    private static int findContainingPiece(List<LngLatAlt> holePiece, List<List<LngLatAlt>> exteriorPieces) {
        // Prefer a representative point that is not on the antimeridian itself
        LngLatAlt representative = holePiece.get(0);
        for (LngLatAlt point : holePiece) {
            if (Math.abs(normalizeLongitude(point.getLongitude())) != ANTIMERIDIAN) {
                representative = point;
                break;
            }
        }

        for (int i = 0; i < exteriorPieces.size(); i++) {
            if (isPointInRing(representative, exteriorPieces.get(i))) {
                return i;
            }
        }

        // Fallback for degenerate cases: the piece with the closest average longitude
        double holeLon = calculateAverageLongitude(holePiece);
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < exteriorPieces.size(); i++) {
            double distance = Math.abs(normalizeLongitude(calculateAverageLongitude(exteriorPieces.get(i)) - holeLon));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    /**
     * Tests whether a point lies inside a ring using even-odd ray casting on normalized
     * longitudes. The ring must not cross the antimeridian.
     */
    private static boolean isPointInRing(LngLatAlt point, List<LngLatAlt> ring) {
        double lon = normalizeLongitude(point.getLongitude());
        double lat = point.getLatitude();
        boolean inside = false;
        for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
            double loni = normalizeLongitude(ring.get(i).getLongitude());
            double lati = ring.get(i).getLatitude();
            double lonj = normalizeLongitude(ring.get(j).getLongitude());
            double latj = ring.get(j).getLatitude();
            if ((lati > lat) != (latj > lat) && lon < (lonj - loni) * (lat - lati) / (latj - lati) + loni) {
                inside = !inside;
            }
        }
        return inside;
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
