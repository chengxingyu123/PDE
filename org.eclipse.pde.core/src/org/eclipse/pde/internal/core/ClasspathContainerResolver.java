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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ClasspathContainerResolver {

	public static IClasspathEntry[] resolve(IPluginModelBase model) {
		ArrayList result = new ArrayList();
		resolve(model, result, null);
		return (IClasspathEntry[]) result.toArray(new IClasspathEntry[result.size()]);
	}
	
	private static void resolve(
			IPluginModelBase model,
			ArrayList result,
			IProgressMonitor monitor) {
		
		BundleDescription desc = model.getBundleDescription();
		if (desc == null)
			return;
		
		HashSet added = new HashSet();

		HostSpecification hostSpec = desc.getHost();
		BundleDescription host = (hostSpec != null) ? addHost(hostSpec, result, added) : null;
		
		BundleSpecification[] required = desc.getRequiredBundles();
		for (int i = 0; i < required.length; i++) {
			addRequiredBundle(required[i], required[i].isExported(), result, added);
		}
		
		if (host != null)
			addHostDependencies(host, result, added);
		
		addImportedPackages(desc, host, result, added);
		
		addImplicitDependencies();
		
		addExtraClasspathEntries();
	}
	
	private static BundleDescription addHost(HostSpecification hostSpec, ArrayList result, Set added) {
		BundleDescription[] hosts = hostSpec.getHosts();
		if (hosts.length == 0) 
			return null;
		
		BundleDescription host = hosts[0];
		if (added.add(host.getSymbolicName()))
			addClasspathEntries(host, result);
		return host;
	}
	
	private static void addHostDependencies(BundleDescription host, ArrayList result, Set added) {
		boolean inWorkspace = inWorkspace(host);
		BundleSpecification[] required = host.getRequiredBundles();
		for (int i = 0; i < required.length; i++) {
			if (!inWorkspace || !required[i].isExported()) {
				addRequiredBundle(required[i], false, result, added);
			}
		}
	}
	
	private static void addRequiredBundle(BundleSpecification spec, boolean exported, ArrayList result, Set added) {
		if (!added.add(spec.getName()))
			return;
		
		BaseDescription supplier = spec.getSupplier();
		if (supplier instanceof BundleDescription) {
			BundleDescription desc = (BundleDescription)supplier;
			addClasspathEntries(desc, result);
			BundleSpecification[] required = desc.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				if (required[i].isExported())
					addRequiredBundle(required[i], exported, result, added);
			}
		}
	}
	
	private static void addImportedPackages(BundleDescription desc, BundleDescription host, ArrayList result, HashSet added) {
		
	}
	
	private static void addExtraClasspathEntries() {
	}

	private static void addImplicitDependencies() {
	}
	
	private static void addClasspathEntries(BundleDescription desc, ArrayList result) {
	}
	
	private static boolean inWorkspace(BundleDescription desc) {
		IPath path = new Path(desc.getLocation());
		return PDECore.getWorkspace().getRoot().findMember(path) != null;
	}


}
