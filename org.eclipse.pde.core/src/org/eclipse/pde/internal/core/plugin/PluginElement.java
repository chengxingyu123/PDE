/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;

public class PluginElement extends PluginParent implements IPluginElement {
	private static final long serialVersionUID = 1L;

	static final String ATTRIBUTE_SHIFT = "      "; //$NON-NLS-1$

	static final String ELEMENT_SHIFT = "   "; //$NON-NLS-1$

	private transient ISchemaElement fElementInfo;

	protected String fText;

	protected Hashtable fAttributes;
	
	public PluginElement() {
	}
	
	PluginElement(PluginElement element) {
		setModel(element.getModel());
		setParent(element.getParent());
		fName = element.getName();
		IPluginAttribute[] atts = element.getAttributes();
		for (int i = 0; i < atts.length; i++) {
			PluginAttribute att = (PluginAttribute) atts[i];
			getAttributeMap().put(att.getName(), att.clone());
		}
		fText = element.getText();
		fElementInfo = (ISchemaElement) element.getElementInfo();
	}
	
	protected void initialize() {
		super.initialize();
		fAttributes = new Hashtable();
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IPluginElement) {
			IPluginElement target = (IPluginElement) obj;
			if (target.getModel().equals(getModel()))
				return false;
			if (target.getAttributeCount() != getAttributeCount())
				return false;
			IPluginAttribute tatts[] = target.getAttributes();
			for (int i = 0; i < tatts.length; i++) {
				IPluginAttribute tatt = tatts[i];
				if (tatt.equals(getAttributeMap().get(tatt.getName())) == false)
					return false;
			}
			return super.equals(obj);
		}
		return false;
	}

	public IPluginElement createCopy() {
		return new PluginElement(this);
	}

	public IPluginAttribute getAttribute(String name) {
		return (IPluginAttribute) getAttributeMap().get(name);
	}

	public IPluginAttribute[] getAttributes() {
		Collection values = getAttributeMap().values();
		IPluginAttribute[] result = new IPluginAttribute[values.size()];
		return (IPluginAttribute[]) values.toArray(result);
	}

	public int getAttributeCount() {
		return getAttributeMap().size();
	}

	public Object getElementInfo() {
		if (fElementInfo != null) {
			ISchema schema = fElementInfo.getSchema();
			if (schema.isDisposed()) {
				fElementInfo = null;
			}
		}
		if (fElementInfo == null) {
			IPluginObject parent = getParent();
			while (parent != null && !(parent instanceof IPluginExtension)) {
				parent = parent.getParent();
			}
			if (parent != null) {
				PluginExtension extension = (PluginExtension) parent;
				ISchema schema = (ISchema) extension.getSchema();
				if (schema != null) {
					fElementInfo = schema.findElement(getName());
				}
			}
		}
		return fElementInfo;
	}

	public String getText() {
		return fText;
	}

	public void removeAttribute(String name) throws CoreException {
		ensureModelEditable();
		PluginAttribute att = (PluginAttribute) getAttributeMap().remove(name);
		String oldValue = att.getValue();
		if (att != null) {
			att.setInTheModel(false);
		}
		firePropertyChanged(P_ATTRIBUTE, oldValue, null);
	}

	public void setAttribute(String name, String value) throws CoreException {
		ensureModelEditable();
		if (value == null) {
			removeAttribute(name);
			return;
		}
		IPluginAttribute attribute = getAttribute(name);
		if (attribute == null) {
			attribute = getModel().getFactory().createAttribute(this);
			attribute.setName(name);
			getAttributeMap().put(name, attribute);
			((PluginAttribute) attribute).setInTheModel(true);
		}
		attribute.setValue(value);
	}

	public void setElementInfo(ISchemaElement newElementInfo) {
		fElementInfo = newElementInfo;
		if (fElementInfo == null) {
			for (Enumeration atts = getAttributeMap().elements(); atts
					.hasMoreElements();) {
				PluginAttribute att = (PluginAttribute) atts.nextElement();
				att.setAttributeInfo(null);
			}
		}
	}

	public void setText(String newText) throws CoreException {
		ensureModelEditable();
		String oldValue = fText;
		fText = newText;
		firePropertyChanged(P_TEXT, oldValue, fText);

	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<" + getName()); //$NON-NLS-1$
		String newIndent = indent + ATTRIBUTE_SHIFT;
		if (getAttributeMap().isEmpty() == false) {
			writer.println();
			for (Iterator iter = getAttributeMap().values().iterator(); iter
					.hasNext();) {
				IPluginAttribute attribute = (IPluginAttribute) iter.next();
				attribute.write(newIndent, writer);
				if (iter.hasNext())
					writer.println();
			}
		}
		writer.println(">"); //$NON-NLS-1$
		newIndent = indent + ELEMENT_SHIFT;
		IPluginObject[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement element = (IPluginElement) children[i];
			element.write(newIndent, writer);
		}
		if (getText() != null) {
			writer.println(newIndent + getWritableString(getText()));
		}
		writer.println(indent + "</" + getName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	protected Hashtable getAttributeMap() {
		return fAttributes;
	}
}
