package org.geojson;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A Polygon geometry.
 * <p>
 * According to RFC 7946, a Polygon consists of a linear ring (the exterior ring),
 * and zero or more linear rings (the interior rings or holes).
 * <p>
 * The exterior ring must be counterclockwise, and the interior rings must be clockwise
 * when RFC 7946 compliance is enabled.
 */
public class Polygon extends Geometry<List<LngLatAlt>> {

    public Polygon() {
    }

    public Polygon(List<LngLatAlt> polygon) {
        add(polygon);
    }

    public Polygon(LngLatAlt... polygon) {
        add(Arrays.asList(polygon));
    }

    @Override
    public Geometry<List<LngLatAlt>> add(List<LngLatAlt> elements) {
        if (elements == null || elements.isEmpty()) {
            return this;
        }

        if (getConfig().isValidatePolygonOrientation()) {
            // Check if the ring has at least 4 points
            if (elements.size() < 4) {
                throw new IllegalArgumentException("Ring must have at least 4 points (3 unique points + closure)");
            }

            // Check if the ring is closed (first and last points are the same)
            LngLatAlt first = elements.get(0);
            LngLatAlt last = elements.get(elements.size() - 1);
            if (first.getLongitude() != last.getLongitude() || first.getLatitude() != last.getLatitude()) {
                throw new IllegalArgumentException("Ring must be closed (first and last points must be the same)");
            }
        }

        super.add(elements);
        processPolygon();
        return this;
    }

    @JsonIgnore
    public List<LngLatAlt> getExteriorRing() {
        assertExteriorRing();
        return coordinates.get(0);
    }

    /**
     * Set the exterior ring of the polygon.
     *
     * @param points The points of the exterior ring
     */
    public void setExteriorRing(List<LngLatAlt> points) {
        if (coordinates.isEmpty()) {
            coordinates.add(0, points);
        } else {
            coordinates.set(0, points);
        }

        // Process according to RFC 7946 if enabled
        processPolygon();
    }

    @JsonIgnore
    public List<List<LngLatAlt>> getInteriorRings() {
        assertExteriorRing();
        return coordinates.subList(1, coordinates.size());
    }

    public List<LngLatAlt> getInteriorRing(int index) {
        assertExteriorRing();
        return coordinates.get(1 + index);
    }

    public void addInteriorRing(List<LngLatAlt> points) {
        assertExteriorRing();
        coordinates.add(points);
        processPolygon();
    }

    public void addInteriorRing(LngLatAlt... points) {
        assertExteriorRing();
        coordinates.add(Arrays.asList(points));
        processPolygon();
    }

    /**
     * Process the polygon according to RFC 7946 requirements if enabled.
     * This includes validating or fixing ring orientation.
     */
    private void processPolygon() {
        if (coordinates.isEmpty()) {
            return;
        }

        if (getConfig().isAutoFixPolygonOrientation()) {
            coordinates = GeoJsonUtils.fixPolygonOrientation(coordinates);
        }

        if (getConfig().isValidatePolygonOrientation()) {
            GeoJsonUtils.validatePolygonOrientation(coordinates);
        }
    }

    private void assertExteriorRing() {
        if (coordinates.isEmpty())
            throw new RuntimeException("No exterior ring defined");
    }

    @Override
    public <T> T accept(GeoJsonObjectVisitor<T> geoJsonObjectVisitor) {
        return geoJsonObjectVisitor.visit(this);
    }

    @Override
    public String toString() {
        return buildToString("Polygon");
    }
}
