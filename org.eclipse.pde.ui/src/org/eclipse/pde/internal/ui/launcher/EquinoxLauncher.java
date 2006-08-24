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
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.launcher.EquinoxLaunchConfiguration;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class EquinoxLauncher extends EquinoxLaunchConfiguration {
	
	public static final String ID = "org.eclipse.pde.ui.EquinoxLauncher"; //$NON-NLS-1$

	public ISourceContainer[] getSourceContainers() {
		return null;
	}

	public void initialize(ILaunchConfigurationWorkingCopy configuration) {
		try {
			
			ModelEntry base = PDECore.getDefault().getModelManager().findEntry("org.eclipse.osgi"); //$NON-NLS-1$
			if (base == null)
				return;
			boolean isProject = base.getWorkspaceModel() != null;
			String constant = isProject ? IPDELauncherConstants.WORKSPACE_BUNDLES 
					: IPDELauncherConstants.TARGET_BUNDLES;
			String value = configuration.getAttribute(constant, new String());
			if (value.indexOf("org.eclipse.osgi@") == -1) {  //$NON-NLS-1$
				if (value.length() > 0)
					value += ","; //$NON-NLS-1$
				value += "org.eclipse.osgi@default:default"; //$NON-NLS-1$
				configuration.setAttribute(constant, value);
			}
		} catch (CoreException e) {
			PDECore.log(e);
		}
	}

}
