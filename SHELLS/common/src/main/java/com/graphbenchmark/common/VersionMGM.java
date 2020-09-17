package com.graphbenchmark.common;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class VersionMGM {
	public static String readGitProperties(Class<?> cls) {

		ClassLoader classLoader = cls.getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream("git.properties");

		try {
			return readFromInputStream(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
			return "Version information could not be retrieved";
		}
	}

	private static String readFromInputStream(InputStream inputStream) throws IOException {
		StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = br.readLine()) != null) {
				resultStringBuilder.append(line).append("\n");
			}
		}
		return resultStringBuilder.toString();
	}

	public static String getVersion(Class<?> cls) {
		ClassLoader classLoader = cls.getClassLoader();
		Gson gson = new Gson();
		HashMap<String, String> v = gson.fromJson(readGitProperties(cls), HashMap.class);
		return v.get("git.commit.id.describe-short");
	}
}
