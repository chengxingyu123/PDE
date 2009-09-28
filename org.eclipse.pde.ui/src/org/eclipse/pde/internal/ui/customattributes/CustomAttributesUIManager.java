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
package org.eclipse.pde.internal.ui.customattributes;

import java.util.HashMap;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.dynamichelpers.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.customattributes.ICustomAttributeCompletionProvider;
import org.eclipse.pde.ui.customattributes.ICustomAttributeEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class CustomAttributesUIManager implements IExtensionChangeHandler {
	public class CustomAttributeDesc {
		private String fId;
		private Image fImage;

		public CustomAttributeDesc(String id, Image image) {
			super();
			this.fId = id;
			this.fImage = image;
		}

		public String getId() {
			return fId;
		}

		public Image getImage() {
			return fImage;
		}
	}

	private static final String POINT_ID = "org.eclipse.pde.ui.customExtensionPointAttributes"; //$NON-NLS-1$

	private static final String ATT_ID = "id"; //$NON-NLS-1$
	private static final String ATT_ICON = "icon"; //$NON-NLS-1$
	private static final String ATT_EDITOR = "editor"; //$NON-NLS-1$
	private static final String ATT_COMPLETION_PROVIDER = "completionProvider"; //$NON-NLS-1$

	private static CustomAttributesUIManager instance;

	private HashMap fCustomAttributes;

	private ImageRegistry fImageRegistry;

	private CustomAttributesUIManager() {
		fImageRegistry = new ImageRegistry();
		initCache();
		registerExtensionTracker();
	}

	private void registerExtensionTracker() {
		IExtensionTracker tracker = PlatformUI.getWorkbench().getExtensionTracker();
		tracker.registerHandler(this, ExtensionTracker.createExtensionPointFilter(Platform.getExtensionRegistry().getExtensionPoint(POINT_ID)));
	}

	public static CustomAttributesUIManager getInstance() {
		if (null == instance) {
			instance = new CustomAttributesUIManager();
		}
		return instance;
	}

	private void initCache() {
		fCustomAttributes = new HashMap();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(POINT_ID);
		for (int i = 0; i < elements.length; i++) {
			String id = elements[i].getAttribute(ATT_ID);
			fCustomAttributes.put(id, elements[i]);
		}
	}

	public CustomAttributeDesc[] getCustomAttributesDesc() {
		CustomAttributeDesc[] res = new CustomAttributeDesc[fCustomAttributes.size()];
		for (int i = 0; i < fCustomAttributes.size(); i++) {
			String id = ((IConfigurationElement) fCustomAttributes.values().toArray()[i]).getAttribute(ATT_ID);
			if (fImageRegistry.getDescriptor(id) == null)
				fImageRegistry.put(id, getCustomAttributeIcon(id));
			res[i] = new CustomAttributeDesc(id, fImageRegistry.get(id));
		}
		return res;
	}

	public ImageDescriptor getCustomAttributeIcon(String id) {
		IConfigurationElement configurationElement = (IConfigurationElement) fCustomAttributes.get(id);
		return AbstractUIPlugin.imageDescriptorFromPlugin(configurationElement.getNamespaceIdentifier(), configurationElement.getAttribute(ATT_ICON));
	}

	public ICustomAttributeEditor getCustomAttributeEditor(String id) {
		if (fCustomAttributes.containsKey(id)) {
			try {
				IConfigurationElement element = (IConfigurationElement) fCustomAttributes.get(id);
				if (element.getAttribute(ATT_EDITOR) != null) {
					Object result = element.createExecutableExtension(ATT_EDITOR);
					if (result instanceof ICustomAttributeEditor)
						return (ICustomAttributeEditor) result;
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
		return null;
	}

	public ICustomAttributeCompletionProvider getCustomAttributeCompletionProvider(String id) {
		if (fCustomAttributes.containsKey(id)) {
			try {
				IConfigurationElement element = (IConfigurationElement) fCustomAttributes.get(id);
				if (element.getAttribute(ATT_COMPLETION_PROVIDER) != null) {
					Object result = element.createExecutableExtension(ATT_COMPLETION_PROVIDER);
					if (result instanceof ICustomAttributeCompletionProvider)
						return (ICustomAttributeCompletionProvider) result;
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
		return null;
	}

	public void addExtension(IExtensionTracker tracker, IExtension extension) {
		// TODO
	}

	public void removeExtension(IExtension extension, Object[] objects) {
		// TODO
	}
}
