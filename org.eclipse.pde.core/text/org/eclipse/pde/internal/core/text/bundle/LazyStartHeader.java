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

import org.eclipse.pde.internal.core.ibundle.IBundle;

public class LazyStartHeader extends ManifestHeader {

	private static final long serialVersionUID = 1L;

	public LazyStartHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

	public boolean isLazyStart() {
		return fManifestElements.size() > 0
			&& "true".equals(fManifestElements.get(0).getValue()); //$NON-NLS-1$
	}
	
	public void setLazyStart(boolean lazy) {
		String old = getValue();
		if (fManifestElements.size() > 0) {
			fManifestElements.get(0).setValue(Boolean.toString(lazy));
		}
		firePropertyChanged(this, fName, old, getValue());
	}

}
