/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.osgi.framework.Version;

public class PluginManager implements IAdaptable {
	
	private static final String OSGI_RUNTIME ="org.eclipse.osgi"; //$NON-NLS-1$
	
	private IModelProviderListener fProviderListener;
	private ArrayList fListeners;
	
	private ExternalModelManager fExternalManager;
	private WorkspaceModelManager fWorkspaceManager;
	//private SearchablePluginsManager fSearchablePluginsManager;
	
	private State fCurrentState;
	private Map fEntries;

	public PluginManager() {
		fProviderListener = new IModelProviderListener() {
			public void modelsChanged(IModelProviderEvent e) {
				handleModelsChanged(e);
			}
		};
		fListeners = new ArrayList();
		//fSearchablePluginsManager = new SearchablePluginsManager(this);
	}
	
	public String getTargetVersion() {
		BundleDescription desc = fCurrentState.getBundle(OSGI_RUNTIME, null); 
		if (desc == null) 
			return ICoreConstants.TARGET21;
		
		Version version = desc.getVersion();
		if (version != null && version.getMajor() == 3 && version.getMinor() == 0)
			return ICoreConstants.TARGET30;	
		
		return ICoreConstants.TARGET31;	
	}

	public void addPluginModelListener(IPluginModelListener listener) {
		if (!fListeners.contains(listener))
			fListeners.add(listener);
	}
	
	public void removePluginModelListener(IPluginModelListener listener) {
		if (fListeners.contains(listener))
			fListeners.remove(listener);
	}
	
	private void handleModelsChanged(IModelProviderEvent e) {
		PluginModelDelta delta = new PluginModelDelta();
		ArrayList changedPlugins = new ArrayList();
		//boolean javaSearchAffected=false;

		if ((e.getEventTypes() & IModelProviderEvent.MODELS_REMOVED) != 0) {
		}
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_ADDED) != 0) {
		}
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_CHANGED) != 0) {
		}
		
		if (changedPlugins.size() > 0)
			fExternalManager.getState().resolveState(true);
		//if (javaSearchAffected)
			//fSearchablePluginsManager.updateClasspathContainer();
		fireDelta(delta);
	}
	
	/*
	 * Allow access to the table only through this getter.
	 * It always calls initialize to make sure the table is initialized.
	 * If more than one thread tries to read the table at the same time,
	 *  and the table is not initialized yet, thread2 would wait. 
	 *  This way there are no partial reads.
	 */
	protected Map getEntryTable() {
		initializeTable();
		return fEntries;
	}

	/*
	 * This method must be synchronized so that only one thread
	 * initializes the table, and the rest would block until
	 * the table is initialized.
	 * 
	 */
	private synchronized void initializeTable() {
		if (fEntries != null) return;
		fEntries = Collections.synchronizedMap(new TreeMap());
		//fSearchablePluginsManager.initialize();
	}

	protected void updateBundleDescription(IPluginModelBase model) {
		BundleDescription description = model.getBundleDescription();
		if (description == null)
			return;
		PDEState state = fExternalManager.getState();
		state.removeBundleDescription(description);
		
		BundleDescription newDesc = state.addBundle(model);
		model.setBundleDescription(newDesc);
	}
	
	private void fireDelta(PluginModelDelta delta) {
		Object [] entries = fListeners.toArray();
		for (int i=0; i<entries.length; i++) {
			((IPluginModelListener)entries[i]).modelsChanged(delta);
		}
	}

	public void connect(WorkspaceModelManager wm, ExternalModelManager em) {
		fExternalManager = em;
		fWorkspaceManager = wm;
		fExternalManager.addModelProviderListener(fProviderListener);
		fWorkspaceManager.addModelProviderListener(fProviderListener);
	}
	
	public void shutdown() {
		if (fWorkspaceManager != null)	
			fWorkspaceManager.removeModelProviderListener(fProviderListener);
		if (fExternalManager != null)
			fExternalManager.removeModelProviderListener(fProviderListener);
		//fSearchablePluginsManager.shutdown();
	}
	
	public void setInJavaSearch(ModelEntry [] entries, boolean value, IProgressMonitor monitor) throws CoreException {
		PluginModelDelta delta = new PluginModelDelta();
		for (int i=0; i<entries.length; i++) {
			ModelEntry entry = entries[i];
			if (entry.isInJavaSearch()!=value) {
				entry.setInJavaSearch(value);
				delta.addEntry(entry, PluginModelDelta.CHANGED);
			}
		}
		if (delta.getKind()!=0) {
			//fSearchablePluginsManager.persistStates( monitor);
			fireDelta(delta);
		}
	}
	 
	public Object getAdapter(Class adapter) {
		return null;
	}
	
}
