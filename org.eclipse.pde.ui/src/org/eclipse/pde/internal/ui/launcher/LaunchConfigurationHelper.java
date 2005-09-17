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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
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
	
	private static String getSubstitutedString(String text) throws CoreException {
		if (text == null)
			return "";
		IStringVariableManager mgr = VariablesPlugin.getDefault().getStringVariableManager();
		return mgr.performStringSubstitution(text);
	}
	

}
