/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.help;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.help.ContextHelpModel;
import org.eclipse.pde.internal.core.text.plugin.XMLTextChangeListener;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

public class ContextHelpWizard extends Wizard {

	private IFile[] fContexts;
	private ContextHelpModel[] fModels;
	private IDocument[] fDocuments;
	private ITextFileBuffer[] fBuffers;
	private int fSC; // successfull buffer connections

	public ContextHelpWizard(IFile[] contexts) {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_XHTML_CONVERT_WIZ);
		setWindowTitle(PDEUIMessages.XHTMLConversionWizard_title);
		setNeedsProgressMonitor(true);
		fContexts = contexts;
	}
	
	private void loadContexts() {
		if (fContexts == null || fContexts.length == 0)
			return;
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		IProgressMonitor monitor = new NullProgressMonitor();
		fSC = 0;
		try {
			fBuffers = new ITextFileBuffer[fContexts.length];
			fDocuments = new IDocument[fContexts.length];
			fModels = new ContextHelpModel[fContexts.length];
			for (int i = 0; i < fContexts.length; i++) {
				if (fContexts[i] == null || !fContexts[i].exists())
					continue;
				manager.connect(fContexts[i].getFullPath(), monitor);
				fSC++;
				fBuffers[i] = manager.getTextFileBuffer(fContexts[i].getFullPath());
				if (fBuffers[i].isDirty())
					fBuffers[i].commit(monitor, true);
				fDocuments[i] = fBuffers[i].getDocument();
			}
			
			for (int i = 0; i < fContexts.length; i++) {
				fModels[i] = new ContextHelpModel(fDocuments[i], true);
				fModels[i].setUnderlyingResource(fContexts[i]);
				fModels[i].addModelChangedListener(new XMLTextChangeListener(fDocuments[i]));
				try {
					fModels[i].load();
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}
	
	private void cleanupBuffers() {
		if (fContexts == null || fContexts.length == 0)
			return;
		int dc = 0;
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		IProgressMonitor monitor = new NullProgressMonitor();
		for (int i = 0; i < fContexts.length && dc <= fSC; i++) {
			if (fContexts[i] == null || !fContexts[i].exists())
				continue;
			try {
				manager.disconnect(fContexts[i].getFullPath(), monitor);
				dc++;
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
	}
	
	public boolean performFinish() {
		Runnable run = new Runnable() {
			public void run() {
				for (int i = 0; i < fModels.length; i++) {
					IModelTextChangeListener listener = fModels[i].getLastTextChangeListener();
					TextEdit[] edits = listener.getTextOperations();
					NullProgressMonitor monitor = new NullProgressMonitor();
					if (edits.length > 0) {
						MultiTextEdit multi = new MultiTextEdit();
						multi.addChildren(edits);
						try {
							multi.apply(fDocuments[i]);
							fBuffers[i].commit(monitor, true);
						} catch (MalformedTreeException e) {
							PDEPlugin.logException(e);
						} catch (BadLocationException e) {
							PDEPlugin.logException(e);
						} catch (CoreException e) {
							PDEPlugin.logException(e);
						}
					}
				}
				cleanupBuffers();
			}
		};
		getShell().getDisplay().asyncExec(run);
		return true;
	}
	
	public boolean performCancel() {
		cleanupBuffers();
		return super.performCancel();
	}
	
	public void addPages() {
		loadContexts();
		addPage(new ContextHelpWizardPage(fModels));
	}
}
