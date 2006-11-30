package org.eclipse.pde.internal.core.text.help;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;

public class ContextTopicNode extends AbstractContextNode {

	public static final String F_TOPIC = "topic";
	public static final String F_LABEL = "label";
	public static final String F_HREF = "href";
	private static final long serialVersionUID = 1L;

	protected String getName() {
		return F_TOPIC;
	}
	
	public String getHref() {
		return getXMLAttributeValue(F_HREF);
	}
	
	public String getLabel() {
		return getXMLAttributeValue(F_LABEL);
	}
	
	public void setHref(String href) {
		if (href == null) {
			IDocumentAttribute attr = getDocumentAttribute(F_HREF);
			if (attr != null)
				removeDocumentAttribute(attr);
		} else
			setXMLAttribute(F_HREF, href);
		if (getModel() != null)
			getModel().fireStructureChanged(this, IModelChangedEvent.CHANGE, F_HREF);
	}
	
	public void setLabel(String label) {
		if (label == null) {
			IDocumentAttribute attr = getDocumentAttribute(F_LABEL);
			if (attr != null)
				removeDocumentAttribute(attr);
		} else
			setXMLAttribute(F_LABEL, label);
		if (getModel() != null)
			getModel().fireStructureChanged(this, IModelChangedEvent.CHANGE, F_LABEL);
	}
	
	public IFile getTarget() {
		String href = getHref();
		if (href == null || href.trim().length() == 0)
			return null;
		return getModel().getUnderlyingResource().getProject().getFile(href);
	}
	
	public boolean targetExists() {
		IFile file = getTarget();
		// if getTarget() returns null, HREF isint specified, which is valid
		return file != null ? file.exists() : true;
	}
	
	public String write(boolean indent) {
		return indent ? getIndent() + writeShallow(true) : writeShallow(true);
	}
}
