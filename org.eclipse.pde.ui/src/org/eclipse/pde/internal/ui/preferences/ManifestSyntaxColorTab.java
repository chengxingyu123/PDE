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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.IPDEColorConstants;

public class ManifestSyntaxColorTab extends SyntaxColorTab {

	private static final String[][] COLOR_STRINGS = new String[][] {
			{"Header Name", IPDEColorConstants.P_HEADER_NAME},
			{"Assignment", IPDEColorConstants.P_HEADER_ASSIGNMENT},
			{"Header Value", IPDEColorConstants.P_HEADER_VALUE}};

	public ManifestSyntaxColorTab(IColorManager manager) {
		super(manager);
	}
	
	protected IDocument getDocument() {
		StringBuffer buffer = new StringBuffer();
		String delimiter = System.getProperty("line.separator");
		buffer.append("Manifest-Version: 1.0");
		buffer.append(delimiter);
		buffer.append("Bundle-Name: %name");
		buffer.append(delimiter);
		buffer.append("Bundle-SymbolicName: com.example.xyz");
		buffer.append(delimiter);
		buffer.append("Require-Bundle:");
		buffer.append(delimiter);
		buffer.append(" org.eclipse.core.runtime; bundle-version=\"3.0.0\",");
		buffer.append(delimiter);
		buffer.append(" org.eclipse.ui; resolution:=optional");
		IDocument document = new Document(buffer.toString());
		return document;
	}

	protected ChangeAwareSourceViewerConfiguration getSourceViewerConfiguration() {
		return null;
	}

	protected String[][] getColorStrings() {
		return COLOR_STRINGS;
	}

}
