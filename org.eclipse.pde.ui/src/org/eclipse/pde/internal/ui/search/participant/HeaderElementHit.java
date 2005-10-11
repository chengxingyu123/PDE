package org.eclipse.pde.internal.ui.search.participant;

import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;

public class HeaderElementHit {
	private ManifestHeader fHeader;
	public HeaderElementHit(ManifestHeader header) {
		fHeader = header;
	}
	public ManifestHeader getHeader() {
		return fHeader;
	}
}
