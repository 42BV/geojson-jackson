package org.geojson.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.geojson.LngLatAlt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for JSON comparison in tests.
 * Provides methods to compare JSON strings while ignoring formatting differences.
 */
public class JsonTestUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void assertLngLatAlt(double expectedLongitude, double expectedLatitude, double expectedAltitude,
            LngLatAlt point) {
        assertLngLatAlt(expectedLongitude, expectedLatitude, expectedAltitude, new double[0], point);
    }

    public static void assertLngLatAlt(double expectedLongitude, double expectedLatitude, double expectedAltitude,
            double[] expectedAdditionalElements, LngLatAlt point) {
        assertEquals(expectedLongitude, point.getLongitude(), 0.00001);
        assertEquals(expectedLatitude, point.getLatitude(), 0.00001);
        if (Double.isNaN(expectedAltitude)) {
            assertFalse(point.hasAltitude());
        } else {
            assertEquals(expectedAltitude, point.getAltitude(), 0.00001);
            assertArrayEquals(expectedAdditionalElements, point.getAdditionalElements(), 0.0);
        }
    }

    /**
     * Compares two JSON strings for equality, ignoring formatting differences.
     * The strings are parsed into JsonNode objects and then compared structurally.
     *
     * @param expected The expected JSON string
     * @param actual   The actual JSON string
     * @return true if the JSON structures are equal, false otherwise
     * @throws IOException If there is an error parsing the JSON
     */
    public static boolean jsonEquals(String expected, String actual) throws IOException {
        JsonNode expectedNode = MAPPER.readTree(expected);
        JsonNode actualNode = MAPPER.readTree(actual);
        return expectedNode.equals(actualNode);
    }

    /**
     * Asserts that two JSON strings are equal, ignoring formatting differences.
     * The strings are parsed into JsonNode objects and then compared structurally.
     * If they are not equal, an AssertionError is thrown with a detailed message.
     *
     * @param expected The expected JSON string
     * @param actual   The actual JSON string
     * @throws IOException    If there is an error parsing the JSON
     * @throws AssertionError If the JSON structures are not equal
     */
    public static void assertJsonEquals(String expected, String actual) throws IOException {
        JsonNode expectedNode = MAPPER.readTree(expected);
        JsonNode actualNode = MAPPER.readTree(actual);

        if (!expectedNode.equals(actualNode)) {
            throw new AssertionError("JSON structures are not equal.\nExpected: " +
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(expectedNode) +
                    "\nActual: " +
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(actualNode));
        }
    }
}
