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
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ExtraSchemaSpecSection extends PDESection {

	 public ExtraSchemaSpecSection(SchemaOverviewPage page, Composite parent) {
 		 super(page, parent, Section.DESCRIPTION);
 		 getSection().setText("Extra Schema Stuff");
 		 getSection().setDescription("Placeholder................");
 		 createClient(getSection(), page.getManagedForm().getToolkit());
	 }

	 public void createClient(Section section, FormToolkit toolkit) {
 		 Composite container = toolkit.createComposite(section);
 		 GridLayout layout = new GridLayout();
 		 layout.verticalSpacing = 9;
 		 container.setLayout(layout);

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
 		 schema.addModelChangedListener(this);
	 }
}
