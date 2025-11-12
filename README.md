### Note: This is a continuation of opendatalab-de/geojson-jackson

GeoJson POJOs for Jackson
=========================

A small package of all GeoJson POJOs (Plain Old Java Objects) for serializing and
deserializing of objects via JSON Jackson Parser. This library supports both the 2008 GeoJSON specification and the newer RFC 7946 standard (published in 2016).

GeoJSON Standards Support
------------------------

This library supports two GeoJSON standards:

1. **2008 GeoJSON Specification** (default for backward compatibility)
   - Supports custom Coordinate Reference Systems (CRS)
   - No specific requirements for polygon ring orientation
   - No handling of geometries crossing the antimeridian

2. **RFC 7946 Specification** (modern standard, opt-in)
   - All coordinates use WGS 84 as the coordinate reference system
   - Polygon rings must follow the right-hand rule (exterior rings counterclockwise, interior rings clockwise)
   - Geometries crossing the antimeridian should be cut into multiple parts
   - Media type is "application/geo+json"

If migrating from 2008 to RFC 7946, refer to the [Migration Guide](RFC_7946_MIGRATION_GUIDE.md).

Usage
-----

### Basic Usage (2008 GeoJSON Specification)

If you know what kind of object you expect from a GeoJson file you can directly read it like this:

```java
FeatureCollection featureCollection =
	new ObjectMapper().readValue(inputStream, FeatureCollection.class);
```

If you want to read any GeoJson file read the value as GeoJsonObject and then test for the contents via instanceOf:

```java
GeoJsonObject object = new ObjectMapper().readValue(inputStream, GeoJsonObject.class);
if (object instanceof Polygon) {
	...
} else if (object instanceof Feature) {
	...
}
```
and so on.

Or you can use the GeoJsonObjectVisitor to visit the right method:

```java
GeoJsonObject object = new ObjectMapper().readValue(inputStream, GeoJsonObject.class);
object.accept(visitor);
```

Writing Json is even easier. You just have to create the GeoJson objects and pass them to the Jackson ObjectMapper.

```java
FeatureCollection featureCollection = new FeatureCollection();
featureCollection.add(new Feature());

String json= new ObjectMapper().writeValueAsString(featureCollection);
```

### RFC 7946 Compliance

To enable RFC 7946 compliance, use the `GeoJsonMapper` with RFC 7946 mode enabled:

```java
// Create a mapper with RFC 7946 compliance enabled
GeoJsonMapper mapper = new GeoJsonMapper(true);

// Use it like a regular ObjectMapper
FeatureCollection featureCollection = mapper.readValue(inputStream, FeatureCollection.class);
String json = mapper.writeValueAsString(featureCollection);

// Process a GeoJSON object according to RFC 7946 recommendations
GeoJsonObject processed = mapper.process(geoJsonObject);
```

You can also create a mapper with a specific configuration:

```java
// Create a custom configuration based on RFC 7946
GeoJsonConfig config = GeoJsonConfig.rfc7946()
                .setValidatePolygonOrientation(true)
                .setAutoFixPolygonOrientation(true);

// Create a mapper with the custom configuration
GeoJsonMapper mapper = new GeoJsonMapper(config);

// Use it like a regular ObjectMapper
FeatureCollection featureCollection = mapper.readValue(inputStream, FeatureCollection.class);
String json = mapper.writeValueAsString(featureCollection);
```

### Customizing RFC 7946 Behavior

You can customize the RFC 7946 compliance behavior using the `GeoJsonConfig` class:

```java
// Create a new RFC 7946 compliant configuration
GeoJsonConfig config = GeoJsonConfig.rfc7946();

// Customize the configuration
config.setValidatePolygonOrientation(true)
// Automatically fix polygon orientation instead of throwing exceptions
.setAutoFixPolygonOrientation(true)
// Enable antimeridian cutting (already enabled by default in rfc7946 mode)
.setCutAntimeridian(true)
// Disable warnings when CRS is used
.setWarnOnCrsUse(false);

// Apply the configuration to a GeoJSON object
Polygon polygon = new Polygon();
polygon.setConfig(config);
```

You can also use the factory methods to create common configurations:

```java
// Create an RFC 7946 compliant configuration
GeoJsonConfig rfc7946Config = GeoJsonConfig.rfc7946();

// Create a legacy mode (2008 GeoJSON specification) configuration
GeoJsonConfig legacyConfig = GeoJsonConfig.legacy();
```

### Polygon Ring Orientation

RFC 7946 requires that polygon rings follow the right-hand rule (exterior rings counterclockwise, interior rings clockwise). This is validated automatically
when RFC 7946 compliance is enabled.

You can choose to either validate orientation (which throws exceptions for invalid rings) or automatically fix orientation:

```java
// Create a configuration that validates orientation (throws exceptions for invalid rings)
GeoJsonConfig config = GeoJsonConfig.rfc7946()
                .setValidatePolygonOrientation(true)
                .setAutoFixPolygonOrientation(false);

// Apply the configuration to a polygon
Polygon polygon = new Polygon();
polygon.setConfig(config);

// Or create a configuration that auto-fixes orientation (silently fixes invalid rings)
GeoJsonConfig autoFixConfig = GeoJsonConfig.rfc7946()
        .setValidatePolygonOrientation(true)
        .setAutoFixPolygonOrientation(true);

// Apply the configuration to another polygon
Polygon anotherPolygon = new Polygon();
anotherPolygon.setConfig(autoFixConfig);
```

### Antimeridian Cutting

RFC 7946 recommends cutting geometries that cross the antimeridian. You can enable this feature:

```java
// Create a configuration with antimeridian cutting enabled
GeoJsonConfig config = GeoJsonConfig.rfc7946()
                .setCutAntimeridian(true); // Already enabled by default in rfc7946 mode

// Process a GeoJSON object using GeoJsonMapper with this configuration
GeoJsonMapper mapper = new GeoJsonMapper(config);
GeoJsonObject processed = mapper.process(geoJsonObject);
```

Maven Central
-------------

Currently this fork is not hosted on maven central. Release can be downloaded as a jar file.