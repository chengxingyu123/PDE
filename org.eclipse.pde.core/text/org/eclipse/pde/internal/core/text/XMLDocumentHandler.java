/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;

public class XMLDocumentHandler extends DocumentHandler {
	
	protected XMLEditingModel fModel;
	private IXMLNodeFactory fFactory;

	/**
	 * @param model
	 */
	public XMLDocumentHandler(XMLEditingModel model, boolean reconciling) {
		super(reconciling);
		fModel = model;
		fFactory = getModel().getNodeFactory();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocument()
	 */
	protected IDocument getDocument() {
		return fModel.getDocument();
	}
	
	protected XMLEditingModel getModel() {
		return fModel;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocumentNode(java.lang.String, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	protected IDocumentNode getDocumentNode(String name, IDocumentNode parent) {
		IDocumentNode node = null;
		if (parent == null) {
			node = (IDocumentNode)getModel().getRootNode();
			if (node != null) {
				node.setOffset(-1);
				node.setLength(-1);
			}
		} else {
			IDocumentNode[] children = parent.getChildNodes();
			for (int i = 0; i < children.length; i++) {
				if (children[i].getOffset() < 0) {
					if (name.equals(children[i].getXMLTagName())) {
						node = children[i];
					}
					break;
				}
			}
		}
		
		if (node == null)
			return fFactory.createDocumentNode(name, parent);
		
		IDocumentAttribute[] attrs = node.getNodeAttributes();
		for (int i = 0; i < attrs.length; i++) {
			attrs[i].setNameOffset(-1);
			attrs[i].setNameLength(-1);
			attrs[i].setValueOffset(-1);
			attrs[i].setValueLength(-1);
		}
		
		for (int i = 0; i < node.getChildNodes().length; i++) {
			IDocumentNode child = node.getChildAt(i);
			child.setOffset(-1);
			child.setLength(-1);
		}
		
		// clear text nodes if the user is typing on the source page
		// they will be recreated in the characters() method
		if (isReconciling()) {
			node.removeTextNode();
			node.setIsErrorNode(false);
		}
		
		return node;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocumentAttribute(java.lang.String, java.lang.String, org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	protected IDocumentAttribute getDocumentAttribute(String name, String value, IDocumentNode parent) {
		IDocumentAttribute attr = parent.getDocumentAttribute(name);
		try {
			if (attr == null) {
				attr = fFactory.createAttribute(name, value, parent);				
			} else {
				if (!name.equals(attr.getAttributeName()))
					attr.setAttributeName(name);
				if (!value.equals(attr.getAttributeValue()))
					attr.setAttributeValue(value);
			}
		} catch (CoreException e) {
		}
		return attr;
	}
	
}
