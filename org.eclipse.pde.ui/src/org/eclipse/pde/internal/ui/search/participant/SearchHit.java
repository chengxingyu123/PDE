package org.eclipse.pde.internal.ui.search.participant;

import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.plugin.IPluginObject;

public class SearchHit {
	private String fValue;
	private IPluginObject fPlugObj;
	public SearchHit(IPluginObject object, String value) {
		fValue = value;
		fPlugObj = object;
	}
	public Object getHitElement() {
		return fPlugObj;
	}
	public String getValue() {
		return fValue;
	}
	public IResource getResource() {
		return fPlugObj.getModel().getUnderlyingResource();
	}
}