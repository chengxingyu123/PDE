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
package org.eclipse.pde.internal.ui.wizards.help;

import org.eclipse.pde.internal.core.text.help.ContextHelpModel;
import org.eclipse.pde.internal.core.text.help.ContextNode;
import org.eclipse.pde.internal.core.text.help.ContextTopicNode;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;

public class ContextHelpLabelProvider extends SharedLabelProvider {
	public String getColumnText(Object obj, int index) {
		if (obj instanceof ContextTopicNode) {
			switch(index) {
			case 0:
				return ((ContextTopicNode)obj).getLabel();
			case 1:
				return ((ContextTopicNode)obj).getHref(); 
			}
		}
		return super.getColumnText(obj, index);
	}
	public Image getColumnImage(Object element, int index) {
		if (element instanceof ContextNode) {
			ContextTopicNode[] topics = ((ContextNode)element).getTopics();
			for (int i = 0; i < topics.length; i++)
				if (!topics[i].validTarget())
					return super.get(getBlankImage(), F_ERROR);
		} else if (element instanceof ContextTopicNode) {
			if (index == 1 && !((ContextTopicNode)element).validTarget())
				return super.get(getBlankImage(), F_ERROR);
		}
		return super.getColumnImage(element, index);
	}
	public String getText(Object element) {
		if (element instanceof ContextHelpModel)
			return ((ContextHelpModel)element).getUnderlyingResource().getProjectRelativePath().toString();
		if (element instanceof ContextNode)
			return ((ContextNode)element).getId();
		return super.getText(element);
	}
}
