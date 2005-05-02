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
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.bundle.BundleFragmentModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginModelBase;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.plugin.WorkspaceExtensionsModel;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.osgi.framework.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class PDEState extends MinimalState {
	
	class PluginInfo {
		String name;
		String providerName;
		String className;
		boolean hasExtensibleAPI;
		String[] libraries;
	}
	
	private URL[] fWorkspaceURLs;
	private URL[] fTargetURLs;
	private IProgressMonitor fMonitor;
	private Map fPluginInfos;
	private Map fExtensions;
	private Dictionary fPlatformProperties;
	private ArrayList fTargetModels = new ArrayList();
	private ArrayList fWorkspaceModels = new ArrayList();
	private boolean fCombined;
	private long fTargetTimestamp;
	private boolean fResolve = true;
	
	public PDEState(URL[] urls, boolean resolve, IProgressMonitor monitor) {
		this(new URL[0], urls, TargetPlatform.getTargetEnvironment(), monitor);
		fResolve = resolve;
	}
	
	public PDEState(URL[] workspace, URL[] target, Dictionary properties, IProgressMonitor monitor) {
		fWorkspaceURLs = workspace;
		fTargetURLs = target;
		fMonitor = monitor;
		fPlatformProperties = properties;
		if (fResolve) {
			load();
		} else {
			createState();
		}
		createModels();
	}
	
	private void load() {
		fTargetTimestamp = computeTimestamp(fTargetURLs);
		long workspace = computeTimestamp(fWorkspaceURLs);
		long combined = fTargetTimestamp ^ workspace;
		
		// read combined (workspace + target) state first
		File dir = new File(DIR, Long.toString(combined) + ".state");
		fCombined = readCachedState(dir);
		if (!fCombined) {
			dir = new File(DIR, Long.toString(fTargetTimestamp) + ".target");
			// do not give up.
			// attempt to read cached target state, if any
			if (!readCachedState(dir)) {
				// no target state.  Create one from scratch.
				createState();
				saveState(dir);
				savePluginInfo(dir);
				saveExtensions(dir);
			} else {
				fId = fState.getBundles().length;
			}
		} else {
			fId = fState.getBundles().length;
		}
		fState.setResolver(Platform.getPlatformAdmin().getResolver());
		fState.setPlatformProperties(fPlatformProperties);
		fState.resolve(false);
		logResolutionErrors();
	}
	
	public boolean isCombined() {
		return fCombined;
	}
	
	private void createState() {
		fState = stateObjectFactory.createState();
		fPluginInfos = new HashMap();
		fMonitor.beginTask("Reading plug-ins...", fTargetURLs.length);
		for (int i = 0; i < fTargetURLs.length; i++) {
			try {
				File file = new File(fTargetURLs[i].getFile());
				fMonitor.subTask(file.getName());
				addBundle(file, true, -1);
			} catch (PluginConversionException e) {
			} catch (CoreException e) {
			} finally {
				fMonitor.worked(1);
			}
		}		
	}
	
	public BundleDescription addBundle(Dictionary manifest, File bundleLocation, boolean keepLibraries, long bundleId) {
		BundleDescription desc = super.addBundle(manifest, bundleLocation, keepLibraries, bundleId);
		if (desc != null && keepLibraries)
			createPluginInfo(desc, manifest);
		return desc;
	}

	private void createPluginInfo(BundleDescription desc, Dictionary manifest) {
		PluginInfo info = new PluginInfo();
		info.name = (String)manifest.get(Constants.BUNDLE_NAME);
		info.providerName = (String)manifest.get(Constants.BUNDLE_VENDOR);
		
		String className = (String)manifest.get("Plugin-Class"); //$NON-NLS-1$
		info.className	= className != null ? className : (String)manifest.get(Constants.BUNDLE_ACTIVATOR);	
		info.libraries = PDEStateHelper.getClasspath(manifest);
		info.hasExtensibleAPI = "true".equals((String)manifest.get(ICoreConstants.EXTENSIBLE_API)); //$NON-NLS-1$ 
		
		fPluginInfos.put(Long.toString(desc.getBundleId()), info);
	}
	
	private void createPluginInfo(Map map, Element element) {
		PluginInfo info = new PluginInfo();
		info.name = element.getAttribute("name"); //$NON-NLS-1$
		info.providerName = element.getAttribute("provider"); //$NON-NLS-1$
		info.className	= element.getAttribute("class"); //$NON-NLS-1$
		info.hasExtensibleAPI = "true".equals(element.getAttribute("hasExtensibleAPI")); //$NON-NLS-1$ //$NON-NLS-2$
		
		NodeList libs = element.getElementsByTagName("library"); //$NON-NLS-1$
		info.libraries = new String[libs.getLength()];
		for (int i = 0; i < libs.getLength(); i++) {
			Element lib = (Element)libs.item(i);
			info.libraries[i] = lib.getAttribute("name"); //$NON-NLS-1$
		}
		map.put(element.getAttribute("bundleID"), info); //$NON-NLS-1$
	}
	
	private void saveExtensions(File dir) {
		fExtensions = new HashMap();
		File file = new File(dir, ".extensions"); //$NON-NLS-1$
		OutputStream out = null;
		Writer writer = null;
		try {
			out = new FileOutputStream(file);
			writer = new OutputStreamWriter(out, "UTF-8"); //$NON-NLS-1$
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			Element root = doc.createElement("extensions"); //$NON-NLS-1$
			
			BundleDescription[] bundles = fState.getBundles();
			for (int i = 0; i < bundles.length; i++) {
				BundleDescription desc = bundles[i];
				Element element = doc.createElement("bundle"); //$NON-NLS-1$
				element.setAttribute("bundleID", Long.toString(desc.getBundleId())); //$NON-NLS-1$
				PDEStateHelper.parseExtensions(desc, element);
				if (element.hasChildNodes()) {
					root.appendChild(element);
					fExtensions.put(Long.toString(desc.getBundleId()), element);
				}
			}	
			doc.appendChild(root);
			XMLPrintHandler.printNode(writer, doc, "UTF-8"); //$NON-NLS-1$
		} catch (Exception e) {
			PDECore.log(e);
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e1) {
			}
			try {
				if (out != null)
					out.close();
			} catch (IOException e1) {
			}
		}
	}

	private void savePluginInfo(File dir) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			Element root = doc.createElement("map"); //$NON-NLS-1$
			
			Iterator iter = fPluginInfos.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next().toString();
				Element element = doc.createElement("bundle"); //$NON-NLS-1$
				element.setAttribute("bundleID", key); //$NON-NLS-1$
				PluginInfo info = (PluginInfo)fPluginInfos.get(key);
				if (info.className != null)
					element.setAttribute("class", info.className); //$NON-NLS-1$
				if (info.providerName != null)
					element.setAttribute("provider", info.providerName); //$NON-NLS-1$
				if (info.name != null)
					element.setAttribute("name", info.name); //$NON-NLS-1$
				if (info.hasExtensibleAPI)
					element.setAttribute("hasExtensibleAPI", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				if (info.libraries != null) {
					for (int i = 0; i < info.libraries.length; i++) {
						Element lib = doc.createElement("library"); //$NON-NLS-1$
						lib.setAttribute("name", info.libraries[i]); //$NON-NLS-1$
						element.appendChild(lib);
					}
				}
				root.appendChild(element);
			}
			doc.appendChild(root);
			writeXMLFile(doc, new File(dir, ".pluginInfo")); //$NON-NLS-1$
		} catch (Exception e) {
			PDECore.log(e);
		} 
	}
	
	private void writeXMLFile(Document doc, File file) throws IOException {
		Writer writer = null;
		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			writer = new OutputStreamWriter(out, "UTF-8"); //$NON-NLS-1$
			XMLPrintHandler.printNode(writer, doc, "UTF-8"); //$NON-NLS-1$
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e1) {
			}
			try {
				if (out != null)
					out.close();
			} catch (IOException e1) {
			}
		}
	}

	private boolean readCachedState(File dir) {
		fState = readStateCache(dir);
		fPluginInfos = readPluginInfoCache(dir);
		fExtensions = readExtensionsCache(dir);
		return fState != null && fPluginInfos != null && fExtensions != null;
	}
	
	private Map readExtensionsCache(File dir) {
		File file = new File(dir, ".extensions"); //$NON-NLS-1$
		if (file.exists() && file.isFile()) {
			try {
				Map map = new HashMap();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				Document doc = factory.newDocumentBuilder().parse(file);
				Element root = doc.getDocumentElement();
				if (root != null) {
					root.normalize();
					NodeList bundles = root.getElementsByTagName("bundle"); //$NON-NLS-1$
					for (int i = 0; i < bundles.getLength(); i++) {
						Element bundle = (Element)bundles.item(i); 
						String id = bundle.getAttribute("bundleID"); //$NON-NLS-1$
						map.put(id, bundle.getChildNodes());
					}
				}
				return map;
			} catch (org.xml.sax.SAXException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} catch (ParserConfigurationException e) {
				PDECore.log(e);
			}
		} 
		return null;
	}

	private Map readPluginInfoCache(File dir) {
		File file = new File(dir, ".pluginInfo"); //$NON-NLS-1$
		if (file.exists() && file.isFile()) {
			try {
				Map map = new HashMap();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				Document doc = factory.newDocumentBuilder().parse(file);
				Element root = doc.getDocumentElement();
				if (root != null) {
					NodeList bundles = root.getElementsByTagName("bundle"); //$NON-NLS-1$
					for (int i = 0; i < bundles.getLength(); i++) {
						createPluginInfo(map, (Element)bundles.item(i));
					}
				}
				return map;
			} catch (org.xml.sax.SAXException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} catch (ParserConfigurationException e) {
				PDECore.log(e);
			}
		} 
		return null;
	}

	private State readStateCache(File dir) {
		if (dir.exists() && dir.isDirectory()) {
			try {
				fState = stateObjectFactory.readState(dir);	
				return fState;
			} catch (IllegalStateException e) {
				PDECore.log(e);
			} catch (FileNotFoundException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} finally {
			}
		} 
		return null;
	}
	
 	private long computeTimestamp(URL[] urls) {
		long timestamp = 0;
		for (int i = 0; i < urls.length; i++) {
			File file = new File(urls[i].getFile());
			if (file.exists()) {
				if (file.isFile()) {
					timestamp ^= file.lastModified();
				} else {
					File manifest = new File(file, "META-INF/MANIFEST.MF"); //$NON-NLS-1$
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, "plugin.xml"); //$NON-NLS-1$
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, "fragment.xml"); //$NON-NLS-1$
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
				}
				timestamp ^= file.getAbsolutePath().hashCode();
			}
		}
		return timestamp;
	}
 	
 	private void createModels() {
		BundleDescription[] bundleDescriptions = fCombined || !fResolve ? fState.getBundles() : fState.getResolvedBundles();
		for (int i = 0; i < bundleDescriptions.length; i++) {
			BundleDescription desc = bundleDescriptions[i];
			fMonitor.subTask(bundleDescriptions[i].getSymbolicName());
			IPluginModelBase model = createModel(desc);
			if (model.getUnderlyingResource() == null)
				fTargetModels.add(model);
			else
				fWorkspaceModels.add(model);
			fExtensions.remove(Long.toString(desc.getBundleId()));
			fPluginInfos.remove(Long.toString(desc.getBundleId()));
		}
	}
 	
 	private IPluginModelBase createModel(BundleDescription desc) {
		IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
 		IContainer container = root.getContainerForLocation(new Path(desc.getLocation()));
 		return (container == null) ? createExternalModel(desc) : createWorkspaceModel(desc, (IProject) container);
 	}
 	
 	private IPluginModelBase createWorkspaceModel(BundleDescription desc, IProject project) {
 		if (WorkspaceModelManager.hasBundleManifest(project)) {
 			BundlePluginModelBase model = null;
 			if (desc.getHost() == null)
 				model = new BundlePluginModel();
 			else
 				model = new BundleFragmentModel();
 			WorkspaceBundleModel bundle = new WorkspaceBundleModel(project.getFile("META-INF/MANIFEST.MF"));
 			bundle.load(desc, this);
 			model.setBundleDescription(desc);
 			model.setBundleModel(bundle);
 			
 			String filename = (desc.getHost() == null) ? "plugin.xml" : "fragment.xml";
 			IFile file = project.getFile(filename);
 			if (file.exists()) {
 				WorkspaceExtensionsModel extensions = new WorkspaceExtensionsModel(file);
 				extensions.load(desc, this);
 				extensions.setBundleModel(model);
 				model.setExtensionsModel(extensions);
 			}
 			return model;
 		}
 		
		WorkspacePluginModelBase model = null;
		if (desc.getHost() == null)
			model = new WorkspacePluginModel(project.getFile("plugin.xml"), true);
		else
			model = new WorkspaceFragmentModel(project.getFile("fragment.xml"), true);
		model.load(desc, this, false);
		return model;
	}

	private IPluginModelBase createExternalModel(BundleDescription desc) {
 		ExternalPluginModelBase model = null;
 		if (desc.getHost() == null)
			model = new ExternalPluginModel();
		else
			model = new ExternalFragmentModel();
		model.load(desc, this, !fResolve);
		return model;
 	}
 	
 	public IPluginModelBase[] getTargetModels() {
 		return (IPluginModelBase[])fTargetModels.toArray(new IPluginModelBase[fTargetModels.size()]);
 	}
 	
 	public IPluginModelBase[] getWorkspaceModels() {
 		return (IPluginModelBase[])fWorkspaceModels.toArray(new IPluginModelBase[fWorkspaceModels.size()]);		
 	}
 	
 	public IPluginModelBase[] getModels() {
 		IPluginModelBase[] workspace = getWorkspaceModels();
 		IPluginModelBase[] target = getTargetModels();
 		IPluginModelBase[] all = new IPluginModelBase[workspace.length + target.length];
 		if (workspace.length > 0)
 			System.arraycopy(workspace, 0, all, 0, workspace.length);
 		if (target.length > 0)
 			System.arraycopy(target, 0, all, workspace.length, target.length);
 		return all;
 	}
	
	public String getClassName(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.className;
	}
	
	public boolean hasExtensibleAPI(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? false : info.hasExtensibleAPI;		
	}
	
	public String getPluginName(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.name;
	}
	
	public String getProviderName(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.providerName;
	}
	
	public String[] getLibraryNames(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? new String[0] : info.libraries;
	}
	
	public NodeList getExtensions(long bundleID) {
		return getChildren(bundleID, "extension"); //$NON-NLS-1$
	}
	
	public NodeList getExtensionPoints(long bundleID) {
		return getChildren(bundleID, "extension-point"); //$NON-NLS-1$
	}
	
	private NodeList getChildren(long bundleID, String tagName) {
		if (fExtensions != null) {
			Element bundle = (Element)fExtensions.get(Long.toString(bundleID));
			if (bundle != null) {
				return bundle.getElementsByTagName(tagName);
			}
		}
		return null;
	}
	
	public void shutdown() {
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getWorkspaceModels();
		long combined = 0;
		if (shouldSaveState(models)) {
			combined = fTargetTimestamp ^ computeTimestamp(models);
			File dir = new File(DIR, Long.toString(combined) + ".state");
			saveState(dir);
			writePluginInfo(models, new File(DIR, Long.toString(fTargetTimestamp) + ".target"), dir);
			writeExtensions(models, new File(DIR, Long.toString(fTargetTimestamp) + ".target"), dir);
		}
		clearStaleStates(".target", fTargetTimestamp);
		clearStaleStates(".state", combined);
		clearStaleStates(".cache", 0);
	}
	
	public void writePluginInfo(IPluginModelBase[] models, File origin, File destination) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = null;
			if (fTargetTimestamp != 0) {
				File file = new File(origin, ".pluginInfo"); //$NON-NLS-1$
				if (file.exists() && file.isFile()) {
					doc = builder.parse(file);
				}
			}
			if (doc == null)
				doc = builder.newDocument();
		
			Element root = doc.getDocumentElement();
			if (root == null) {
				root = doc.createElement("map"); //$NON-NLS-1$
				doc.appendChild(root);
			}
			for (int i = 0; i < models.length; i++) {
				IPluginBase plugin = models[i].getPluginBase();
				BundleDescription desc = models[i].getBundleDescription();
				Element element = doc.createElement("bundle"); //$NON-NLS-1$
				element.setAttribute("bundleID", Long.toString(desc.getBundleId())); //$NON-NLS-1$
				if (plugin instanceof IPlugin && ((IPlugin)plugin).getClassName() != null)
					element.setAttribute("class", ((IPlugin)plugin).getClassName()); //$NON-NLS-1$
				if (plugin.getProviderName() != null)
					element.setAttribute("provider", plugin.getProviderName()); //$NON-NLS-1$
				if (plugin.getName() != null)
					element.setAttribute("name", plugin.getName()); //$NON-NLS-1$
				if (plugin instanceof IPlugin && ClasspathUtilCore.hasExtensibleAPI((IPlugin)plugin))
					element.setAttribute("hasExtensibleAPI", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				IPluginLibrary[] libraries = plugin.getLibraries();
				for (int j = 0; j < libraries.length; j++) {
						Element lib = doc.createElement("library"); //$NON-NLS-1$
						lib.setAttribute("name", libraries[j].getName()); //$NON-NLS-1$
						if (!libraries[j].isExported())
							lib.setAttribute("exported", "false");
						element.appendChild(lib);
				}
				root.appendChild(element);
			}
			writeXMLFile(doc, new File(destination, ".pluginInfo"));
		} catch (ParserConfigurationException e) {
		} catch (FactoryConfigurationError e) {
		} catch (SAXException e) {
		} catch (IOException e) {
		}
	}
	
	public void writeExtensions(IPluginModelBase[] models, File origin, File destination) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = null;
			if (fTargetTimestamp != 0) {
				File file = new File(origin, ".extensions"); //$NON-NLS-1$
				if (file.exists() && file.isFile()) {
					doc = builder.parse(file);
				}
			}
			if (doc == null)
				doc = builder.newDocument();
		
			Element root = doc.getDocumentElement();
			if (root == null) {
				root = doc.createElement("extensions"); //$NON-NLS-1$
				doc.appendChild(root);
			}
			for (int i = 0; i < models.length; i++) {
				IPluginBase plugin = models[i].getPluginBase();
				IPluginExtension[] extensions = plugin.getExtensions();
				IPluginExtensionPoint[] extPoints = plugin.getExtensionPoints();
				if (extensions.length == 0 && extensions.length == 0)
					continue;
				Element element = doc.createElement("bundle"); //$NON-NLS-1$
				element.setAttribute("bundleID", Long.toString(models[i].getBundleDescription().getBundleId())); //$NON-NLS-1$
				for (int j = 0; j < extensions.length; j++) {
					element.appendChild(CoreUtility.writeExtension(doc, extensions[j]));
				}				
				for (int j = 0; j < extPoints.length; j++) {
					element.appendChild(CoreUtility.writeExtensionPoint(doc, extPoints[j]));
				}			
				root.appendChild(element);
			}
			writeXMLFile(doc, new File(destination, ".extensions"));
		} catch (ParserConfigurationException e) {
		} catch (FactoryConfigurationError e) {
		} catch (SAXException e) {
		} catch (IOException e) {
		}
		
	}
	
	private long computeTimestamp(IPluginModelBase[] models) {
		URL[] urls = new URL[models.length];
		for (int i = 0; i < models.length; i++) {
			try {
				urls[i] = new File(models[i].getInstallLocation()).toURL();
			} catch (MalformedURLException e) {
			}
		}
		return computeTimestamp(urls);
	}
	
	private boolean shouldSaveState(IPluginModelBase[] models) {
		for (int i = 0; i < models.length; i++) {
			if (!models[i].isInSync() || models[i].getBundleDescription() == null)
				return false;
		}
		return models.length > 0;
	}
	
	private void clearStaleStates(String extension, long latest) {
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString());
		File[] children = dir.listFiles();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				File child = children[i];
				if (child.isDirectory()) {
					String name = child.getName();
					if (name.endsWith(extension)
							&& name.length() > extension.length()
							&& !name.equals(Long.toString(latest) + extension)) { //$NON-NLS-1$
						CoreUtility.deleteContent(child);
					}
				}
			}
		}
	}	

}
