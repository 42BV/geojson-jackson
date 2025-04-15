package org.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A custom ObjectMapper for GeoJSON processing.
 * Provides options for RFC 7946 compliance.
 */
public class GeoJsonMapper extends ObjectMapper {

    /**
     * The configuration for this mapper.
     */
    private GeoJsonConfig config;

    /**
     * Creates a new GeoJsonMapper with default settings (legacy mode).
     * By default, this uses the 2008 GeoJSON specification for backward compatibility.
     */
    public GeoJsonMapper() {
        super();
        this.config = GeoJsonConfig.legacy();
    }

    /**
     * Creates a new GeoJsonMapper with the specified configuration.
     *
     * @param config The configuration to use
     */
    public GeoJsonMapper(GeoJsonConfig config) {
        super();
        this.config = config;
    }

    /**
     * Creates a new GeoJsonMapper with RFC 7946 compliance enabled or disabled.
     *
     * @param rfc7946Compliance Whether to enable RFC 7946 compliance
     */
    public GeoJsonMapper(boolean rfc7946Compliance) {
        this(rfc7946Compliance, false);
    }

    /**
     * Creates a new GeoJsonMapper with RFC 7946 compliance enabled or disabled.
     *
     * @param rfc7946Compliance          Whether to enable RFC 7946 compliance
     * @param validatePolygonOrientation Whether to validate polygon orientation
     */
    public GeoJsonMapper(boolean rfc7946Compliance, boolean validatePolygonOrientation) {
        super();
        if (rfc7946Compliance) {
            this.config = GeoJsonConfig.rfc7946().setValidatePolygonOrientation(validatePolygonOrientation);
            return;
        }
        this.config = GeoJsonConfig.legacy();
    }

    /**
     * Gets the configuration for this mapper.
     *
     * @return The configuration
     */
    public GeoJsonConfig getConfig() {
        return config;
    }

    /**
     * Sets the configuration for this mapper.
     *
     * @param config The configuration to set
     * @return This mapper for method chaining
     */
    public GeoJsonMapper setConfig(GeoJsonConfig config) {
        this.config = config;
        return this;
    }

    /**
     * Process a GeoJSON object according to RFC 7946 recommendations.
     * This includes:
     * - Cutting geometries that cross the antimeridian
     * - Fixing polygon ring orientation
     *
     * @param object The GeoJSON object to process
     * @return The processed GeoJSON object
     */
    public GeoJsonObject process(GeoJsonObject object) {
        return GeoJsonProcessor.process(object, config);
    }
}
