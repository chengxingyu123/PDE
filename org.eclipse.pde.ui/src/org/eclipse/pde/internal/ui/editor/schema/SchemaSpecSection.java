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
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class SchemaSpecSection extends PDESection {
	private FormEntry pluginText;
	private FormEntry pointText;
	private FormEntry nameText;
	private Button fDependsButton;
		 public SchemaSpecSection(SchemaOverviewPage page, Composite parent) {
		 		 super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEUIMessages.SchemaEditor_SpecSection_title);
		getSection().setDescription(PDEUIMessages.SchemaEditor_SpecSection_desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}
	public void commit(boolean onSave) {
		pluginText.commit();
		pointText.commit();
		nameText.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		pluginText.cancelEdit();
		pointText.cancelEdit();
		nameText.cancelEdit();
		super.cancelEdit();
	}
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		final Schema schema = (Schema) getPage().getModel();
		pluginText = new FormEntry(container, toolkit, PDEUIMessages.SchemaEditor_SpecSection_plugin, null, false);
		pluginText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				schema.setPluginId(text.getValue());
			}
		});
		pointText = new FormEntry(container, toolkit, PDEUIMessages.SchemaEditor_SpecSection_point, null, false);
		pointText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				schema.setPointId(text.getValue());
			}
		});
		nameText = new FormEntry(container, toolkit, PDEUIMessages.SchemaEditor_SpecSection_name, null, false);
		nameText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				schema.setName(text.getValue());
				getPage().getManagedForm().getForm().setText(schema.getName());
			}
		});
		
		Composite comp = toolkit.createComposite(container);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		comp.setLayout(layout);
		comp.setLayoutData(gd);
		fDependsButton = toolkit.createButton(comp, "Extending plug-ins must declare dependency", SWT.CHECK);
		fDependsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ISchema schema = (ISchema)getPage().getModel();
				schema.setExtensionsDependant(fDependsButton.getSelection());
			}
		});
		
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}
	public void dispose() {
		ISchema schema = (ISchema) getPage().getModel();
		if (schema!=null)
			schema.removeModelChangedListener(this);
		super.dispose();
	}
	public void initialize() {
		ISchema schema = (ISchema) getPage().getModel();
		refresh();
		if (!(schema instanceof IEditable)) {
			pluginText.getText().setEnabled(false);
			pointText.getText().setEnabled(false);
			nameText.getText().setEnabled(false);
			fDependsButton.setEnabled(false);
		}
		schema.addModelChangedListener(this);
	}

	public void setFocus() {
		if (pointText != null)
			pointText.getText().setFocus();
	}
	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}

	public void refresh() {
		ISchema schema = (ISchema)getPage().getModel();
		setIfDefined(pluginText, schema.getPluginId());
		setIfDefined(pointText, schema.getPointId());
		setIfDefined(nameText, schema.getName());
		fDependsButton.setSelection(schema.extensionsDependant());
		getPage().getManagedForm().getForm().setText(schema.getName());
		super.refresh();
	}
}
