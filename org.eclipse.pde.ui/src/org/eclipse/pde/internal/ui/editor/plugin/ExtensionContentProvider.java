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
package org.eclipse.pde.internal.ui.editor.plugin;


import java.util.ArrayList;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;

public class ExtensionContentProvider extends DefaultContentProvider
implements
ITreeContentProvider {
	
	/**
	 * @param extensionsSection
	 */
	public ExtensionContentProvider() {
	}
	
	public Object[] getChildren(Object parent) {
		Object[] children = null;
		if (parent instanceof IPluginBase)
			children = ((IPluginBase) parent).getExtensions();
		else if (parent instanceof IPluginExtension) {
			children = ((IPluginExtension) parent).getChildren();
		} else if (parent instanceof IPluginElement) {
			children = ((IPluginElement) parent).getChildren();
		} else if (parent instanceof IProduct) {
			IProductPlugin[] productPlugins = ((IProduct) parent).getPlugins();
			ArrayList plugins = new ArrayList(productPlugins.length);
			for (int i = 0; i < productPlugins.length; i++) {
				IProductPlugin productPlugin = productPlugins[i];
				Object baseModel = PDECore.getDefault().getModelManager().findModel(productPlugin.getId());
				if (baseModel instanceof IPluginModel) {
					IPlugin plugin = ((IPluginModel)baseModel).getPlugin();
					if (plugin != null)
						plugins.add(plugin);
				}
			}
			
			children = plugins.toArray();
		}
		
		if (children == null)
			children = new Object[0];
		return children;
	}
	public boolean hasChildren(Object parent) {
		return getChildren(parent).length > 0;
	}
	public Object getParent(Object child) {
		if (child instanceof IPluginExtension) 
			return ((IPluginExtension) child).getPluginModel();

		if (child instanceof IPluginObject)
			return ((IPluginObject) child).getParent();
		return null;
	}
	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}
}
