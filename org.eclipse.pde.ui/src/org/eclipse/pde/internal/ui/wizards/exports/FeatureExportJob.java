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

	protected HashMap createBuildProperties() {
		HashMap map = new HashMap(15);
		map.put(IXMLConstants.PROPERTY_BUILD_TEMP, fBuildTempLocation + "/destination");
		map.put(IXMLConstants.PROPERTY_FEATURE_TEMP_FOLDER, fBuildTempLocation + "/destination");
		map.put(IXMLConstants.PROPERTY_INCLUDE_CHILDREN, "true");
		map.put("eclipse.running", "true");
		map.put(IXMLConstants.PROPERTY_BASE_OS, TargetPlatform.getOS());
		map.put(IXMLConstants.PROPERTY_BASE_WS, TargetPlatform.getWS());
		map.put(IXMLConstants.PROPERTY_BASE_ARCH, TargetPlatform.getOSArch());
		map.put(IXMLConstants.PROPERTY_BASE_NL, TargetPlatform.getNL());
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		map.put(IXMLConstants.PROPERTY_JAVAC_FAIL_ON_ERROR, "false");
		map.put(IXMLConstants.PROPERTY_JAVAC_DEBUG_INFO, store.getBoolean(PROP_JAVAC_DEBUG_INFO) ? "on" : "off");
		map.put(IXMLConstants.PROPERTY_JAVAC_VERBOSE, store.getString(PROP_JAVAC_VERBOSE));
		map.put(IXMLConstants.PROPERTY_JAVAC_SOURCE, store.getString(PROP_JAVAC_SOURCE));
		map.put(IXMLConstants.PROPERTY_JAVAC_TARGET, store.getString(PROP_JAVAC_TARGET));
		
		// for the assembler...
		map.put(IXMLConstants.PROPERTY_BUILD_DIRECTORY,  fBuildTempLocation + "/assemblyLocation");	//TODO this should be set to the folder location
		map.put(IXMLConstants.PROPERTY_BUILD_LABEL, ".");
		map.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, ".");
		map.put(IXMLConstants.PROPERTY_ARCHIVE_PREFIX, ".");
		if (fExportType == EXPORT_AS_ZIP)
			map.put(IXMLConstants.PROPERTY_ARCHIVE_FULLPATH, fDestinationDirectory + File.separator + fZipFilename);
		else 
			map.put(IXMLConstants.PROPERTY_ASSEMBLY_TMP, fDestinationDirectory);
		return map;
	}
	
	protected void doExport(IModel model, IProgressMonitor monitor)
			throws CoreException, InvocationTargetException {
		IFeatureModel feature = (IFeatureModel) model;
		String label = PDEPlugin.getDefault().getLabelProvider().getObjectText(feature);
		monitor.beginTask("", 5);
		monitor.setTaskName(PDEPlugin.getResourceString("ExportJob.exporting") + " " + label);
		try {
			HashMap properties = createBuildProperties();
			makeScript(feature);
			monitor.worked(1);
			runScript(getBuildScriptName(feature), getBuildExecutionTargets(),
					properties, new SubProgressMonitor(monitor, 2));
			runScript(getAssemblyScriptName(feature), new String[]{"main"},
					properties, new SubProgressMonitor(monitor, 2));
		} finally {
			deleteBuildFiles(feature);
			monitor.done();
		}
	}
	
	private String getBuildScriptName(IFeatureModel feature) {
		return feature.getInstallLocation() + Path.SEPARATOR + "build.xml";
	}
	
	private String getAssemblyScriptName(IFeatureModel feature) {
		return feature.getInstallLocation() + Path.SEPARATOR + "assemble."
				+ feature.getFeature().getId() + ".xml";
	}
	
	private String[] getBuildExecutionTargets() {
		if (fExportSource && fExportType != EXPORT_AS_UPDATE_JARS)
			return new String[] {"build.jars", "build.sources"};
		return new String[] {"build.jars"};
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
		String directory =
			(model instanceof IFeatureModel)
				? ((IFeatureModel) model).getInstallLocation()
				: ((IPluginModelBase) model).getInstallLocation();
		if (!isCustomBuild(model)) {
			File file = new File(directory, "build.xml");
			if (file.exists() && !file.isDirectory()) {
				file.delete();
			}
		}
		if (model instanceof IFeatureModel) {
			String id = ((IFeatureModel)model).getFeature().getId();
			File file = new File(directory, "assemble." + id + ".all.xml");
			if (file.exists() && !file.isDirectory())
				file.delete();
			file = new File(directory, "assemble." + id + ".xml");
			if (file.exists() && !file.isDirectory())
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
		generator.setOutputFormat(fExportType == EXPORT_AS_ZIP ? "antzip" : "folder");
		generator.setForceUpdateJar(fExportType == EXPORT_AS_UPDATE_JARS);
		generator.setEmbeddedSource(fExportSource && fExportType != EXPORT_AS_UPDATE_JARS);
		BuildScriptGenerator.setConfigInfo("*,*,*");
		generator.generate();	
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
