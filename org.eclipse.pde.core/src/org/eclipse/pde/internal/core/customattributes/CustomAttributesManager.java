/*******************************************************************************
 * Copyright (c) 2009 Anyware Technologies Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - initial API and implementation
 *******************************************************************************/
 package org.eclipse.pde.internal.core.customattributes;

import java.util.HashMap;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.dynamichelpers.*;
import org.eclipse.pde.core.ICustomAttributeValidator;

public class CustomAttributesManager implements IExtensionChangeHandler {
	private static final String POINT_ID = "org.eclipse.pde.core.customExtensionPointAttributes"; //$NON-NLS-1$

	/** The tag name of the attribute expression (value: <code>attribute</code>) */
	public static final String ATTRIBUTE = "attribute"; //$NON-NLS-1$
	/** The tag name of the category expression (value: <code>category</code>) */
	public static final String CATEGORY = "category"; //$NON-NLS-1$

	public static final String ATT_ID = "id"; //$NON-NLS-1$
	public static final String ATT_NAME = "name"; //$NON-NLS-1$
	public static final String ATT_VALIDATOR = "validator"; //$NON-NLS-1$

	private static CustomAttributesManager instance;

	private HashMap fCustomAttributes;
	private HashMap fCustomAttributesCategories;

	private CustomAttributesManager() {
		initCache();
		registerExtensionTracker();
	}

	private void registerExtensionTracker() {
		new ExtensionTracker().registerHandler(this, ExtensionTracker.createExtensionPointFilter(Platform.getExtensionRegistry().getExtensionPoint(POINT_ID)));
	}

	public static CustomAttributesManager getInstance() {
		if (null == instance) {
			instance = new CustomAttributesManager();
		}
		return instance;
	}

	private void initCache() {
		fCustomAttributes = new HashMap();
		fCustomAttributesCategories = new HashMap();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(POINT_ID);
		for (int i = 0; i < elements.length; i++) {
			String id = elements[i].getAttribute(ATT_ID);
			if (elements[i].getName().equals(ATTRIBUTE)) {
				fCustomAttributes.put(id, elements[i]);
			} else if (elements[i].getName().equals(CATEGORY))
				fCustomAttributesCategories.put(id, elements[i].getAttribute(ATT_NAME));
		}
	}

	public String[] getCustomAttributes() {
		String[] res = new String[fCustomAttributes.size()];
		for (int i = 0; i < fCustomAttributes.size(); i++)
			res[i] = ((IConfigurationElement) fCustomAttributes.values().toArray()[i]).getAttribute(ATT_ID);
		return res;
	}

	public String getCustomAttributeName(String id) {
		return ((IConfigurationElement) fCustomAttributes.get(id)).getAttribute(ATT_NAME);
	}

	public ICustomAttributeValidator getCustomAttributeValidator(String id) {
		if (fCustomAttributes.containsKey(id)) {
			try {
				IConfigurationElement element = (IConfigurationElement) fCustomAttributes.get(id);
				if (element.getAttribute(ATT_VALIDATOR) != null) {
					Object result = element.createExecutableExtension(ATT_VALIDATOR);
					if (result instanceof ICustomAttributeValidator)
						return (ICustomAttributeValidator) result;
				}
			} catch (CoreException e) {
			}
		}
		return null;
	}

	public void addExtension(IExtensionTracker tracker, IExtension extension) {
	}

	public void removeExtension(IExtension extension, Object[] objects) {
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			String id = elements[i].getAttribute(ATT_ID);
			if (elements[i].getName().equals(ATTRIBUTE)) {
				fCustomAttributes.remove(id);
			} else if (elements[i].getName().equals(CATEGORY))
				fCustomAttributesCategories.remove(id);
		}
	}
}
