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
				if (!topics[i].targetExists())
					return super.get(getBlankImage(), F_ERROR);
		} else if (element instanceof ContextTopicNode) {
			if (index == 1 && !((ContextTopicNode)element).targetExists())
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
