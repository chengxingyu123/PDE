/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;

public class BundleValidationOperation implements IWorkspaceRunnable {
	
	private static StateObjectFactory FACTORY;
	
	private IPluginModelBase[] fModels;
	private Dictionary[] fProperties;
	private State fState;
	
	public BundleValidationOperation(IPluginModelBase[] models) {
		this(models, new Dictionary[] { getDefaultEnvironment()});
	}
	
	public BundleValidationOperation(IPluginModelBase[] models, Dictionary[] properties) {
		fModels = models;
		fProperties = properties;
	}
	
	public static Dictionary getDefaultEnvironment() {
		Dictionary result = new Hashtable();
		result.put ("osgi.os", TargetPlatform.getOS()); //$NON-NLS-1$
		result.put ("osgi.ws", TargetPlatform.getWS()); //$NON-NLS-1$
		result.put ("osgi.nl", TargetPlatform.getNL()); //$NON-NLS-1$
		result.put ("osgi.arch", TargetPlatform.getOSArch()); //$NON-NLS-1$
		result.put("osgi.resolveOptional", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		result.put("osgi.resolverMode", "development"); //$NON-NLS-1$ //$NON-NLS-2$
		return result;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		if (FACTORY == null)
			FACTORY = Platform.getPlatformAdmin().getFactory();
		monitor.beginTask("", fModels.length + 1); //$NON-NLS-1$
		fState = FACTORY.createState(true);
		for (int i = 0; i < fModels.length; i++) {
			BundleDescription bundle = fModels[i].getBundleDescription();
			if (bundle != null)
				fState.addBundle(FACTORY.createBundleDescription(bundle));
			monitor.worked(1);
		}
		fState.setPlatformProperties(fProperties);
		fState.resolve(false);
		monitor.done();
	}
	
	public Map getResolverErrors() {
		Map map = new HashMap();
		BundleDescription[] bundles = fState.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (!bundles[i].isResolved()) {
				map.put(bundles[i], fState.getResolverErrors(bundles[i]));
			}
		}
		return map;
	}
	
	public State getState() {
		return fState;
	}
	
	public boolean hasErrors() {
		return fState.getHighestBundleId() > -1 
				&& fState.getBundles().length > fState.getResolvedBundles().length;
	}
		
}