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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaInclude;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaInclude;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class SchemaIncludesSection extends TableSection {

	private TableViewer fViewer;

	class SchemaIncludesLabelProvider extends LabelProvider {
		public String getText(Object element) {
			return super.getText(element);
		}
	}
	class SchemaContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ISchema)
				return ((ISchema)inputElement).getIncludes();
			return new Object[0];
		}
	}

	public SchemaIncludesSection(SchemaOverviewPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] { "Add...", "Remove" });
		getSection().setText("Schema Inclusions");
		getSection().setDescription("This schema includes the following schemas:");
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fViewer = tablePart.getTableViewer();
		fViewer.setLabelProvider(new SchemaIncludesLabelProvider());
		fViewer.setContentProvider(new ArrayContentProvider());
		fViewer.setInput(getSchema());

		getSchema().addModelChangedListener(this);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleNewInclude();
		else
			handleRemoveInclude();
	}

	public void dispose() {
		ISchema schema = getSchema();
		if (schema != null)
			schema.removeModelChangedListener(this);
		super.dispose();
	}

	public void modelChanged(IModelChangedEvent e) {
		int changeType = e.getChangeType();
		if (changeType == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object[] objects = e.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof ISchemaInclude) {
				if (changeType == IModelChangedEvent.INSERT) {
					fViewer.add(objects[i]);
				} else if (changeType == IModelChangedEvent.REMOVE) {
					fViewer.remove(objects[i]);
				}
			}
		}
	}

	private ISchema getSchema() {
		return (ISchema) getPage().getModel();
	}

	protected void handleRemoveInclude() {
		Object[] selected = new Object[0];
		ISelection selection = fViewer.getSelection();
		if (selection.isEmpty())
			return;
		if (selection instanceof StructuredSelection) {
			selected = ((StructuredSelection) selection).toArray();
			Schema schema = (Schema) getSchema();
			for (int i = 0; i < selected.length; i++) {
				schema.removeInclude((ISchemaInclude) selected[i]);
			}
		}
	}

	protected void handleNewInclude() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
								getPage().getSite().getShell(), 
								new WorkbenchLabelProvider(),
								new WorkbenchContentProvider());
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.ProductExportWizardPage_fileSelection);
		dialog.setMessage("Select an extension point schema file:");
		dialog.addFilter(new FileExtensionFilter("exsd")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			Object result = dialog.getFirstResult();
			if (!(result instanceof IFile))
				return;
			IFile newInclude = (IFile) result;

			String location = "schema:/" + newInclude.getFullPath().toString();
			ISchemaInclude include = new SchemaInclude(getSchema(), location, false);
			ISchema schema = getSchema();
			if (schema instanceof Schema)
				((Schema) schema).addInclude(include);
		}
	}
}
