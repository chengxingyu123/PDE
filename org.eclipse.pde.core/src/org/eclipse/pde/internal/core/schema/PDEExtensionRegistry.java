package org.eclipse.pde.internal.core.schema;

import java.io.File;
import java.util.HashMap;

import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;

public class PDEExtensionRegistry {
	
	private Object fMasterKey = new Object();
	private Object fUserKey = new Object();
	
	private IExtensionRegistry fRegistry = null;
	private PDERegistryStrategy fStrategy = null;
	
	public void stop() {
		if (fRegistry != null)
			fRegistry.stop(fMasterKey);
	}
	
	private IExtensionRegistry getRegistry() {
		if (fRegistry == null) {
			long start = System.currentTimeMillis();
			File targetDirectory = PDECore.getDefault().getModelManager().getState().getTargetDirectory();
			fStrategy = new PDERegistryStrategy(new File[] {targetDirectory}, new boolean[] {false}, fMasterKey);
			fRegistry  = RegistryFactory.createRegistry(fStrategy, fMasterKey, fUserKey);
			System.out.println("Time to create registry: " + (System.currentTimeMillis() - start));
		}
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
		IContributor contibutor = point.getContributor();
		return PluginRegistry.findModel(contibutor.getName());
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

}
