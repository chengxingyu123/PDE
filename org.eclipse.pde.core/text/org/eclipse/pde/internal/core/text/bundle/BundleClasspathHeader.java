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

import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.core.ibundle.IBundle;


public class BundleClasspathHeader extends ManifestHeader {
	
	private static final long serialVersionUID = 1L;

	public BundleClasspathHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}
	
	public void addLibrary(String name) {
		ManifestElement element = new ManifestElement(this);
		element.setValue(name);
		addManifestElement(element);
	}
	
	public void removeLibrary(String name) {
		if (fManifestElements != null) {
			for (int i = 0; i < fManifestElements.size(); i++) {
				ManifestElement element = (ManifestElement)fManifestElements.get(i);
				if (name.equals(element.getValue()))
					removeManifestElement(element);
			}
		}	
	}
	
	public void swap(String library1, String library2) {
		
	}
	
	public void updateLibrary(IPluginLibrary library) {
		
	}

}
