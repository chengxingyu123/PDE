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

import org.eclipse.pde.internal.core.XMLPrintHandler;

/**
 * Toc - represents the root element of a Table of Contents
 * The TOC root element, like TOC topics, can hold many child topics,
 * links and anchors. Aside from being the root element of the TOC,
 * the element differs from regular topics by having an optional
 * anchor attribute that determines which anchors this TOC will plug
 * into.
 */
public class Toc extends TocTopic {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new Toc. Only takes a model,
	 * since the root element cannot have a parent.
	 * 
	 * @param model The model associated with this TOC.
	 */
	public Toc(TocModel model)
	{	super(model, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getElement()
	 */
	public String getElement()
	{	return ELEMENT_TOC;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getType()
	 */
	public int getType()
	{	return TYPE_TOC;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#write()
	 */
	public void write(String indent, PrintWriter writer)
	{	//This is the first element that is written to file, since
		//this element is the root
		try {
			// Print XML declaration
			XMLPrintHandler.printHead(writer, ATTRIBUTE_VALUE_ENCODING);
			super.write(indent, writer);
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 			
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#writeAttributes(java.lang.StringBuffer)
	 */
	protected void writeAttributes(StringBuffer buffer)
	{	super.writeAttributes(buffer); 
		
		printAnchorAttribute(buffer);
	}

	/**
	 * Override the topic behaviour for writing out the page link field,
	 * because the TOC root element uses a different attribute name
	 * ('topic' instead of 'href')
	 * 
	 * @param buffer The buffer to write the attribute to
	 * @see org.eclipse.pde.internal.core.toc.TocTopic#printLinkAttribute(org.w3c.dom.Element)
	 */
	protected void printLinkAttribute(StringBuffer buffer)
	{	if ((getFieldRef() != null) && 
				(getFieldRef().length() > 0)) {
			// Trim leading and trailing whitespace
			// Encode characters
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_TOPIC, getFieldRef().trim()));
		}
	}
	
	/**
	 * Print the anchor attribute out to the buffer.
	 * 
	 * @param buffer
	 */
	private void printAnchorAttribute(StringBuffer buffer)
	{	if ((getFieldAnchorTo() != null) && 
				(getFieldAnchorTo().length() > 0))
		{	// No trim required
			// No encode required
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_LABEL, getFieldAnchorTo().trim()));
		}
	}

	/**
	 * @return the link associated with this topic, <br />
	 * or <code>null</code> if none exists.
	 */
	public String getFieldRef() {
		return getXMLAttributeValue(ATTRIBUTE_TOPIC);
	}

	/**
	 * Change the value of the link field and 
	 * signal a model change if needed.
	 * 
	 * @param value The new page location to be linked by this topic
	 */
	public void setFieldRef(String value) {
		String old = getXMLAttributeValue(ATTRIBUTE_TOPIC);
		setXMLAttribute(ATTRIBUTE_TOPIC, value);
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_TOPIC, old, value);
		}
	}

	/**
	 * @return the anchor path associated with this TOC
	 */
	public String getFieldAnchorTo()
	{	return getXMLAttributeValue(ATTRIBUTE_LINK_TO);
	}

	/**
	 * Change the value of the anchor field and 
	 * signal a model change if needed.
	 * 
	 * @param The new anchor path to associate with this TOC
	 */
	public void setFieldAnchorTo(String name)
	{	String old = getXMLAttributeValue(ATTRIBUTE_LINK_TO);
		setXMLAttribute(ATTRIBUTE_LINK_TO, name);
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_LINK_TO, old, name);
		}
	}
}
