package org.eclipse.pde.internal.ui.search.participant;

import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;

public class HeaderElementHit {
	private ManifestHeader fHeader;
	private String fValue;
	public HeaderElementHit(ManifestHeader header, String value) {
		fHeader = header;
		fValue = value;
	}
	public ManifestHeader getHeader() {
		return fHeader;
	}
	public String getValue() {
		return fValue;
	}
}
