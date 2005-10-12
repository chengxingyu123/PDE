package org.eclipse.pde.internal.ui.search.participant;

import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;

public class SearchHit {
	private ManifestHeader fHeader;
	private String fValue;
	private IPluginAttribute fAttrib;
	public SearchHit(ManifestHeader header, String value) {
		fValue = value;
		fHeader = header;
	}
	public SearchHit(IPluginAttribute attrib, String value) {
		fValue = value;
		fAttrib = attrib;
	}
	public Object getHitElement() {
		if (fHeader != null)
			return fHeader.getBundle();
		return fAttrib;
	}
	public String getValue() {
		return fValue;
	}
	public IResource getResource() {
		if (fHeader != null)
			return fHeader.getModel().getUnderlyingResource();
		return fAttrib.getModel().getUnderlyingResource();
	}
}