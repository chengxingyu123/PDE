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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.core.ischema.ISchemaObjectReference;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class SchemaElementReferenceDetails extends AbstractSchemaDetails {

	private SchemaElementReference fElement;
	private FormEntry fMaxOccurs;
	private FormEntry fMinOccurs;
	private Hyperlink fReferenceLink;
	
	public SchemaElementReferenceDetails(ISchemaObjectReference compositor, ElementSection section) {
		super(section, true);
		fElement = (SchemaElementReference)compositor;
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		
		fMinOccurs = new FormEntry(parent, toolkit, "Min Occurences:", SWT.NONE);
		fMaxOccurs = new FormEntry(parent, toolkit, "Max Occurences:", SWT.NONE);
		
		toolkit.createLabel(parent, "Reference:").setForeground(
				toolkit.getColors().getColor(FormColors.TITLE));
		fReferenceLink = toolkit.createHyperlink(parent, fElement.getName(), SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fReferenceLink.setLayoutData(gd);
		
		setText("Element Reference Details");
		setDecription("Properties for the \"" + fElement.getName() + "\" element reference.");
	}

	public void updateFields() {
		if (fElement == null)
			return;
		String curr = fElement.getMinOccurs() + "";
		fMinOccurs.getText().setText(curr);
		int max = fElement.getMaxOccurs();
		if (max == Integer.MAX_VALUE)
			curr = "*";
		else
			curr = max + "";
		fMaxOccurs.getText().setText(curr);
	}

	public void hookListeners() {
		fMaxOccurs.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				String maxString = fMaxOccurs.getText().getText();
				if (maxString.equals("*"))
					fElement.setMaxOccurs(Integer.MAX_VALUE);
				else {
					try {
						int max = Integer.parseInt(maxString);
						if (max > 0)
							fElement.setMaxOccurs(max);
					} catch (NumberFormatException e) {}
				}
			}
		});
		fMinOccurs.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				try {
					int min = Integer.parseInt(fMinOccurs.getText().getText());
					if (min >= 0)
						fElement.setMinOccurs(min);
				} catch (NumberFormatException e) {}
			}
		});
		fReferenceLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				fireMasterSelection(new StructuredSelection(fElement.getReferencedObject()));
			}
		});
	}
}
