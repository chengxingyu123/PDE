/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.Constants;

public class RequireBundleHeader extends CompositeManifestHeader {

	private static final long serialVersionUID = 1L;

	public RequireBundleHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}
	
	public void addBundle(IPluginImport iimport) {
		addBundle(iimport.getId(), iimport.getVersion(), iimport.isReexported(), iimport.isOptional());
	}
	
	public void addBundle(String id) {
		addBundle(id, null, false, false);
	}
	
	public void addBundle(String id, String version, boolean exported, boolean optional) {
		PDEManifestElement element = new PDEManifestElement(this);
		updateBundle(element, id, version, exported, optional);		
		addManifestElement(element);
	}
	
	public void removeBundle(String id) {
		removeManifestElement(id);		
	}
	
	public void updateBundle(int index, IPluginImport iimport) {
		PDEManifestElement element = getElementAt(index);
		if (element != null) {
			updateBundle(element, 
					iimport.getId(), iimport.getVersion(),
					iimport.isReexported(), iimport.isOptional());
		}
		updateValue();
	}
	
	public void updateBundle(PDEManifestElement element, String id, String version, boolean exported, boolean optional) {
		element.setValue(id);

		int bundleManifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
		if (optional)
			if (bundleManifestVersion > 1)
				element.addDirective(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL); 
			else
				element.addAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE, "true"); 
		
		if (exported)
			if (bundleManifestVersion > 1)
				element.addDirective(Constants.VISIBILITY_DIRECTIVE, Constants.VISIBILITY_REEXPORT); 
			else
				element.addAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE, "true");

		if (version != null && version.trim().length() > 0)
			element.addAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, version.trim()); 		
	}
	
}
