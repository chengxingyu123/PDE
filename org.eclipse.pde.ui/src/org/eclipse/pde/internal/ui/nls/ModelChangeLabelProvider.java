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
package org.eclipse.pde.internal.ui.nls;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;

public class ModelChangeLabelProvider extends SharedLabelProvider {
	
	private ModelChangeTable fModelChangeTable;
	private Image manifestImage;
	private Image xmlImage;
	
	public ModelChangeLabelProvider(ModelChangeTable changeTable) {
		fModelChangeTable = changeTable;
		xmlImage = PDEPluginImages.DESC_PLUGIN_MF_OBJ.createImage();
		manifestImage = PDEPluginImages.DESC_PAGE_OBJ.createImage();
	}
	
	public String getText(Object obj) {
		if (obj instanceof ModelChange)
			return getObjectText(((ModelChange) obj));
		if (obj instanceof ModelChangeFile)
			return getObjectText((ModelChangeFile)obj);
		return super.getText(obj);
	}
	
	private String getObjectText(ModelChangeFile pair) {
		StringBuffer text = new StringBuffer(pair.getFile().getName());
		int count = pair.getNumChanges();
		text.append(" [");
		text.append(count);
		text.append(" instance");
		if (count != 1) text.append("s");
			text.append("]");

		return text.toString();
	}
	
	private String getObjectText(ModelChange model) {
		IPluginBase pluginBase = model.getParentModel().getPluginBase();
		String name =
			PDEPlugin.isFullNameModeEnabled()
				? pluginBase.getTranslatedName()
				: pluginBase.getId();
		name = name != null ? name : "";
		if (name == null) name = pluginBase.getName();
		String version = pluginBase.getVersion();

		StringBuffer text = new StringBuffer();
		text.append(fModelChangeTable.getNumberOfChangesInModel(model.getParentModel()));
		text.append(" in ");
		text.append(name);
		
		if (version != null && version.length() > 0) {
			text.append(" (");
			text.append(pluginBase.getVersion());
			text.append(")");
		}
		if (pluginBase.getModel() != null && !pluginBase.getModel().isInSync()) {
			text.append(" ");
			text.append(PDEUIMessages.PluginModelManager_outOfSync);
		}
		return text.toString();
	}

	public Image getImage(Object obj) {
		if (obj instanceof ModelChange) {
			IPluginModelBase model = ((ModelChange)obj).getParentModel();
			if (model instanceof IPluginModel)
				return getObjectImage(((IPluginModel) model).getPlugin());
			if (model instanceof IFragmentModel)
				return getObjectImage(((IFragmentModel) model).getFragment());
		}
		if (obj instanceof ModelChangeFile)
			return getObjectImage((ModelChangeFile)obj);
		return super.getImage(obj);
	}

	private Image getObjectImage(ModelChangeFile file) {
		String type = file.getFile().getFileExtension();
		if ("xml".equalsIgnoreCase(type))
			return xmlImage;
		if ("MF".equalsIgnoreCase(type))
			return manifestImage;
		return null;
	}

	private Image getObjectImage(IPlugin plugin) {
		IPluginModelBase model = plugin.getPluginModel();
		int flags = getModelFlags(model);
		ImageDescriptor desc = PDEPluginImages.DESC_PLUGIN_OBJ;
		return get(desc, flags);
	}

	private Image getObjectImage(IFragment fragment) {
		IPluginModelBase model = fragment.getPluginModel();
		int flags = getModelFlags(model);
		ImageDescriptor desc = PDEPluginImages.DESC_FRAGMENT_OBJ;
		return get(desc, flags);
	}
	
	private int getModelFlags(IPluginModelBase model) {
		int flags = 0;
		if (!(model.isLoaded() && model.isInSync()))
			flags = F_ERROR;
		IResource resource = model.getUnderlyingResource();
		if (resource == null) {
			flags |= F_EXTERNAL;
		} else {
			IProject project = resource.getProject();
			try {
				if (WorkspaceModelManager.isBinaryPluginProject(project)) {
					String property = project.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY);
					if (property != null)
						flags |= F_BINARY;
				}
			} catch (CoreException e) {
			}
		}
		return flags;
	}
	
	public void setChangeSet(ModelChangeTable changeSet) {
		fModelChangeTable = changeSet;
	}
	
	public void dispose() {
		if (manifestImage != null)
			manifestImage.dispose();
		if (xmlImage != null)
			xmlImage.dispose();
		super.dispose();
	}
}
