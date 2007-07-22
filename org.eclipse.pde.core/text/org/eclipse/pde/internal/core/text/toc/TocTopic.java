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

import java.io.PrintWriter;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;

/**
 * The TocTopic class represents a topic element in a TOC.
 * A topic can link to a specific Help page. It can also have
 * children, which can be more topics.
 */
public class TocTopic extends TocObject
{	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a topic with the given model and parent.
	 * 
	 * @param model The model associated with the new topic.
	 * @param parent The parent TocObject of the new topic.
	 */
	public TocTopic(TocModel model, TocObject parent) {
		super(model, parent);
	}
	
	/**
	 * Constructs a topic with the given model, parent and file.
	 * 
	 * @param model The model associated with the new link.
	 * @param parent The parent TocObject of the new link.
	 * @param file The page to link to.
	 */
	public TocTopic(TocModel model, TocObject parent, IFile file) {
		super(model, parent);

		IPath path = file.getFullPath();
		if(file.getProject().equals(getModel().getUnderlyingResource().getProject()))
		{	//If the file is from the same project,
			//remove the project name segment
			setFieldRef(path.removeFirstSegments(1).toString()); //$NON-NLS-1$
		}
		else
		{	//If the file is from another project, add ".."
			//to traverse outside this model's project
			setFieldRef(".." + path.toString()); //$NON-NLS-1$
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#canBeParent()
	 */
	public boolean canBeParent() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getElement()
	 */
	public String getElement() {
		return ELEMENT_TOPIC;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getName()
	 */
	public String getName() {
		return getFieldLabel();
	}

	public String getPath() {
		return getFieldRef();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_TOPIC;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#isFirstChildObject(org.eclipse.pde.internal.core.toc.TocObject)
	 */
	public boolean isFirstChildObject(TocObject tocObject) {
		return getChildNodes().length > 0 
			&& getChildNodes()[0] == tocObject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#isLastChildObject(org.eclipse.pde.internal.core.toc.TocObject)
	 */
	public boolean isLastChildObject(TocObject tocObject) {
		return getChildNodes().length > 0 
			&& getChildNodes()[getChildNodes().length - 1] == tocObject;
	}

	/**
	 * Add a TocObject child to this topic
	 * and signal the model if necessary.
	 * 
	 * @param child The child to add to the TocObject
	 */
	public void addChild(TocObject child) {
		addChildNode(child);
		child.setParent(this);
		if (isEditable()) {
			fireStructureChanged(child, IModelChangedEvent.INSERT);
		}
	}

	/**
	 * Add a TocObject child to this topic
	 * beside a specified sibling
	 * and signal the model if necessary.
	 * 
	 * @param child The child to add to the TocObject
	 * @param sibling The object that will become the child's direct sibling
	 * @param insertBefore If the object should be inserted before the sibling
	 */
	public void addChild(TocObject child, TocObject sibling, boolean insertBefore) {
		int currentIndex = indexOf(sibling);
		if(!insertBefore)
		{	currentIndex++;
		}

		addChildNode(child, currentIndex);
		child.setParent(this);
		if (isEditable()) {
			fireStructureChanged(child, IModelChangedEvent.INSERT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#moveChild(org.eclipse.pde.internal.core.toc.TocObject, int)
	 */
	public void moveChild(TocObject tocObject, int newRelativeIndex) {
		// Get the current index of the child
		int currentIndex = indexOf(tocObject);
		// Ensure the object is found
		if (currentIndex == -1) {
			return;
		}
		// Calculate the new location of the child
		int newIndex = newRelativeIndex + currentIndex;
		// Validate the new location
		if ((newIndex < 0) ||
				(newIndex >= getChildNodesList().size())) {
			return;
		}
		// Remove the child and add it back at the new location
		removeChildNode(tocObject);
		addChildNode(tocObject, newIndex);
		// Send an insert event
		if (isEditable()) {
			fireStructureChanged(tocObject, IModelChangedEvent.INSERT);
		}	
	}
	
	/**
	 * Remove a TocObject child from this topic
	 * and signal the model if necessary.
	 * 
	 * @param child The child to add to the TocObject
	 */
	public void removeChild(TocObject tocObject) {
		removeChildNode(tocObject);
		if (isEditable()) {
			fireStructureChanged(tocObject, IModelChangedEvent.REMOVE);
		}	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#writeAttributes(java.lang.StringBuffer)
	 */
	protected void writeAttributes(StringBuffer buffer) {
		printLabelAttribute(buffer);		
		printLinkAttribute(buffer);
	}

	/**
	 * Print out the label attribute of the topic.
	 *  
	 * @param buffer the buffer to which the attribute is written.
	 */
	private void printLabelAttribute(StringBuffer buffer) {
		// Print label attribute
		if ((getFieldLabel() != null) && 
				(getFieldLabel().length() > 0)) {
			// No trim required
			// No encode required
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_LABEL, getFieldLabel().trim()));
		}
	}

	/**
	 * Print out the link attribute of the topic, if it exists.
	 *  
	 * @param buffer the buffer to which the attribute is written.
	 */
	protected void printLinkAttribute(StringBuffer buffer) {
		// Print link attribute
		if ((getFieldRef() != null) && 
				(getFieldRef().length() > 0)) {
			// Trim leading and trailing whitespace
			// Encode characters
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_HREF, getFieldRef().trim()));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#writeElements(java.lang.String, java.io.PrintWriter)
	 */
	protected void writeElements(String indent, PrintWriter writer) {
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		
		// Print elements
		Iterator iterator = getChildNodesList().iterator();
		while (iterator.hasNext()) {
			TocObject object = (TocObject)iterator.next();
			object.write(newIndent, writer);
		}
	}

	/**
	 * @return the label associated with this topic.
	 */
	public String getFieldLabel() {
		return getXMLAttributeValue(ATTRIBUTE_LABEL);
	}

	/**
	 * Change the value of the label field and 
	 * signal a model change if needed.
	 * 
	 * @param name The new label for the topic
	 */
	public void setFieldLabel(String name)
	{	setXMLAttribute(ATTRIBUTE_LABEL, name);
	}

	/**
	 * @return the link associated with this topic, <br />
	 * or <code>null</code> if none exists.
	 */
	public String getFieldRef()
	{	return getXMLAttributeValue(ATTRIBUTE_HREF);
	}

	/**
	 * Change the value of the link field and 
	 * signal a model change if needed.
	 * 
	 * @param value The new page location to be linked by this topic
	 */
	public void setFieldRef(String value)
	{	setXMLAttribute(ATTRIBUTE_HREF, value);
	}
}
