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
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.pde.ui.launcher.OSGiLaunchConfiguration;

public class OSGiSourceLookupDirector implements IPersistableSourceLocator {
	
	IPersistableSourceLocator fLocator;

	public String getMemento() throws CoreException {
		if (fLocator != null)
			return fLocator.getMemento();
		return null;
	}

	public void initializeDefaults(ILaunchConfiguration configuration)
			throws CoreException {
		String id = configuration.getAttribute(OSGiLaunchConfiguration.OSGI_ENV_ID, (String)null);
		if (id == null)
			id = EquinoxLauncher.ID;
		String locatorId = null;
		locatorId = getFrameworkSourceLocator(id);
		if (locatorId == null) 
			locatorId = "org.eclipse.jdt.launching.sourceLocator.JavaSourceLookupDirector"; //$NON-NLS-1$
		Object obj = getClass(locatorId);
		fLocator = (IPersistableSourceLocator)obj;
		if (fLocator != null)
			fLocator.initializeDefaults(configuration);
	}
	
	private String getFrameworkSourceLocator(String id) {
		if (id != null) {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.pde.ui.osgiLaunchers"); //$NON-NLS-1$
			for (int i = 0; i < elements.length; i++) {
				if (elements[i].getAttribute("id").equals(id)) { //$NON-NLS-1$
					String attr = elements[i].getAttribute("sourceLocatorId"); //$NON-NLS-1$
					if (attr != null)
						return attr;
				}
			}
		}
		return null;
	}
	
	private IPersistableSourceLocator getClass(String locatorId) {
		IPersistableSourceLocator javaLocator = null;
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.debug.core.sourceLocators"); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			try {
				if (elements[i].getAttribute("id").equals(locatorId)) { //$NON-NLS-1$
					Object o = elements[i].createExecutableExtension("class"); //$NON-NLS-1$
					if (o instanceof IPersistableSourceLocator)
						return (IPersistableSourceLocator)o;
				}
				if (elements[i].getAttribute("id").equals("org.eclipse.jdt.launching.sourceLocator.JavaSourceLookupDirector")) //$NON-NLS-1$ //$NON-NLS-2$
					javaLocator = (IPersistableSourceLocator)elements[i].createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
			}
		}
		return javaLocator;
	}

	public void initializeFromMemento(String memento) throws CoreException {
		if (fLocator != null)
			fLocator.initializeFromMemento(memento);
	}

	public Object getSourceElement(IStackFrame stackFrame) {
		if (fLocator != null)
			return fLocator.getSourceElement(stackFrame);
		return null;
	}

}
