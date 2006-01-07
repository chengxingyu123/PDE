/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PlatformUI;


public class FeatureExportWizardPage extends BaseExportWizardPage {
	
	private static int JNLP_INDEX = 3;
	
	private JNLPTab fJNLPTab;
	private Control control;

	public FeatureExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"featureExport", //$NON-NLS-1$
			PDEUIMessages.ExportWizard_Feature_pageBlock); 
		setTitle(PDEUIMessages.ExportWizard_Feature_pageTitle); 
	}

	public Object[] getListElements() {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getFeatureModels();
	}
	
	protected void hookHelpContext(Control control) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.FEATURE_EXPORT_WIZARD);
	}
	
	protected boolean isValidModel(IModel model) {
		return model instanceof IFeatureModel;
	}
	
	protected void createTabs(TabFolder folder) {
		super.createTabs(folder);
		IDialogSettings settings = getDialogSettings();
		boolean useDirectory = settings.getBoolean(ExportDestinationTab.S_EXPORT_DIRECTORY);
		boolean useJARFormat = settings.getBoolean(ExportOptionsTab.S_JAR_FORMAT);
		if (useDirectory && useJARFormat)
			createJNLPTab(folder);
	}
	
	protected void initializeTabs(IDialogSettings settings) {
		super.initializeTabs(settings);
		if (fJNLPTab != null)
			fJNLPTab.initialize(settings);
	}
	
	private void createJNLPTab(TabFolder folder) {
		fJNLPTab = new JNLPTab(this);
		TabItem item = new TabItem(folder, SWT.NONE);
		control = fJNLPTab.createControl(folder);
		item.setControl(control);
		item.setText(PDEUIMessages.AdvancedFeatureExportPage_jnlp); 
	}

	protected void createOptionsTab(TabFolder folder) {
		fOptionsTab = new FeatureOptionsTab(this);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setControl(fOptionsTab.createControl(folder));
		item.setText(PDEUIMessages.ExportWizard_options); 		
	}
	
	protected IModel findModelFor(IAdaptable object) {
		IProject project = (IProject) object.getAdapter(IProject.class);
		if (project != null)
			return PDECore.getDefault().getWorkspaceModelManager().getFeatureModel(project);
		return null;
	}
	
	protected void saveSettings(IDialogSettings settings) {
		super.saveSettings(settings);
		if (fJNLPTab != null)
			fJNLPTab.saveSettings(settings);
	}
	
	protected String validateTabs() {
		String message = super.validateTabs();
		if (message == null && fTabFolder.getItemCount() >= JNLP_INDEX + 1)
			message = fJNLPTab.validate();
		return message;
	}
	
	protected void showJNLPTab(boolean show) {
		if (show) {
			if (fTabFolder.getItemCount() < JNLP_INDEX + 1) {
				createJNLPTab(fTabFolder);
				fJNLPTab.initialize(getDialogSettings());
			}			
		} else {
			if (fJNLPTab != null && fTabFolder.getItemCount() >= JNLP_INDEX + 1) {
				fJNLPTab.saveSettings(getDialogSettings());
				fTabFolder.getItem(JNLP_INDEX).dispose();
			}
		}
		pageChanged();
	}
	
	protected boolean doMultiPlatform() {
		return ((FeatureOptionsTab)fOptionsTab).doMultiplePlatform();
	}
	
	protected String[] getJNLPInfo() {
		if (fJNLPTab == null || fTabFolder.getItemCount() < JNLP_INDEX + 1)
			return null;
		return fJNLPTab.getJNLPInfo();
	}
	
}
