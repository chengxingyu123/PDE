/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.importing.provisional;

import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A bundle importer delegate is contributed by a bundle importer extension and is capable
 * of importing projects into the workspace from a repository based on bundle manifest entries.
 * <p>
 * Following is an example extension:
 * <pre>
 * &lt;extension point=&quot;org.eclipse.pde.core.bundleImporters&quot;&gt;
 *  &lt;importer
 *   id=&quot;com.example.ExampleIdentifier&quot;
 *   class=&quot;com.example.ExampleBundleImporterDelegate&quot;&gt;
 *  &lt;/importer&gt;
 * &lt;/extension&gt;
 * </pre>
 * </p>
 * <p>
 * Clients contributing bundle importer extensions are intended to implement this interface.
 * </p>
 * <p>
 * <strong>EXPERIMENTAL</strong>. This interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same. Please do not use this API without
 * consulting with the PDE team.
 * </p>
 * @since 3.6
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IBundleImporterDelegate {

	/**
	 * Returns an array of objects describing how each given bundle (manifest headers and values)
	 * can be imported into a workspace project. A <code>null</code> entry in the returned array
	 * indicates the corresponding bundle cannot be imported by this delegate.
	 * 
	 * @param manifests array of maps containing manifest headers and values of the associated bundles
	 * @return array of bundle import descriptions that may contain <code>null</code> entries
	 */
	public BundleImportDescription[] validateImport(Map[] manifests);

	/**
	 * Imports bundles into the workspace creating a project for each import description.
	 * Reports progress to the given monitor, if not <code>null</code>.
	 * 
	 * @param descriptions description of bundles to import
	 * @param monitor progress monitor or <code>null</code>
	 * @return collection of projects created in the workspace or <code>null</code> if none
	 * @throws CoreException if unable to import projects
	 */
	public IProject[] performImport(BundleImportDescription[] descriptions, IProgressMonitor monitor) throws CoreException;
}
