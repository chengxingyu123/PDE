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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.pde.core.IModelProviderListener;

public abstract class AbstractModelManager {
	
	private ArrayList fListeners = new ArrayList();

	protected abstract void initialize();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelProvider#removeModelProviderListener(org.eclipse.pde.core.IModelProviderListener)
	 */
	public synchronized void removeModelProviderListener(IModelProviderListener listener) {
		fListeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelProvider#addModelProviderListener(org.eclipse.pde.core.IModelProviderListener)
	 */
	public synchronized void addModelProviderListener(IModelProviderListener listener) {
		if (!fListeners.contains(listener))
			fListeners.add(listener);
	}
	
	protected synchronized void fireModelProviderEvent(ModelProviderEvent event) {
		for (Iterator iter = fListeners.iterator(); iter.hasNext();) {
			((IModelProviderListener) iter.next()).modelsChanged(event);
		}
	}

	public void shutdown() {
		removeListeners();
	}

	protected void removeListeners() {
		if (fListeners.size() > 0) {
			fListeners.clear();
		}
	}

}
