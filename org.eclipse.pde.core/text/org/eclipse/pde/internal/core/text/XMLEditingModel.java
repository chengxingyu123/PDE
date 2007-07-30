/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.util.SAXParserWrapper;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class XMLEditingModel extends AbstractEditingModel {
	
	public XMLEditingModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync) {
		try {
			fLoaded = true;
			SAXParserWrapper parser = new SAXParserWrapper();
			parser.parse(source, createDocumentHandler(this, true));
		} catch (SAXException e) {
			fLoaded = false;
		} catch (IOException e) {
		} catch (ParserConfigurationException e) {
		} catch (FactoryConfigurationError e) {
		}
	}

	/**
	 * @param document
	 */
	public void reload(IDocument document) {
		// Get the document's text
		String text = document.get();
		InputStream stream = null;

		try {
			// Turn the document's text into a stream
			stream = new ByteArrayInputStream(text.getBytes("UTF8")); //$NON-NLS-1$
			// Reload the model using the stream
			reload(stream, false);
			// Remove the dirty (*) indicator from the editor window
			setDirty(false);
		} catch (UnsupportedEncodingException e) {
			PDECore.logException(e);
		} catch (CoreException e) {
			// Ignore
		}
	}

	protected abstract DefaultHandler createDocumentHandler(IModel model, boolean reconciling);
	
	public void adjustOffsets(IDocument document) {
		try {
			SAXParserWrapper parser = new SAXParserWrapper();
			parser.parse(getInputStream(document), createDocumentHandler(this, false));
		} catch (SAXException e) {
		} catch (IOException e) {
		} catch (ParserConfigurationException e) {
		} catch (FactoryConfigurationError e) {
		}
	}	

	public void save() {
		if (getUnderlyingResource() == null 
				|| !(getUnderlyingResource() instanceof IFile))
			return;
		try {
			IFile file = (IFile)getUnderlyingResource();
			String contents = getContents();
			ByteArrayInputStream stream = new ByteArrayInputStream(contents
					.getBytes("UTF8")); //$NON-NLS-1$
			if (file.exists()) {
				file.setContents(stream, false, false, null);
			} else {
				file.create(stream, false, null);
			}
			stream.close();
		} catch (CoreException e) {
			PDECore.logException(e);
		} catch (IOException e) {
		}
	}

	public String getContents() {
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		save(writer);
		writer.flush();
		try {
			swriter.close();
		} catch (IOException e) {
		}
		return swriter.toString();
	}

	public void save(PrintWriter writer) {
		if (isLoaded()) {
			getRoot().write("", writer); //$NON-NLS-1$
		}
		setDirty(false);
	}

	protected abstract IWritable getRoot();
}