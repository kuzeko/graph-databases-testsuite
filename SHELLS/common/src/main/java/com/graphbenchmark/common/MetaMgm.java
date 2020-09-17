package com.graphbenchmark.common;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MetaMgm {
	public static List enumerateConfigurations(String meta) {
		return Sets.cartesianProduct(parseMeta(meta)).stream()
			.map(c -> c.stream().collect(Collectors.toMap(p -> p.getValue0(), p -> p.getValue1())))
			.collect(Collectors.toUnmodifiableList());
	}

	public static List<ImmutableSet<Pair>> parseMeta(String meta) {
		List<ImmutableSet<Pair>> domains = new ArrayList<>();

		for (String variable : meta.split(";")) {
			String[] decomposedVariable = variable.split("=", 2);
			String name = decomposedVariable[0].strip();
			String domSpec = decomposedVariable[1].strip();

			boolean isNumericRange = domSpec.charAt(0) == '[';


			ImmutableSet<Pair> values;
			String domStr = domSpec.substring(1, domSpec.length() - 1).strip();
			if (isNumericRange) { // Numeric Range
				String[] boundaries = domStr.split("-");
				values = IntStream.range(Integer.parseInt(boundaries[0].strip()), Integer.parseInt(boundaries[1].strip()))
						.mapToObj(v -> new Pair(name, v))
						.collect(ImmutableSet.toImmutableSet());
			} else { // Set of strings
				values = Arrays.stream(domStr.split(","))
						.map(val -> val.replace('\'', ' ').strip())
						.map(val -> new Pair(name, val))
						.collect(ImmutableSet.toImmutableSet());
			}
			domains.add(values);
		}
		return domains;
	}
}
