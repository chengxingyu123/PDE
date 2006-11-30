package org.eclipse.pde.internal.ui.wizards.help;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.help.ContextTopicNode;
import org.eclipse.pde.internal.ui.nls.StringHelper;
import org.eclipse.swt.widgets.TableItem;

public class ContextHelpCellModifier implements ICellModifier {

	public boolean canModify(Object element, String property) {
		return true;
	}
	public Object getValue(Object element, String property) {
		if (property.equals(ContextTopicNode.F_LABEL)) {
			IDocumentAttribute attr = ((ContextTopicNode)element).getDocumentAttribute(property);
			return StringHelper.unwindEscapeChars(attr != null ? attr.getAttributeValue() : new String());
		} else if (property.equals(ContextTopicNode.F_HREF)) {
			return ((ContextTopicNode)element).getTarget();
		}
		return null;
	}
	public void modify(Object element, String property, Object value) {
		if (element instanceof TableItem)
			element = ((TableItem)element).getData();
		if (element instanceof ContextTopicNode) {
			ContextTopicNode node = (ContextTopicNode)element;
			if (value == null) {
				IDocumentAttribute attr = node.getDocumentAttribute(property);
				if (attr != null)
					node.removeDocumentAttribute(attr);
				return;
			} 
			if (property.equals(ContextTopicNode.F_HREF)) {
				if (value instanceof IResource)
					value = ((IResource)value).getProjectRelativePath().toString();
				node.setHref(StringHelper.windEscapeChars(value.toString()));
			} else if (property.equals(ContextTopicNode.F_LABEL))
				node.setLabel(StringHelper.windEscapeChars(value.toString()));
		}
	}
}
