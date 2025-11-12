package org.geojson.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Utility class for loading test resources.
 */
public class TestResourceLoader {

    /**
     * Loads a JSON resource file from the classpath.
     *
     * @param resourcePath The path to the resource, relative to the classpath
     * @return The content of the resource as a string
     * @throws IOException If the resource cannot be read
     */
    public static String loadJson(String resourcePath) throws IOException {
        try (InputStream inputStream = TestResourceLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }

            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next().trim();
            }
        }
    }
}
