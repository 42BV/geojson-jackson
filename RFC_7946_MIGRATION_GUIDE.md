# Migration Guide: Moving to RFC 7946 Compliance

This guide helps you migrate your application from using the 2008 GeoJSON specification to the newer RFC 7946 standard.

## Overview of Changes in RFC 7946

RFC 7946 (published in 2016) made several significant changes to the GeoJSON format:

1. **Coordinate Reference System (CRS)**:
    - The 2008 specification allowed custom CRS definitions.
    - RFC 7946 mandates that all GeoJSON coordinates use WGS 84 (longitude, latitude in decimal degrees).
    - The `crs` member has been removed from the specification.

2. **Polygon Ring Orientation**:
    - RFC 7946 requires that polygon rings follow the right-hand rule:
        - Exterior rings must be counterclockwise.
        - Interior rings (holes) must be clockwise.

3. **Antimeridian Cutting**:
    - Geometries that cross the antimeridian (180° longitude) should be cut into multiple parts.

4. **Media Type**:
    - The media type for GeoJSON is now "application/geo+json" (previously often used as "application/json").

## Migration Steps

### Step 1: Enable RFC 7946 Compliance

Use the `GeoJsonMapper` with RFC 7946 mode enabled:

```java
// Using the GeoJsonMapper with RFC 7946 mode enabled
GeoJsonMapper mapper = new GeoJsonMapper(true);
```

Or create a custom configuration:

```java
// Create an RFC 7946 compliant configuration
GeoJsonConfig config = GeoJsonConfig.rfc7946();

// Create a mapper with this configuration
GeoJsonMapper mapper = new GeoJsonMapper(config);
```

### Step 2: Remove Custom CRS Definitions

If your application uses custom CRS definitions, you'll need to:

1. Convert coordinates to WGS 84 before creating GeoJSON objects.
2. Remove any code that sets the `crs` property on GeoJSON objects.

Example:

```java
// Before:
Point point = new Point(100, 0);
Crs crs = new Crs();
crs.getProperties().put("name","EPSG:3857");
point.setCrs(crs);

// After:
// Convert coordinates to WGS 84 first, then:
Point point = new Point(longitude, latitude);
// No CRS setting
```

### Step 3: Fix Polygon Ring Orientation

When RFC 7946 compliance is enabled, polygon rings must follow the right-hand rule. The library will validate this automatically, but you may need to fix your
existing code:

```java
// Create a polygon with counterclockwise exterior ring (valid)
Polygon polygon = new Polygon(
                new LngLatAlt(0, 0),
                new LngLatAlt(1, 0),
                new LngLatAlt(1, 1),
                new LngLatAlt(0, 1),
                new LngLatAlt(0, 0)
        );
```

If you need to fix ring orientation programmatically, you can use the `PolygonOrientationUtils` utility:

```java
List<LngLatAlt> ring = polygon.getExteriorRing();
if(!PolygonOrientationUtils.isCounterClockwise(ring)){
        PolygonOrientationUtils.reverseRing(ring);
}
```

Alternatively, you can configure the library to automatically fix polygon orientation:

```java
// Create a configuration that auto-fixes polygon orientation
GeoJsonConfig config = GeoJsonConfig.rfc7946()
                .setValidatePolygonOrientation(true)
                .setAutoFixPolygonOrientation(true);

// Apply the configuration to your GeoJSON objects
Polygon polygon = new Polygon();
polygon.setConfig(config);
```

### Step 4: Handle Antimeridian Crossing

For geometries that cross the antimeridian (180° longitude), use the `GeoJsonMapper`:

```java
// Create a configuration with antimeridian cutting enabled
GeoJsonConfig config = GeoJsonConfig.rfc7946()
                .setCutAntimeridian(true); // Already enabled by default in rfc7946 mode

// Process a GeoJSON object using GeoJsonMapper with this configuration
GeoJsonMapper mapper = new GeoJsonMapper(config);
GeoJsonObject processed = mapper.process(geoJsonObject);
```

### Step 5: Update Media Type

If your application specifies media types for HTTP requests or responses, update to the new media type:

```
Content-Type: application/geo+json
```

## Backward Compatibility

This library maintains backward compatibility with the 2008 GeoJSON specification by default. RFC 7946 compliance is opt-in, so your existing code should
continue to work without changes.

If you need to support both standards, you can create separate mappers:

```java
// For 2008 GeoJSON specification
GeoJsonMapper legacyMapper = new GeoJsonMapper(false);

// For RFC 7946
GeoJsonMapper rfc7946Mapper = new GeoJsonMapper(true);
```

## Common Issues

### CRS Warnings

When using RFC 7946 compliance mode, you'll see warnings if you use the deprecated `crs` property. To disable these warnings:

```java
// Create a configuration with CRS warnings disabled
GeoJsonConfig config = GeoJsonConfig
                .rfc7946()
                .setWarnOnCrsUse(false);

// Apply the configuration to your GeoJSON objects
Point point = new Point(100, 0);
point.setConfig(config);
```

### Polygon Validation Errors

If you get `IllegalArgumentException` errors about polygon ring orientation, you need to fix the orientation of your rings. See Step 3 above.

### Multiple Configurations

One of the advantages of the new configuration system is that you can have multiple configurations active at the same time. This is especially useful in
multi-threaded environments:

```java
// Create mappers with different configurations
GeoJsonConfig legacyConfig = GeoJsonConfig.legacy();
GeoJsonConfig rfc7946Config = GeoJsonConfig.rfc7946();

GeoJsonMapper legacyMapper = new GeoJsonMapper(legacyConfig);
GeoJsonMapper rfc7946Mapper = new GeoJsonMapper(rfc7946Config);

// Use the appropriate mapper for each task
GeoJsonObject legacyObject = legacyMapper.readValue(inputStream, GeoJsonObject.class);
GeoJsonObject rfc7946Object = rfc7946Mapper.readValue(inputStream, GeoJsonObject.class);

// You can also apply configurations directly to GeoJSON objects
Polygon legacyPolygon = new Polygon();
legacyPolygon.setConfig(legacyConfig);

Polygon rfc7946Polygon = new Polygon();
rfc7946Polygon.setConfig(rfc7946Config);
```
