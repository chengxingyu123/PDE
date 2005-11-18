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

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaInclude;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaInclude;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class SchemaIncludesSection extends PDESection {

	private TableViewer fViewer;
	private Button fRemove;
	private Button fAdd;
	
	class SchemaIncludesLabelProvider extends LabelProvider {
		public String getText(Object element) {
			return super.getText(element);
		}
	}
	class SchemaIncludesContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object inputElement) {
			return getSchema().getIncludes();
		}
	}
	
	public SchemaIncludesSection(SchemaOverviewPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText("Schema Includes");
		getSection().setDescription("Placeholder................");
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		container.setLayout(new TableWrapLayout());
		
		toolkit.createLabel(container, "Schema Includes:")
			.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		Composite comp = toolkit.createComposite(container);
		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = layout.topMargin = layout.rightMargin = layout.leftMargin = 0;
		layout.numColumns = 2;
		comp.setLayout(layout);
		comp.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		Table table = toolkit.createTable(comp, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		TableWrapData twd = new TableWrapData(TableWrapData.FILL_GRAB);
		twd.heightHint = 80;
		twd.maxWidth = 200;
		table.setLayoutData(twd);
		Composite buttonComp = toolkit.createComposite(comp);
		layout = new TableWrapLayout();
		layout.leftMargin = layout.topMargin = layout.rightMargin = layout.leftMargin = 0;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new TableWrapData());
		fAdd = toolkit.createButton(buttonComp, "Add...", SWT.PUSH);
		fAdd.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		fAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleNewInclude();
			}
		});
		fRemove = toolkit.createButton(buttonComp, "Remove", SWT.PUSH);
		fRemove.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		fRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveInclude();
				
			}
		});
		
		fViewer = new TableViewer(table);
		fViewer.setLabelProvider(new SchemaIncludesLabelProvider());
		fViewer.setContentProvider(new SchemaIncludesContentProvider());
		fViewer.setInput(new Object());
		
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	protected void handleRemoveInclude() {
		Object[] selected = new Object[0];
		ISelection selection = fViewer.getSelection();
		if (selection.isEmpty()) return;
		if (selection instanceof StructuredSelection) {
			selected = ((StructuredSelection)selection).toArray();
			Schema schema = (Schema)getSchema();
			for (int i = 0; i < selected.length; i++) {
				schema.removeInclude((ISchemaInclude)selected[i]);
			}
		}
		fViewer.remove(selected);
	}

	public void dispose() {
		ISchema schema = getSchema();
		if (schema != null)
			schema.removeModelChangedListener(this);
		super.dispose();
	}

	public void initialize() {
		ISchema schema = getSchema();
		refresh();
		schema.addModelChangedListener(this);
	}
	
	private ISchema getSchema() {
		return (ISchema) getPage().getModel();
	}
	
	private void handleNewInclude() {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(getPage().getSite().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.ProductExportWizardPage_fileSelection); 
		dialog.setMessage(PDEUIMessages.ProductExportWizardPage_productSelection); 
		dialog.addFilter(new FileExtensionFilter("exsd"));  //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			Object result = dialog.getFirstResult();
			if (!(result instanceof IFile)) return;
			IFile newInclude = (IFile)result;
			
			String location = "schema:/" + newInclude.getFullPath().toString();
			ISchemaInclude include = new SchemaInclude(getSchema(), location, false);
			ISchema schema = getSchema();
			if (schema instanceof Schema)
				((Schema)schema).addInclude(include);
			fViewer.add(include);
		}
	}
}
