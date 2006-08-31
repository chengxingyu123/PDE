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
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.EquinoxLauncher;

/**
 * A launch delegate for launching OSGi frameworks
 * <p>
 * Clients may subclass and instantiate this class.
 * </p>
 * @since 3.3
 */
public class OSGiLaunchConfiguration extends LaunchConfigurationDelegate {
	
	public final static String OSGI_ENV_ID = "osgi.env.id"; //$NON-NLS-1$

	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String id = configuration.getAttribute(OSGI_ENV_ID, (String)null);
		// if no OSGi Env specified, then launch with Equinox
		if (id == null)
			id = EquinoxLauncher.ID;
		AbstractOSGiLaunchConfiguration launcher = findLauncher(id);
		if (launcher != null) 
			launcher.launch(configuration, mode, launch, monitor);
	}
	
	private AbstractOSGiLaunchConfiguration findLauncher(String id ) throws CoreException {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.pde.ui.osgiLaunchers"); //$NON-NLS-1$
		IConfigurationElement elem = null;
		for (int i = 0; i < elements.length; i++) {
			if (id.equals(elements[i].getAttribute("id"))) { //$NON-NLS-1$
				elem = elements[i];
				break;
			}
		}
		if (elem != null) 
			try {
				return (AbstractOSGiLaunchConfiguration)elem.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
			} catch (ClassCastException e) {
			}
		String name = (elem == null) ? null : elem.getAttribute("name"); //$NON-NLS-1$
		String message = NLS.bind(PDEUIMessages.OSGiLaunchConfiguration_cannotFindLaunchConfiguration, (name == null) ? PDEUIMessages.OSGiLaunchConfiguration_selected : name);
		IStatus status = new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, IStatus.OK, message , null);
		throw new CoreException(status);
	}

}
