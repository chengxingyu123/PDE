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

import org.eclipse.pde.internal.core.text.DocumentTextNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;


public class ContextDescriptionNode extends AbstractContextNode {

	public static final String F_DESC = "description";
	private static final long serialVersionUID = 1L;

	protected String getName() {
		return F_DESC;
	}
	
	public String getDescription() {
		return getTextNode() != null ? getTextNode().getText() : null;
	}
	
	public void setDescription(String description) {
		if (description == null)
			removeTextNode();
		else {
			IDocumentTextNode node = getTextNode();
			if (node == null) {
				node = new DocumentTextNode();
				node.setEnclosingElement(this);
				addTextNode(node);
			}
			node.setText(description);
		}
	}
}
