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
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class LaunchConfigurationHelper {

	public static String getWorkspaceLocation(ILaunchConfiguration configuration) 
				throws CoreException {
		String location = configuration.getAttribute(IPDELauncherConstants.LOCATION, (String)null);
		if (location == null) {
			// backward compatibility
			location = configuration.getAttribute(IPDELauncherConstants.LOCATION + "0", (String)null); 
			if (location != null) {
				ILaunchConfigurationWorkingCopy wc = null;
				if (configuration.isWorkingCopy()) {
					wc = (ILaunchConfigurationWorkingCopy) configuration;
				} else {
					wc = configuration.getWorkingCopy();
				}
				wc.setAttribute(IPDELauncherConstants.LOCATION + "0", (String)null);
				wc.setAttribute(IPDELauncherConstants.LOCATION, location);
				wc.doSave();
			}
		}
		return getSubstitutedString(location);
	}
	
	public static String[] getUserProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		String args = configuration.getAttribute(
				IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, 
				(String)null);
		if (args == null) {
			// backward compatibility
			args = configuration.getAttribute("progargs", (String)null);
			if (args != null) {
				ILaunchConfigurationWorkingCopy wc = null;
				if (configuration.isWorkingCopy()) {
					wc = (ILaunchConfigurationWorkingCopy) configuration;
				} else {
					wc = configuration.getWorkingCopy();
				}
				wc.setAttribute("progargs", (String)null);
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, args);
				wc.doSave();			
			}
		}
		return new ExecutionArguments("", getSubstitutedString(args)).getProgramArgumentsArray();
	}
	
	public static String[] getUserVMArguments(ILaunchConfiguration configuration) throws CoreException {
		String args = configuration.getAttribute(
				IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, 
				(String)null);
		if (args == null) {
			// backward compatibility
			args = configuration.getAttribute("vmargs", (String)null);
			if (args != null) {
				ILaunchConfigurationWorkingCopy wc = null;
				if (configuration.isWorkingCopy()) {
					wc = (ILaunchConfigurationWorkingCopy) configuration;
				} else {
					wc = configuration.getWorkingCopy();
				}
				wc.setAttribute("vmargs", (String)null);
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, args);
				wc.doSave();			
			}
		}
		return new ExecutionArguments(getSubstitutedString(args), "").getVMArgumentsArray();
	}
	
	public static String getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
		String working = configuration.getAttribute(
				IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, 
				LauncherUtils.getDefaultPath().toString());
		File dir = new File(getSubstitutedString(working));
		if (!dir.exists())
			dir.mkdirs();
		return dir.getAbsolutePath();			
	}
	
	public static File getConfigurationArea(ILaunchConfiguration config) {
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString(), config.getName());
		try {
			if (!config.getAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, true)) {
				String userPath = config.getAttribute(IPDELauncherConstants.CONFIG_LOCATION, (String)null);
				if (userPath != null) {
					userPath = getSubstitutedString(userPath);
					dir = new File(userPath);
				}
			}
		} catch (CoreException e) {
		}		
		if (!dir.exists()) 
			dir.mkdirs();		
		return dir;		
	}

	
	private static String getSubstitutedString(String text) throws CoreException {
		if (text == null)
			return "";
		IStringVariableManager mgr = VariablesPlugin.getDefault().getStringVariableManager();
		return mgr.performStringSubstitution(text);
	}
	

}
