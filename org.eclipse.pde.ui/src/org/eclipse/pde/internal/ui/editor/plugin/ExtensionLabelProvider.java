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

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.BidiUtil;

public class ExtensionLabelProvider extends LabelProvider {
	private static final String[] COMMON_LABEL_PROPERTIES = {
		"label", //$NON-NLS-1$
		"name", //$NON-NLS-1$
		"id"}; //$NON-NLS-1$
	
	private static final String[] VALID_IMAGE_TYPES = {
		"png", "bmp", "ico", "gif", "jpg", "tiff" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	
	
	private Image fExtensionImage;
	private Image fGenericElementImage;
	private SchemaRegistry fSchemaRegistry;
	
	/**
	 * 
	 */
	public ExtensionLabelProvider() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		fExtensionImage = provider.get(PDEPluginImages.DESC_EXTENSION_OBJ);
		fGenericElementImage = provider.get(PDEPluginImages.DESC_GENERIC_XML_OBJ);
	}
	
	public String getText(Object obj) {
		return resolveObjectName(obj);
	}
	
	private String resolveObjectName(Object obj) {
		return resolveObjectName(getSchemaRegistry(), obj);
	}
	
	private SchemaRegistry getSchemaRegistry() {
		if (fSchemaRegistry == null)
			fSchemaRegistry = PDECore.getDefault().getSchemaRegistry();
		return fSchemaRegistry;
	}
	
	public String resolveObjectName(SchemaRegistry schemaRegistry, Object obj) {
		boolean fullNames = PDEPlugin.isFullNameModeEnabled();
		if (obj instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) obj;
			if (!fullNames) {
				return extension.getPoint();
			}
			if (extension.getName() != null)
				return extension.getTranslatedName();
			ISchema schema = schemaRegistry.getSchema(extension.getPoint());
			// try extension point schema definition
			if (schema != null) {
				// exists
				return schema.getName();
			}
			return extension.getPoint();		
		} else if (obj instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) obj;
			String baseName = element.getName();			
			String fullName = null;
			ISchemaElement elementInfo = ExtensionsSection.getSchemaElement(element);
			IPluginAttribute labelAtt = null;
			if (elementInfo != null && elementInfo.getLabelProperty() != null) {
				labelAtt = element.getAttribute(elementInfo.getLabelProperty());
			}
			if (labelAtt == null) {
				// try some hard-coded attributes that
				// are used frequently
				for (int i = 0; i < COMMON_LABEL_PROPERTIES.length; i++) {
					labelAtt = element.getAttribute(COMMON_LABEL_PROPERTIES[i]);
					if (labelAtt != null)
						break;
				}
				if (labelAtt == null) {
					// Last try - if there is only one attribute,
					// use that
					if (element.getAttributeCount() == 1)
						labelAtt = element.getAttributes()[0];
				}
			}
			if (labelAtt != null && labelAtt.getValue() != null)
				fullName = ExtensionsSection.stripShortcuts(labelAtt.getValue());
			fullName = element.getResourceString(fullName);
			if (fullNames)
				return fullName != null ? fullName : baseName;
			if (fullName == null)
				return baseName;
			// Bug 183417 - Bidi3.3: Elements' labels in the extensions page in the fragment manifest characters order is incorrect
			// add RTL zero length character just before the ( and the LTR character just after to ensure:
			// 1. The leading parenthesis takes proper orientation when running in bidi configuration
			// Assumption: baseName (taken from the schema definition), is only latin characters and is therefore always displayed LTR
			if (BidiUtil.isBidiPlatform())
				return fullName + " \u200f(\u200e" + baseName + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			return fullName + " (" + baseName + ')'; //$NON-NLS-1$
		}
		else if (obj instanceof IPlugin) {
			IPlugin plugin = (IPlugin) obj;
			return plugin.getId();
		}
		return obj.toString();
	}
	
	public Image getImage(Object obj) {
		return resolveObjectImage(obj);
	}
	
	private Image resolveObjectImage(Object obj) {
		if (obj instanceof IPluginExtension) {
			return fExtensionImage;
		}
		Image elementImage = fGenericElementImage;
		if (obj instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) obj;
			Image customImage = getCustomImage(element);
			if (customImage != null)
				elementImage = customImage;
			String bodyText = element.getText();
			boolean hasBodyText = bodyText != null && bodyText.length() > 0;
			if (hasBodyText) {
				elementImage = PDEPlugin.getDefault().getLabelProvider().get(
						elementImage, SharedLabelProvider.F_EDIT);
			}
		}
		return elementImage;
	}
	
	private boolean isStorageModel(IPluginObject object) {
		IPluginModelBase modelBase = object.getPluginModel();
		return modelBase.getInstallLocation()==null;
	}

	private Image getCustomImage(IPluginElement element) {
		if (isStorageModel(element))return null;
		ISchemaElement elementInfo = ExtensionsSection.getSchemaElement(element);
		if (elementInfo != null && elementInfo.getIconProperty() != null) {
			String iconProperty = elementInfo.getIconProperty();
			IPluginAttribute att = element.getAttribute(iconProperty);
			String iconPath = null;
			if (att != null && att.getValue() != null) {
				iconPath = att.getValue();
			}
			// we have a value from a resource attribute
			if (iconPath != null) {
				String ext = new Path(iconPath).getFileExtension();
				// if the resource targets a folder, the file extension will be null
				if (ext == null)
					return null;
				boolean valid = false;
				// ensure the resource is an image
				for (int i = 0; i < VALID_IMAGE_TYPES.length; i++) {
					if (ext.equalsIgnoreCase(VALID_IMAGE_TYPES[i])) {
						valid = true;
						break;
					}
				}
				// if the resource is an image, get the image, otherwise return null
				return valid ? getImageFromPlugin(element, iconPath) : null;
			}
		}
		return null;
	}

	private Image getImageFromPlugin(IPluginElement element,
			String iconPathName) {
		// 39283 - ignore icon paths that
		// point at plugin.properties
		if (iconPathName.startsWith("%")) //$NON-NLS-1$
			return null;

		IPluginModelBase model = element.getPluginModel();
		if (model == null)
			return null;

		return PDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(model, iconPathName);
	}
}
