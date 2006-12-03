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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.text.DocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;

public abstract class AbstractContextNode extends DocumentNode implements IContextObject {

	private ContextHelpModel fModel;
	
	public AbstractContextNode() {
		setXMLTagName(getName());
	}
	
	protected abstract String getName();
	
	public ContextHelpModel getModel() {
		return fModel;
	}
	
	public void setModel(ContextHelpModel model) {
		fModel = model;
	}
	
	public boolean isRoot() {
		return false;
	}

	public void setXMLAttribute(String name, String value) {
		String oldValue = getXMLAttributeValue(name);
		if (oldValue != null && oldValue.equals(value))
			return;
		IDocumentAttribute attr = (IDocumentAttribute) fAttributes.get(name);
		try {
			if (value == null)
				value = ""; //$NON-NLS-1$
				if (attr == null) {
					attr = new ContextHelpAttribute();
					attr.setAttributeName(name);
					attr.setEnclosingElement(this);
					fAttributes.put(name, attr);
				}
				attr.setAttributeValue(value == null ? "" : value); //$NON-NLS-1$
		} catch (CoreException e) {
			PDECore.log(e);
		}
	}

	private String getLineDelimiter() {
		return TextUtilities.getDefaultLineDelimiter(getModel().getDocument());
	}
	
	// TODO: this needs to be fixed in the general DocumentNode sense.. indents
	// should be stored as strings not numbers
//	protected String getIndent() {
//		StringBuffer buffer = new StringBuffer();
//		for (int i = 0; i < getLineIndent(); i++) {
//			buffer.append("\t"); //$NON-NLS-1$
//		}
//		return buffer.toString();
//	}
	
	public String write(boolean indent) {
		String sep = getLineDelimiter();
		StringBuffer buffer = new StringBuffer();
		if (indent)
			buffer.append(getIndent());
		
		IDocumentNode[] children = getChildNodes();
		IDocumentTextNode node = getTextNode();
		String text = node == null ? new String() : node.getText();
		buffer.append(writeShallow(false));
		if (fAttributes.size() > 0 || children.length > 0 || text.length() > 0)
			buffer.append(sep);
		if (children.length > 0 || text.length() > 0) {
			if (text.length() > 0) {
				buffer.append(getIndent());
				buffer.append("   "); //$NON-NLS-1$
				buffer.append(text);
				buffer.append(sep);
			}
			for (int i = 0; i < children.length; i++) {
				children[i].setLineIndent(getLineIndent() + 3);
				buffer.append(children[i].write(true));
				buffer.append(sep);
			}
		}
		if (fAttributes.size() > 0 || children.length > 0 || text.length() > 0)
			buffer.append(getIndent());

		buffer.append("</" + getXMLTagName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$	
		return buffer.toString();
	}

	public String writeShallow(boolean terminate) {
		StringBuffer buffer = new StringBuffer("<" + getXMLTagName()); //$NON-NLS-1$

		IDocumentAttribute[] attrs = getNodeAttributes();
		for (int i = 0; i < attrs.length; i++)
			if (attrs[i].getAttributeValue().length() > 0)
				buffer.append(" " + attrs[i].write()); //$NON-NLS-1$
		if (terminate)
			buffer.append("/"); //$NON-NLS-1$
		buffer.append(">"); //$NON-NLS-1$
		return buffer.toString();
	}

	public IContextObject getParent() {
		return (IContextObject) getParentNode();
	}
}
