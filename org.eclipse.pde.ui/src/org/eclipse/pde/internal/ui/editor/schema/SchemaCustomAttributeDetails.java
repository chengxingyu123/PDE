/*******************************************************************************
 * Copyright (c) 2009 Anyware Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.customattributes.CustomAttributesManager;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.customattributes.CustomAttributesUIManager;
import org.eclipse.pde.internal.ui.customattributes.CustomAttributesUIManager.CustomAttributeDesc;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaCustomAttributeDetails extends SchemaAttributeDetails {

	private FormEntry fCustomEntry;

	public SchemaCustomAttributeDetails(ElementSection section) {
		super(section);
	}

	protected void createTypeDetails(Composite parent, FormToolkit toolkit) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		gd.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		fCustomEntry = new FormEntry(parent, toolkit, PDEUIMessages.SchemaCustomAttributeDetails_customAttributeID, PDEUIMessages.SchemaAttributeDetails_browseButton, false, 11);
	}

	public void updateFields(ISchemaObject object) {
		if (!(object instanceof SchemaAttribute))
			return;
		super.updateFields(object);

		String basedOn = getAttribute().getBasedOn();
		if ((basedOn != null) && (basedOn.length() > 0)) {
			fCustomEntry.setValue(basedOn, true);
		} else {
			fCustomEntry.setValue("", true); //$NON-NLS-1$
		}

		boolean editable = isEditableElement();
		fCustomEntry.setEditable(editable);
	}

	public void hookListeners() {
		super.hookListeners();
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fCustomEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				if (blockListeners())
					return;
				getAttribute().setBasedOn(fCustomEntry.getValue());
			}

			public void browseButtonSelected(FormEntry entry) {
				if (blockListeners())
					return;
				doOpenSelectionDialog(fCustomEntry);
			}
		});
	}

	private void doOpenSelectionDialog(FormEntry entry) {
		ListDialog dialog = new ListDialog(PDEPlugin.getActiveWorkbenchShell());
		dialog.setContentProvider(new ArrayContentProvider());
		dialog.setInput(CustomAttributesUIManager.getInstance().getCustomAttributesDesc());
		dialog.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return CustomAttributesManager.getInstance().getCustomAttributeName(((CustomAttributeDesc) element).getId());
			}

			public Image getImage(Object element) {
				return ((CustomAttributeDesc) element).getImage();
			}
		});
		dialog.setTitle("Choose a custom attribute type"); //$NON-NLS-1$
		int status = dialog.open();
		if (status == Window.OK) {
			Object[] result = dialog.getResult();
			entry.setValue(((CustomAttributeDesc) result[0]).getId());
			entry.commit();
		}
	}
}
