/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ConfigurationTemplateBlock extends BaseBlock {

	private Button fGenerateFileButton;
	private Button fUseTemplateButton;

	public ConfigurationTemplateBlock(AbstractLauncherTab tab) {
		super(tab);
	}
	
	public void createControl(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.ConfigurationTab_configFileGroup); 
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fGenerateFileButton = new Button(group, SWT.RADIO);
		fGenerateFileButton.setText(PDEUIMessages.ConfigurationTab_defaultConfigIni); 
		GridData gd = new GridData();
		gd.horizontalSpan = 4;
		fGenerateFileButton.setLayoutData(gd);
		fGenerateFileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				enableBrowseSection(!fGenerateFileButton.getSelection());
			}
		});
		
		fUseTemplateButton = new Button(group, SWT.RADIO);
		fUseTemplateButton.setText(PDEUIMessages.ConfigurationTab_existingConfigIni); 
		gd = new GridData();
		gd.horizontalSpan = 4;
		fUseTemplateButton.setLayoutData(gd);
		
		createText(group, PDEUIMessages.ConfigurationTab_templateLoc, 20); 

		Label label = new Label(group, SWT.NONE);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		createButtons(group);
	}
	
	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		boolean generateDefault = configuration.getAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true);
		fGenerateFileButton.setSelection(generateDefault);
		fUseTemplateButton.setSelection(!generateDefault);
		enableBrowseSection(!generateDefault);
		fLocationText.setText(configuration.getAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, "")); //$NON-NLS-1$
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, fGenerateFileButton.getSelection());
		if (!fGenerateFileButton.getSelection())
			configuration.setAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, getLocation());
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {		
		configuration.setAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true);
		configuration.setAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, ""); //$NON-NLS-1$
	}
	

	protected String getName() {
		return "template file location";
	}
	
	protected void handleBrowseWorkspaceFile() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				getControl().getShell(), new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
		
		IFile file = PDEPlugin.getWorkspace().getRoot().getFileForLocation(new Path(fTemplateLocationText.getText()));
		if (file != null)
			dialog.setInitialSelection(file);
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IFile)
					return ((IFile)element).getName().equals("config.ini"); //$NON-NLS-1$
				return true;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.ConfigurationTab_fileSelection); 
		dialog.setMessage(PDEUIMessages.ConfigurationTab_fileDialogMessage); 
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null && selection.length > 0
						&& selection[0] instanceof IFile)
					return new Status(IStatus.OK, PDEPlugin.getPluginId(),
							IStatus.OK, "", null); //$NON-NLS-1$
				
				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
						IStatus.ERROR, "", null); //$NON-NLS-1$
			}
		});
		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			file = (IFile) dialog.getFirstResult();
			fTemplateLocationText.setText(file.getLocation().toOSString());
		}
	}

	protected void handleBrowseDirectory() {
		DirectoryDialog dialog = new DirectoryDialog(getControl().getShell());
		dialog.setFilterPath(fConfigAreaText.getText().trim());
		dialog.setText(PDEUIMessages.ConfigurationTab_configLocTitle); 
		dialog.setMessage(PDEUIMessages.ConfigurationTab_configLocMessage); 
		String res = dialog.open();
		if (res != null)
			fConfigAreaText.setText(res);
	}



}
