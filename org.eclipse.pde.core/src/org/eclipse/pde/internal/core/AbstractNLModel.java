/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

public abstract class AbstractNLModel extends AbstractModel {
	protected transient NLResourceHelper fNLHelper;

	public NLResourceHelper getNLResourceHelper() {
		if (fNLHelper == null)
			fNLHelper = createNLResourceHelper();
		return fNLHelper;
	}

	public void resetNLResourceHelper() {
		fNLHelper = null;
	}

	public void dispose() {
		if (fNLHelper != null) {
			fNLHelper.dispose();
			fNLHelper = null;
		}
		super.dispose();
	}

	public String getResourceString(String key) {
		if (key == null)
			return ""; //$NON-NLS-1$

		if (fNLHelper == null)
			fNLHelper = createNLResourceHelper();
		
		return fNLHelper != null ? fNLHelper.getResourceString(key) : null;
	}
	
	protected abstract NLResourceHelper createNLResourceHelper();

}
