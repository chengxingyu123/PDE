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
import org.eclipse.pde.internal.core.ibundle.IBundle;

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
		
	}
	
	public void removeBundle(String id) {
		removeManifestElement(id);		
	}
	
	public void updateBundle(IPluginImport iimport) {
	}
	
}
