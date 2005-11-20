/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class SchemaOverviewPage extends PDEFormPage {
		 
	 public static final String PAGE_ID = "overview"; //$NON-NLS-1$
	 private IColorManager colorManager = ColorManager.getDefault();
	 private DocSection docSection;
	 private SchemaSpecSection extensionPointSection;
	 private SchemaIncludesSection extraSpecSection;
	 public SchemaOverviewPage(PDEFormEditor editor) {
 		 super(editor, PAGE_ID, PDEUIMessages.SchemaEditor_DocPage_title);
	 }
	 
	 /**
	  * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#becomesInvisible(IFormPage)
	  */
	 public void setActive(boolean active) {
 		 if (!active)
	 		 getManagedForm().commit(false);
 		 super.setActive(active);
	 }
	 protected void createFormContent(IManagedForm managedForm) {
 		 super.createFormContent(managedForm);
 		 ScrolledForm form = managedForm.getForm();
 		 GridLayout layout = new GridLayout(2, true);
 		 layout.marginWidth = 10;
 		 layout.horizontalSpacing = 15;
 		 form.getBody().setLayout(layout);

 		 extensionPointSection = new SchemaSpecSection(this, form.getBody());
 		 extensionPointSection.getSection().setLayoutData(
	 		 new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
 		 
 		 extraSpecSection = new SchemaIncludesSection(this, form.getBody());
 		 extraSpecSection.getSection().setLayoutData(
	 		 new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
 		 
 		 
 		 docSection = new DocSection(this, form.getBody(), colorManager);
 		 GridData gd = new GridData(GridData.FILL_BOTH);
 		 gd.horizontalSpan = 2;
 		 gd.verticalIndent = 20;
 		 docSection.getSection().setLayoutData(gd);

 		 managedForm.addPart(extensionPointSection);
 		 managedForm.addPart(extraSpecSection);
 		 managedForm.addPart(docSection);
 		 
 		 PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.SCHEMA_EDITOR_DOC);
 		 form.setText(PDEUIMessages.SchemaEditor_DocForm_title);		 		 
	 }
	 
	 public void dispose() {
 		 colorManager.dispose();
 		 super.dispose();
	 }

	 public void updateEditorInput(Object obj) {
 		 docSection.updateEditorInput(obj);
	 }
}
