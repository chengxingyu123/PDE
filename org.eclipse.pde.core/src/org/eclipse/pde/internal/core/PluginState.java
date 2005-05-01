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
package org.eclipse.pde.internal.core;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;


public class PluginState extends PDEState {
	
	private long fWorkspaceTimestamp;
	private long fTargetTimestamp;
	
	private static URL[] combine(URL[] workspace, URL[] external) {
		URL[] all = new URL[workspace.length + external.length];
		if (workspace.length > 0)
			System.arraycopy(workspace, 0, all, 0, workspace.length);
		if (external.length > 0)
			System.arraycopy(external, 0, all, workspace.length, external.length);
		return all;
	}
	
	
	public PluginState(URL[] workspace, URL[] external, IProgressMonitor monitor) {
		super(combine(workspace, external),  true, monitor);
		fWorkspaceTimestamp = computeTimestamp(workspace);
		fTargetTimestamp = computeTimestamp(external);
	}
	
	protected void load(URL[] urls) {
		File dir = new File(DIR, Long.toString(fTimestamp) + ".workspace"); //$NON-NLS-1$
		restoreState(urls, dir);
		restoreExtensions(dir);
		fState.setResolver(Platform.getPlatformAdmin().getResolver());
		fState.setPlatformProperties(fPlatformProperties);
		fState.resolve(false);
		logResolutionErrors();
	}

	

}
