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

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;

public class ContextNode extends AbstractContextNode {
	
	public static final String F_CONTEXT = "context";
	public static final String F_ID = "id";
	private static final long serialVersionUID = 1L;
	
	protected String getName() {
		return F_CONTEXT;
	}
	
	protected void updateID(String newVal, String oldVal, IDocumentAttribute attr) {
		try {
			ContextsNode parent = (ContextsNode)getParent();
			ContextHelpModel model = getModel();
			if (model == null)
				return;
			if (newVal == null)
				parent.removeContextNode(oldVal);
			else if (newVal.equals(oldVal))
				return;
			else if (oldVal == null)
				parent.addContextNode(this);
			else
				parent.changeContextID(oldVal, newVal);
		} catch (CoreException e) {
			PDECore.log(e);
		}
	}
	
	public String getId() {
		return getXMLAttributeValue(F_ID);
	}

	public void setId(String id) {
		IDocumentAttribute attr = getDocumentAttribute(F_ID);
		String oldId = attr != null ? attr.getAttributeValue() : null;
		setXMLAttribute(F_ID, id);
		updateID(id, oldId, attr);
	}
	
	public ContextTopicNode[] getTopics() {
		IDocumentNode[] children = getChildNodes();
		ArrayList topics = new ArrayList();
		for (int i = 0; i < children.length; i++)
			if (children[i].getXMLTagName().equals(ContextTopicNode.F_TOPIC))
				topics.add(children[i]);
		return (ContextTopicNode[])topics.toArray(new ContextTopicNode[topics.size()]);
	}
	
	public void removeTopic(ContextTopicNode topic) {
		removeChildNode(topic);
		if (getModel() != null)
			getModel().fireStructureChanged(topic, IModelChangedEvent.REMOVE);
	}
	
	public ContextTopicNode addTopic(String label, String href) {
		ContextTopicNode topic = getModel().fFactory.createTopicNode();
		addChildNode(topic);
		if (href != null)
			topic.setXMLAttribute(ContextTopicNode.F_HREF, href);
		if (label != null)
			topic.setXMLAttribute(ContextTopicNode.F_LABEL, label);
		if (getModel() != null)
			getModel().fireStructureChanged(topic, IModelChangedEvent.INSERT);
		return topic;
	}
	
	public ContextDescriptionNode setDescription(String desc) {
		if (getModel() == null)
			return null;
		IDocumentNode[] children = getChildNodes();
		ContextDescriptionNode descNode = null;
		
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof ContextDescriptionNode) {
				descNode = (ContextDescriptionNode)children[i];
				break;
			}
		}
		if (desc == null) {
			if (descNode != null) {
				removeChildNode(descNode);
				getModel().fireStructureChanged(descNode, IModelChangedEvent.REMOVE);
			}
		} else {
			if (descNode == null) {
				descNode = getModel().fFactory.createDescription();
				descNode.setDescription(desc);
				addChildNode(descNode);
				getModel().fireStructureChanged(descNode, IModelChangedEvent.INSERT);
			} else {
				descNode.setDescription(desc);
				getModel().fireTextNodeStructureChanged(descNode, IModelChangedEvent.CHANGE);
			}
		}
		return descNode;
	}
	
	public String getDescription() {
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++)
			if (children[i] instanceof ContextDescriptionNode)
				return ((ContextDescriptionNode)children[i]).getDescription();
		return null;
	}
	
	public ContextsNode getContexts() {
		return (ContextsNode)getParent();
	}
}
