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
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

public class ProductDestinationGroup extends ExportDestinationTab {

	public ProductDestinationGroup(ProductExportWizardPage page) {
		super(page);
	}

	public Control createControl(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.ExportWizard_destination);
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fDirectoryButton = new Button(group, SWT.RADIO);
		fDirectoryButton.setText(PDEUIMessages.ExportWizard_directory);

		fDirectoryCombo = new Combo(group, SWT.BORDER);
		fDirectoryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fBrowseDirectory = new Button(group, SWT.PUSH);
		fBrowseDirectory.setText(PDEUIMessages.ExportWizard_browse);
		fBrowseDirectory.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseDirectory);

		fArchiveFileButton = new Button(group, SWT.RADIO);
		fArchiveFileButton.setText(PDEUIMessages.ExportWizard_archive);

		fArchiveCombo = new Combo(group, SWT.BORDER);
		fArchiveCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fBrowseFile = new Button(group, SWT.PUSH);
		fBrowseFile.setText(PDEUIMessages.ExportWizard_browse);
		fBrowseFile.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseFile);

		return group;
	}

	protected void updateDestination(IFile file) {
		
	}
	

}
