package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.PluginExtension;
import org.eclipse.pde.internal.core.plugin.PluginExtensionPoint;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class PDEExtensionRegistry {
	
	private Object fMasterKey = new Object();
	private Object fUserKey = new Object();
	
	private IExtensionRegistry fRegistry = null;
	private PDERegistryStrategy fStrategy = null;
	
	private static final String EXTENSION_DIR = ".extensions"; //$NON-NLS-1$
	
	public PDEExtensionRegistry() {
		if (fStrategy == null) {
			File extensionsDir = new File(PDECore.getDefault().getStateLocation().toFile(), EXTENSION_DIR);
			// create the strategy without creating registry.  That way we create the registry at the last possible moment.
			// This way we can listen to events in PDE without creating the registry until we need it.
			fStrategy = new PDERegistryStrategy(new File[] {extensionsDir}, new boolean[] {false}, fMasterKey, this);
		}
	}
	
	public void stop() {
		if (fRegistry != null)
			fRegistry.stop(fMasterKey);
	}
	
	private synchronized IExtensionRegistry getRegistry() {
		if (fRegistry == null)
			createRegistry();
		return fRegistry;
	}
	
	public void targetReloaded() {
		// disconnect listeners
		fStrategy.dispose();
		// stop old registry (which will write contents to FS) and delete the cache it creates
		// might see if we can dispose of a registry without writing to file system
		stop();
		CoreUtility.deleteContent(new File(PDECore.getDefault().getStateLocation().toFile(), EXTENSION_DIR));
		fRegistry = null;
	}
	
	public IPluginModelBase[] findExtensionPlugins(String pointId) {
		IExtensionPoint point = getExtensionPoint(pointId);
		if (point == null) {
			// if extension point for extension does not exist, search all plug-ins manually
			return PluginRegistry.getAllModels();
		}
		IExtension[] exts = point.getExtensions();
		HashMap plugins = new HashMap();
		for (int i = 0; i < exts.length; i++) {
			String pluginId = exts[i].getContributor().getName();
			if (plugins.containsKey(pluginId))
				continue;
			IPluginModelBase base = PluginRegistry.findModel(pluginId);
			if (base != null)
				plugins.put(pluginId, base);
		}
		java.util.Collection values = plugins.values();
		return (IPluginModelBase[])values.toArray(new IPluginModelBase[values.size()]);
	}
	
	public IPluginModelBase findExtensionPointPlugin(String pointId) {
		IExtensionPoint point = getExtensionPoint(pointId);
		if (point == null) {
			return null;
		}
		IContributor contributor = point.getContributor();
		return PluginRegistry.findModel(contributor.getName());
	}
	
	private IExtensionPoint getExtensionPoint(String pointId) {
		return getRegistry().getExtensionPoint(pointId);
	}
	
	public boolean hasExtensionPoint(String pointId) {
		return getExtensionPoint(pointId) != null;
	}
	
	public IPluginExtensionPoint findExtensionPoint(String pointId) {
		IExtensionPoint extPoint = getExtensionPoint(pointId);
		if (extPoint != null) {
			IPluginModelBase model = PluginRegistry.findModel(extPoint.getContributor().getName());
			if (model != null) {
				IPluginExtensionPoint[] points = model.getPluginBase().getExtensionPoints();
				for (int i = 0; i < points.length; i++) {
					IPluginExtensionPoint point = points[i];
					if (points[i].getFullId().equals(pointId)) {
						return point;
					}
				}
			}
		}
		return null;
	}
	
	void createRegistry() {
		fRegistry  = RegistryFactory.createRegistry(fStrategy, fMasterKey, fUserKey);
	}
	
	public IPluginExtension[] findExtensionsForPlugin(String pluginId) {
		IPluginModelBase base = PluginRegistry.findModel(pluginId);
		IContributor contributor = fStrategy.createContributor(base);
		if (contributor == null)
			return new IPluginExtension[0];
		IExtension[] extensions = getRegistry().getExtensions(fStrategy.createContributor(base));
		ArrayList list = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			PluginExtension extension = new PluginExtension(extensions[i]);
			extension.setModel(getExtensionsModel(base));
			extension.setParent(base.getExtensions());
			list.add(extension);
		}
		return (IPluginExtension[]) list.toArray(new IPluginExtension[list.size()]);
	}
	
	public IPluginExtensionPoint[] findExtensionPointsForPlugin(String pluginId) {
		IPluginModelBase base = PluginRegistry.findModel(pluginId);
		IContributor contributor = fStrategy.createContributor(base);
		if (contributor == null) 
			return new IPluginExtensionPoint[0];
		IExtensionPoint[] extensions = getRegistry().getExtensionPoints(fStrategy.createContributor(base));
		ArrayList list = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			PluginExtensionPoint point = new PluginExtensionPoint(extensions[i]);
			point.setModel(getExtensionsModel(base));
			point.setParent(base.getExtensions());
			list.add(point);
		}
		return (IPluginExtensionPoint[]) list.toArray(new IPluginExtensionPoint[list.size()]);
	}
	
	private ISharedPluginModel getExtensionsModel(IPluginModelBase base) {
		if (base instanceof IBundlePluginModelBase) 
			return ((IBundlePluginModelBase)base).getExtensionsModel();
		return base;
	}
	
	public IExtension[] findExtensions(String extensionPointId) {
		IExtensionPoint point = getExtensionPoint(extensionPointId);
		if (point != null) 
			return point.getExtensions();
		ArrayList list = new ArrayList();
		IPluginModelBase[] bases = PluginRegistry.getActiveModels();
		for (int i = 0; i < bases.length; i++) {
			IContributor contributor = fStrategy.createContributor(bases[i]);
			if (contributor == null)
				continue;
			IExtension[] extensions = getRegistry().getExtensions(contributor);
			for (int j = 0; j < extensions.length; j++) {
				if (extensions[j].getExtensionPointUniqueIdentifier().equals(extensionPointId))
					list.add(extensions[j]);
			}
		}
		return (IExtension[]) list.toArray(new IExtension[list.size()]);
	}
	
	public void addListener(IRegistryChangeListener listener) {
		fRegistry.addRegistryChangeListener(listener);
	}
	
	public void removeListener(IRegistryChangeListener listener) {
		fRegistry.removeRegistryChangeListener(listener);
	}

}
