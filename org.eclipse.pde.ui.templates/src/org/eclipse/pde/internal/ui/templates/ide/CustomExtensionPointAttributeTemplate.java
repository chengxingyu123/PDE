/*******************************************************************************
 * Copyright (c) 2009 Anyware Technologies Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anyware Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.templates.ide;

import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.templates.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.PluginReference;

public class CustomExtensionPointAttributeTemplate extends PDETemplateSection {
	public static final String CUSTOM_ATTRIBUTE_EDITOR_CLASS_NAME = "editorClassName"; //$NON-NLS-1$
	public static final String CUSTOM_ATTRIBUTE_COMPLETIONPROVIDER_CLASS_NAME = "completionProviderClassName"; //$NON-NLS-1$
	public static final String CUSTOM_ATTRIBUTE_VALIDATOR_CLASS_NAME = "validatorClassName"; //$NON-NLS-1$
	public static final String CUSTOM_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	public static final String CUSTOM_ATTRIBUTE_ICON = "icon"; //$NON-NLS-1$

	private WizardPage page;

	/**
	 * Constructor for CustomExtensionPointAttributeTemplate
	 */
	public CustomExtensionPointAttributeTemplate() {
		setPageCount(1);
		createOptions();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.OptionTemplateSection#getSectionId()
	 */
	public String getSectionId() {
		return "customAttributes"; //$NON-NLS-1$
	}

	/**
	 * Creates the options to be displayed on the template wizard.
	 * Various string options, blank fields and a multiple choice 
	 * option are used.
	 */
	private void createOptions() {
		addOption(KEY_PACKAGE_NAME, PDETemplateMessages.CustomExtensionPointAttributeTemplate_packageName, (String) null, 0);
		addOption(CUSTOM_ATTRIBUTE_EDITOR_CLASS_NAME, PDETemplateMessages.CustomExtensionPointAttributeTemplate_rowFactoryClass, PDETemplateMessages.CustomExtensionPointAttributeTemplate_editorClassName, 0);
		addOption(CUSTOM_ATTRIBUTE_COMPLETIONPROVIDER_CLASS_NAME, PDETemplateMessages.CustomExtensionPointAttributeTemplate_completionProviderClass, PDETemplateMessages.CustomExtensionPointAttributeTemplate_completionProviderClassName, 0);
		addOption(CUSTOM_ATTRIBUTE_VALIDATOR_CLASS_NAME, PDETemplateMessages.CustomExtensionPointAttributeTemplate_validatorClass, PDETemplateMessages.CustomExtensionPointAttributeTemplate_validatorClassName, 0);
		addOption(CUSTOM_ATTRIBUTE_NAME, PDETemplateMessages.CustomExtensionPointAttributeTemplate_attribute, PDETemplateMessages.CustomExtensionPointAttributeTemplate_attributeName, 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#addPages(org.eclipse.jface.wizard.Wizard)
	 */
	public void addPages(Wizard wizard) {
		int pageIndex = 0;

		page = createPage(pageIndex, IHelpContextIds.TEMPLATE_EDITOR);
		page.setTitle(PDETemplateMessages.CustomExtensionPointAttributeTemplate_title);
		page.setDescription(PDETemplateMessages.CustomExtensionPointAttributeTemplate_desc);

		wizard.addPage(page);
		markPagesAdded();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#isDependentOnParentWizard()
	 */
	public boolean isDependentOnParentWizard() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#initializeFields(org.eclipse.pde.ui.IFieldData)
	 */
	protected void initializeFields(IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String id = data.getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(id));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.BaseOptionTemplateSection#initializeFields(org.eclipse.pde.core.plugin.IPluginModelBase)
	 */
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, getFormattedPackageName(pluginId));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#updateModel(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension uiExtension = createExtension(getUsedExtensionPoint(), true);
		IPluginModelFactory factory = model.getPluginFactory();

		IPluginElement attributeUiElement = factory.createElement(uiExtension);
		attributeUiElement.setName("attributeUI"); //$NON-NLS-1$
		attributeUiElement.setAttribute("id", getStringOption(KEY_PACKAGE_NAME) + ".color"); //$NON-NLS-1$ //$NON-NLS-2$
		attributeUiElement.setAttribute("icon", "/icons/color_wheel.png"); //$NON-NLS-1$ //$NON-NLS-2$
		attributeUiElement.setAttribute("completionProvider", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(CUSTOM_ATTRIBUTE_COMPLETIONPROVIDER_CLASS_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
		attributeUiElement.setAttribute("editor", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(CUSTOM_ATTRIBUTE_EDITOR_CLASS_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
		uiExtension.add(attributeUiElement);

		IPluginExtension coreExtension = createExtension("org.eclipse.pde.core.customExtensionPointAttributes", true); //$NON-NLS-1$
		IPluginElement attributeElement = factory.createElement(uiExtension);
		attributeElement.setName("attribute"); //$NON-NLS-1$
		attributeElement.setAttribute("id", getStringOption(KEY_PACKAGE_NAME) + ".color"); //$NON-NLS-1$ //$NON-NLS-2$
		attributeElement.setAttribute("name", getStringOption(CUSTOM_ATTRIBUTE_NAME)); //$NON-NLS-1$
		attributeElement.setAttribute("validator", getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption(CUSTOM_ATTRIBUTE_VALIDATOR_CLASS_NAME)); //$NON-NLS-1$ //$NON-NLS-2$
		coreExtension.add(attributeElement);

		if (!coreExtension.isInTheModel())
			plugin.add(coreExtension);
		if (!uiExtension.isInTheModel())
			plugin.add(uiExtension);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getNewFiles()
	 */
	public String[] getNewFiles() {
		return new String[] {"icons/color_wheel.png"}; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.templates.PDETemplateSection#getFormattedPackageName(java.lang.String)
	 */
	protected String getFormattedPackageName(String id) {
		// Package name addition to create a location for containing
		// any classes required by the decorator. 
		String packageName = super.getFormattedPackageName(id);
		if (packageName.length() != 0)
			return packageName + ".customattributes"; //$NON-NLS-1$
		return "customattributes"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.ITemplateSection#getUsedExtensionPoint()
	 */
	public String getUsedExtensionPoint() {
		return "org.eclipse.pde.ui.customExtensionPointAttributes"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.AbstractTemplateSection#getDependencies(java.lang.String)
	 */
	public IPluginReference[] getDependencies(String schemaVersion) {
		ArrayList result = new ArrayList();
		result.add(new PluginReference("org.eclipse.core.runtime", null, 0)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.core.resources", null, 0)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui", null, 0)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.ui.forms", null, 0)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.pde.ui", null, 0)); //$NON-NLS-1$
		result.add(new PluginReference("org.eclipse.jface.text", null, 0)); //$NON-NLS-1$

		return (IPluginReference[]) result.toArray(new IPluginReference[result.size()]);
	}

}
