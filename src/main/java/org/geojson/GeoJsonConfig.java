package org.geojson;

/**
 * Configuration for GeoJSON processing.
 * Controls compliance with different GeoJSON specifications and processing behavior.
 */
public class GeoJsonConfig {

    /**
     * Whether to validate polygon ring orientation according to the right-hand rule.
     * RFC 7946 requires exterior rings to be counterclockwise and interior rings to be clockwise.
     */
    private boolean validatePolygonOrientation = false;

    /**
     * Whether to warn when CRS is used in RFC 7946 mode.
     * RFC 7946 removed support for custom coordinate reference systems.
     */
    private boolean warnOnCrsUse = true;

    /**
     * Whether to automatically cut geometries that cross the antimeridian.
     * RFC 7946 recommends cutting geometries that cross the antimeridian into multiple parts.
     */
    private boolean cutAntimeridian = false;

    /**
     * Whether to automatically fix polygon ring orientation.
     * If true, rings will be reversed if they don't follow the right-hand rule.
     * If false, an exception will be thrown for invalid orientation when validation is enabled.
     */
    private boolean autoFixPolygonOrientation = false;

    /**
     * Creates a new GeoJsonConfig with default settings (legacy mode).
     */
    public GeoJsonConfig() {
    }

    /**
     * Creates a new GeoJsonConfig with RFC 7946 compliance settings.
     *
     * @param rfc7946Compliance Whether to enable RFC 7946 compliance
     */
    public GeoJsonConfig(boolean rfc7946Compliance) {
        if (rfc7946Compliance) {
            this.setValidatePolygonOrientation(true);
            this.autoFixPolygonOrientation = true;
            this.cutAntimeridian = true;
        }
    }

    /**
     * Creates a new RFC 7946 compliant configuration.
     *
     * @return A new configuration with RFC 7946 compliance enabled
     */
    public static GeoJsonConfig rfc7946() {
        return new GeoJsonConfig(true);
    }

    /**
     * Creates a new legacy mode (2008 GeoJSON specification) configuration.
     *
     * @return A new configuration with legacy mode enabled
     */
    public static GeoJsonConfig legacy() {
        return new GeoJsonConfig(false);
    }

    public boolean isValidatePolygonOrientation() {
        return validatePolygonOrientation;
    }

    public GeoJsonConfig setValidatePolygonOrientation(boolean validatePolygonOrientation) {
        this.validatePolygonOrientation = validatePolygonOrientation;
        return this;
    }

    public boolean isWarnOnCrsUse() {
        return warnOnCrsUse;
    }

    public GeoJsonConfig setWarnOnCrsUse(boolean warnOnCrsUse) {
        this.warnOnCrsUse = warnOnCrsUse;
        return this;
    }

    public boolean isCutAntimeridian() {
        return cutAntimeridian;
    }

    public GeoJsonConfig setCutAntimeridian(boolean cutAntimeridian) {
        this.cutAntimeridian = cutAntimeridian;
        return this;
    }

    public boolean isAutoFixPolygonOrientation() {
        return autoFixPolygonOrientation;
    }

    public GeoJsonConfig setAutoFixPolygonOrientation(boolean autoFixPolygonOrientation) {
        this.autoFixPolygonOrientation = autoFixPolygonOrientation;
        return this;
    }
}
