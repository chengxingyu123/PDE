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
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class ContextHelpAttribute implements IDocumentAttribute, IContextObject {
	
	private static final long serialVersionUID = 1L;

	private IDocumentNode fEnclosingElement;
	private String fValue;
	private String fName;
	private int fNameOffset = -1;
	private int fNameLength = -1;
	private int fValueOffset = -1;
	private int fValueLength = -1;
	
	public void setEnclosingElement(IDocumentNode node) {
		fEnclosingElement = node;
	}
	
	public IDocumentNode getEnclosingElement() {
		return fEnclosingElement;
	}
	
	public void setNameOffset(int offset) {
		fNameOffset = offset;
	}
	
	public int getNameOffset() {
		return fNameOffset;
	}
	
	public void setNameLength(int length) {
		fNameLength = length;
	}
	
	public int getNameLength() {
		return fNameLength;
	}
	
	public void setValueOffset(int offset) {
		fValueOffset = offset;
	}
	
	public int getValueOffset() {
		return fValueOffset;
	}
	
	public void setValueLength(int length) {
		fValueLength = length;
	}
	
	public int getValueLength() {
		return fValueLength;
	}
	
	public String getAttributeName() {
		return fName;
	}
	
	public String getAttributeValue() {
		return fValue;
	}
	
	public String write() {
		return fName + "=\"" + getWritableString(getAttributeValue()) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void setAttributeName(String name) throws CoreException {
		fName = name;
	}
	
	public void setAttributeValue(String value) throws CoreException {
		fValue = value;
	}
	
	private String getWritableString(String source) {
		return CoreUtility.getWritableString(source)
				.replaceAll("\\r", "&#x0D;") //$NON-NLS-1$ //$NON-NLS-2$
				.replaceAll("\\n", "&#x0A;"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public ContextHelpModel getModel() {
		if (getEnclosingElement() instanceof IContextObject)
			return (((IContextObject)getEnclosingElement()).getModel());
		return null;
	}

	public IContextObject getParent() {
		if (getEnclosingElement() instanceof IContextObject)
			return (IContextObject) getEnclosingElement();
		return null;
	}

}
