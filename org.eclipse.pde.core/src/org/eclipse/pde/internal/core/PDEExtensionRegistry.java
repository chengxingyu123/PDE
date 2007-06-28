package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.PluginExtension;
import org.eclipse.pde.internal.core.plugin.PluginExtensionPoint;

public class PDEExtensionRegistry {
	
	private Object fMasterKey = new Object();
	private Object fUserKey = new Object();
	
	private IExtensionRegistry fRegistry = null;
	private PDERegistryStrategy fStrategy = null;
	
	public PDEExtensionRegistry() {
		if (fStrategy == null) {
			File targetDirectory = PDECore.getDefault().getModelManager().getState().getTargetDirectory();
			// create the strategy without creating registry.  That way we create the registry at the last possible moment.
			// This way we can listen to events in PDE without creating the registry until we need it.
			fStrategy = new PDERegistryStrategy(new File[] {targetDirectory}, new boolean[] {false}, fMasterKey, this);
		}
	}
	
	public void stop() {
		if (fRegistry != null)
			fRegistry.stop(fMasterKey);
	}
	
	private IExtensionRegistry getRegistry() {
		if (fRegistry == null)
			createRegistry();
		return fRegistry;
	}
	
	public void targetReloaded() {
		fStrategy.dispose();
		fRegistry = null;
	}
	
	public IPluginModelBase[] findExtensionPlugins(String pointId) {
		IExtensionPoint point = getExtensionPoint(pointId);
		if (point == null) {
			return new IPluginModelBase[0];
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
		BundleDescription desc = (base != null) ? base.getBundleDescription() : null;
		if (desc == null)
			return new IPluginExtension[0];
		IExtension[] extensions = ((ExtensionRegistry)getRegistry()).getExtensionsFrom(Long.toString(desc.getBundleId()));
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
		BundleDescription desc = (base != null) ? base.getBundleDescription() : null;
		if (desc == null)
			return new IPluginExtensionPoint[0];
		IExtensionPoint[] extensions = ((ExtensionRegistry)getRegistry()).getExtensionPointsFrom(Long.toString(desc.getBundleId()));
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

}
