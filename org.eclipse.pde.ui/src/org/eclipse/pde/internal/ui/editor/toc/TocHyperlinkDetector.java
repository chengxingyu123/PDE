package org.eclipse.pde.internal.ui.editor.toc;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.pde.internal.core.itoc.ITocConstants;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.toc.TocModel;
import org.eclipse.pde.internal.core.text.toc.TocObject;
import org.eclipse.pde.internal.ui.editor.PDEHyperlinkDetector;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.ResourceHyperlink;

public class TocHyperlinkDetector extends PDEHyperlinkDetector {

	/**
	 * @param editor the editor in which to detect the hyperlink
	 */
	public TocHyperlinkDetector(PDESourcePage page) {
		super(page);
	}

	protected IHyperlink[] detectAttributeHyperlink(IDocumentAttribute attr) {
		String attrValue = attr.getAttributeValue();
		if (attrValue.length() == 0)
			return null;
		
		IDocumentNode node = attr.getEnclosingElement();
		if (node == null 
				|| !(node instanceof TocObject) 
				|| !((TocObject)node).getModel().isEditable())
		{	return null;
		}

		TocObject tocObject = (TocObject)node;
		TocModel model = tocObject.getModel();
		IResource res = model.getUnderlyingResource();
		IRegion linkRegion = new Region(attr.getValueOffset(), attr.getValueLength());

		IHyperlink[] link = new IHyperlink[1];
		if (tocObject.getType() == ITocConstants.TYPE_TOC) {
			if (attr.getAttributeName().equals(ITocConstants.ATTRIBUTE_TOPIC))
			{	link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			}
		} else if (tocObject.getType() == ITocConstants.TYPE_TOPIC) {
			if (attr.getAttributeName().equals(ITocConstants.ATTRIBUTE_HREF))
			{	link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			}
		} else if (tocObject.getType() == ITocConstants.TYPE_LINK) {
			if (attr.getAttributeName().equals(ITocConstants.ATTRIBUTE_TOC))
			{	link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			}
		}

		if (link[0] != null)
		{	return link;
		}

		return null;
	}

}