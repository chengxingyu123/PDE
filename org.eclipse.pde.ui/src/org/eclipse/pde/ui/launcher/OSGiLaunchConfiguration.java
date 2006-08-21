package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

public class OSGiLaunchConfiguration extends LaunchConfigurationDelegate {
	
	public final static String OSGI_ENV_ID = "osgi.env.id";

	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String id = configuration.getAttribute(OSGI_ENV_ID, (String)null);
		if (id != null) {
			AbstractOSGiLaunchConfiguration launcher = findLauncher(id);
			launcher.launch(configuration, mode, launch, monitor);
		}
	}
	
	private AbstractOSGiLaunchConfiguration findLauncher(String id ) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.pde.ui.osgiLauncher"); //$NON-NLS-1$
		IConfigurationElement elem = null;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].getAttribute("id").equals(id)) {
				elem = elements[i];
				break;
			}
		}
		if (elem != null)
			try {
				return (AbstractOSGiLaunchConfiguration)elem.createExecutableExtension("class");
			} catch (CoreException e) {
			}
		return null;
	}

}
