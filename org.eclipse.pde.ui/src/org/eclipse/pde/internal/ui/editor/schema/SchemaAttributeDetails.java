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
import java.util.ArrayList;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.core.schema.ChoiceRestriction;
import org.eclipse.pde.internal.core.schema.SchemaAttribute;
import org.eclipse.pde.internal.core.schema.SchemaEnumeration;
import org.eclipse.pde.internal.core.schema.SchemaSimpleType;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaAttributeDetails extends AbstractSchemaDetails {
	
	private SchemaAttribute fAttribute;
	private FormEntry fValue;
	private FormEntry fName;
	private ComboPart fDeprecated;
	private ComboPart fKind;
	private ComboPart fTranslatable;
	private ComboPart fType;
	private ComboPart fUse;
	private TableViewer fRestrictionsTable;
	private FormEntry fClassEntry;
	private FormEntry fInterfaceEntry;
	private Text fNewRestriction;
	private Button fAddRestriction;
	private Button fRemoveRestriction;
	private Label fResLabel;
	
	public SchemaAttributeDetails(ISchemaAttribute attribute, ElementSection section) {
		super(section, false);
		fAttribute = (SchemaAttribute)attribute;
	}

	class SchemaAttributeLabelProvider extends LabelProvider {
		public String getText(Object element) {
			return super.getText(element);
		}
	}
	class SchemaAttributeContentProvider extends DefaultTableProvider {
		
		public Object[] getElements(Object inputElement) {
			ISchemaSimpleType type = fAttribute.getType();
			ISchemaRestriction restriction = type.getRestriction();
			if (type.getName().equals(BOOLEAN_TYPE))
				return BOOLS;
			else if (restriction != null)
				return restriction.getChildren();
			return new Object[0];
		}
	}
	
	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Color foreground = toolkit.getColors().getColor(FormColors.TITLE);
		
		fName = new FormEntry(parent, toolkit, "Name:", SWT.NONE);
		
		toolkit.createLabel(parent, "Deprecated:").setForeground(foreground);
		fDeprecated = createComboPart(parent, toolkit, BOOLS, 2);
	
		toolkit.createLabel(parent, "Translatable:").setForeground(foreground);
		fTranslatable = createComboPart(parent, toolkit, BOOLS, 2);
		
		toolkit.createLabel(parent, "Type:").setForeground(foreground);
		fType = createComboPart(parent, toolkit, new String[] {STRING_TYPE, BOOLEAN_TYPE}, 2);
		
		fResLabel = toolkit.createLabel(parent, "Restrictions:");
		fResLabel.setForeground(foreground);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		gd.horizontalIndent = 6;
		gd.verticalIndent = 2;
		fResLabel.setLayoutData(gd);

		Composite tableComp = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout(); layout.marginHeight = layout.marginWidth = 0;
		tableComp.setLayout(layout);
		tableComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fNewRestriction = toolkit.createText(tableComp, "");
		fNewRestriction.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Table table = toolkit.createTable(tableComp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		table.setLayoutData(gd);
		fRestrictionsTable = new TableViewer(table);
		fRestrictionsTable.setContentProvider(new SchemaAttributeContentProvider());
		fRestrictionsTable.setLabelProvider(new SchemaAttributeLabelProvider());
		
		Composite resButtonComp = toolkit.createComposite(parent);
		layout = new GridLayout(); layout.marginHeight = layout.marginWidth = 0;
		resButtonComp.setLayout(layout);
		resButtonComp.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		fAddRestriction = toolkit.createButton(resButtonComp, "Add", SWT.NONE);
		fRemoveRestriction = toolkit.createButton(resButtonComp, "Remove", SWT.NONE);
		fAddRestriction.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveRestriction.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		toolkit.createLabel(parent, "Use:").setForeground(foreground);
		fUse = createComboPart(parent, toolkit, new String[] {"optional", "required", "default"}, 2);
		
		fValue = new FormEntry(parent, toolkit, "Default Value:", null, false, 6);
		fValue.setDimLabel(true);
		
		toolkit.createLabel(parent, "Kind:").setForeground(foreground);
		fKind = createComboPart(parent, toolkit, new String[] {STRING_TYPE, "java", "resource"}, 2);
		
		fClassEntry = new FormEntry(parent, toolkit, "Superclass:", "Browse...", true, 6);
		fInterfaceEntry = new FormEntry(parent, toolkit, "Interface:", "Browse...", true, 6);
		
		setText("Attribute Details");
		setDecription("Properties for the \"" + fAttribute.getName() + "\" attribute.");
	}

	public void updateFields() {
		if (fAttribute == null)
			return;
		String curr = fAttribute.getName();
		fName.setValue(curr != null ? curr : "");
		
		fDeprecated.select(fAttribute.isDeprecated() ? 0 : 1);
		
		fTranslatable.select(fAttribute.isTranslatable() ? 0 : 1);
		
		boolean isStringType = fAttribute.getType().getName().equals(STRING_TYPE);
		fType.select(isStringType ? 0 : 1);
		
		fUse.select(fAttribute.getUse());
		Object value = fAttribute.getValue();
		fValue.setEditable(fAttribute.getUse() == 2);
		fValue.setValue(value != null ? value.toString() : "");
		
		int kind = fAttribute.getKind();

		fKind.select(kind);
		
		fInterfaceEntry.setVisible(kind == IMetaAttribute.JAVA);
		fClassEntry.setVisible(kind == IMetaAttribute.JAVA);
		if (kind == IMetaAttribute.JAVA) {
			curr = fAttribute.getBasedOn();
			if (curr != null && curr.length() > 0) {
				int index = curr.indexOf(":");
				if (index == -1) {
					String className = curr.substring(curr.lastIndexOf(".") + 1);
					if (className.length() > 1 && className.charAt(0) == 'I')
						fInterfaceEntry.setValue(curr);
					else
						fClassEntry.setValue(curr);
				} else {
					fClassEntry.setValue(curr.substring(0, index));
					fInterfaceEntry.setValue(curr.substring(index + 1));
				}
			}
		} else {
			if (fAttribute.getBasedOn() != null)
				fAttribute.setBasedOn(null);
		}
		
		updateResTable(isStringType);
	}

	public void hookListeners() {
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fValue.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fAttribute.setValue(fValue.getValue());
			}
		});
		fName.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				fAttribute.setName(fName.getValue());
			}
		});
		fDeprecated.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAttribute.setDeprecatedProperty(fDeprecated.getSelection().equals(BOOLS[0]));
			}
		});
		fKind.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int kind = fKind.getSelectionIndex();
				fAttribute.setKind(kind);
				fInterfaceEntry.setVisible(kind == IMetaAttribute.JAVA);
				fClassEntry.setVisible(kind == IMetaAttribute.JAVA);
				updateResTable(kind == IMetaAttribute.STRING);
				if (kind != IMetaAttribute.STRING)
					fType.select(0);
				
				ISchemaSimpleType type = fAttribute.getType();
				if (type instanceof SchemaSimpleType
						&& ((SchemaSimpleType) type).getRestriction() != null) {
					((SchemaSimpleType) type).setRestriction(null);
				}

			}
		});
		fTranslatable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAttribute.setTranslatableProperty(fTranslatable.getSelection().equals(BOOLS[0]));
			}
		});
		fType.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAttribute.setType(new SchemaSimpleType(fAttribute.getSchema(), fType.getSelection()));
				updateResTable(fType.getSelection().equals(STRING_TYPE));
			}
		});
		fUse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int use = fUse.getSelectionIndex();
				fAttribute.setUse(use);
				if (use == 2 && fValue.getValue().length() == 0) {
					fValue.setValue("(ENTER DEFAULT)");
					fValue.getText().setSelection(0, fValue.getValue().length());
					fValue.getText().setFocus();
				}
				if (use != 2)
					fValue.setValue("");
				fValue.setEditable(use == 2);
			}
		});
		fClassEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				setBasedOn();
			}
			public void linkActivated(HyperlinkEvent e) {
				String value = fClassEntry.getValue();
				value = handleLinkActivated(value, false);
				if (value != null)
					fClassEntry.setValue(value);
			}
			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(
						IJavaElementSearchConstants.CONSIDER_CLASSES, fClassEntry);
			}
		});
		fInterfaceEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				setBasedOn();
			}
			public void linkActivated(HyperlinkEvent e) {
				String value = fInterfaceEntry.getValue();
				value = handleLinkActivated(value, true);
				if (value != null)
					fInterfaceEntry.setValue(value);
			}
			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(
						IJavaElementSearchConstants.CONSIDER_INTERFACES, fInterfaceEntry);
			}
		});
		fAddRestriction.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String text = fNewRestriction.getText();
				if (text.length() > 0) {
					ChoiceRestriction res = (ChoiceRestriction)fAttribute.getType().getRestriction();
					Vector vres = new Vector();
					if (res != null)  {
						Object[] currRes = res.getChildren();
						for (int i = 0; i < currRes.length; i++) {
							vres.add(currRes[i]);
						}
					}
					vres.add(new SchemaEnumeration(fAttribute.getSchema(), text));
					if (res == null)
						res = new ChoiceRestriction(fAttribute .getSchema());
					ISchemaSimpleType type = fAttribute.getType();
					if (type instanceof SchemaSimpleType)
							((SchemaSimpleType)type).setRestriction(res);
					res.setChildren(vres);
					fRestrictionsTable.refresh();
				}
			}
		});
		fRemoveRestriction.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ISelection selection = fRestrictionsTable.getSelection();
				if (selection.isEmpty()) return;
			}
		});
	}
	
	private String handleLinkActivated(String value, boolean isInter) {
		IProject project = getPage().getPDEEditor().getCommonProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement element = javaProject.findType(value.replace('$', '.'));
				if (element != null)
					JavaUI.openInEditor(element);
				else {
					NewClassCreationWizard wizard = new NewClassCreationWizard(project, isInter);
					WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					if (dialog.open() == WizardDialog.OK) {
						return wizard.getClassName();
					}
				}
			}
		} catch (PartInitException e1) {
		} catch (CoreException e1) {
		}
		return null;
	}
	
	private void setBasedOn() {
		String classEntry = fClassEntry.getValue();
		String interfaceEntry = fInterfaceEntry.getValue();
		StringBuffer sb = new StringBuffer();
		if (classEntry.length() > 0)
			sb.append(classEntry);
		if (classEntry.length() > 0 && interfaceEntry.length() > 0)
			sb.append(":");
		if (interfaceEntry.length() > 0)
			sb.append(interfaceEntry);
		fAttribute.setBasedOn(sb.length() > 0 ? sb.toString() : null);
	}
	
	private void doOpenSelectionDialog(int scopeType, FormEntry entry) {
		try {
			IProject project = getPage().getPDEEditor().getCommonProject();
			if (project != null) {
				String filter = entry.getValue();
				filter = filter.substring(filter.lastIndexOf(".") + 1);
				SelectionDialog dialog = JavaUI.createTypeDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						PlatformUI.getWorkbench().getProgressService(),
						getSearchScope(project), scopeType, false, filter); //$NON-NLS-1$
				dialog.setTitle(PDEUIMessages.GeneralInfoSection_selectionTitle); 
				if (dialog.open() == SelectionDialog.OK) {
					IType type = (IType) dialog.getResult()[0];
					entry.setValue(type.getFullyQualifiedName('$'));
				}
			}
		} catch (CoreException e) {
		}
	}
	
	private IJavaSearchScope getSearchScope(IProject project) {
		return SearchEngine.createJavaSearchScope(getDirectRoots(JavaCore.create(project)));
	}
	
	private void updateResTable(boolean enabled) {
		fResLabel.setEnabled(enabled);
		fNewRestriction.setEnabled(enabled);
		fRestrictionsTable.getControl().setEnabled(enabled);
		fAddRestriction.setEnabled(enabled);
		fRemoveRestriction.setEnabled(enabled);
		fRestrictionsTable.refresh();
	}
	
	private IPackageFragmentRoot[] getDirectRoots(IJavaProject project) {
		ArrayList result = new ArrayList();
		try {
			IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE
						|| (roots[i].isArchive() && !roots[i].isExternal())) {
					result.add(roots[i]);
				}
			}
		} catch (JavaModelException e) {
		}
		return (IPackageFragmentRoot[]) result.toArray(new IPackageFragmentRoot[result.size()]);
	}
}
