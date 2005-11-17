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

import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaElementDetails extends AbstractSchemaDetails {

	private SchemaElement fElement;
	private FormEntry fIcon;
	private FormEntry fLabelProperty;
	private FormEntry fName;
	private ComboPart fDeprecated;
	private ComboPart fTranslatable;
	
	public SchemaElementDetails(ISchemaElement element, ElementSection section) {
		super(section, true);
		if (element instanceof SchemaElementReference)
			element = (SchemaElement)((SchemaElementReference)element).getReferencedObject();
		fElement = (SchemaElement)element;
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		
		fName = new FormEntry(parent, toolkit, "Name:", SWT.NONE);
		fName.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				
			}
		});
		fLabelProperty = new FormEntry(parent, toolkit, "Label Property:", SWT.NONE);
		fIcon = new FormEntry(parent, toolkit, "Icon:", SWT.NONE);
		
		toolkit.createLabel(parent, "Deprecated:").setForeground(foreground);
		fDeprecated = createComboPart(parent, toolkit, BOOLS, 2);

		
		toolkit.createLabel(parent, "Translatable:").setForeground(foreground);
		fTranslatable = createComboPart(parent, toolkit, BOOLS, 2);
		
		setText("Element Details");
		setDecription("Properties for the \"" + fElement.getName() + "\" element.");
	}

	public void updateFields() {
		if (fElement == null)
			return;
		String curr = fElement.getName();
		fName.getText().setText(curr != null ? curr : "");
		curr = fElement.getLabelProperty();
		fLabelProperty.getText().setText(curr != null ? curr : "");
		curr = fElement.getIconProperty();
		fIcon.getText().setText(curr != null ? curr : "");
		
		fDeprecated.select(fElement.isDeprecated() ? 0 : 1);
		
		fTranslatable.select(fElement.hasTranslatableContent() ? 0 : 1);
	}

	public void hookListeners() {
		fIcon.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fElement.setIconProperty(fIcon.getText().getText());
			}
		});
		fLabelProperty.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fElement.setLabelProperty(fLabelProperty.getText().getText());
			}
		});
		fName.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fElement.setName(fName.getText().getText());
			}
		});
		fDeprecated.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fElement.setDeprecatedProperty(fDeprecated.getSelectionIndex() == 0);
			}
		});
		fTranslatable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fElement.setTranslatableProperty(fTranslatable.getSelectionIndex() == 0);
			}
		});
	}
}
