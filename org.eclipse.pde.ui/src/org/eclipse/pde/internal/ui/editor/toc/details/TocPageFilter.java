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

package org.eclipse.pde.internal.ui.editor.toc.details;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.internal.ui.editor.toc.TocExtensionUtil;

public class TocPageFilter extends ViewerFilter
{	public boolean select(Viewer viewer, Object parent, Object element)
	{	if (element instanceof IFile)
		{	IPath path = ((IFile)element).getFullPath();

			return TocExtensionUtil.hasValidPageExtension(path);
		}

		if (element instanceof IProject && !((IProject)element).isOpen())
		{	return false;
		}

		if (element instanceof IContainer)
		{	try {
				IResource[] resources = ((IContainer)element).members();
				for (int i = 0; i < resources.length; i++){
					if (select(viewer, parent, resources[i]))
					{	return true;
					}
				}
			} catch (CoreException e) {
			}
		}

		return false;
	}
}