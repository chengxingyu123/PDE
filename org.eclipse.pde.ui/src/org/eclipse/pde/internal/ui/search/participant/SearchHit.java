package org.eclipse.pde.internal.ui.search.participant;

import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;

public class SearchHit {
	private ManifestHeader fHeader;
	private String fValue;
	private IPluginObject fPlugObj;
	public SearchHit(ManifestHeader header, String value) {
		fValue = value;
		fHeader = header;
	}
	public SearchHit(IPluginObject object, String value) {
		fValue = value;
		fPlugObj = object;
	}
	public Object getHitElement() {
		if (fHeader != null)
			return fHeader.getBundle();
		return fPlugObj;
	}
	public String getValue() {
		return fValue;
	}
	public IResource getResource() {
		if (fHeader != null)
			return fHeader.getModel().getUnderlyingResource();
		return fPlugObj.getModel().getUnderlyingResource();
	}
}