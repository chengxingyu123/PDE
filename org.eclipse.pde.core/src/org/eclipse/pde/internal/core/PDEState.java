/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.osgi.framework.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PDEState extends MinimalState {
	
	class PluginInfo {
		String name;
		String providerName;
		String className;
		boolean hasExtensibleAPI;
		String[] libraries;
	}
	
	private IProgressMonitor fMonitor;

	private IPluginModelBase[] fModels;
	private HashMap fPluginInfos;
	private HashMap fExtensions;
	private long fTimestamp;

	private Dictionary fPlatfromProperties;

	public PDEState(URL[] urls, boolean resolve, IProgressMonitor monitor) {
		this(urls, TargetPlatform.getTargetEnvironment(), resolve, monitor);
	}
	
	public PDEState(URL[] urls, Dictionary properties, boolean resolve, IProgressMonitor monitor) {
		super(urls, resolve);
		fMonitor = monitor;
		fPlatfromProperties = properties;
		load();
	}
	
	protected void load() {
		if (fResolve) {
			fTimestamp = computeTimestamp(fURLs);
			File dir = new File(DIR, Long.toString(fTimestamp) + ".cache"); //$NON-NLS-1$
			restoreState(dir);
			restoreExtensions(dir);
		} else {
			createState();
		}
		fState.setResolver(Platform.getPlatformAdmin().getResolver());
		fState.setPlatformProperties(fPlatfromProperties);
		fState.resolve(false);
		if (fResolve)
			logResolutionErrors();
		createModels();		
	}
	
	private void restoreState(File dir) {
		if (dir.exists() && (!readStateCache(dir) || !readPluginInfoCache(dir))) {
			createState();
			saveState(dir);
			savePluginInfo(dir);
		} else {
			if (fState != null) {
				fId = fState.getBundles().length;
			} else {
				dir.mkdirs();
				createState();
				saveState(dir);
				savePluginInfo(dir);					
			}				
		}
	}
	
	private void createState() {
		fState = stateObjectFactory.createState();
		fPluginInfos = new HashMap();
		setTargetMode();
		fMonitor.beginTask("", fURLs.length); //$NON-NLS-1$
		for (int i = 0; i < fURLs.length; i++) {
			addBundle(new File(fURLs[i].getFile()), true, -1);
			fMonitor.worked(1);
		}		
	}
	
	private void restoreExtensions(File dir) {
		if (!readExtensionsCache(dir)) {
			saveExtensions(dir);
		}		
	}
	
	private boolean readPluginInfoCache(File dir) {
		File file = new File(dir, ".pluginInfo"); //$NON-NLS-1$
		fPluginInfos = new HashMap();
		if (file.exists() && file.isFile()) {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				Document doc = factory.newDocumentBuilder().parse(file);
				Element root = doc.getDocumentElement();
				if (root != null) {
					NodeList bundles = root.getElementsByTagName("bundle"); //$NON-NLS-1$
					for (int i = 0; i < bundles.getLength(); i++) {
						createPluginInfo((Element)bundles.item(i));
					}
				}
				return true;
			} catch (org.xml.sax.SAXException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} catch (ParserConfigurationException e) {
				PDECore.log(e);
			}
		} 
		return false;
	}

	private boolean readExtensionsCache(File dir) {
		fExtensions = new HashMap();
		File file = new File(dir, ".extensions"); //$NON-NLS-1$
		if (file.exists() && file.isFile()) {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				Document doc = factory.newDocumentBuilder().parse(file);
				Element root = doc.getDocumentElement();
				if (root != null) {
					root.normalize();
					NodeList bundles = root.getElementsByTagName("bundle"); //$NON-NLS-1$
					for (int i = 0; i < bundles.getLength(); i++) {
						Element bundle = (Element)bundles.item(i); 
						String id = bundle.getAttribute("bundleID"); //$NON-NLS-1$
						fExtensions.put(id, bundle.getChildNodes());
					}
				}
				return true;
			} catch (org.xml.sax.SAXException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} catch (ParserConfigurationException e) {
				PDECore.log(e);
			}
		} 
		return false;
	}
	
	private boolean readStateCache(File dir) {
		if (dir.exists() && dir.isDirectory()) {
			try {
				fState = stateObjectFactory.readState(dir);	
				return fState != null;
			} catch (IllegalStateException e) {
				PDECore.log(e);
			} catch (FileNotFoundException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} finally {
			}
		} 
		return false;
	}

	private void savePluginInfo(File dir) {
		File file = new File(dir, ".pluginInfo"); //$NON-NLS-1$
		OutputStream out = null;
		Writer writer = null;
		try {
			out = new FileOutputStream(file);
			writer = new OutputStreamWriter(out, "UTF-8"); //$NON-NLS-1$
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

	public BundleDescription addBundle(Dictionary manifest, File bundleLocation, boolean keepLibraries, long bundleId) {
		BundleDescription desc = super.addBundle(manifest, bundleLocation, keepLibraries, bundleId);
		if (desc != null && keepLibraries)
			createPluginInfo(desc, manifest);
		return desc;
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
	
	private void createPluginInfo(Element element) {
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
		fPluginInfos.put(element.getAttribute("bundleID"), info); //$NON-NLS-1$
	}
	
	private void createModels() {
		BundleDescription[] bundleDescriptions = fResolve ? fState.getResolvedBundles() : fState.getBundles();
		fModels = new IPluginModelBase[bundleDescriptions.length];
		for (int i = 0; i < bundleDescriptions.length; i++) {
			BundleDescription desc = bundleDescriptions[i];
			fMonitor.subTask(bundleDescriptions[i].getSymbolicName());
			ExternalPluginModelBase model = null;
			if (desc.getHost() == null)
				model = new ExternalPluginModel();
			else
				model = new ExternalFragmentModel();
			model.load(desc, this, !fResolve);
			fModels[i] = model;
			fExtensions.remove(Long.toString(desc.getBundleId()));
			fPluginInfos.remove(Long.toString(desc.getBundleId()));
		}

		fMonitor.done();		
	}
	
	public IPluginModelBase[] getModels() {
		return fModels;
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
	
	public long getTimestamp() {
		return fTimestamp;
	}

}
