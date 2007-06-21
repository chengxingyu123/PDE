package org.eclipse.pde.internal.core.schema;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
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
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.osgi.util.tracker.ServiceTracker;

public class PDERegistryStrategy extends RegistryStrategy{

	/**
	 * Tracker for the XML parser service
	 */
	private ServiceTracker xmlTracker = null;
	
	private HashSet contributedIds = new HashSet(); 
	
	private Object fKey = null;
		
	public PDERegistryStrategy(File[] storageDirs, boolean[] cacheReadOnly, Object key) {
		super(storageDirs, cacheReadOnly);
		fKey = key;
	}
	
	public void onStart(IExtensionRegistry registry) {
		super.onStart(registry);
		if (!(registry instanceof ExtensionRegistry))
			return;
		// Possibly add a listener to listen for models being added/removed
		if (!((ExtensionRegistry) registry).filledFromCache()) {
			processBundles(registry);
		}
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
		IPluginModelBase[] models = PluginRegistry.getAllModels();
		for (int i = 0; i < models.length; i++) {
			addBundle(registry, models[i]);
		}
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
		if (contributedIds.contains(id)) 
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
//				findResourceBundle(IPluginModelBase base),
				null,
				fKey);
		
		contributedIds.add(id);
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
	
//	WORK DONE TO TRY TO SUPPORT NL TRANSLATION STRINGS IN EXTENSIONS
//	private ResourceBundle findResourceBundle(IPluginModelBase base) {
//		String localization = null;
//		if (base instanceof IBundlePluginModelBase) {
//			IBundlePluginModelBase bbase = (IBundlePluginModelBase)base;
//			String localization = bbase.getBundleModel().getBundle().getHeader(Constants.BUNDLE_LOCALIZATION);
//			if (localization == null)
//				localization = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
//		} else 
//			localization = "plugin";
//
//		String loc = base.getInstallLocation();
//		File file = new File(loc);
//		if (file.isFile())
//		
//		
//		String[] varients = buildNLVariants(TargetPlatform.getNL());
//		for (int i = 0; i < varients.length; i++) {
//			
//		}
//	}
	
//	private String[] buildNLVariants(String nl) {
//		ArrayList result = new ArrayList();
//		int lastSeparator;
//		while ((lastSeparator = nl.lastIndexOf('_')) != -1) {
//			result.add(nl);
//			if (lastSeparator != -1) {
//				nl = nl.substring(0, lastSeparator);
//			}
//		}
//		result.add(nl);
//		// always add the default locale string
//		result.add(""); //$NON-NLS-1$
//		return (String[]) result.toArray(new String[result.size()]);
//	}
}
