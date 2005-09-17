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
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.widgets.Display;

public class LaunchPluginValidator {

	public static TreeSet parseWorkspacePluginIds(ILaunchConfiguration config)
			throws CoreException {
		TreeSet set = new TreeSet();
		String ids = config.getAttribute(IPDELauncherConstants.WSPROJECT, (String) null);
		if (ids != null) {
			StringTokenizer tok = new StringTokenizer(ids, File.pathSeparator);
			while (tok.hasMoreTokens())
				set.add(tok.nextToken());
		}
		return set;
	}

	public static TreeSet parseExternalPluginIds(ILaunchConfiguration config)
			throws CoreException {
		TreeSet selected = new TreeSet();
		String ids = config.getAttribute(IPDELauncherConstants.EXTPLUGINS, (String) null);
		if (ids != null) {
			StringTokenizer tok = new StringTokenizer(ids, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				int loc = token.lastIndexOf(',');
				if (loc == -1) {
					selected.add(token);
				} else if (token.charAt(loc + 1) == 't') {
					selected.add(token.substring(0, loc));
				}
			}
		}
		return selected;
	}

	public static TreeMap getPluginsToRun(ILaunchConfiguration config)
			throws CoreException {
		TreeMap map = null;
		ArrayList statusEntries = new ArrayList();

		if (!config.getAttribute(IPDELauncherConstants.USE_DEFAULT, true)) {
			map = validatePlugins(getSelectedPlugins(config), statusEntries);
		}

		if (map == null)
			map = validatePlugins(PDECore.getDefault().getModelManager().getPlugins(),
					statusEntries);

		final String requiredPlugin;
		if (PDECore.getDefault().getModelManager().isOSGiRuntime())
			requiredPlugin = "org.eclipse.osgi"; //$NON-NLS-1$
		else
			requiredPlugin = "org.eclipse.core.boot"; //$NON-NLS-1$

		if (!map.containsKey(requiredPlugin)) {
			final Display display = getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					MessageDialog
							.openError(
									display.getActiveShell(),
									PDEUIMessages.WorkbenchLauncherConfigurationDelegate_title,
									NLS
											.bind(
													PDEUIMessages.WorkbenchLauncherConfigurationDelegate_missingRequired,
													requiredPlugin));
				}
			});
			return null;
		}

		// alert user if any plug-ins are not loaded correctly.
		if (statusEntries.size() > 0) {
			final MultiStatus multiStatus = new MultiStatus(PDEPlugin.getPluginId(),
					IStatus.OK, (IStatus[]) statusEntries
							.toArray(new IStatus[statusEntries.size()]),
					PDEUIMessages.WorkbenchLauncherConfigurationDelegate_brokenPlugins,
					null);
			if (!ignoreValidationErrors(multiStatus)) {
				return null;
			}
		}

		return map;
	}

	public static IPluginModelBase[] getSelectedPlugins(ILaunchConfiguration config)
			throws CoreException {
		TreeMap map = new TreeMap();
		boolean automaticAdd = config.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD,
				true);
		IPluginModelBase[] wsmodels = PDECore.getDefault().getModelManager()
				.getWorkspaceModels();
		Set wsPlugins = parseWorkspacePluginIds(config);
		for (int i = 0; i < wsmodels.length; i++) {
			String id = wsmodels[i].getPluginBase().getId();
			// see the documentation of
			// AdvancedLauncherUtils.initWorkspacePluginsState
			if (id != null && automaticAdd != wsPlugins.contains(id))
				map.put(id, wsmodels[i]);
		}

		Set exModels = parseExternalPluginIds(config);
		IPluginModelBase[] exmodels = PDECore.getDefault().getModelManager()
				.getExternalModels();
		for (int i = 0; i < exmodels.length; i++) {
			String id = exmodels[i].getPluginBase().getId();
			if (id != null && exModels.contains(id) && !map.containsKey(id))
				map.put(id, exmodels[i]);
		}

		return (IPluginModelBase[]) map.values()
				.toArray(new IPluginModelBase[map.size()]);
	}

	public static IProject[] getAffectedProjects(ILaunchConfiguration config)
			throws CoreException {
		boolean doAdd = config.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
		boolean useFeatures = config.getAttribute(IPDELauncherConstants.USEFEATURES,
				false);
		boolean useDefault = config.getAttribute(IPDELauncherConstants.USE_DEFAULT, true);

		ArrayList projects = new ArrayList();
		IPluginModelBase[] models = PDECore.getDefault().getModelManager()
				.getWorkspaceModels();
		Set wsPlugins = parseWorkspacePluginIds(config);
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id == null)
				continue;
			// see the documentation of PluginBlock.initWorkspacePluginsState
			if (useDefault || useFeatures || doAdd != wsPlugins.contains(id)) {
				IProject project = models[i].getUnderlyingResource().getProject();
				if (project.hasNature(JavaCore.NATURE_ID))
					projects.add(project);
			}
		}

		// add fake "Java Search" project
		SearchablePluginsManager manager = PDECore.getDefault().getModelManager()
				.getSearchablePluginsManager();
		IJavaProject proxy = manager.getProxyProject();
		if (proxy != null) {
			IProject project = proxy.getProject();
			if (project.isOpen())
				projects.add(project);
		}
		return (IProject[]) projects.toArray(new IProject[projects.size()]);
	}

	private static TreeMap validatePlugins(IPluginModelBase[] models,
			ArrayList statusEntries) {
		TreeMap map = new TreeMap();
		for (int i = 0; i < models.length; i++) {
			IStatus status = validateModel(models[i]);
			if (status == null) {
				String id = models[i].getPluginBase().getId();
				if (id != null) {
					map.put(id, models[i]);
				}
			} else {
				statusEntries.add(status);
			}
		}
		return map;
	}

	private static IStatus validateModel(IPluginModelBase model) {
		return model.isLoaded() ? null : new Status(IStatus.WARNING, PDEPlugin
				.getPluginId(), IStatus.OK, model.getPluginBase().getId(), null);
	}

	private static boolean ignoreValidationErrors(final MultiStatus status) {
		final boolean[] result = new boolean[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				result[0] = MessageDialog.openConfirm(getDisplay().getActiveShell(),
						PDEUIMessages.WorkbenchLauncherConfigurationDelegate_title,
						status.getMessage());
			}
		});
		return result[0];
	}
	
	private static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}


}
