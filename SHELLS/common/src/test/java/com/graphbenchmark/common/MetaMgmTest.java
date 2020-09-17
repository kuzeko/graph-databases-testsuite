package com.graphbenchmark.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetaMgmTest {

	@Test
	public void testParseMeta_Range() {
		List l = new ArrayList();

		for (int i=0; i<3; i++) {
			HashMap<String, Object> m = new HashMap<>();
			m.put("LABEL", i);
			l.add(m);
		}

		assertEquals(l, (List)MetaMgm.enumerateConfigurations("LABEL=[0-3]"));
	}

	@Test
	public void testParseMeta_Composition() {
		List l = new ArrayList();

		for (int i=0; i<3; i++) {
			for (int j=5; j<7; j++) {
				HashMap<String, Object> m = new HashMap<>();
				m.put("A", i);
				m.put("B", j);
				l.add(m);
			}
		}

		assertEquals(l, (List)MetaMgm.enumerateConfigurations("A=[0-3];B=[5-7]"));
	}

	@Test
	public void testParseMeta_Set() {
		List l = new ArrayList();

		String[] words = {"a", "b", "c"};
		for (int i=0; i<3; i++) {
			for (int j=0; j<words.length; j++) {
				HashMap<String, Object> m = new HashMap<>();
				m.put("A", i);
				m.put("B", words[j]);
				l.add(m);
			}
		}

		assertEquals(l, (List)MetaMgm.enumerateConfigurations("A=[0-3];B={a,b,c}"));
	}


	@Test
	public void testParseMeta_SpacesAndSet() {
		List l = new ArrayList();

		String[] words = {"a", "b", "c x"};
		for (int i=0; i<3; i++) {
			for (int j=0; j<words.length; j++) {
				HashMap<String, Object> m = new HashMap<>();
				m.put("A", i);
				m.put("B", words[j]);
				l.add(m);
			}
		}

		assertEquals(l, (List)MetaMgm.enumerateConfigurations("A =  [0 - 3] ;  B  = {  a,b  ,' c x '  }  "));
	}

}