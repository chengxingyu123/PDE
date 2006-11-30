/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.help;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;

public class GetContextHelpOperation implements IRunnableWithProgress {

	private static final String F_CONTEXTS_EXT = "org.eclipse.help.contexts"; //$NON-NLS-1$
	
	private IFile fBaseFile;
	private ArrayList fContexts = new ArrayList();
	
	public GetContextHelpOperation(ISelection selection) {
		Object object = ((IStructuredSelection)selection).getFirstElement();
		if (object instanceof IProject) {
			fBaseFile = ((IProject)object).getFile("plugin.xml"); //$NON-NLS-1$
			if (!fBaseFile.exists())
				fBaseFile = ((IProject)object).getFile("fragment.xml"); //$NON-NLS-1$
		}
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (fBaseFile == null || !fBaseFile.exists())
			return;
		PDEModelUtility.modifyModel(new ModelModification(fBaseFile) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IPluginModelBase))
					return;
				findContextsFiles(monitor, (IPluginModelBase)model);
			}
		}, monitor);
	}

	private void findContextsFiles(IProgressMonitor monitor, IPluginModelBase pluginModel) {
		IPluginExtension[] extensions = pluginModel.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length && !monitor.isCanceled(); i++) {
			if (extensions[i].getPoint().equals(F_CONTEXTS_EXT)) {
				IPluginObject[] children = extensions[i].getChildren();
				for (int j = 0; j < children.length && !monitor.isCanceled(); j++) {
					if (children[j].getName().equals("contexts") //$NON-NLS-1$
							&& children[j] instanceof IPluginElement) {
						IPluginElement element = (IPluginElement) children[j];
						IPluginAttribute fileAttrib = element.getAttribute("file"); //$NON-NLS-1$
						if (fileAttrib != null) {
							String location = fileAttrib.getValue();
							IProject project = fBaseFile.getProject();
							IFile file = project.getFile(location);
							if (file != null && file.exists())
								fContexts.add(file);
						}
					}
				}
			}
		}
	}
	
	public IFile[] getContexts() {
		return (IFile[])fContexts.toArray(new IFile[fContexts.size()]);
	}
}
