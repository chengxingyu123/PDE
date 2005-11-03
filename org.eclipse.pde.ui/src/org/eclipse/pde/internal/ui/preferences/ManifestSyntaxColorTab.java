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
import org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IPDEColorConstants;

public class ManifestSyntaxColorTab extends SyntaxColorTab {

	private ArrayList fMFColorData;

	private String[][] fMFColorStrings = new String[][] {
			{"Header Name", IPDEColorConstants.P_HEADER_NAME},
			{"Assignment", IPDEColorConstants.P_HEADER_ASSIGNMENT},
			{"Header Value", IPDEColorConstants.P_HEADER_VALUE}};

	public ManifestSyntaxColorTab(ColorManager manager) {
		super(manager);
		fMFColorData = loadColorData(null, false, fMFColorStrings);
	}
	
	protected ArrayList getViewerInput() {
		return fMFColorData;
	}
	
	public void dispose() {
		for (int i = 0; i < fMFColorData.size(); i++)
			((StoreLinkedDisplayItem)fMFColorData.get(i)).disposeColor();
	}
	
	protected IDocument getDocument() {
		StringBuffer buffer = new StringBuffer();
		String delimiter = System.getProperty("line.separator");
		buffer.append("Manifest-Version: 1.0");
		buffer.append(delimiter);
		buffer.append("Bundle-ManifestVersion: 2");
		buffer.append(delimiter);
		buffer.append("Bundle-Name: %name");
		IDocument document = new Document(buffer.toString());
		return document;
	}

	protected ChangeAwareSourceViewerConfiguration getSourceViewerConfiguration() {
		return null;
	}

	public void performDefaults() {
	}

}
