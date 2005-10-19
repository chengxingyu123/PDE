package org.eclipse.pde.internal.ui.model.bundle;

import java.util.ArrayList;

public class ManifestElementList {

	private static final long serialVersionUID = 1L;
	private ArrayList fArrayList;
	
	public ManifestElementList() {
		fArrayList = new ArrayList();
	}
	
	public ManifestElementList(int size) {
		fArrayList = new ArrayList(size);
	}
	
	public void add(ManifestElement element) {
		fArrayList.add(element);
	}
	public void add(int index, ManifestElement element) {
		fArrayList.add(index, element);
	}
	public void remove(int index) {
		ManifestElement element = get(index);
		element.clear();
		fArrayList.remove(index);
	}
	public ManifestElement get(int index) {
		return (ManifestElement)fArrayList.get(index);
	}
	public int size() {
		return fArrayList.size();
	}
}
