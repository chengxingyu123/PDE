/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.toc;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;

/**
 * TocEditor
 *
 */
public class TocEditor extends PDEFormEditor {

	/**
	 * 
	 */
	public TocEditor() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getEditorID()
	 */
	protected String getEditorID() {
		return IPDEUIConstants.TABLE_OF_CONTENTS_EDITOR_ID;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getContextIDForSaveAs()
	 */
	public String getContextIDForSaveAs() {
		return TocInputContext.CONTEXT_ID;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#addEditorPages()
	 */
	protected void addEditorPages() {
		try {
			addPage(new TocPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createContentOutline()
	 */
	protected ISortableContentOutlinePage createContentOutline() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createInputContextManager()
	 */
	protected InputContextManager createInputContextManager() {
		return new TocInputContextManager(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createResourceContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.ui.IFileEditorInput)
	 */
	protected void createResourceContexts(InputContextManager contexts,
			IFileEditorInput input) {
		contexts.putContext(input, new TocInputContext(this, input, true));
		contexts.monitorFile(input.getFile());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createStorageContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.ui.IStorageEditorInput)
	 */
	protected void createStorageContexts(InputContextManager contexts,
			IStorageEditorInput input) {
		contexts.putContext(input, new TocInputContext(this, input, true));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createSystemFileContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.pde.internal.ui.editor.SystemFileEditorInput)
	 */
	protected void createSystemFileContexts(InputContextManager contexts,
			SystemFileEditorInput input) {
		File file = (File)input.getAdapter(File.class);
		if (file != null) {
			IEditorInput in = new SystemFileEditorInput(file);
			contexts.putContext(in, new TocInputContext(this, in, true));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#editorContextAdded(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void editorContextAdded(InputContext context) {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		return fInputContextManager.findContext(TocInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextRemoved(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void contextRemoved(InputContext context) {
		close(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileAdded(org.eclipse.core.resources.IFile)
	 */
	public void monitoredFileAdded(IFile monitoredFile) {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileRemoved(org.eclipse.core.resources.IFile)
	 */
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getSelection()
	 */
	public ISelection getSelection() {
		return super.getSelection();
	}
	
}
