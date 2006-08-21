package org.eclipse.pde.ui.launcher;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;

public abstract class AbstractOSGiLaunchConfiguration extends AbstractPDELaunchConfiguration{
	
	public abstract void initialize(ILaunchConfigurationWorkingCopy configuration);
	
	public ISourceContainer[] getSourceContainers() {
		return new ISourceContainer[0];
	}

}
