/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.toc;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.itoc.ITocConstants;
import org.eclipse.pde.internal.core.toc.Toc;
import org.eclipse.pde.internal.core.toc.TocModel;
import org.eclipse.pde.internal.core.toc.TocObject;
import org.eclipse.pde.internal.core.toc.TocTopic;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.editor.actions.CollapseAction;
import org.eclipse.pde.internal.ui.editor.plugin.FormFilteredTree;
import org.eclipse.pde.internal.ui.editor.toc.actions.TocAddAnchorAction;
import org.eclipse.pde.internal.ui.editor.toc.actions.TocAddLinkAction;
import org.eclipse.pde.internal.ui.editor.toc.actions.TocAddObjectAction;
import org.eclipse.pde.internal.ui.editor.toc.actions.TocAddTopicAction;
import org.eclipse.pde.internal.ui.editor.toc.details.TocAbstractDetails;
import org.eclipse.pde.internal.ui.editor.toc.details.TocDetails;
import org.eclipse.pde.internal.ui.editor.toc.actions.TocRemoveObjectAction;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class TocTreeSection extends TreeSection {
	private TocModel fModel;
	private TreeViewer fTocTree;
	private FormFilteredTree fFilteredTree;
	private Clipboard fClipboard;

	private static final int F_BUTTON_ADD_TOPIC = 0;
	private static final int F_BUTTON_ADD_LINK = 3;
	private static final int F_BUTTON_ADD_ANCHOR = 4;
	private static final int F_BUTTON_REMOVE = 5;
	private static final int F_BUTTON_UP = 6;
	private static final int F_BUTTON_DOWN = 7;
	private static final int F_UP_FLAG = -1;
	private static final int F_DOWN_FLAG = 1;

	private CollapseAction fCollapseAction;
	private TocAddTopicAction fAddTopicAction;
	private TocAddLinkAction fAddLinkAction;
	private TocAddAnchorAction fAddAnchorAction;
	private TocRemoveObjectAction fRemoveObjectAction;
	
	public TocTreeSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, 
			new String[] { PDEUIMessages.TocPage_addTopic,
				null,
				null,
				PDEUIMessages.TocPage_addLink,
				PDEUIMessages.TocPage_addAnchor,
				PDEUIMessages.TocPage_remove,
				PDEUIMessages.TocPage_up,
				PDEUIMessages.TocPage_down } );

		fAddTopicAction = new TocAddTopicAction();
		fAddLinkAction = new TocAddLinkAction();
		fAddAnchorAction = new TocAddAnchorAction();
		fRemoveObjectAction = new TocRemoveObjectAction();
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		// Get the model
		fModel = (TocModel)getPage().getModel();		
		
		Composite container = createClientContainer(section, 2, toolkit);
		createTree(container, toolkit);
		toolkit.paintBordersFor(container);
		section.setText(PDEUIMessages.TocTreeSection_title);
		section.setDescription(PDEUIMessages.TocTreeSection_sectionDescription);
		section.setClient(container);
		initializeTreeViewer();
		createSectionToolbar(section, toolkit);
		// Create the adapted listener for the filter entry field
		fFilteredTree.createUIListenerEntryFilter(this);
		// TODO: Implement drag and drop
	}

	/**
	 * Adds a link (with hand cursor) for tree 'Collapse All' action,
	 * which collapses the TOC tree down to the second level
	 * 
	 * @param section
	 * @param toolkit
	 */
	private void createSectionToolbar(Section section, FormToolkit toolkit) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		// Cursor needs to be explicitly disposed
		toolbar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if ((handCursor != null) &&
						(handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});
		// Add collapse action to the tool bar
		fCollapseAction = new CollapseAction(fTocTree, 
				PDEUIMessages.ExtensionsPage_collapseAll, 
				1, 
				fModel.getToc());
		toolBarManager.add(fCollapseAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	/**
	 * @param container
	 * @param toolkit
	 */
	private void createTree(Composite container, FormToolkit toolkit) {
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		fTocTree = treePart.getTreeViewer();
		fTocTree.setContentProvider(new TocContentProvider());
		fTocTree.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		createTreeListeners();
		fClipboard = new Clipboard(getPage().getSite().getShell().getDisplay());
		initDragAndDrop();
	}

	private void initDragAndDrop()
	{	int ops = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
		Transfer[] dragTransfers = new Transfer[] { ModelDataTransfer.getInstance(), TextTransfer.getInstance() };
		Transfer[] dropTransfers = new Transfer[] { ModelDataTransfer.getInstance(), TextTransfer.getInstance(), FileTransfer.getInstance() };
		fTocTree.addDragSupport(ops, dragTransfers, new TocDragAdapter(fTocTree, this));
		fTocTree.addDropSupport(ops | DND.DROP_DEFAULT, dropTransfers, new TocDropAdapter(fTocTree, this));
	}

	/**
	 * 
	 */
	private void createTreeListeners() {
		// Create listener for the outline view 'link with editor' toggle 
		// button
		fTocTree.addPostSelectionChangedListener(
				getPage().getPDEEditor().new PDEFormEditorChangeListener());
	}

	private void initializeTreeViewer() {
		if (fModel == null) {
			return;
		}
		fTocTree.setInput(fModel);
		Toc toc = fModel.getToc();

		// Nodes can always be added to the root TOC node
		getTreePart().setButtonEnabled(F_BUTTON_ADD_TOPIC, true);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_ANCHOR, true);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_LINK, true);

		// Set to false because initial node selected is the root TOC node
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, false);
		// Set to false because initial node selected is the root TOC node
		getTreePart().setButtonEnabled(F_BUTTON_UP, false);
		// Set to false because initial node selected is the root TOC node
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, false);

		fTocTree.setSelection(new StructuredSelection(toc), true);
		fTocTree.expandToLevel(2);
	}

	public ISelection getSelection() {
		return fTocTree.getSelection();
	}

	public void fireSelection() {
		fTocTree.setSelection(fTocTree.getSelection());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		updateButtons();
	}

	public void updateButtons() {
		if (!fModel.isEditable()) {
			return;
		}
		Object object = ((IStructuredSelection) fTocTree.getSelection()).getFirstElement();
		TocObject tocObject = (TocObject)object;
		boolean canAddObject = false;
		boolean canRemove = false;
		boolean canMoveUp = false;
		boolean canMoveDown = false;

		if (tocObject != null) {
			canRemove = tocObject.canBeRemoved();
			
			TocObject parent = tocObject.getParent();
			if (tocObject.getType() == ITocConstants.TYPE_TOC ||
					parent.getType() == ITocConstants.TYPE_TOPIC ||
					parent.getType() == ITocConstants.TYPE_TOC) {
				//Semantic rule: 
				//As long as the parent of the selection is a child of a 
				//TOC root or a topic, then a new object can be added
				//either to the selection or to the parent
				canAddObject = true;
			}
			
			//Semantic rule:
			//You cannot rearrange the TOC root itself
			if(tocObject.getType() != ITocConstants.TYPE_TOC)
			{	if(parent != null)
				{	TocTopic topic = (TocTopic)parent;
					canMoveUp = !topic.isFirstChildObject(tocObject);
					canMoveDown = !topic.isLastChildObject(tocObject);
				}
			}
		}

		getTreePart().setButtonEnabled(F_BUTTON_ADD_TOPIC, canAddObject);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_LINK, canAddObject);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_ANCHOR, canAddObject);
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, canRemove);
		getTreePart().setButtonEnabled(F_BUTTON_UP, canMoveUp);
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, canMoveDown);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager manager) {
		// Get the current selection
		ISelection selection = fTocTree.getSelection();
		Object object = ((IStructuredSelection) selection).getFirstElement();
		// Has to be null or a TOC object
		TocObject tocObject = (TocObject)object;
		// Create the "New" sub-menu
		MenuManager submenu = new MenuManager(PDEUIMessages.Menus_new_label);
		// Add the "New" sub-menu to the main context menu
		manager.add(submenu);
		if (tocObject != null)
		{	// Remove task action
			fillContextMenuRemoveAction(manager, tocObject);
			fillContextMenuAddActions(submenu, tocObject);
		}
	}

	private void fillContextMenuAddActions(MenuManager submenu, 
			TocObject tocObject) {
		
		TocObject parentObject;
		if (tocObject.getType() == ITocConstants.TYPE_TOPIC || tocObject.getType() == ITocConstants.TYPE_TOC)
		{	parentObject = tocObject;
		}
		else
		{	parentObject = tocObject.getParent();
		}

		// Add to the "New" sub-menu
		// Add topic action
		fAddTopicAction.setParentObject(parentObject);
		fAddTopicAction.setEnabled(fModel.isEditable());
		submenu.add(fAddTopicAction);
		// Add to the "New" sub-menu
		// Add link action
		fAddLinkAction.setParentObject(parentObject);
		fAddLinkAction.setEnabled(fModel.isEditable());
		submenu.add(fAddLinkAction);
		// Add to the "New" sub-menu
		// Add anchor action
		fAddAnchorAction.setParentObject(parentObject);
		fAddAnchorAction.setEnabled(fModel.isEditable());
		submenu.add(fAddAnchorAction);
	}

	/**
	 * @param manager
	 * @param tocObject
	 */
	private void fillContextMenuRemoveAction(IMenuManager manager,
			TocObject tocObject) {
		// Add to the main context menu
		// Add a separator to the main context menu
		manager.add(new Separator());
		// Delete task object action
		fRemoveObjectAction.setToRemove(tocObject);
		manager.add(fRemoveObjectAction);

		fRemoveObjectAction.setEnabled(tocObject.canBeRemoved() && fModel.isEditable());
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		boolean cutAction = actionId.equals(ActionFactory.CUT.getId());
		
		if (cutAction || actionId.equals(ActionFactory.DELETE.getId())) {
			handleDeleteAction();
			return !cutAction;
		}

		return false;
	}	

	public void handleDrop(Object currentTarget, Object dropped, int currentOperation)
	{	if(dropped instanceof Object[])
		{	Object[] droppings = (Object[]) dropped;
			for(int i = 0; i < droppings.length; ++i)
			{	if(droppings[i] instanceof IResource)
				{	System.out.println(((IResource)droppings[i]).getFullPath());
				}
				else if(droppings[i] instanceof TocObject)
				{	
				}
			
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
		switch (index) {
		case F_BUTTON_ADD_TOPIC:
			handleAddAction(fAddTopicAction);
			break;
		case F_BUTTON_ADD_LINK:
			handleAddAction(fAddLinkAction);
			break;
		case F_BUTTON_ADD_ANCHOR:
			handleAddAction(fAddAnchorAction);
			break;
		case F_BUTTON_REMOVE:
			handleDeleteAction();
			break;
		case F_BUTTON_UP:
			handleMoveAction(F_UP_FLAG);
			break;
		case F_BUTTON_DOWN:
			handleMoveAction(F_DOWN_FLAG);
			break;
		}
	}
	
	private void handleAddAction(TocAddObjectAction action) {
		//Currently, all additions in the TOC editor are semantically similar
		//Thus, all addition operations can follow the same procedure
		
		ISelection sel = fTocTree.getSelection();
		Object object = ((IStructuredSelection) sel).getFirstElement();
		if (object == null) {
			return;
		}
	
		TocObject tocObject = (TocObject)object;
		
		if (tocObject.getType() == ITocConstants.TYPE_TOPIC
				|| tocObject.getType() == ITocConstants.TYPE_TOC) {
			action.setParentObject(tocObject);
			action.run();
		} else if (tocObject.getType() == ITocConstants.TYPE_LINK
				|| tocObject.getType() == ITocConstants.TYPE_ANCHOR) {
			action.setParentObject(tocObject.getParent());
			action.run();
		}
	}

	/**
	 * @param object
	 */
	private void handleDeleteAction()
	{	List objects = ((IStructuredSelection)fTocTree.getSelection()).toList();
	
		boolean beep = false;
		
		for(Iterator i = objects.iterator(); i.hasNext();)
		{	Object object = i.next();
			if (object instanceof TocObject)
			{	TocObject tocObject = (TocObject)object;
	
				if (!tocObject.canBeRemoved()) {
					i.remove();
					beep = true;
				}
			}
		}
	
		if(beep)
		{	Display.getCurrent().beep();
		}
	
		handleRemove(objects);
	}

	public void handleRemove(List itemsToRemove)
	{	if(!itemsToRemove.isEmpty())
		{	fRemoveObjectAction.setToRemove((TocObject[])itemsToRemove.toArray(new TocObject[itemsToRemove.size()]));
			fRemoveObjectAction.run();
		}
	}

	private void handleMoveAction(int positionFlag) {
		ISelection sel = fTocTree.getSelection();
		Object object = ((IStructuredSelection) sel).getFirstElement();
		if (object == null) {
			return;
		} else if (object instanceof TocObject) {
			TocObject tocObject = (TocObject)object;
			TocTopic parent = null;
			// Determine the parents type
			if (tocObject.getParent().getType() == ITocConstants.TYPE_TOPIC
				|| tocObject.getParent().getType() == ITocConstants.TYPE_TOC) {
				parent = (TocTopic)tocObject.getParent();
			} else {
				return;
			}
			// Move the object up or down one position
			parent.moveChild(tocObject, positionFlag);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		// No need to call super, world changed event handled here
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(event);
		} else if (event.getChangeType() == IModelChangedEvent.INSERT) {
			handleModelInsertType(event);
		} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
			handleModelRemoveType(event);
		} else if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			handleModelChangeType(event);
		}
	}
	
	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		TocObject object = (TocObject) objects[0];		
		if (object != null) 
		{	if (object.getType() == ITocConstants.TYPE_TOC) {
				// Get the form page
				TocPage page = (TocPage)getPage();			
				// Remember the currently selected page
				IDetailsPage previousDetailsPage = 
					page.getBlock().getDetailsPart().getCurrentPage();
				// Replace the current dirty model with the model reloaded from
				// file
				fModel = object.getModel();
				
				// Reset the tree viewer using the new model as input
				// TODO: MP: CompCS:  This is redundant and should be deleted
				// fTocTree.setInput(fModel);
				
				// Re-initialize the tree viewer.  Makes a details page selection
				initializeTreeViewer();
				
				// Get the current details page selection
				IDetailsPage currentDetailsPage = 
					page.getBlock().getDetailsPart().getCurrentPage();
				
				// If the selected page before the revert is the same as the 
				// selected page after the revert, then its fields will need to
				// be updated
				// TODO: MP: REVERT: LOW: Revisit to see if updating details page is necessary - especially after making static
				if (currentDetailsPage.equals(previousDetailsPage) && 
						currentDetailsPage instanceof TocDetails) {
					((TocAbstractDetails)currentDetailsPage).updateFields();
				}
			}		
		}
	}	
	
	/**
	 * @param event
	 */
	private void handleModelInsertType(IModelChangedEvent event) {
		// Insert event
		Object[] objects = event.getChangedObjects();
		TocObject object = (TocObject) objects[0];
		if (object != null)
		{	if (object.getType() != ITocConstants.TYPE_TOC) {
				handleTaskObjectInsert(object);
			}
		}
	}	

	/**
	 * @param object
	 */
	private void handleTaskObjectInsert(TocObject object) {
		// Refresh the parent element in the tree viewer
		// TODO: Can we get away with an update instead of a refresh here?
		fTocTree.refresh(object.getParent());
		// Select the new object in the tree
		fTocTree.setSelection(new StructuredSelection(object), true);		
	}
	
	/**
	 * @param event
	 */
	private void handleModelRemoveType(IModelChangedEvent event) {
		// Remove event
		Object[] objects = event.getChangedObjects();
		TocObject object = (TocObject) objects[0];
		if (object != null)
		{	if (object.getType() != ITocConstants.TYPE_TOC) {
				handleTaskObjectRemove(object);
			}
		}
	}

	/**
	 * @param object
	 */
	private void handleTaskObjectRemove(TocObject object) {
		// Remove the item
		fTocTree.remove(object);

		// Select the appropriate object
		TocObject tocObject = fRemoveObjectAction.getNextSelection();
		if (tocObject == null)
		{	tocObject = object.getParent();
		}

		if(tocObject.equals(object.getParent()))
		{	fTocTree.refresh(object.getParent());
		}
		
		fTocTree.setSelection(new StructuredSelection(tocObject), true);
	}	

	/**
	 * @param event
	 */
	private void handleModelChangeType(IModelChangedEvent event) {
		// Change event
		Object[] objects = event.getChangedObjects();
		TocObject object = (TocObject) objects[0];
		if (object != null)
		{	// Update the element in the tree viewer
			fTocTree.update(object, null);
		}
	}	

	protected TreeViewer createTreeViewer(Composite parent, int style) {
		fFilteredTree = new FormFilteredTree(parent, style, new PatternFilter());
		parent.setData("filtered", Boolean.TRUE); //$NON-NLS-1$
		return fFilteredTree.getViewer();
	}
	
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		if (fClipboard != null) {
			fClipboard.dispose();
			fClipboard = null;
		}
		
		super.dispose();
	}
}
