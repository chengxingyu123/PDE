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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.toc.TocModel;
import org.eclipse.pde.internal.core.toc.TocWorkspaceModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.XMLInputContext;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * CompCSInputContext
 *
 */
public class TocInputContext extends XMLInputContext {

	public static final String CONTEXT_ID = "toc-context"; //$NON-NLS-1$	
	
	/**
	 * @param editor
	 * @param input
	 * @param primary
	 */
	public TocInputContext(PDEFormEditor editor, IEditorInput input,
			boolean primary) {
		super(editor, input, primary);
		create();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#addTextEditOperation(java.util.ArrayList, org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#createModel(org.eclipse.ui.IEditorInput)
	 */
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		/*if (input instanceof IStorageEditorInput)
		{	boolean isReconciling = input instanceof IFileEditorInput;
			IDocument document = getDocumentProvider().getDocument(input);
			
			TocModel model = new TocModel(document, isReconciling);
			
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput)input).getFile();
				model.setUnderlyingResource(file);
				model.setCharset(file.getCharset());
			} else if (input instanceof SystemFileEditorInput){
				File file = (File)((SystemFileEditorInput)input).getAdapter(File.class);
				model.setInstallLocation(file.getParent());
				model.setCharset(getDefaultCharset());
			} else if (input instanceof JarEntryEditorInput){
				File file = (File)((JarEntryEditorInput)input).getAdapter(File.class);
				model.setInstallLocation(file.toString());
				model.setCharset(getDefaultCharset());
			} else {
				model.setCharset(getDefaultCharset());				
			}
			model.load();
			
			return model;
		}

		return null;*/
		TocModel model = null;
		if (input instanceof IStorageEditorInput) {
			try {
				if (input instanceof IFileEditorInput) {
					IFile file = ((IFileEditorInput) input).getFile();
					model = new TocWorkspaceModel(file, true);
					model.load();
				} else if (input instanceof IStorageEditorInput) {
					InputStream is = new BufferedInputStream(
							((IStorageEditorInput) input).getStorage()
									.getContents());
					model = new TocModel();
					model.load(is, false);
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
				return null;
			}
		}
		return model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#getPartitionName()
	 */
	protected String getPartitionName() {
		return "___toc_partition"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#flushModel(org.eclipse.jface.text.IDocument)
	 */
	protected void flushModel(IDocument doc) {
		if (!(getModel() instanceof IEditable)) {
			return;
		}
		IEditable editableModel = (IEditable)getModel();
		// No need to flush the model if it is not dirty
		if (editableModel.isDirty() == false) {
			return;
		}
		try {
			StringWriter swriter = new StringWriter();
			PrintWriter writer = new PrintWriter(swriter);
			editableModel.save(writer);
			writer.flush();
			swriter.close();
			doc.set(swriter.toString());
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.XMLInputContext#reorderInsertEdits(java.util.ArrayList)
	 */
	protected void reorderInsertEdits(ArrayList ops) {
		// NO-OP
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#flushEditorInput()
	 */
	public void flushEditorInput() {
		// Override parent, since this editor does not utilize edit operations
		// Relevant during revert operations
		IDocumentProvider provider = getDocumentProvider();
		IEditorInput input = getInput();
		IDocument doc = provider.getDocument(input);
		provider.aboutToChange(input);
		flushModel(doc);
		provider.changed(input);
		setValidated(false);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#synchronizeModel(org.eclipse.jface.text.IDocument)
	 */
	protected boolean synchronizeModel(IDocument document) {
		// Method used to synchronize the source page changes with the form
		// page
		// Not needed if using text edit operations
		// Get the model
		IBaseModel baseModel = getModel();
		// Ensure the model is a workspace model
		if (baseModel  == null) {
			return false;
		} else if (!(baseModel instanceof TocWorkspaceModel)) {
			return false;
		}
		TocWorkspaceModel model = (TocWorkspaceModel)baseModel;
		// Reload the model using the unsaved contents of the source page
		model.reload(document);
		
		return true;
	}	
}
