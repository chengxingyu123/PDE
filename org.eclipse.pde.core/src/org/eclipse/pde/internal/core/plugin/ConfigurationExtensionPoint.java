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

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class ConfigurationExtensionPoint extends PluginExtensionPoint {
	
	private static final long serialVersionUID = 1L;
	private IExtensionPoint fPoint = null;
	
	public ConfigurationExtensionPoint(IExtensionPoint point) {
		fPoint = point;
		setModel();
	}
	
	public String getFullId() {
		return fPoint.getUniqueIdentifier();
	}
	
	public String getSchema() {
		if (fSchema == null)
			fSchema = fPoint.getSchemaReference();
		return fSchema;
	}
	
	public String getName() {
		if (fName == null) 
			fName = fPoint.getLabel();
		return fName;
	}
	
	public String getId() {
		if (fID == null) {
			fID = fPoint.getUniqueIdentifier();
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
	
	private void setModel() {
		setModel(PluginRegistry.findModel(fPoint.getContributor().getName()));
	}

}
