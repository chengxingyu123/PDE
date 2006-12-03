/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.help;

import org.eclipse.pde.internal.core.text.XMLDocumentHandler;
import org.xml.sax.SAXException;

public class ContextDocumentHandler extends XMLDocumentHandler {

	public ContextDocumentHandler(ContextHelpModel model, boolean reconciling) {
		super(model, reconciling);
	}

	public void endDocument() throws SAXException {
		super.endDocument();
		((ContextHelpModel)getModel()).updateContextsIds();
	}
}
