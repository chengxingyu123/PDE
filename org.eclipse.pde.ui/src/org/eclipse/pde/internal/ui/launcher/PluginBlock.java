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
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class PluginBlock extends AbstractPluginBlock {

	private String fProduct;
	private String fApplication;

	public PluginBlock(AbstractLauncherTab tab) {
		super(tab);
	}
	
	public void initializeFrom(ILaunchConfiguration config, boolean defaultSelection) throws CoreException {
		initializeProductFrom(config, defaultSelection);
		super.initializeFrom(config, defaultSelection);
	}

	public void activated(ILaunchConfigurationWorkingCopy config, boolean isJUnit) {
		initializeProductFrom(config, isJUnit);
	}
	
	public void initializeProductFrom(ILaunchConfiguration config, boolean isJUnit) {
		try {
			if (config.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
				fProduct = config.getAttribute(IPDELauncherConstants.PRODUCT, (String)null);
				fApplication = null;
			} else {
				String appToRun = config.getAttribute(IPDELauncherConstants.APPLICATION, LaunchConfigurationHelper.getDefaultApplicationName());
				if (!isJUnit)
					fApplication = appToRun;
				else {
					if(JUnitLaunchConfiguration.CORE_APPLICATION.equals(appToRun)){
						fApplication = null;
					} else {
						fApplication = config.getAttribute(IPDELauncherConstants.APP_TO_TEST, LaunchConfigurationHelper.getDefaultApplicationName());
					}
				}
				fProduct = null;
			}
		} catch (CoreException e) {
		}
	}
	
	protected PluginValidationOperation createValidationOperation() {
		return new PluginValidationOperation(getPluginsToValidate(), fProduct, fApplication);
	}
	
	/*
	 * if the "automatic add" option is selected, then we save the ids of plugins
	 * that have been "deselected" by the user.
	 * When we initialize the tree, we first set the workspace plugins subtree to 'checked',
	 * then we check the plugins that had been deselected and saved in the config.
	 *
	 * If the "automatic add" option is not selected, then we save the ids of plugins
	 * that were "selected" by the user.
	 * When we initialize the tree, we first set the workspace plugins subtree to 'unchecked',
	 * then we check the plugins that had been selected and saved in the config.
	 */
	protected void initWorkspacePluginsState(ILaunchConfiguration config) throws CoreException {
		boolean automaticAdd = config.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);		
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, automaticAdd);
		fNumWorkspaceChecked = automaticAdd ? fWorkspaceModels.length : 0;
		
		TreeSet ids = LaunchPluginValidator.parseWorkspacePluginIds(config);
		for (int i = 0; i < fWorkspaceModels.length; i++) {
			String id = fWorkspaceModels[i].getPluginBase().getId();
			if (id == null)
				continue;
			if (automaticAdd && ids.contains(id)) {
				if (fPluginTreeViewer.setChecked(fWorkspaceModels[i], false))
					fNumWorkspaceChecked -= 1;
			} else if (!automaticAdd && ids.contains(id)) {
				if (fPluginTreeViewer.setChecked(fWorkspaceModels[i], true))
					fNumWorkspaceChecked += 1;
			} 
		}			

		fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
		fPluginTreeViewer.setGrayed(
			fWorkspacePlugins,
			fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
	}
	
	protected void initExternalPluginsState(ILaunchConfiguration config)
			throws CoreException {
		fNumExternalChecked = 0;

		fPluginTreeViewer.setSubtreeChecked(fExternalPlugins, false);
		TreeSet selected = LaunchPluginValidator.parseExternalPluginIds(config);
		for (int i = 0; i < fExternalModels.length; i++) {
			if (selected.contains(fExternalModels[i].getPluginBase().getId())) {
				if (fPluginTreeViewer.setChecked(fExternalModels[i], true))
					fNumExternalChecked += 1;
			}
		}

		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(fExternalPlugins, fNumExternalChecked > 0
				&& fNumExternalChecked < fExternalModels.length);
	}

	protected void savePluginState(ILaunchConfigurationWorkingCopy config) {
		if (isEnabled()) {
			// store deselected projects
			StringBuffer wbuf = new StringBuffer();
			for (int i = 0; i < fWorkspaceModels.length; i++) {
				IPluginModelBase model = fWorkspaceModels[i];
				// if "automatic add" option is selected, save "deselected" workspace plugins
				// Otherwise, save "selected" workspace plugins
				if (fPluginTreeViewer.getChecked(model) != fAddWorkspaceButton.getSelection())
					wbuf.append(model.getPluginBase().getId() + File.pathSeparatorChar);
			}
			config.setAttribute(IPDELauncherConstants.WSPROJECT, wbuf.toString());

			// Store selected external models
			StringBuffer exbuf = new StringBuffer();
			Object[] checked = fPluginTreeViewer.getCheckedElements();
			for (int i = 0; i < checked.length; i++) {
				if (checked[i] instanceof ExternalPluginModelBase) {
					IPluginModelBase model = (IPluginModelBase) checked[i];
					exbuf.append(model.getPluginBase().getId() + File.pathSeparatorChar);
				}
			}
			config.setAttribute(IPDELauncherConstants.EXTPLUGINS, exbuf.toString());
		} else {
			config.setAttribute(IPDELauncherConstants.WSPROJECT, (String) null);
			config.setAttribute(IPDELauncherConstants.EXTPLUGINS, (String) null);
		}
	}

}
