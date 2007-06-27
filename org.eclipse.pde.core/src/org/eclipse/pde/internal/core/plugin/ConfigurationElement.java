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
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class ConfigurationElement extends PluginElement implements IPluginElement {
	
	private static final long serialVersionUID = 1L;
	private IConfigurationElement fElement = null;
	
	public ConfigurationElement(IConfigurationElement config) {
		fElement = config;
		// TODO: can possibly make this a lazy evaluation.
		setModel();
	}
	
	protected ConfigurationElement(ConfigurationElement element) {
		super(element);
		fElement = element.fElement;
		setModel();
	}

	public IPluginElement createCopy() {
		return new ConfigurationElement(this);
	}

	public IPluginAttribute getAttribute(String name) {
		if (fAttributes == null)
			return createAttribute(name, fElement.getAttribute(name));
		return (IPluginAttribute)fAttributes.get(name);
	}

	public int getAttributeCount() {
		return fElement.getAttributeNames().length;
	}

	public String getText() {
		if (fText == null)
			fText = fElement.getValue();
		return fText;
	}
	
	protected Hashtable getAttributeMap() {
		if (fAttributes == null) {
			fAttributes = new Hashtable();
			String[] names = fElement.getAttributeNames();
			for (int i = 0; i < names.length; i++) {
				IPluginAttribute attr = createAttribute(names[i], fElement.getAttribute(names[i]));
				if (attr != null)
					fAttributes.put(names[i], attr);
			}
		}
		return fAttributes;
	}
	
	private IPluginAttribute createAttribute(String name, String value) {
		if (name == null || value == null)
			return null;
		try {
			IPluginAttribute attr = getPluginModel().getFactory().createAttribute(this);
			if (attr instanceof PluginAttribute)
				((PluginAttribute)attr).load(name, value);
			else {
				attr.setName(name);
				attr.setValue(value);
			}
			return attr;
		} catch (CoreException e) {
		}
		return null;
	}
	
	private void setModel() {
		setModel(PluginRegistry.findModel(fElement.getContributor().getName()));
	}
	
	protected ArrayList getChildrenList() {
		if (fChildren == null) {
			fChildren = new ArrayList();
			if (fElement != null) {
				IConfigurationElement[] elements = fElement.getChildren();
				for (int i = 0; i < elements.length; i++) 
					fChildren.add(new ConfigurationElement(elements[i]));
			}
		}
		return fChildren;
	}
	
	public String getName() {
		if (fName == null) {
			fName = fElement.getName();
		}
		return fName;
	}
	
	protected void initialize() {
		// lazy initialization
	}

}
