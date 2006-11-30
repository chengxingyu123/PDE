/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IXMLNodeFactory;

public class ContextHelpNodeFactory implements IXMLNodeFactory {
	
	private ContextHelpModel fModel;

	public ContextHelpNodeFactory(ContextHelpModel model) {
		fModel = model;
	}
	
	public IDocumentNode createDocumentNode(String name, IDocumentNode parent) {
		if (parent == null && ContextsNode.F_CONTEXTS.equals(name))
			return createContexts();
		if (parent instanceof ContextsNode && ContextNode.F_CONTEXT.equals(name))
			return createContextNode((ContextsNode)parent);
		if (parent instanceof ContextNode) {
			if (ContextDescriptionNode.F_DESC.equals(name))
				return createDescription(); 
			if (ContextTopicNode.F_TOPIC.equals(name))
				return createTopicNode();
		}
		return null;
	}
	
	public IDocumentAttribute createAttribute(String name, String value, IDocumentNode enclosingElement) {
		ContextHelpAttribute attribute = new ContextHelpAttribute();
		try {
			attribute.setAttributeName(name);
			attribute.setAttributeValue(value);
		} catch (CoreException e) {
			PDECore.log(e);
		}
		attribute.setEnclosingElement(enclosingElement);
		return attribute;
	}
	
	public ContextsNode createContexts() {
		return fModel.createContexts();
	}

	public ContextNode createContextNode(ContextsNode parent) {
		ContextNode node = new ContextNode();
		node.setModel(fModel);
		return node;
	}
	
	public ContextTopicNode createTopicNode() {
		ContextTopicNode node = new ContextTopicNode();
		node.setModel(fModel);
		return node;
	}
	
	public ContextDescriptionNode createDescription() {
		ContextDescriptionNode node = new ContextDescriptionNode();
		node.setModel(fModel);
		return node;
	}
}
