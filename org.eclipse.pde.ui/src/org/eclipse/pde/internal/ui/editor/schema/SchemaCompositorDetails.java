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
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.schema.SchemaCompositor;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaCompositorDetails extends AbstractSchemaDetails {

	private SchemaCompositor fCompositor;
	private FormEntry fMaxOccurs;
	private FormEntry fMinOccurs;
	private ComboPart fKind;
	
	public SchemaCompositorDetails(ISchemaCompositor compositor, ElementSection section) {
		super(section, true);
		fCompositor = (SchemaCompositor)compositor;
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		
		fMinOccurs = new FormEntry(parent, toolkit, "Min Occurences:", SWT.NONE);
		fMaxOccurs = new FormEntry(parent, toolkit, "Max Occurences:", SWT.NONE);
		
		toolkit.createLabel(parent, "Type:").setForeground(
				toolkit.getColors().getColor(FormColors.TITLE));;
		fKind = new ComboPart();
		fKind.createControl(parent, toolkit, SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fKind.getControl().setLayoutData(gd);
		fKind.setItems(new String[] {
				ISchemaCompositor.kindTable[ISchemaCompositor.CHOICE],
				ISchemaCompositor.kindTable[ISchemaCompositor.SEQUENCE]});
		fKind.getControl().setEnabled(isEditable());
		
		
		setText("Compositor Details");
		setDecription("Properties for the \"" + fCompositor.getName() + "\" compositor.");
	}

	public void updateFields() {
		if (fCompositor == null)
			return;
		String curr = fCompositor.getMinOccurs() + "";
		fMinOccurs.getText().setText(curr);
		int max = fCompositor.getMaxOccurs();
		if (max == Integer.MAX_VALUE)
			curr = "*";
		else
			curr = max + "";
		fMaxOccurs.getText().setText(curr);
		fKind.select(fCompositor.getKind() - 1);
	}

	public void hookListeners() {
		fMaxOccurs.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				String maxString = fMaxOccurs.getText().getText();
				if (maxString.equals("*"))
					fCompositor.setMaxOccurs(Integer.MAX_VALUE);
				else {
					try {
						int max = Integer.parseInt(maxString);
						if (max > 0)
							fCompositor.setMaxOccurs(max);
					} catch (NumberFormatException e) {}
				}
			}
		});
		fMinOccurs.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				try {
					int min = Integer.parseInt(fMinOccurs.getText().getText());
					if (min >= 0)
						fCompositor.setMinOccurs(min);
				} catch (NumberFormatException e) {}
			}
		});
		fKind.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fCompositor.setKind(fKind.getSelectionIndex() + 1);
				setDecription("Properties for the \"" + fCompositor.getName() + "\" compositor.");
			}
		});
	}
}
