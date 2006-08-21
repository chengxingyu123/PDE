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
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.launcher.EquinoxLaunchConfiguration;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class EquinoxLauncher extends EquinoxLaunchConfiguration {

	public ISourceContainer[] getSourceContainers() {
		return null;
	}

	public void initialize(ILaunchConfigurationWorkingCopy configuration) {
		try {
			String value = configuration.getAttribute(IPDELauncherConstants.TARGET_BUNDLES, "");
			value = value + ",org.apache.ant@default:default";
			configuration.setAttribute(IPDELauncherConstants.TARGET_BUNDLES, value);
		} catch (CoreException e) {
			PDECore.log(e);
		}
	}

}
