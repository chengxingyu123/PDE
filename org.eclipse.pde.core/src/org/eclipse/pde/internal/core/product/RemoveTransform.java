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
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductTransform;
import org.w3c.dom.Node;

/**
 *
 */
public class RemoveTransform extends ProductObject implements IProductTransform {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Object fTransformedObject = null;

	/**
	 * @param selected
	 */
	public RemoveTransform(IProductModel model, Object transformedObject) {
		super(model);
		fTransformedObject = transformedObject;
	}
	
	/**
	 * @param model
	 */
	public RemoveTransform(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		Node targetAttribute = node.getAttributes().getNamedItem("target");
		fTransformedObject = ProductTransformFactory.fromXPath(targetAttribute.getNodeValue());
		if (fTransformedObject == null)
			throw new IllegalStateException();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.println("<remove target=\"" + ProductTransformFactory.toXPath(fTransformedObject, true) + "\" />");
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof RemoveTransform) {
			RemoveTransform other = (RemoveTransform) obj;
			if (other.fTransformedObject.equals(fTransformedObject))
				return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fTransformedObject.hashCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductTransform#getTransformedObject()
	 */
	public Object getTransformedObject() {
		return fTransformedObject;
	}
}
