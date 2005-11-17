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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public abstract class AbstractSchemaDetails extends PDEDetails {

	protected static final String STRING_TYPE = "string";
	protected static final String BOOLEAN_TYPE = "boolean";
	protected static final String[] BOOLS = 
		new String[] { Boolean.toString(true), Boolean.toString(false) };
	
	private Section fSection;
	private Text dtdLabel;
	private ElementSection fElementSection;
	private boolean fShowDTD;
	
	public AbstractSchemaDetails(ElementSection section, boolean showDTD) {
		fElementSection = section;
		fShowDTD = showDTD;
	}
	
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.REMOVE)
			return;
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof ISchemaCompositor)
				updateDTDLabel(objects[i]);
		}
	}
	
	public final void createContents(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		parent.setLayout(layout);
		FormToolkit toolkit = getManagedForm().getToolkit();
		fSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		fSection.marginHeight = 5;
		fSection.marginWidth = 5; 
		GridData gd = new GridData(GridData.FILL_BOTH);
		fSection.setLayoutData(gd);
		Composite client = toolkit.createComposite(fSection);
		GridLayout glayout = new GridLayout(3, false);
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		if (paintedBorder) glayout.verticalSpacing = 7;
		client.setLayout(glayout);
		
		createDetails(client);
		updateFields();
		
		if (fShowDTD) {
			Label label = toolkit.createLabel(client, PDEUIMessages.SchemaEditor_GrammarSection_dtd);
			label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 3;
			gd.verticalIndent = 15;
			label.setLayoutData(gd);
			
			dtdLabel = toolkit.createText(client, "", SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);//$NON-NLS-1$
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 3;
			gd.heightHint = 40;
			dtdLabel.setLayoutData(gd);
			dtdLabel.setEditable(false);
			dtdLabel.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		}
		
		toolkit.paintBordersFor(client);
		fSection.setClient(client);
		markDetailsPart(fSection);
		
		hookListeners();
	}
	
	public abstract void createDetails(Composite parent);
	public abstract void updateFields();
	public abstract void hookListeners();
	
	protected void setDecription(String desc) {
		fSection.setDescription(desc); 
	}
	protected void setText(String title) {
		fSection.setText(title);
	}
	public String getContextId() {
		return SchemaInputContext.CONTEXT_ID;
	}
	public PDEFormPage getPage() {
		return (PDEFormPage)getManagedForm().getContainer();
	}
	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}
	public void selectionChanged(IFormPart part, ISelection selection) {
		if (!(part instanceof ElementSection))
			return;
		updateDTDLabel(((IStructuredSelection)selection).getFirstElement());
	}
	
	private void updateDTDLabel(Object changeObject) {
		if (!fShowDTD) return;
		if (changeObject instanceof ISchemaAttribute) {
			changeObject = ((ISchemaAttribute) changeObject).getParent();
		} else if (changeObject instanceof ISchemaCompositor) {
			while (changeObject != null) {
				if (changeObject instanceof ISchemaElement)
					break;
				changeObject = ((ISchemaCompositor)changeObject).getParent();
			}
		}
		if (changeObject instanceof ISchemaElement)
			dtdLabel.setText(((ISchemaElement)changeObject).getDTDRepresentation(false));
	}
	
	protected void fireMasterSelection(ISelection selection) {
		fElementSection.fireSelection(selection);
	}
	
	protected ComboPart createComboPart(Composite parent, FormToolkit toolkit, String[] items, int colspan) {
		ComboPart cp = new ComboPart();
		cp.createControl(parent, toolkit, SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colspan;
		cp.getControl().setLayoutData(gd);
		cp.setItems(items);
		cp.getControl().setEnabled(isEditable());
		return cp;
	}
}
