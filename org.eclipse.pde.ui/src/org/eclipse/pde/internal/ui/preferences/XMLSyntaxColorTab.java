/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.util.ArrayList;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.IPDEColorConstants;
import org.eclipse.pde.internal.ui.editor.text.XMLSourceViewerConfiguration;

public class XMLSyntaxColorTab extends SyntaxColorTab {

	private ArrayList fXMLColorData;

	private String[][] fXMLColorStrings = new String[][] {
			/*		{Display name, IPreferenceStore key}		*/
					{PDEUIMessages.EditorPreferencePage_text, IPDEColorConstants.P_DEFAULT},
					{PDEUIMessages.EditorPreferencePage_proc, IPDEColorConstants.P_PROC_INSTR},
					{PDEUIMessages.EditorPreferencePage_tag, IPDEColorConstants.P_TAG},
					{PDEUIMessages.EditorPreferencePage_string, IPDEColorConstants.P_STRING},
					{PDEUIMessages.EditorPreferencePage_comment, IPDEColorConstants.P_XML_COMMENT}};

	public XMLSyntaxColorTab(IColorManager manager) {
		super(manager);
		fXMLColorData = loadColorData(fXMLColorStrings);
	}
	
	protected ArrayList getViewerInput() {
		return fXMLColorData;
	}
	
	protected IDocument getDocument() {
		StringBuffer buffer = new StringBuffer();
		String delimiter = System.getProperty("line.separator");
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		buffer.append(delimiter);
		buffer.append("<plugin>");
		buffer.append(delimiter);
		buffer.append("<!-- Comment -->");
		buffer.append(delimiter);
		buffer.append("   <extension point=\"some.id\">");
		buffer.append(delimiter);
		buffer.append("      <tag> body text </tag>");
		buffer.append(delimiter);
		buffer.append("   </extension>");
		buffer.append(delimiter);
		buffer.append("</plugin>");
		
		IDocument document = new Document(buffer.toString());
		new XMLDocumentSetupParticpant().setup(document);
		return document;
	}

	protected ChangeAwareSourceViewerConfiguration getSourceViewerConfiguration() {
		return new XMLSourceViewerConfiguration(null, fColorManager);
	}

}
