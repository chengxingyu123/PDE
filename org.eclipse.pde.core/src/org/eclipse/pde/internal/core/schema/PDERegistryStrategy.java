package org.eclipse.pde.internal.core.schema;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.IExtensionDeltaEvent;
import org.eclipse.pde.internal.core.IExtensionDeltaListener;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.plugin.AbstractExtensions;
import org.osgi.util.tracker.ServiceTracker;

public class PDERegistryStrategy extends RegistryStrategy{

	/**
	 * Tracker for the XML parser service
	 */
	private ServiceTracker xmlTracker = null;
	
	private Object fKey = null;
	
	private ModelListener fModelListener = null;
	private ExtensionListener fExtensionListener = null;
	private PDEExtensionRegistry fPDERegistry = null;
	
	
	class RegistryListener {
		IExtensionRegistry fRegistry;
		
		protected final void removeModels(IPluginModelBase[] bases, boolean onlyInactive) {
			for (int i = 0; i < bases.length; i++) {
				resetModel(bases[i]);
				if (onlyInactive && bases[i].isEnabled())
					continue;
				removeBundle(fRegistry, bases[i]);
			}
		}
		
		public void setRegistry(IExtensionRegistry registry) {
			fRegistry = registry;
		}
		
		private void resetModel(IPluginModelBase model) {
			IPluginBase base = model.getPluginBase();
			if (base instanceof BundlePluginBase) {
				IExtensions ext = ((BundlePluginBase)base).getExtensionsRoot();
				if (ext != null && ext instanceof AbstractExtensions) {
					((AbstractExtensions)ext).reset();
				}
			} else if (base instanceof AbstractExtensions){
				((AbstractExtensions)base).resetExtensions();
			}
		}
	}
	
	class ModelListener extends RegistryListener implements IPluginModelListener{
		
		public void modelsChanged(PluginModelDelta delta) {
			if (fRegistry == null)
				createRegistry();
			// can ignore removed models since the ModelEntries is empty
			ModelEntry[] entries = delta.getChangedEntries();
			for (int i = 0; i < entries.length; i++) {
				// remove all external models if there are any workspace models since they are considered 'activeModels'.  See ModelEntry.getActiveModels().
				removeModels(entries[i].getExternalModels(), !entries[i].hasWorkspaceModels());
				removeModels(entries[i].getWorkspaceModels(), true);
				addBundles(fRegistry, entries[i].getActiveModels());
			}
			entries = delta.getAddedEntries();
			for (int i = 0; i < entries.length; i++) 
				addBundles(fRegistry, entries[i].getActiveModels());
		}
		
	}
	
	class ExtensionListener extends RegistryListener implements IExtensionDeltaListener {
		
		public void extensionsChanged(IExtensionDeltaEvent event) {
			if (fRegistry == null)
				createRegistry();
			removeModels(event.getRemovedModels(), false);
			removeModels(event.getChangedModels(), false);
			addBundles(fRegistry, event.getChangedModels());
			addBundles(fRegistry, event.getAddedModels());
		}
		
	}
		
	public PDERegistryStrategy(File[] storageDirs, boolean[] cacheReadOnly, Object key, PDEExtensionRegistry registry) {
		super(storageDirs, cacheReadOnly);
		fKey = key;
		
		// Listen for model changes to register new bundles and unregister removed bundles
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		manager.addPluginModelListener(fModelListener = new ModelListener());
		manager.addExtensionDeltaListener(fExtensionListener = new ExtensionListener());
		
		fPDERegistry = registry;
	}
	
	public void onStart(IExtensionRegistry registry) {
		super.onStart(registry);
		if (!(registry instanceof ExtensionRegistry))
			return;
		
		fModelListener.setRegistry(registry);
		fExtensionListener.setRegistry(registry);
		
		if (!((ExtensionRegistry) registry).filledFromCache()) {
			processBundles(registry);
		}
	}
	
	public void onStop(IExtensionRegistry registry) {
		super.onStop(registry);
		dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.spi.RegistryStrategy#getXMLParser()
	 */
	public SAXParserFactory getXMLParser() {
		if (xmlTracker == null) {
			xmlTracker = new ServiceTracker(PDECore.getDefault().getBundleContext(), SAXParserFactory.class.getName(), null);
			xmlTracker.open();
		}
		return (SAXParserFactory) xmlTracker.getService();
	}
	
	private void processBundles(IExtensionRegistry registry) {
		addBundles(registry, PluginRegistry.getActiveModels());
		// TODO: think about saving information of external plug-ins.  Then add workspace plug-ins
	}
	
	private void addBundles(IExtensionRegistry registry, IPluginModelBase[] bases) {
		for (int i = 0; i < bases.length; i++)
			addBundle(registry, bases[i]);
	}
	
	private void addBundle(IExtensionRegistry registry, IPluginModelBase base) {
		// make sure model has BundleDescription
		BundleDescription desc =  base.getBundleDescription();
		if (desc == null)
			return;
		
		// make sure model is a singleton.  If it is a fragment, make sure host is singleton
		HostSpecification hostDesc = desc.getHost();
		if (hostDesc != null && hostDesc.getBundle() != null && !hostDesc.getBundle().isSingleton() ||
				hostDesc == null && !desc.isSingleton())
			return;
		
		String id = Long.toString(desc.getBundleId());
		if (((ExtensionRegistry)registry).hasContribution(id)) 
			return;
		
		File input = getFile(base);
		if (input == null)
			return;
		long timeStamp = input.lastModified();
		timeStamp += desc.getBundleId();
		InputStream is = getInputStream(input, base);
		if (is == null)
			return;
		registry.addContribution(
				new BufferedInputStream(is),
				createContributor(base), 
				true, 
				input.getPath(), 
				null,
				fKey);
	}
	
	private void removeBundle(IExtensionRegistry registry, IPluginModelBase base) {
		BundleDescription desc =  base.getBundleDescription();
		if (desc == null)
			return;
		String id = Long.toString(desc.getBundleId());
		if (((ExtensionRegistry)registry).hasContribution(id))
			((ExtensionRegistry)registry).remove(id);
	}
	
	private File getFile(IPluginModelBase base) {
		String loc = base.getInstallLocation();
		File file = new File(loc);
		if (!file.exists())
			return null;
		if (file.isFile())
			return file;
		String fileName = (base.isFragmentModel()) ? "fragment.xml" : "plugin.xml";
		File inputFile = new File(file, fileName);
		return (inputFile.exists()) ? inputFile : null;
	}
	
	private InputStream getInputStream(File file, IPluginModelBase base) {
		if (file.getName().endsWith(".jar")) {
			try {
				ZipFile jfile = new ZipFile(file, ZipFile.OPEN_READ);
				String fileName = (base.isFragmentModel()) ? "fragment.xml" : "plugin.xml";
				ZipEntry entry = jfile.getEntry(fileName);
				if (entry != null)
					return jfile.getInputStream(entry);
			} catch (IOException e) {
			}
			return null;
		}
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
		}
		return null;
	}
	
	private IContributor createContributor(IPluginModelBase base) {
		BundleDescription desc = base.getBundleDescription();
		String name = desc.getSymbolicName();
		String id = Long.toString(desc.getBundleId());
		String hostName = null;
		String hostId = null;
		
		HostSpecification host = desc.getHost();
		if (host != null) {
			BundleDescription hostDesc = host.getBundle();
			hostName = hostDesc.getSymbolicName();
			hostId = Long.toString(hostDesc.getBundleId());
		}
		return new RegistryContributor(id, name, hostId, hostName);
	}
	
	public void dispose() {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		manager.removePluginModelListener(fModelListener);
		manager.removeExtensionDeltaListener(fExtensionListener);
	}
	
	private void createRegistry() {
		fPDERegistry.createRegistry();
	}

}
