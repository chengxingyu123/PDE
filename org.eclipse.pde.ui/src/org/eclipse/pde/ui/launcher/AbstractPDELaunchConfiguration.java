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
package org.eclipse.pde.ui.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.ui.launcher.LaunchConfigurationHelper;
import org.eclipse.pde.internal.ui.launcher.LaunchVMHelper;
import org.eclipse.pde.internal.ui.launcher.LauncherUtils;

public abstract class AbstractPDELaunchConfiguration extends LaunchConfigurationDelegate {

	protected File fConfigDir = null;

	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {
		try {
			fConfigDir = null;
			monitor.beginTask("", 4); //$NON-NLS-1$
						
			preLaunchCheck(configuration, launch, monitor);
			
			// Program arguments
			String[] programArgs = getProgramArguments(configuration);
			if (programArgs == null) {
				monitor.setCanceled(true);
				return;
			}
	
			VMRunnerConfiguration runnerConfig = new VMRunnerConfiguration(
														"org.eclipse.core.launcher.Main",  //$NON-NLS-1$
														getClasspath(configuration)); 
			runnerConfig.setVMArguments(getVMArguments(configuration));
			runnerConfig.setProgramArguments(programArgs);
			runnerConfig.setWorkingDirectory(getWorkingDirectory(configuration).getAbsolutePath());
			runnerConfig.setEnvironment(getEnvironment(configuration));
			runnerConfig.setVMSpecificAttributesMap(getVMSpecificAttributesMap(configuration));

			monitor.worked(1);
					
			setDefaultSourceLocator(configuration);
			LaunchConfigurationHelper.synchronizeManifests(configuration, getConfigDir(configuration));
			PDEPlugin.getDefault().getLaunchListener().manage(launch);
			IVMRunner runner = getVMRunner(configuration, mode);
			if (runner != null)
				runner.run(runnerConfig, launch, monitor);
			else
				monitor.setCanceled(true);
			monitor.worked(1);
		} catch (CoreException e) {
			monitor.setCanceled(true);
			throw e;
		}
	}
	
	public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) throws CoreException {
		IVMInstall launcher = LaunchVMHelper.createLauncher(configuration);
		return launcher.getVMRunner(mode);
	}
	
	protected void setDefaultSourceLocator(ILaunchConfiguration configuration) throws CoreException {
		LauncherUtils.setDefaultSourceLocator(configuration);		
	}
	
	public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		String[] classpath = LaunchArgumentsHelper.constructClasspath(configuration);
		if (classpath == null) {
			String message = PDEUIMessages.WorkbenchLauncherConfigurationDelegate_noStartup;
			throw new CoreException(LaunchVMHelper.createErrorStatus(message));
		}
		return classpath;
	}
	
	public String[] getEnvironment(ILaunchConfiguration configuration) throws CoreException {
		return DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
	}
	
	public File getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getWorkingDirectory(configuration);
	}
	
	public Map getVMSpecificAttributesMap(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getVMSpecificAttributesMap(configuration);
	}
	
	public String[] getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		return LaunchArgumentsHelper.getUserVMArgumentArray(configuration);
	}

 	public String[] getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
 		ArrayList programArgs = new ArrayList();
 
 		// add tracing, if turned on	
 		if (configuration.getAttribute(IPDELauncherConstants.TRACING, false)
				&& !IPDELauncherConstants.TRACING_NONE.equals(configuration.getAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null))) {
			programArgs.add("-debug"); //$NON-NLS-1$
			programArgs.add(
					LaunchArgumentsHelper.getTracingFileArgument(
					configuration,
					getConfigDir(configuration).toString() + Path.SEPARATOR + ".options")); //$NON-NLS-1$
		}

		// add the program args specified by the user
		String[] userArgs = LaunchArgumentsHelper.getUserProgramArgumentArray(configuration);
		for (int i = 0; i < userArgs.length; i++) {
			// be forgiving if people have tracing turned on and forgot
			// to remove the -debug from the program args field.
			if (userArgs[i].equals("-debug") && programArgs.contains("-debug")) //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			programArgs.add(userArgs[i]);
		}

		if (!programArgs.contains("-os")) { //$NON-NLS-1$
			programArgs.add("-os"); //$NON-NLS-1$
			programArgs.add(TargetPlatform.getOS());
		}
		if (!programArgs.contains("-ws")) { //$NON-NLS-1$
			programArgs.add("-ws"); //$NON-NLS-1$
			programArgs.add(TargetPlatform.getWS());
		}
		if (!programArgs.contains("-arch")) { //$NON-NLS-1$
			programArgs.add("-arch"); //$NON-NLS-1$
			programArgs.add(TargetPlatform.getOSArch());
		}
		return (String[])programArgs.toArray(new String[programArgs.size()]);
 	}
 	
	protected IPath getProductPath() {
		return PDEPlugin.getWorkspace().getRoot().getLocation().removeLastSegments(1);
	}

	protected abstract void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) 
			throws CoreException;

	protected File getConfigDir(ILaunchConfiguration config) {
		if (fConfigDir == null)
			fConfigDir = LaunchConfigurationHelper.getConfigurationArea(config);
	
		return fConfigDir;
	}


}
