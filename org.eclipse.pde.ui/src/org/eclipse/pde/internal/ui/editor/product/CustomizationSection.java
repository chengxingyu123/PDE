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
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo;
import org.eclipse.pde.internal.core.iproduct.ICustomizationInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * 
 */
public class CustomizationSection extends PDESection implements
		IHyperlinkListener {

	private boolean showPage;
	private Composite noControlComposite;
	private Composite controlComposite;
	private StackLayout clientLayout;
	private Composite client;
	private Composite customizationComposite;

	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public CustomizationSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Customization");

		client = toolkit.createComposite(section);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData sectionData = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(sectionData);
		section.setClient(client);

		clientLayout = new StackLayout();
		client.setLayout(clientLayout);

		createWarningControls(noControlComposite = toolkit
				.createComposite(client), toolkit);

		createCustomizationControls(controlComposite = toolkit
				.createComposite(client), toolkit);

		// default to not showing it. we'll tweak this momentarily with the real
		// value
		clientLayout.topControl = noControlComposite;

		IProductModel productModel = (IProductModel) getPage().getPDEEditor()
				.getAggregateModel();

		updatePageState(productModel.getProduct().getConfigurationFileInfo()
				.getUse());
		productModel.addModelChangedListener(new IModelChangedListener() {

			public void modelChanged(IModelChangedEvent event) {
				for (int i = 0; i < event.getChangedObjects().length; i++) {
					if (event.getChangedObjects()[i] instanceof IConfigurationFileInfo) {
						if (event.getChangedProperty().equals(
								IConfigurationFileInfo.P_USE)) {
							String newValue = event.getNewValue().toString();
							updatePageState(newValue);
						}
					}
				}
			}

		});
	}

	/**
	 * @param composite
	 * @param toolkit
	 */
	private void createCustomizationControls(Composite composite,
			FormToolkit toolkit) {
		composite.setLayout(FormLayoutFactory.createSectionClientGridLayout(
				false, 1));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// radio buttons to turn customization on and off
		{
			final Button noTransformsButton = toolkit.createButton(composite,
					"Do not use transforms for this product", SWT.RADIO);
			ICustomizationInfo customizationInfo = getCustomizationInfo();
			noTransformsButton.setSelection(customizationInfo == null);
			final Button transformsButton = toolkit.createButton(composite,
					"Use the following transforms for this product", SWT.RADIO);
			transformsButton.setSelection(customizationInfo != null);
			SelectionAdapter selectionListener = new SelectionAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				public void widgetSelected(SelectionEvent e) {
					boolean doingTransforms = e.widget == transformsButton;
					if (doingTransforms) {
						ensureCustomizationInfo();
					}
					enableCustomControls(doingTransforms);
				}
			};

			noTransformsButton.addSelectionListener(selectionListener);
			transformsButton.addSelectionListener(selectionListener);
		}

		// pretty separator
		{
			Composite separator = toolkit.createCompositeSeparator(composite);
			GridData data = new GridData();
			data.heightHint = 1;
			data.horizontalAlignment = SWT.FILL;
			separator.setLayoutData(data);
		}

		// customization controls
		{
			customizationComposite = toolkit.createComposite(composite);
			GridData data = new GridData(GridData.FILL_BOTH);
			customizationComposite.setLayoutData(data);
			customizationComposite.setLayout(FormLayoutFactory
					.createSectionClientGridLayout(false, 1));
			
			toolkit.createText(customizationComposite, "Controls go here");

		}
		FormText text = toolkit.createFormText(composite, true);

		try {
			text.setText("<form><p>Edit away friend.</p></form>", true, true);
		} catch (SWTException e) {
			text.setText(e.getMessage(), false, false);
		}
		text.addHyperlinkListener(this);
	}

	/**
	 * The customization info for this product.
	 * 
	 * @return the info
	 */
	private ICustomizationInfo getCustomizationInfo() {
		return ((IProductModel) getPage().getPDEEditor().getAggregateModel())
				.getProduct().getCustomizationInfo();
	}

	/**
	 * Ensures that the customization info for this product has been created.
	 */
	protected void ensureCustomizationInfo() {
		if (getCustomizationInfo() == null) {
			IProductModel productModel = (IProductModel) getPage()
					.getPDEEditor().getAggregateModel();
			ICustomizationInfo customizationInfo = productModel.getFactory()
					.createCustomizationInfo();
			productModel.getProduct().setCustomizationInfo(customizationInfo);
		}
	}

	/**
	 * @param b
	 */
	protected void enableCustomControls(boolean b) {
		customizationComposite.setEnabled(b);
	}

	/**
	 * @param composite
	 * @param toolkit
	 */
	private void createWarningControls(Composite composite, FormToolkit toolkit) {
		composite.setLayout(FormLayoutFactory.createSectionClientGridLayout(
				false, 3));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		FormText text = toolkit.createFormText(composite, true);
		try {
			text
					.setText(
							"<form><p>You cannot customize a product with a custom configuration.  Use the <a href=\"action.goToConfigPage\">default configuration</a> if you wish to proceed with product customization.</p></form>",
							true, true);
		} catch (SWTException e) {
			text.setText(e.getMessage(), false, false);
		}
		text.addHyperlinkListener(this);
	}

	/**
	 * Call updatePageState based on the value provided. Maps to the known
	 * values used to call {@link IConfigurationFileInfo#setUse(String)} in
	 * {@link ConfigurationSection#createClient(org.eclipse.ui.forms.widgets.Section, FormToolkit)}.
	 * If the value is "custom" then <code>false</code> will be passed to
	 * {@link #updatePageState(boolean)}. In all other cases <code>true</code>
	 * will be used.
	 * 
	 * @param newValue
	 */
	private void updatePageState(String newValue) {
		boolean showPage = true;
		if ("custom".equals(newValue))
			showPage = false;
		updatePageState(showPage);
	}

	/**
	 * Update the page to either show customization controls or inform the user
	 * that they have to be using a particular configuration setting.
	 * 
	 * @param showPage
	 */
	protected void updatePageState(boolean showPage) {
		if (showPage != this.showPage) {
			this.showPage = showPage;
			if (showPage)
				clientLayout.topControl = controlComposite;
			else
				clientLayout.topControl = noControlComposite;

			client.layout();

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.IHyperlinkListener#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		if ("action.goToConfigPage".equals(href)) {
			IFormPage page = getPage().getPDEEditor().setActivePage(
					ConfigurationPage.PLUGIN_ID);
			if (page == null)
				page = getPage().getPDEEditor().setActivePage(
						ConfigurationPage.FEATURE_ID);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.IHyperlinkListener#linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkEntered(HyperlinkEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.IHyperlinkListener#linkExited(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkExited(HyperlinkEvent e) {
	}
}
