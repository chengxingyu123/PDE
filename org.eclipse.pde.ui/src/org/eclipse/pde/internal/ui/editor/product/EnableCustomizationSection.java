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

import java.util.ArrayList;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.jface.fieldassist.AutoCompleteField;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.iproduct.ICustomizationInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.wizards.plugin.NewTransformationPluginProjectWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;

/**
 * First customization section - merely asks if you wish to use customizations.
 */
public class EnableCustomizationSection extends PDESection {

	private Composite fClient;

	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public EnableCustomizationSection(PDEFormPage page, Composite parent) {
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

		fClient = toolkit.createComposite(section);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData sectionData = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(sectionData);
		section.setClient(fClient);
		fClient.setLayout(FormLayoutFactory.createSectionClientGridLayout(
				false, 2));

		{
			ICustomizationInfo customizationInfo = getCustomizationInfo();

			final Button noTransformsButton = toolkit.createButton(fClient,
					"Do not use transforms for this product", SWT.RADIO);
			noTransformsButton.setSelection(!customizationInfo
					.getUseCustomizations());
			GridData data = new GridData(GridData.BEGINNING,
					GridData.BEGINNING, true, false);
			noTransformsButton.setLayoutData(data);

			final Button transformsButton = toolkit.createButton(fClient,
					"Use the following transforms for this product", SWT.RADIO);
			transformsButton.setSelection(customizationInfo
					.getUseCustomizations());
			data = new GridData(GridData.BEGINNING, GridData.BEGINNING, true,
					false);
			transformsButton.setLayoutData(data);

			SelectionAdapter selectionListener = new SelectionAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				public void widgetSelected(SelectionEvent e) {
					boolean doingTransforms = e.widget == transformsButton;
					getCustomizationInfo()
							.setUseCustomizations(doingTransforms);
					if (doingTransforms) {
						IProductPlugin [] plugins = new IProductPlugin[2];
						IProductModelFactory factory = getProduct().getModel().getFactory();
						plugins[0] = factory.createPlugin();
						plugins[0].setId("org.eclipse.equinox.transforms");
						
						plugins[1] = factory.createPlugin();
						plugins[1].setId("org.eclipse.equinox.transforms.xslt");
						
						getProduct().addPlugins(plugins);
					}
				}
			};
			noTransformsButton.addSelectionListener(selectionListener);
			transformsButton.addSelectionListener(selectionListener);
		}

		{
			// transformation bundle

			ProjectSelectionField projectField = new ProjectSelectionField(
					fClient, toolkit, getProduct(), getCustomizationInfo());
			GridData data = new GridData(GridData.FILL, GridData.BEGINNING,
					true, false, 2, 1);
			projectField.setLayoutData(data);
		}
		/*
		 * { FileSelectionField fileField = new FileSelectionField(fClient,
		 * toolkit); GridData data = new GridData(GridData.FILL,
		 * GridData.BEGINNING, true, false, 2, 1);
		 * fileField.setLayoutData(data); }
		 */
	}

	/**
	 * The customization info for this product.
	 * 
	 * @return the info
	 */
	private ICustomizationInfo getCustomizationInfo() {
		ICustomizationInfo customizationInfo = getProduct()
				.getCustomizationInfo();
		if (customizationInfo == null) {
			IProductModel productModel = (IProductModel) getPage()
					.getPDEEditor().getAggregateModel();
			customizationInfo = productModel.getFactory()
					.createCustomizationInfo();
			productModel.getProduct().setCustomizationInfo(customizationInfo);
		}
		return customizationInfo;
	}

	/**
	 * @return
	 */
	private IProduct getProduct() {
		return ((IProductModel) getPage()
				.getPDEEditor().getAggregateModel()).getProduct();
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
}

abstract class SelectionField extends Composite {

	protected AutoCompleteField fAutoCompleteField;
	protected Text fTextField;

	/**
	 * 
	 */
	SelectionField(final Composite parent, FormToolkit toolkit, String label) {
		super(parent, SWT.NONE);

		setLayout(FormLayoutFactory.createClearGridLayout(false, 3));

		{
			Hyperlink creationLink = toolkit.createHyperlink(this, label,
					SWT.NULL);

			creationLink.addHyperlinkListener(new HyperlinkAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.ui.forms.events.HyperlinkAdapter#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
				 */
				public void linkActivated(HyperlinkEvent e) {
					handleCreate(e);
				}
			});
		}
		{
			fTextField = toolkit.createText(this, "");
			fTextField.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					handleModify(e);
				}
			});
			GridData data = new GridData(GridData.FILL, GridData.END, true,
					false);
			fTextField.setLayoutData(data);
			fAutoCompleteField = new AutoCompleteField(fTextField,
					new TextContentAdapter(), new String[] {});
		}
		{
			Button browseButton = toolkit
					.createButton(this, "Browse", SWT.PUSH);
			browseButton.addSelectionListener(new SelectionAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				public void widgetSelected(SelectionEvent e) {
					handleBrowse(e);

				}
			});
		}
	}

	/**
	 * @param e
	 * 
	 */
	protected abstract void handleModify(ModifyEvent e);

	/**
	 * @param e
	 * 
	 */
	protected abstract void handleCreate(HyperlinkEvent e);

	/**
	 * @param e
	 * 
	 */
	protected abstract void handleBrowse(SelectionEvent e);

	/**
	 * 
	 */
	protected abstract void resetFieldAssist();

}

class ProjectSelectionField extends SelectionField {

	private IResourceChangeListener fResourceListener;
	private ICustomizationInfo fCustomizationInfo;
	private String[] fCandidates;
	private IProduct fProduct;

	/**
	 * @param toolkit
	 * @param product 
	 * 
	 */
	ProjectSelectionField(Composite parent, FormToolkit toolkit,
			IProduct product, ICustomizationInfo customizationInfo) {
		super(parent, toolkit, "Project to contain transforms :");
		fProduct = product;
		fCustomizationInfo = customizationInfo;
		fResourceListener = new IResourceChangeListener() {

			public void resourceChanged(IResourceChangeEvent event) {
				resetFieldAssist();
			}
		};
		PDECore.getWorkspace().addResourceChangeListener(fResourceListener);
		String targetPlugin = fCustomizationInfo.getTargetPlugin();
		if (targetPlugin != null)
			fTextField.setText(targetPlugin);
		resetFieldAssist();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose() {
		PDECore.getWorkspace().removeResourceChangeListener(fResourceListener);
		super.dispose();
	}

	protected void resetFieldAssist() {
		IPluginModelBase[] models = PDECore.getDefault().getModelManager()
				.getActiveModels();
		ArrayList candidatePlugins = new ArrayList(models.length);
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase pluginModelBase = models[i];
			if (pluginModelBase.getUnderlyingResource() != null) {
				IPluginImport [] imports = pluginModelBase.getPluginBase().getImports();
				boolean foundUnsupportedImport = false;
				// check to see if the bundles import anything other than the transform bundles.  If so exclude them.
				for (int j = 0; j < imports.length; j++) {
					IPluginImport pluginImport = imports[j];
					if (pluginImport.getId().equals("org.eclipse.osgi")) 
						continue;
					if (pluginImport.getId().equals("org.eclipse.equinox.transforms.xslt"))
						continue;
					foundUnsupportedImport = true;
					break;
				}
				if (!foundUnsupportedImport)
					candidatePlugins.add(pluginModelBase.getPluginBase().getId());
			}
		}

		fCandidates = (String[]) candidatePlugins
				.toArray(new String[candidatePlugins.size()]);
		fAutoCompleteField.setProposals(fCandidates);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.product.SelectionField#handleBrowse()
	 */
	protected void handleBrowse(SelectionEvent e) {
		ArrayList modelList = new ArrayList(fCandidates.length);
		for (int i = 0; i < fCandidates.length; i++) {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(fCandidates[i]);
			if (model != null)
				modelList.add(model);
		}
		
		PluginSelectionDialog dialog = new PluginSelectionDialog(getParent()
				.getShell(), (IPluginModelBase[]) modelList
				.toArray(new IPluginModelBase[modelList.size()]), false);
		if (dialog.open() == Window.OK) {
			IPluginModelBase selected = (IPluginModelBase) dialog
					.getFirstResult();
			setPlugin(selected.getPluginBase().getId());
		}
	}

	/**
	 * @param underlyingResource
	 */
	private void setPlugin(String pluginId) {
		fCustomizationInfo.setTargetPlugin(pluginId);
		IProductPlugin [] plugins = new IProductPlugin[1];
		IProductModelFactory factory = fCustomizationInfo.getProduct().getModel().getFactory();
		plugins[0] = factory.createPlugin();
		plugins[0].setId(pluginId);
		fProduct.addPlugins(plugins);
		fTextField.setText(pluginId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.product.SelectionField#handleCreate()
	 */
	protected void handleCreate(HyperlinkEvent e) {
		NewTransformationPluginProjectWizard wizard = new NewTransformationPluginProjectWizard();
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
		WizardDialog dialog = new WizardDialog(fTextField.getShell(), wizard);
		if (dialog.open() == Window.OK) {
			String pluginId = wizard.getPluginId();
			IPluginModelBase base = PDECore.getDefault().getModelManager()
					.findModel(pluginId);
			if (base != null)
				setPlugin(pluginId);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.product.SelectionField#handleModify()
	 */
	protected void handleModify(ModifyEvent e) {
		String value = fTextField.getText();
		if (fCandidates == null)
			return;
		for (int i = 0; i < fCandidates.length; i++) {
			if (value.equals(fCandidates[i])) {
				if (value != null)
					fCustomizationInfo.setTargetPlugin(value);
				return;
			}
		}
	}
}
