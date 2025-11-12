package org.geojson.util;

import java.util.ArrayList;
import java.util.List;

import org.geojson.LngLatAlt;

/**
 * Utility class for handling polygon ring orientation.
 * Contains methods for checking and fixing polygon ring orientation according to RFC 7946.
 */
public class PolygonOrientationUtils {

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
    public static void validateRingClosed(List<LngLatAlt> ring, String ringName) {
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
}
