/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.builder.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;

public class FeatureExportJob extends BaseExportJob {

	public FeatureExportJob(
		int exportType,
		boolean exportSource,
		String destination,
		String zipFileName,
		Object[] items) {
		super(exportType, exportSource, destination, zipFileName, items);
	}

	protected HashMap createProperties(String destination, int exportType) {
		HashMap map = new HashMap(5);
		map.put("buildTempFolder", buildTempLocation + "/destination");
		map.put("include.children", "true");
//		if (exportType != BaseExportJob.EXPORT_AS_UPDATE_JARS) {
//			map.put("plugin.destination", destination);
//			map.put("feature.destination", destination);
//		} else {
//			String dest = destination;
//			File file = new File(destination, "plugins");
//			file.mkdirs();
//			if (file.exists()) {
//				dest = file.getAbsolutePath();
//			}
//			map.put("plugin.destination", dest);
//			dest = destination;
//			file = new File(destination, "features");
//			file.mkdirs();
//			if (file.exists()) {
//				dest = file.getAbsolutePath();
//			}
//			map.put("feature.destination", dest);
//		}
		map.put("baseos", TargetPlatform.getOS());
		map.put("basews", TargetPlatform.getWS());
		map.put("basearch", TargetPlatform.getOSArch());
		map.put("basenl", TargetPlatform.getNL());
//		map.put("eclipse.running", "true");
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		map.put("javacFailOnError", "false");
		map.put("javacDebugInfo", store.getBoolean(PROP_JAVAC_DEBUG_INFO) ? "on" : "off");
		map.put("javacVerbose", store.getString(PROP_JAVAC_VERBOSE));
		map.put("javacSource", store.getString(PROP_JAVAC_SOURCE));
		map.put("javacTarget", store.getString(PROP_JAVAC_TARGET));
		
		//For the assembler....
		map.put("buildDirectory",  buildTempLocation + "/assemblyLocation");	//TODO this should be set to the folder location
		map.put("collectingFolder", "eclipse");
		map.put("archivePrefix", ".");
		if (zipFileName != null)
			map.put("archiveFullPath", destination + File.separator + zipFileName);
		else
			map.put("archiveFullPath", destination);
			
		return map;
	}

	protected void doExport(IModel model, IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		IFeatureModel feature = (IFeatureModel) model;
		String label = PDEPlugin.getDefault().getLabelProvider().getObjectText(feature);
		monitor.beginTask("", 5);
		monitor.setTaskName(PDEPlugin.getResourceString("ExportJob.exporting") + " " + label);
		try {
			makeScript(feature);
			monitor.worked(1);
			runScript(feature.getInstallLocation() + Path.SEPARATOR
					+ "build.xml", new String[]{"build.jars"}, destination,
					exportType, exportSource, createProperties(destination,
							exportType), new SubProgressMonitor(monitor, 2));
			runScript(feature.getInstallLocation() + Path.SEPARATOR
					+ "assemble." + feature.getFeature().getId() + ".xml",
					new String[]{"main"}, destination, exportType,
					exportSource, createProperties(destination, exportType),
					new SubProgressMonitor(monitor, 2));
		} finally {
//			deleteBuildFiles(feature);
//			monitor.done();
		}
	}

	private void deleteBuildFiles(IFeatureModel model) throws CoreException {

		deleteBuildFile(model);

		IFeaturePlugin[] plugins = model.getFeature().getPlugins();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		for (int i = 0; i < plugins.length; i++) {
			ModelEntry entry =
				manager.findEntry(plugins[i].getId(), plugins[i].getVersion());
			if (entry != null) {
				deleteBuildFile(entry.getActiveModel());
			}

		}

	}

	public void deleteBuildFile(IModel model) throws CoreException {
		if (isCustomBuild(model))
			return;
		String directory =
			(model instanceof IFeatureModel)
				? ((IFeatureModel) model).getInstallLocation()
				: ((IPluginModelBase) model).getInstallLocation();

		File file = new File(directory, "build.xml");
		if (file.exists()) {
			file.delete();
		}
	}

	private void makeScript(IFeatureModel model) throws CoreException {
		BuildScriptGenerator generator = new BuildScriptGenerator();
		generator.setBuildingOSGi(PDECore.getDefault().getModelManager().isOSGiRuntime());
		generator.setChildren(true);
		generator.setWorkingDirectory(model.getUnderlyingResource().getProject().getLocation().toOSString());
		generator.setElements(new String[] {"feature@" + model.getFeature().getId()});
		generator.setPluginPath(getPaths());
		if (exportType == EXPORT_AS_ZIP)
			generator.setOutputFormat("antzip");
		else if (exportType == EXPORT_AS_DIRECTORY)
			generator.setOutputFormat("folder");
		setConfigInfo(model.getFeature());
		generator.generate();	
	}

	private void setConfigInfo(IFeature feature) throws CoreException {
		String os = feature.getOS() == null ? "*" : TargetPlatform.getOS();
		String ws = feature.getWS() == null ? "*" : TargetPlatform.getWS();
		String arch = feature.getArch() == null ? "*" : TargetPlatform.getOSArch();

		FeatureBuildScriptGenerator.setConfigInfo(os + "," + ws + "," + arch);
	}

	private String[] getPaths() throws CoreException {
		ArrayList paths = new ArrayList();
		IFeatureModel[] models = PDECore.getDefault().getWorkspaceModelManager().getFeatureModels();
		for (int i = 0; i < models.length; i++) {
			paths.add(models[i].getInstallLocation() + Path.SEPARATOR + "feature.xml");
		}
		
		String[] plugins = TargetPlatform.createPluginPath();
		String[] features = (String[]) paths.toArray(new String[paths.size()]);
		String[] all = new String[plugins.length + paths.size()];
		System.arraycopy(plugins, 0, all, 0, plugins.length);
		System.arraycopy(features, 0, all, plugins.length, features.length);
		return all;
	}

}
