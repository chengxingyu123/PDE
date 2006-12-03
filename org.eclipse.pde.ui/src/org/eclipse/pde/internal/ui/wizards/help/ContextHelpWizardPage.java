/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.help;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.text.help.ContextHelpModel;
import org.eclipse.pde.internal.core.text.help.ContextNode;
import org.eclipse.pde.internal.core.text.help.ContextTopicNode;
import org.eclipse.pde.internal.core.text.help.ContextsNode;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.FileTreeDialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

public class ContextHelpWizardPage extends WizardPage implements ISelectionChangedListener, IModelChangedListener {

	private static final int F_CTW = 400;
	private static final String[] F_COL_LABS = new String[] {"Label", "HREF"};
	private static final String[] F_COL_PROP = new String[] {ContextTopicNode.F_LABEL,ContextTopicNode.F_HREF};
	
	private ContextHelpModel[] fModels;
	private ContextHelpLabelProvider fLabelProvider;
	private Text fID;
	private ContextContentProvider fContentProvider;
	private TableViewer fContextTopicTable;
	private ContextTopicNode fContextTopicSel;
	private Text fDescription;
	private ContextHelpModel fCurrModel;
	private TreeViewer fContextsViewer;
	private Object fContextsSel;
	private String fNewLabel = new String();
	private Button fAddConButton;
	private Button fRemConButton;
	private Button fAddTopButton;
	private Button fRemTopButton;
	private Button fClearHrefButton;
	private Button fChangeIDButton;
	private ICompilationUnit fCurrJavaCU;
	
	private class ContextContentProvider implements ITreeContentProvider{
		public Object[] getChildren(Object parent) {
			if (parent instanceof ContextHelpModel)
				return ((ContextHelpModel)parent).getContexts().getChildNodes();
			return null;
		}
		public boolean hasChildren(Object element) {
			return element instanceof ContextHelpModel;
		}
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Object[])
				return (Object[])inputElement;
			return new Object[] {inputElement};
		}
		public void dispose() {}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		public Object getParent(Object element) {return null;}
	}
	
	private class JavaContentProvider extends StandardJavaElementContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IJavaProject[])
				return (IJavaProject[])parent;
			return super.getElements(parent);
		}
		// StandardJavaElementContentProvider returns non java resources.. modifed
		// method to return only java elements
		// see org.eclipse.jdt.ui.StandardJavaElementContentProvider#getPackageFragmentRoots(IJavaProject)
		protected Object[] getPackageFragmentRoots(IJavaProject project) throws JavaModelException {
			if (!project.getProject().isOpen())
				return NO_CHILDREN;
				
			IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
			List list= new ArrayList(roots.length);
			for (int i = 0; i < roots.length; i++) {
				IPackageFragmentRoot root= roots[i];
				if (root.isExternal())
					continue;
				if (isProjectPackageFragmentRoot(root)) {
					Object[] fragments= getPackageFragmentRootContent(root);
					for (int j= 0; j < fragments.length; j++)
						list.add(fragments[j]);
				} else
					list.add(root);
			}
			return list.toArray();
		}
	}
	
	protected ContextHelpWizardPage(ContextHelpModel[] models) {
		super("context_help");
		setTitle("Help Context Maintenance");
		setDescription("Maintain you help context files.");
		fModels = models;
	}
	
	public void createControl(Composite parent) {
		Composite parentComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		parentComp.setLayout(layout);
		parentComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		SashForm sash = new SashForm(parentComp, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(GridData.FILL_BOTH));

		fLabelProvider = new ContextHelpLabelProvider();
		fLabelProvider.connect(this);
		fContentProvider = new ContextContentProvider();
		
		createContextsTree(sash);
		createContextInfo(sash);
		
		sash.setWeights(new int[] {1, 2});
		
		createJavaManagment(parentComp);
		
		setControl(parentComp);
		
		fContextsViewer.setInput(fModels);
		fContextsViewer.expandAll();
		
	}
	
	private void createContextsTree(Composite parent) {
		Composite leftComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		leftComp.setLayout(layout);
		leftComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Tree tree = new Tree(leftComp, SWT.BORDER);
		tree.setLayout(new GridLayout());
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		fContextsViewer = new TreeViewer(tree);
		fContextsViewer.setContentProvider(fContentProvider);
		fContextsViewer.setLabelProvider(fLabelProvider);
		
		Composite leftBComp = new Composite(leftComp, SWT.NONE);
		layout = new GridLayout(2, true);
		layout.marginHeight = layout.marginWidth = 0;
		leftBComp.setLayout(layout);
		leftBComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fAddConButton = new Button(leftBComp, SWT.PUSH);
		fAddConButton.setText("Add Context");
		fAddConButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddConButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// PRECONDITION: fContextsViewer's ISelectionChangedListener
				// is only enabling this button when we have a ContextNode or ContextHelpModel selection
				ContextsNode parent = null;
				if (fContextsSel instanceof ContextNode)
					parent = ((ContextNode)fContextsSel).getContexts();
				else
					parent = ((ContextHelpModel)fContextsSel).getContexts();
				ContextNode node = parent.getModel().getContextNodeFactory().createContextNode(parent);
				node.setParentNode(parent);
				node.setId(parent.getUnusedContextID());
			}
		});
		
		fRemConButton = new Button(leftBComp, SWT.PUSH);
		fRemConButton.setText("Remove Context");
		fRemConButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemConButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// PRECONDITION: fContextsViewer's ISelectionChangedListener
				// is only enabling this button when we have a ContextNode selection
				((ContextNode)fContextsSel).getContexts().removeContextNode(
						((ContextNode)fContextsSel).getId());
			}
		});
		
		fContextsViewer.addSelectionChangedListener(this);
		fAddConButton.setEnabled(false);
		fRemConButton.setEnabled(false);
	}
	
	private void createContextInfo(final Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		group.setText("Context Details");
		
		new Label(group, SWT.NONE).setText("Context ID:");
		fID = new Text(group, SWT.BORDER);
		fID.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fID.setEditable(false);
		fChangeIDButton = new Button(group, SWT.PUSH);
		fChangeIDButton.setText("Change ID...");
		fChangeIDButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ChangeContextIDDialog dialog = new ChangeContextIDDialog(parent.getShell(), (ContextNode)fContextsSel);
				if (dialog.open() == Window.OK)
					((ContextNode)fContextsSel).setId(dialog.getValue());
			}
		});

		Table table = new Table(group, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 3;
		gd.widthHint = F_CTW;
		gd.heightHint = 100;
		table.setLayoutData(gd);
		table.setHeaderVisible(true);
		
		TableColumn tc = new TableColumn(table, SWT.NONE);
		tc.setText(F_COL_LABS[0]);
		tc.setWidth(F_CTW/3);
		tc = new TableColumn(table, SWT.NONE);
		tc.setText(F_COL_LABS[1]);
		tc.setWidth(2*F_CTW/3);
		
		fContextTopicTable = new TableViewer(table);
		fContextTopicTable.setLabelProvider(fLabelProvider);
		fContextTopicTable.setContentProvider(fContentProvider);
		fContextTopicTable.setCellEditors(createCellEditors(table));
		fContextTopicTable.setCellModifier(new ContextHelpCellModifier());
		fContextTopicTable.setColumnProperties(F_COL_PROP);

		Composite buttonComp = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginHeight = layout.marginWidth = 0;
		buttonComp.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		buttonComp.setLayoutData(gd);
		
		Label l = new Label(buttonComp, SWT.NONE);
		l.setText("Description:");
		l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END));
		fAddTopButton = new Button(buttonComp, SWT.PUSH);
		fAddTopButton.setText("Add...");
		fAddTopButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fAddTopButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fContextsSel instanceof ContextNode) {
					fNewLabel = new String();
					MessageDialog dialog = new MessageDialog(parent.getShell(), "New Topic", null, "Enter the Topic label" ,
							MessageDialog.QUESTION, new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 0) {
						protected Control createCustomArea(Composite parent) {
							final Text labelText = new Text(parent, SWT.BORDER);
							labelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
							labelText.addModifyListener(new ModifyListener() {
								public void modifyText(ModifyEvent e) {
									fNewLabel = labelText.getText();
								}
							});
							return labelText;
						}
					};
					if (dialog.open() == Window.OK)
						((ContextNode)fContextsSel).addTopic(fNewLabel, null);
				}
			}
		});
		
		fRemTopButton = new Button(buttonComp, SWT.PUSH);
		fRemTopButton.setText("Remove");
		fRemTopButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fRemTopButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				((ContextNode)fContextTopicSel.getParent()).removeTopic(fContextTopicSel);
			}
		});
		
		fClearHrefButton = new Button(buttonComp, SWT.PUSH);
		fClearHrefButton.setText("Clear HREF");
		fClearHrefButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fClearHrefButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fContextTopicSel.setHref(null);
			}
		});
		
		fDescription = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 3;
		gd.heightHint = 50;
		fDescription.setLayoutData(gd);
		fDescription.addFocusListener(new FocusListener() {
			String origText = null;
			public void focusGained(FocusEvent e) {
				origText = fDescription.getText();
			}
			public void focusLost(FocusEvent e) {
				String text = fDescription.getText();
				if (text.equals(origText))
					return;
				if (text.length() == 0)
					text = null;
				((ContextNode)fContextsSel).setDescription(text);
			}
		});
		
		fContextTopicTable.addSelectionChangedListener(this);
		fAddTopButton.setEnabled(false);
		fRemTopButton.setEnabled(false);
		fClearHrefButton.setEnabled(false);
	}
	
	private void createJavaManagment(Composite parent) {
		Group g = new Group(parent, SWT.NONE);
		g.setText("Java ID managment");
		g.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		g.setLayout(new GridLayout());
		new Label(g, SWT.NONE).setText("Java file for IDs:");
		Composite comp = new Composite(g, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final Text javaText = new Text(comp, SWT.BORDER);
		javaText.setEditable(false);
		javaText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final Button editButton = new Button(comp,SWT.PUSH);
		editButton.setText("Manage...");
		editButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ContextJavaUnitManagmentDialog dialog = new ContextJavaUnitManagmentDialog(getShell(), fCurrJavaCU, fCurrModel);
				if (dialog.open() == Window.OK)
					dialog.applyChanges();
			}
		});
		editButton.setEnabled(false);
		
		final Button browseButton = new Button(comp, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), 
						new JavaElementLabelProvider(), new JavaContentProvider());
				IProject[] projects = PDEPlugin.getWorkspace().getRoot().getProjects();
				ArrayList list = new ArrayList();
				for (int i = 0; i < projects.length; i++)
					try {
						if (projects[i].isOpen() && projects[i].hasNature(JavaCore.NATURE_ID))
							list.add(JavaCore.create(projects[i]));
					} catch (CoreException exception) {}
				dialog.setAllowMultiple(false);
				dialog.setValidator(new ISelectionStatusValidator() {
					public IStatus validate(Object[] selection) {
						if (selection.length > 0 && selection[0] instanceof ICompilationUnit)
							return Status.OK_STATUS;
						return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), "Select a Java file.");
					}
				});
				dialog.setInput(list.toArray(new IJavaProject[list.size()]));
				if (dialog.open() == Window.OK) {
					fCurrJavaCU = (ICompilationUnit)dialog.getFirstResult();
					String javaPath = fCurrJavaCU.getPath().toString();
					javaText.setText(javaPath);
					editButton.setEnabled(true);
				} else
					editButton.setEnabled(false);
			}
		});
	}
	
//	private Object getSelectedJavaUnit() {
//		// TODO Auto-generated method stub
//		return null;
//	}
	
	public void dispose() {
		fLabelProvider.disconnect(this);
		super.dispose();
	}

	public void selectionChanged(SelectionChangedEvent event) {
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		Object obj = sel.getFirstElement();
		
		if (event.getSource() == fContextsViewer) {
			fContextsSel = sel != null ? sel.getFirstElement() : null;
			
			boolean isContext = fContextsSel instanceof ContextNode;
			fRemConButton.setEnabled(isContext);
			fAddConButton.setEnabled(isContext || fContextsSel instanceof ContextHelpModel);
			fAddTopButton.setEnabled(isContext);
			fChangeIDButton.setEnabled(isContext);
			
			setCurrentModel(fContextsSel);
			
			if (isContext) {
				fContextTopicTable.setInput(((ContextNode)obj).getTopics());
				fID.setText(((ContextNode)obj).getId());
				String desc = ((ContextNode)obj).getDescription();
				fDescription.setText(desc != null ? desc : new String());
			} else {
				fContextTopicTable.setInput(null);
				fID.setText(new String());
				fDescription.setText(new String());
			}
		} else if (event.getSource() == fContextTopicTable) {
			fContextTopicSel = sel != null && !sel.isEmpty() ? (ContextTopicNode)sel.getFirstElement() : null;
			fClearHrefButton.setEnabled(fContextTopicSel != null);
			fRemTopButton.setEnabled(fContextTopicSel != null);
		}
	}
	
	private void setCurrentModel(Object newSelection) {
		ContextHelpModel newModel = null;
		if (newSelection instanceof ContextNode)
			newModel = ((ContextNode)newSelection).getModel();
		else if (newSelection instanceof ContextHelpModel)
			newModel = (ContextHelpModel)newSelection;
		
		if (newModel == null) {
			if (fCurrModel != null)
				fCurrModel.removeModelChangedListener(this);
			fCurrModel = null;
		} else if (fCurrModel == null) {
			fCurrModel = newModel;
			fCurrModel.addModelChangedListener(this);
		} else if (fCurrModel != newModel) {
			fCurrModel.removeModelChangedListener(this);
			fCurrModel = newModel;
			fCurrModel.addModelChangedListener(this);
		}
	}
	
	private CellEditor[] createCellEditors(Composite parent) {
		CellEditor[] editors = new CellEditor[2];
		editors[0] = new TextCellEditor(parent);
		editors[1] = new FileTreeDialogCellEditor(
				new String[] {"html", "htm", "xhtml"}, 
				parent);
		return editors;
	}

	public void modelChanged(IModelChangedEvent event) {
		Object[] objs = event.getChangedObjects();
		for (int i = 0; i < objs.length; i++) {
			switch(event.getChangeType()) {
			case IModelChangedEvent.INSERT:
				if (objs[i] instanceof ContextTopicNode)
					fContextTopicTable.add(objs[i]);
				if (objs[i] instanceof ContextNode) {
					fContextsViewer.add(fCurrModel, objs[i]);
					fContextsViewer.setSelection(new StructuredSelection(objs[i]));
				}	
				break;
			case IModelChangedEvent.REMOVE:
				if (objs[i] instanceof ContextTopicNode)
					fContextTopicTable.remove(objs[i]);
				if (objs[i] instanceof ContextNode)
					fContextsViewer.remove(objs[i]);
				break;
			case IModelChangedEvent.CHANGE:
				if (objs[i] instanceof ContextNode) {
					fContextsViewer.update(objs[i], null);
					fID.setText(((ContextNode)objs[i]).getId());
					//fDescription.setText(((ContextNode)objs[i]).getDescription());
				}
				if (objs[i] instanceof ContextTopicNode)
					fContextTopicTable.update(objs[i], null);
				break;
			}
		}
	}

}
