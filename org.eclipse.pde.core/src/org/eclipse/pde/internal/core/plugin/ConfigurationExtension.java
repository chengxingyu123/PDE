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
package org.eclipse.pde.internal.core.plugin;

import java.util.ArrayList;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class ConfigurationExtension extends PluginExtension implements IPluginExtension {
	private static final long serialVersionUID = 1L;
	private IExtension fExtension = null;

	public ConfigurationExtension(IExtension extension) {
		fExtension = extension;
		// TODO: can possibly make this a lazy evaluation.
		setModel();
	}
	
	protected ArrayList getChildrenList() {
		if (fChildren == null) {
			fChildren = new ArrayList();
			if (fExtension != null) {
				IConfigurationElement[] elements = fExtension.getConfigurationElements();
				for (int i = 0; i < elements.length;i++) {
					fChildren.add(new ConfigurationElement(elements[i]));
				}
			}
			// release the reference to the Extension handle since we are done with its information.
			// CAN't RELEASE WITHOUT FILLING IN OTHER DATA, FOR EXAMPLE getPoint
//			fExtension = null;
		}
		return fChildren;
	}

	public String getPoint() {
		if (fPoint == null)
			fPoint = fExtension.getExtensionPointUniqueIdentifier(); 
		return fPoint;
	}
	
	private void setModel() {
		setModel(PluginRegistry.findModel(fExtension.getContributor().getName()));
	}
	
	public ISharedPluginModel getModel() {
		setModel();
		return super.getModel();
	}
	
	public IPluginModelBase getPluginModel() {
		setModel();
		return super.getPluginModel();
	}
	
	public String getName() {
		if (fName == null) {
			fName = fExtension.getLabel();
		}
		return fName;
	}
	
	protected void initialize() {
		// lazy initialization
	}
	
	public String getId() {
		if (fID == null) {
			fID = fExtension.getUniqueIdentifier();
			if (fID != null) {
				String pluginId = getPluginBase().getId();
				if (fID.startsWith(pluginId)) {
					String sub = fID.substring(pluginId.length());
					if (sub.lastIndexOf('.') == 0)
						fID = sub.substring(1);
				}
			}
		}
		return fID;
	}

}
