package com.graphbenchmark.common.samples;

import java.util.Objects;

public class Property {
	final public String label; // Node label or Edge label!
	final public String name;
	final public String type;  // .getClass().getName() -> class.forName()

	public Property(String [] items) {
		this.label = items[0];
		this.name = items[1];

		// Temporary fix: issue #21
		if (name.equals("uid"))
			this.type = Long.class.getName();
		else
			this.type = items[2];
	}

	public Property(String label, String name, String type) {
		this.label = label;
		this.name = name;

		// Temporary fix: issue #21
		if (name.equals("uid"))
			this.type = Long.class.getName();
		else
			this.type = type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Property property = (Property) o;
		return label.equals(property.label) &&
				name.equals(property.name) &&
				type.equals(property.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(label, name, type);
	}

	@Override
	public String toString() {
		return "Property{" +
				"label='" + label + '\'' +
				", name='" + name + '\'' +
				", type='" + type + '\'' +
				'}';
	}

	public String toCsv() {
		return label + ',' + name + ',' + type;
	}

	public static Property fromCsv(String line) {
		return new Property(line.split(",", 3));
	}
}
