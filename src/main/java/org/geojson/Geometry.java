package org.geojson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class Geometry<T> extends GeoJsonObject {

	protected List<T> coordinates = new ArrayList<T>();

	public Geometry() {
	}

	public Geometry(T... elements) {
        Collections.addAll(coordinates, elements);
	}

	public Geometry<T> add(T elements) {
		coordinates.add(elements);
		return this;
	}

	public List<T> getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(List<T> coordinates) {
		this.coordinates = coordinates;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Geometry)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		Geometry geometry = (Geometry)o;
		return !(!Objects.equals(coordinates, geometry.coordinates));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (coordinates != null ? coordinates.hashCode() : 0);
		return result;
	}

	/**
	 * Helper method for subclasses to create a consistent toString() implementation.
	 *
	 * @param className The name of the class
	 * @return A string representation of the object
	 */
	protected String buildToString(String className) {
		return className + "{} " + "Geometry{" + "coordinates=" + coordinates + "} " + super.toString();
	}

	@Override
	public String toString() {
		return "Geometry{" + "coordinates=" + coordinates + "} " + super.toString();
	}
}
