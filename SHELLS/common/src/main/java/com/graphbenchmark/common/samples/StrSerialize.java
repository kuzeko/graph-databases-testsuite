package com.graphbenchmark.common.samples;

import com.graphbenchmark.common.GdbLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

public class StrSerialize {

	private static GdbLogger log = GdbLogger.getLogger();


	public static Object deserialize(final String type, final String value) {
		try {
			return getDeserializer(type).apply(value);
		} catch (ClassNotFoundException|NoSuchMethodException e) {
			e.printStackTrace();
			log.fatal("Cannot find suitable method for parsing %s", type);
		}
		return null; // will never happen.
	}

	public static Function<String, Object> getDeserializer(final String type) throws ClassNotFoundException, NoSuchMethodException {
		// Look for existence of "from" method first String then Object.
		Method from  = null;
		try {
			from = Class.forName(type).getDeclaredMethod("from", String.class);
		} catch (NoSuchMethodException ignore1) {
			// Then try with from(Object)
			try {
				from = Class.forName(type).getDeclaredMethod("from", Object.class);
			} catch (NoSuchMethodException ignore2) {
				// Let's try parse (like for janus)
				try {
					from = Class.forName(type).getDeclaredMethod("parse", String.class);
				} catch (NoSuchMethodException ignore3) {
					// Fine, we will try with the constructor.
				}
			}
		}

		// If we found a "from" method
		if (from != null) {
			final Method x = from;
			return (String value) -> {
				try {
					return x.invoke(null, value);
				} catch (IllegalAccessException | InvocationTargetException e) {
					e.printStackTrace();
					log.fatal("Parsing '%s' as %s via 'from'", value, type);
				}
				return null; // Will never happen
			};
		}



		// Try with the constructor or fail.
		Constructor nc = Class.forName(type).getDeclaredConstructor(String.class);
		return (String value) -> {
			try {
				return nc.newInstance(value);
			} catch (IllegalAccessException | InvocationTargetException | InstantiationException ex) {
				ex.printStackTrace();
				log.fatal("Parsing '%s' as %s via 'constructor'", value);
			}
			return null;
		};
	}
}
