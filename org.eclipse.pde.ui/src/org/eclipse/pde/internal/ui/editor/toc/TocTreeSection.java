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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.itoc.ITocConstants;
import org.eclipse.pde.internal.core.toc.Toc;
import org.eclipse.pde.internal.core.toc.TocLink;
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
import org.eclipse.pde.internal.ui.editor.toc.actions.TocRemoveObjectAction;
import org.eclipse.pde.internal.ui.editor.toc.details.TocAbstractDetails;
import org.eclipse.pde.internal.ui.editor.toc.details.TocDetails;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;
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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.keys.IBindingService;

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
	
	private TocDragAdapter fDragAdapter;
	private boolean fPreserveSelection;
	
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

	/**
	 * Initialize this section's drag and drop capabilities
	 */
	private void initDragAndDrop()
	{	int ops = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
		//Content dragged from the tree viewer can be treated as model objects (TocObjects)
		//or as text (XML represenation of the TocObjects)
		Transfer[] dragTransfers = new Transfer[] { ModelDataTransfer.getInstance(), TextTransfer.getInstance() };
		fDragAdapter = new TocDragAdapter(this);
		fTocTree.addDragSupport(ops, dragTransfers, fDragAdapter);

		//Model objects and files can be dropped onto the viewer
		//TODO: Consider allowing dropping of XML text
		Transfer[] dropTransfers = new Transfer[] { ModelDataTransfer.getInstance(), FileTransfer.getInstance() };
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
		// Populate the "New" sub-menu
		fillContextMenuAddActions(submenu, tocObject);
		// Add the "New" sub-menu to the main context menu
		manager.add(submenu);

		// Add a separator to the main context menu
		manager.add(new Separator());

		if (tocObject != null)
		{	String showInLabel = PDEUIMessages.PluginsView_showIn;
			IBindingService bindingService = (IBindingService) PlatformUI
				.getWorkbench().getAdapter(IBindingService.class);
			if (bindingService != null)
			{	String keyBinding = bindingService
				.getBestActiveBindingFormattedFor("org.eclipse.ui.navigate.showInQuickMenu"); //$NON-NLS-1$
				if (keyBinding != null)
				{	showInLabel += '\t' + keyBinding;
				}
			}

			// Add the "Show In" action and its contributions
			IMenuManager showInMenu = new MenuManager(showInLabel);
			showInMenu.add(ContributionItemFactory.VIEWS_SHOW_IN
				.create(getPage().getSite().getWorkbenchWindow()));
				
			manager.add(showInMenu);
			manager.add(new Separator());
			
			// Add the TOC object removal action
			fillContextMenuRemoveAction(manager, tocObject);
		}
	}

	private void fillContextMenuAddActions(MenuManager submenu, 
			TocObject tocObject) {
		
		TocObject parentObject;
		if(tocObject == null)
		{	parentObject = fModel.getToc();
		}
		else if (tocObject.canBeParent())
		{	parentObject = tocObject;
		}
		else
		{	parentObject = tocObject.getParent();
		}

		// Add to the sub-menu
		// Add topic action
		fAddTopicAction.setParentObject(parentObject);
		fAddTopicAction.setEnabled(fModel.isEditable());
		submenu.add(fAddTopicAction);
		// Add to the sub-menu
		// Add link action
		fAddLinkAction.setParentObject(parentObject);
		fAddLinkAction.setEnabled(fModel.isEditable());
		submenu.add(fAddLinkAction);
		// Add to the sub-menu
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

	protected void handleDoubleClick(IStructuredSelection selection) {
		Object selected = selection.getFirstElement();
		if(selected instanceof TocObject)
		{	String path = ((TocObject)selected).getPath();

			if(path != null)
			{	openDocument(path);
			}
		}
	}

	public void openDocument(String path)
	{	IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		Path resourcePath = new Path(path);
		if(!resourcePath.isEmpty())
		{	IPath pluginPath = fModel.getUnderlyingResource().getProject().getFullPath();
			IResource resource = root.findMember(pluginPath.append(resourcePath));
			try
			{	if (resource != null && resource instanceof IFile)
				{	IDE.openEditor(PDEPlugin.getActivePage(), (IFile)resource, true);
				}
				else
				{	MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_warning);
				}
			}
			catch (PartInitException e)
			{	//suppress exception
			}
		}
		else
		{	MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_emptyPath);
		}
	}

	/**
	 * 
	 * @param currentTarget
	 * @param dropped
	 * @param location
	 * @return true iff the drop was successful
	 */
	public boolean performDrop(Object currentTarget, Object dropped, int location)
	{	if(dropped instanceof Object[])
		{	TocObject tocTarget = (TocObject)currentTarget;
			TocTopic targetParent = determineParent(tocTarget, location);

			if(targetParent == tocTarget
					&& location == TocDropAdapter.LOCATION_JUST_AFTER
					&& !tocTarget.getChildren().isEmpty()
					&& fTocTree.getExpandedState(tocTarget))
			{	location = ViewerDropAdapter.LOCATION_BEFORE;
				tocTarget = (TocObject)tocTarget.getChildren().get(0);
			}

			if(targetParent != null)
			{	ArrayList objectsToAdd = getObjectsToAdd((Object[])dropped, targetParent);
				if(objectsToAdd != null && !objectsToAdd.isEmpty())
				{	boolean insertBefore = (location == ViewerDropAdapter.LOCATION_BEFORE);
					handleMultiAddAction(objectsToAdd, tocTarget, insertBefore, targetParent);
					return true;
				}
			}
		}
	
		return false;
	}

	private TocTopic determineParent(TocObject dropTarget, int dropLocation) {
		//We must determine what object will be the parent of the
		//dropped objects. This is done by looking at the drop location
		//and drop target type

		if(dropTarget == null || dropTarget.getType() == ITocConstants.TYPE_TOC)
		{	//Since the TOC root has no parent, it must be the target parent
			return fModel.getToc();
		}
		else if(!dropTarget.canBeParent())
		{	//If the object is a leaf, it cannot be the parent
			//of the new objects,
			//so the target parent must be its parent
			return (TocTopic)dropTarget.getParent();
		}
		else
		{	//In all other cases, it depends on the location of the drop
			//relative to the drop target
			switch(dropLocation)
			{	case TocDropAdapter.LOCATION_JUST_AFTER:
				{	//if the drop occured after an expanded node
					//and all of its children,
					//make the drop target's parent the target parent object
					if(!fTocTree.getExpandedState(dropTarget))
					{	return (TocTopic)dropTarget.getParent();
					}
					//otherwise, the target parent is the drop target,
					//since the drop occured between it and its first child
				}
				case ViewerDropAdapter.LOCATION_ON:
				{	//the drop location is directly on the drop target
					//set the parent object to be the drop target
					return (TocTopic)dropTarget;
				}
				case ViewerDropAdapter.LOCATION_BEFORE:
				case ViewerDropAdapter.LOCATION_AFTER:
				{	//if the drop is before or after the drop target,
					//make the drop target's parent the target parent object
					return (TocTopic)dropTarget.getParent();
				}
			}
		}
		return null;
	}

	private ArrayList getObjectsToAdd(Object[] droppings, TocTopic targetParent) {
		ArrayList tocObjects = new ArrayList(droppings.length);
		for(int i = 0; i < droppings.length; ++i)
		{	if(droppings[i] instanceof String)
			{	Path path = new Path((String)droppings[i]);
				if(TocExtensionUtil.hasValidTocExtension(path))
				{	tocObjects.add(makeNewTocLink(targetParent, path));
				}
				else if(TocExtensionUtil.hasValidPageExtension(path))
				{	TocTopic topic = makeNewTocTopic(targetParent, path); 
					String title = TOCHTMLTitleUtil.findTitle(path.toFile());	
					if(title == null)
					{	topic.setFieldLabel(title);
						int numChildren = targetParent.getChildren().size();
						TocObject[] children = 
							(TocObject[])targetParent.getChildren().toArray(new TocObject[numChildren]);
						
						String[] tocObjectNames = new String[children.length];
						
						for(int j = 0; j < numChildren; ++j)
						{	tocObjectNames[j] = children[j].getName();
						}
						
						title = PDELabelUtility.generateName(tocObjectNames, PDEUIMessages.TocPage_TocTopic);
					}
					tocObjects.add(topic);
				}
			}
			else if(droppings[i] instanceof TocObject)
			{	ArrayList dragged = fDragAdapter.getDraggedElements();
				if(dragged != null && dragged.size() == droppings.length)
				{	TocObject draggedObj = (TocObject)dragged.get(i);

					if(targetParent.descendsFrom(draggedObj))
					{	//Nesting an object inside itself or its children
						//is so stupid and ridiculous that I get a headache
						//just thinking about it. Thus, this drag is not going to complete.
						return null;
					}
				}
				//Reconnect this TocObject, since it was deserialized
				((TocObject)droppings[i]).reconnect(fModel, targetParent);
				tocObjects.add(droppings[i]);
			}
		}
		
		return tocObjects;
	}

	private TocTopic makeNewTocTopic(TocObject target, Path path) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		return fModel.getFactory().createTocTopic(target, root.getFileForLocation(path));
	}
	
	private TocLink makeNewTocLink(TocObject target, Path path) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		return fModel.getFactory().createTocLink(target, root.getFileForLocation(path));
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
		
		if (tocObject.canBeParent()) {
			action.setParentObject(tocObject);
			action.run();
		}
		else
		{	action.setParentObject(tocObject.getParent());
			action.setTargetObject(tocObject);
			action.run();
		}
	}

	private void handleMultiAddAction(List objectsToAdd, TocObject tocTarget, boolean insertBefore, TocObject targetParent)
	{	TocObject[] tocObjects = (TocObject[])objectsToAdd.toArray(new TocObject[objectsToAdd.size()]);
		if (tocObjects == null) return;

		for(int i = 0; i < tocObjects.length; ++i)
		{	if (tocObjects[i] != null) {
				if (targetParent != null && targetParent.canBeParent()) {
					// Add the TOC object
					if(tocTarget != null && tocTarget != targetParent)
					{	((TocTopic)targetParent).addChild(tocObjects[i], tocTarget, insertBefore);
					}
					else
					{	((TocTopic)targetParent).addChild(tocObjects[i]);
					}
				}
			}
		}
	}

	/**
	 * @param object
	 */
	private void handleDeleteAction()
	{	ArrayList objects = new ArrayList(((IStructuredSelection)fTocTree.getSelection()).toList());
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

	public void handleDrag(List itemsToRemove)
	{	if(!itemsToRemove.isEmpty())
		{	fPreserveSelection = true;
			fRemoveObjectAction.setToRemove((TocObject[])itemsToRemove.toArray(new TocObject[itemsToRemove.size()]));
			fRemoveObjectAction.run();
			fPreserveSelection = false;
		}
	}

	private void handleMoveAction(int positionFlag) {
		ISelection sel = fTocTree.getSelection();
		Object object = ((IStructuredSelection) sel).getFirstElement();
		if (object == null) {
			return;
		} else if (object instanceof TocObject) {
			TocObject tocObject = (TocObject)object;
			TocTopic parent = (TocTopic)tocObject.getParent();
			// Determine the parents type
			if (parent != null)
			{	// Move the object up or down one position
				parent.moveChild(tocObject, positionFlag);
			}
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

		if(!fPreserveSelection)
		{	fTocTree.setSelection(new StructuredSelection(tocObject), true);
		}
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
