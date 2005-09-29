package org.eclipse.pde.internal.ui.nls;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class ExternalizeStringsOperation extends WorkspaceModifyOperation {

	private Object[] fChangeFiles;
	
	public ExternalizeStringsOperation(Object[] changeFiles) {
		fChangeFiles = changeFiles;
	}
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		// TODO Auto-generated method stub
		for (int i = 0; i < fChangeFiles.length; i++) {
			if (fChangeFiles[i] instanceof ModelChangeFile) {
				ModelChangeFile changeFile = (ModelChangeFile)fChangeFiles[i];
				IFile pFile = changeFile.getModel().getPropertiesFile();
				if (!pFile.exists()) {
					IPluginModelBase model = changeFile.getModel().getParentModel();
					String propertiesFileComment = "# properties file for " 
						+ model.getUnderlyingResource().getProject().getName();
					ByteArrayInputStream pStream = new ByteArrayInputStream(propertiesFileComment.getBytes());
					pFile.create(pStream, true, monitor);
				}
				
				ITextFileBufferManager pManager = FileBuffers.getTextFileBufferManager();
				try {
					pManager.connect(pFile.getFullPath(), monitor);
					ITextFileBuffer pBuffer = pManager.getTextFileBuffer(pFile.getFullPath());
					IDocument pDoc = pBuffer.getDocument();
					MultiTextEdit pEdit = new MultiTextEdit();
					
					doReplace(changeFile, pDoc, pEdit, monitor);
					
					pEdit.apply(pDoc);
					pBuffer.commit(monitor, true);
					
				} catch (MalformedTreeException e) {
				} catch (BadLocationException e) {
				} finally {
					pManager.disconnect(pFile.getFullPath(), monitor);
				}
			}
		}
	}
	private void doReplace(ModelChangeFile changeFile, IDocument pDoc, MultiTextEdit pEdit, IProgressMonitor monitor) throws CoreException {
		IFile uFile = changeFile.getFile();
		ITextFileBufferManager uManager = FileBuffers.getTextFileBufferManager();
		try {
			uManager.connect(uFile.getFullPath(), monitor);
			ITextFileBuffer uBuffer = uManager.getTextFileBuffer(uFile.getFullPath());
			IDocument uDoc = uBuffer.getDocument();
			MultiTextEdit uEdit = new MultiTextEdit();
			
			String nl = TextUtilities.getDefaultLineDelimiter(pDoc);
			Iterator iter = changeFile.getChanges().iterator();
			
			while (iter.hasNext()) {
				ModelChangeElement changeElement = (ModelChangeElement)iter.next();
				if (changeElement.isExternalized()) {
					uEdit.addChild(new ReplaceEdit(changeElement.getOffset(),
							changeElement.getLength(), 
							changeElement.getExternKey()));
					pEdit.addChild(new InsertEdit(pDoc.getLength(), 
							nl + changeElement.getKey() + " = " + 
							StringWinder.preparePropertiesString(changeElement.getValue(), nl.toCharArray())));
				}
			}		
			uEdit.apply(uDoc);
			uBuffer.commit(monitor, true);
			
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			uManager.disconnect(uFile.getFullPath(), monitor);
		}
 	}
 }
