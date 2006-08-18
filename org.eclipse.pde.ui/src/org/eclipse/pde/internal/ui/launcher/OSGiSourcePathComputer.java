package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourcePathComputer;
import org.eclipse.pde.ui.launcher.IOSGiLauncher;
import org.eclipse.pde.ui.launcher.OSGiLaunchConfiguration;

public class OSGiSourcePathComputer extends JavaSourcePathComputer {
	
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		ISourceContainer[] orgs = super.computeSourceContainers(configuration, monitor);
		ISourceContainer[] additions = getAdditionalContainers(configuration, monitor);
		if (additions == null)
			return orgs;
		
		ISourceContainer[] result = new ISourceContainer[orgs.length + additions.length];
		System.arraycopy(orgs, 0, result, 0, orgs.length);
		System.arraycopy(additions, 0, result, orgs.length, additions.length);
		return result;
	}
	
	private ISourceContainer[] getAdditionalContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) {
		try {
			String id = configuration.getAttribute(OSGiLaunchConfiguration.OSGI_ENV_ID, (String)null);
			if (id != null) {
				try {
					IExtensionRegistry registry = Platform.getExtensionRegistry();
					IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.pde.ui.osgiLauncher"); //$NON-NLS-1$
					IConfigurationElement elem = null;
					for (int i = 0; i < elements.length; i++) {
						if (elements[i].getAttribute("id").equals(id)) {
							elem = elements[i];
							break;
						}
					}
					if (elem != null) {
						IOSGiLauncher launcher= (IOSGiLauncher)elem.createExecutableExtension("class");
						return launcher.getSourceContainers();
					}
				} catch (SecurityException e) {
				} catch (IllegalArgumentException e) {
				}
			}
		} catch (CoreException e) {
		}
		return null;
	}
	
	

}
