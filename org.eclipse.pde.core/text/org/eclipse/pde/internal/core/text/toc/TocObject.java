/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text.toc;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.itoc.ITocConstants;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode;

/**
 * TocObject - All objects modeled in a Table of Contents subclass TocObject
 * This class contains functionality common to all TOC elements.
 */
public abstract class TocObject extends PluginDocumentNode implements ITocConstants, Serializable {

	//The model associated with this TocObject
	private transient TocModel fModel;
	private transient boolean fInTheModel;

	//The TocObject's parent TocObject (can be null for root objects)
	private transient TocObject fParent;	

	/**
	 * Constructs the TocObject and initializes its attributes.
	 * 
	 * @param model The model associated with this TocObject.
	 * @param parent The parent of this TocObject.
	 */
	public TocObject(TocModel model, TocObject parent) {
		fModel = model;
		fParent = parent;
	}

	public void reconnect(TocModel model, TocObject parent) {
		fModel = model;
		fParent = parent;
		List children = getChildren();
		
		for(Iterator iter = children.iterator(); iter.hasNext();)
		{	TocObject child = (TocObject)iter.next();
			child.reconnect(model, this);
		}
	}

	/**
	 * @return the children of the object or an empty List if none exist.
	 */
	public List getChildren()
	{	//Create a copy of the child list instead of 
		//returning the list itself. That way, our list
		//of children cannot be altered from outside
		ArrayList list = new ArrayList();

		// Add children of this topic
		if (getChildNodesList().size() > 0) {
			list.addAll(getChildNodesList());
		}

		return list;
	}

	/**
	 * @return true iff this TOC object is capable of containing children.
	 */
	public abstract boolean canBeParent();

	/**
	 * @return the root TOC element that is an ancestor to this TocObject.
	 */
	public Toc getToc() {
		return fModel.getToc();
	}

	/**
	 * @return the model associated with this TocObject.
	 */
	public TocModel getModel() {
		return fModel;
	}

	/**
	 * @param model the model to associate with this TocObject
	 */
	public void setModel(TocModel model) {
		fModel = model;
	}

	/**
	 * @return the identifier for this TocObject.
	 */
	public abstract String getName();

	/**
	 * @return the path to the resource associated with this TOC object
	 * or <code>null</code> if one does not exist.
	 */
	public abstract String getPath();

	/**
	 * @return the parent of this TocObject, or <br />
	 * <code>null</code> if the TocObject has no parent.
	 */
	public TocObject getParent() {
		return fParent;
	}

	/**
	 * Change the parent of this TocObject. Usually
	 * used when the object is being moved from one
	 * part of the TOC to another
	 * 
	 * @param newParent the new parent of this TocObject
	 */
	void setParent(TocObject newParent) {
		fParent = newParent;
	}

	/**
	 * Check if the object is a direct or indirect descendant
	 * of the object parameter.
	 * 
	 * @param obj The TOC object to find in this object's ancestry
	 * @return true iff obj is an ancestor of this TOC object
	 */
	public boolean descendsFrom(TocObject obj)
	{	if(this.equals(obj))
		{	return true;
		}

		if(fParent != null && obj.canBeParent())
		{	return fParent.descendsFrom(obj);	
		}

		return false;
	}

	/**
	 * Get the concrete type of this TocObject.
	 */
	public abstract int getType();

	/**
	 * @return <code>true</code> iff the child parameter is the first
	 * of the TocObject's children.
	 */
	public boolean isFirstChildObject(TocObject tocObject) {
		//Returns false by default; subclasses that can have children
		//are expected to override this function
		return false;
	}

	/**
	 * @return <code>true</code> iff the child parameter is the last
	 * of the TocObject's children.
	 */
	public boolean isLastChildObject(TocObject tocObject) {
		//Returns false by default; subclasses that can have children
		//are expected to override this function
		return false;
	}

	/**
	 * @param tocObject the child used to locate a sibling
	 * @return the TocObject preceding the specified one
	 * in the list of children
	 */
	public TocObject getPreviousSibling(TocObject tocObject) {
		if(isFirstChildObject(tocObject))
		{	return null;
		}
		
		List children = getChildren();
		int position = children.indexOf(tocObject);
		if ((position == -1) ||
				(position == 0)) {
			// Either the item was not found or the item was found but it is 
			// at the first index
			return null;
		}
		
		return (TocObject)children.get(position - 1);
	}

	/**
	 * @param tocObject the child used to locate a sibling
	 * @return the TocObject proceeding the specified one
	 * in the list of children
	 */
	public TocObject getNextSibling(TocObject tocObject) {
		if(isLastChildObject(tocObject))
		{	return null;
		}

		List children = getChildren();
		int position = children.indexOf(tocObject);
		int lastIndex = children.size() - 1;
		if ((position == -1) ||
				(position == lastIndex)) {
			// Either the item was not found or the item was found but it is 
			// at the last index
			return null;
		}

		return (TocObject)children.get(position + 1);
	}

	/**
	 * @return true iff a child object can be removed
	 */
	public boolean canBeRemoved()
	{	if(getType() == TYPE_TOC)
		{	//Semantic Rule: The TOC root element can never be removed
			return false;
		}
		
		if(fParent != null)
		{	if (fParent.getType() == TYPE_TOC)
			{	//Semantic Rule: The TOC root element must always
				//have at least one child
				return fParent.getChildren().size() > 1;
			}
			
			return true;
		}
	
		return false;
	}

	/**
	 * Writes out the XML representation of this TocObject, and proceeds
	 * to write the elements of its children.
	 * 
	 * @param indent The indentation that will precede this element's data.
	 * @param writer The output stream to write the XML to.
	 */
	public void write(String indent, PrintWriter writer)
	{	StringBuffer buffer = new StringBuffer();
		try {
			// Assemble start element
			buffer.append(getElement());
			// Assemble attributes
			writeAttributes(buffer);
			// Print start element and attributes
			XMLPrintHandler.printBeginElement(writer, buffer.toString(),
					indent, false);
			// Print elements
			writeElements(indent, writer);
			// Print end element
			XMLPrintHandler.printEndElement(writer, getElement(), indent);
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 			
	}

	/**
	 * Signal that one of the TocObject's properties (attributes) has changed.
	 * 
	 * @param property The property that has changed.
	 * @param oldValue The old value of the property.
	 * @param newValue The current value of the property.
	 */
	protected void firePropertyChanged(String property, Object oldValue,
			Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}

	/**
	 * Signal to the model that the object has changed.
	 * 
	 * @param object The object with a changed property.
	 * @param property The property that has changed.
	 * @param oldValue The old value of the property.
	 * @param newValue The current value of the property.
	 */
	private void firePropertyChanged(IDocumentNode object, String property,
		Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}

	/**
	 * Signals a change in the structure of the element, such as
	 * child addition or removal.
	 * 
	 * @param child The child changed in the TocObject.
	 * @param changeType The kind of change the child underwent.
	 */
	protected void fireStructureChanged(TocObject child, int changeType) {
		fireStructureChanged(new TocObject[] { child }, changeType);
	}

	/**
	 * Signal to the model that the TOC structure has changed.
	 * 
	 * @param children The children changed in the TocObject.
	 * @param changeType The kind of change the children underwent.
	 */
	private void fireStructureChanged(TocObject[] children,
			int changeType) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelChanged(new ModelChangedEvent(provider,
					changeType, children, null));
		}
	}

	/**
	 * @return true iff the model is not read-only.
	 */
	protected boolean isEditable() {
		return fModel.isEditable();
	}	

	/**
	 * Write out the XML representations of the attributes
	 * associated with this TocObject.
	 * 
	 * @param buffer the buffer to which the attributes are written.
	 */
	protected abstract void writeAttributes(StringBuffer buffer);

	/**
	 * Writes child elements or child content.
	 * 
	 * @param indent The indentation that will precede the child data.
	 * @param writer The output stream to write the XML to.
	 */
	protected abstract void writeElements(String indent, PrintWriter writer);

	/**
	 * @return the name of the XML element associated with this TocObject
	 */
	public abstract String getElement();

	/**
	 * @return the inTheModel
	 */
	public boolean isInTheModel() {
		return fInTheModel;
	}

	/**
	 * @param inTheModel the inTheModel to set
	 */
	public void setInTheModel(boolean inTheModel) {
		this.fInTheModel = inTheModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setXMLAttribute(java.lang.String,
	 *      java.lang.String)
	 */
	public void setXMLAttribute(String name, String value) {
		String oldValue = getXMLAttributeValue(name);
		if (oldValue != null && oldValue.equals(value))
			return;
		PluginAttribute attr = (PluginAttribute) getDocumentAttribute(name);
		try {
			if (value == null)
				value = ""; //$NON-NLS-1$
				if (attr == null) {
					attr = new PluginAttribute();
					attr.setName(name);
					attr.setEnclosingElement(this);
					setXMLAttribute(attr);
				}
				attr.setValue(value == null ? "" : value); //$NON-NLS-1$
		} catch (CoreException e) {
		}
		if (fInTheModel)
			firePropertyChanged(attr.getEnclosingElement(), attr
					.getAttributeName(), oldValue, value);
	}
}
