package org.eclipse.pde.internal.core.text.bundle;

import java.util.TreeMap;

public class SortedManifestElementList {

	private static final long serialVersionUID = 1L;
	private TreeMap fMap;
	
	public SortedManifestElementList() {
		fMap = new TreeMap();
	}
	
	public void add(ManifestElement element) {
		fMap.put(element.getValue(), element);
	}
	
	public void remove(String name) {
		fMap.remove(name);
	}
	
	public ManifestElement get(String name) {
		return (ManifestElement)fMap.get(name);
	}
	
	public int size() {
		return fMap.size();
	}
}
