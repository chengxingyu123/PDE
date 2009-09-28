/*******************************************************************************
 * Copyright (c) 2009 Anyware Technologies Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin.rows;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.pde.internal.ui.editor.text.PDETextHover;
import org.eclipse.pde.ui.customattributes.ICustomAttributeEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class CustomAttributeRow extends ExtensionAttributeRow {
	private ICustomAttributeEditor editor;

	public CustomAttributeRow(IContextPart part, ISchemaAttribute att, ICustomAttributeEditor editor) {
		super(part, att);
		this.editor = editor;
	}

	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		super.createContents(parent, toolkit, span);
		createLabel(parent, toolkit);
		Composite container = toolkit.createComposite(parent);
		editor.createContents(container, toolkit, this);
		container.setLayoutData(createGridData(span));
		editor.setEditable(part.isEditable());
		PDETextHover.addHoverListenerToControl(fIC, editor.getMainControl(), this);
	}

	protected GridData createGridData(int span) {
		GridData gd = new GridData(span == 2 ? GridData.FILL_HORIZONTAL : GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint = 20;
		gd.horizontalSpan = span - 1;
		gd.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		return gd;
	}

	protected void update() {
		blockNotification = true;
		editor.setValue(getValue());
		blockNotification = false;
	}

	public void commit() {
		if (dirty && input != null) {
			String value = editor.getValue();
			try {
				input.setAttribute(getName(), value);
				dirty = false;
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	public void setFocus() {
		editor.getMainControl().setFocus();
	}

	public void markDirty() {
		super.markDirty();
		PDETextHover.updateHover(fIC, getHoverContent(editor.getMainControl()));
	}
}
