package org.eclipse.pde.internal.ui.nls;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.bundle.Bundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.elements.DefaultElement;
import org.osgi.framework.Constants;


public class ModelChange extends DefaultElement {
	
	private static final String DEFAULT_LOCALIZATION_VALUE = "plugin";
	private static final String LOCALIZATION_FILE_SUFFIX = ".properties";
	
	private Hashtable fileChanges = new Hashtable();
	private int fTotalChanges = 0;
	private IPluginModelBase fParent;
	private boolean fSelected;
	private IFile fPropertiesFile;
	private Properties fProperties;
	
	public ModelChange(IPluginModelBase parent, boolean selected) {
		fParent = parent;
		fSelected = selected;
	}
	
	public void addChange(IFile file, ModelChangeElement change) {
		if (change == null) return;
		HashSet changes;
		if (fileChanges.containsKey(file))
			changes = (HashSet)fileChanges.get(file);
		else {
			changes = new HashSet();
			fileChanges.put(file, changes);
		}
		changes.add(change);
		fTotalChanges++;
	}
	
	public Hashtable getChanges() {
		return fileChanges;
	}
	
	public Set getChangeFiles() {
		return fileChanges.keySet();
	}
	
	public IFile getPropertiesFile() {
		if (fPropertiesFile == null) {
			IProject project = fParent.getUnderlyingResource().getProject();
			if (fParent instanceof IBundlePluginModelBase && modelLoaded(fParent)) {
				IBundlePluginModelBase bundleModel = (IBundlePluginModelBase)fParent;
				Bundle bundle = (Bundle)bundleModel.getBundleModel().getBundle();
				if (bundle != null) {
					String localization = bundle.getHeader(Constants.BUNDLE_LOCALIZATION);
					if (localization == null)
						localization = DEFAULT_LOCALIZATION_VALUE;
					IResource propertiesFile = project.findMember(localization + LOCALIZATION_FILE_SUFFIX);
					if (propertiesFile != null && propertiesFile instanceof IFile)
						fPropertiesFile = (IFile)propertiesFile;
					else
						fPropertiesFile = project.getFile(DEFAULT_LOCALIZATION_VALUE + LOCALIZATION_FILE_SUFFIX);
				}
			} else
				fPropertiesFile = project.getFile(DEFAULT_LOCALIZATION_VALUE + LOCALIZATION_FILE_SUFFIX);
		}
		return fPropertiesFile;
	}
	
	public Properties getProperties() {
		if (fPropertiesFile == null)
			getPropertiesFile();
		if (fProperties == null) {
			try {
				fProperties = new Properties();
				fProperties.load(fPropertiesFile.getContents());
			} catch (CoreException e) {
			} catch (IOException e) {
			}
		}
		return fProperties;
	}
	
	public HashSet getChangesInFile(IFile file) {
		Object changes = (HashSet)fileChanges.get(file);
		return (changes != null) ? (HashSet)changes : null;
	}
	public int getNumberOfChangesInFile(IFile file) {
		HashSet changes = getChangesInFile(file);
		return (changes != null) ? changes.size() : 0;
	}
	
	public int getTotalNumberOfChanges() {
		return fTotalChanges;
	}
	public boolean isSelected() {
		return fSelected;
	}
	
	protected static boolean modelLoaded(IModel model) {
		try {
			model.load();
		} catch (CoreException e) {
		}
		return (model.isLoaded());
	}
	public IPluginModelBase getParentModel() {
		return fParent;
	}
}
