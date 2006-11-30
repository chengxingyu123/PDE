package org.eclipse.pde.internal.core.text.help;

import org.eclipse.pde.internal.core.text.DocumentTextNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;


public class ContextDescriptionNode extends AbstractContextNode {

	public static final String F_DESC = "description";
	private static final long serialVersionUID = 1L;

	protected String getName() {
		return F_DESC;
	}
	
	public String getDescription() {
		return getTextNode() != null ? getTextNode().getText() : null;
	}
	
	public void setDescription(String description) {
		if (description == null)
			removeTextNode();
		else {
			IDocumentTextNode node = getTextNode();
			if (node == null) {
				node = new DocumentTextNode();
				node.setEnclosingElement(this);
				addTextNode(node);
			}
			node.setText(description);
		}
	}
}
