package org.eclipse.pde.internal.ui.search.participant;

import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;

public class SearchHit /*implements IAdaptable*/ {
	private ManifestHeader fHeader;
	private String fValue;
	private IPluginAttribute fAttrib;
	private boolean fManifest;
	public SearchHit(ManifestHeader header, String value) {
		fHeader = header;
		fValue = value;
		fManifest = true;
	}
	public SearchHit(IPluginAttribute attrib, String value) {
		fAttrib = attrib;
		fValue = value;
	}
	public Object getHitElement() {
		if (fManifest)
			return fHeader.getBundle();
		return fAttrib;
	}
	public String getValue() {
		return fValue;
	}
//	public Object getAdapter(Class adapter) {
//		return getResource().getAdapter(adapter);
//	}
	public IResource getResource() {
		if (fManifest)
			return fHeader.getModel().getUnderlyingResource();
		return fAttrib.getModel().getUnderlyingResource();
	}
}