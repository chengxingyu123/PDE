package org.eclipse.pde.internal.ui.editor.text;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.swt.graphics.Image;

public class XMLContentAssistProcessor implements IContentAssistProcessor, ICompletionListener {

	protected static final int F_EP = 0;
	protected static final int F_EX = 1;
	protected static final int F_EL = 2;
	protected static final int F_AT = 3;
	
	private final Image[] F_IMAGES = new Image[4];
	
	public Image getImage(int type) {
		if (F_IMAGES[type] == null) {
			switch(type) {
			case F_EP:
				return F_IMAGES[F_EP] = PDEPluginImages.DESC_EXT_POINT_OBJ.createImage();
			case F_EX:
				return F_IMAGES[F_EX] = PDEPluginImages.DESC_EXTENSION_OBJ.createImage();
			case F_EL:
				return F_IMAGES[F_EL] = PDEPluginImages.DESC_XML_ELEMENT_OBJ.createImage();
			case F_AT:
				return F_IMAGES[F_AT] = PDEPluginImages.DESC_ATT_URI_OBJ.createImage();
			}
		}
		return F_IMAGES[type];
	}
	
	public void disposeImages() {
		for (int i = 0; i < F_IMAGES.length; i++)
			if (F_IMAGES[i] != null && !F_IMAGES[i].isDisposed())
				F_IMAGES[i].dispose();
	}
	
	
	
	class VirutalSchemaObject implements ISchemaObject {
		String vName, vDesc; int vType;
		public VirutalSchemaObject(String name, String description, int type)
			{ vName = name; vDesc = description; vType = type; }
		public String getDescription() {return vDesc;}
		public String getName() {return vName;}
		public ISchemaObject getParent() {return null;}
		public ISchema getSchema() {return null;}
		public void setParent(ISchemaObject parent) {}
		public Object getAdapter(Class adapter) {return null;}
		public void write(String indent, PrintWriter writer) {}
		public int getType() {return vType;}
	}
	
	private PDESourcePage fSourcePage;
	
	public XMLContentAssistProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocumentRange range = fSourcePage.getRangeElement(offset, true);
		if (range instanceof IDocumentAttribute) {
			// if we are rigth AT (cursor before) the attribute, we want to contribute
			// to its parent
			if (((IDocumentAttribute)range).getNameOffset() != offset)
				return computeCompletionProposal((IDocumentAttribute)range, offset);
			range = ((IDocumentAttribute)range).getEnclosingElement();
		}
		if (range instanceof IDocumentNode)
			return computeCompletionProposal((IDocumentNode)range, offset, viewer);
		return null;
	}

	private ICompletionProposal[] computeCompletionProposal(IDocumentAttribute attr, int offset) {
		IPluginObject obj = XMLUtil.getTopLevelParent((IDocumentNode)attr);
		if (obj instanceof IPluginExtension) {
			ISchemaAttribute sAttr = XMLUtil.getSchemaAttribute(attr, ((IPluginExtension)obj).getPoint());
			if (sAttr == null)
				return null;
			
			if (sAttr.getKind() == IMetaAttribute.JAVA) {
				String basedOn = sAttr.getBasedOn();
				if (basedOn == null)
					return null;
				// TODO basedOn story has to be finalized - too clostly to determine if
				// this field is for extending or implementing
				
			} else if (sAttr.getKind() == IMetaAttribute.RESOURCE) {
				// provide proposals with all resources in current plugin?
				if (attr.getAttributeValue().equals(IPluginExtensionPoint.P_SCHEMA)) {
					
				}
			} else { // we have an IMetaAttribute.STRING kind
				if (sAttr.getType() == null)
					return null;
				ISchemaRestriction sRestr = (sAttr.getType()).getRestriction();
				if (sRestr == null)
					return null;
				Object[] restrictions = sRestr.getChildren();
				for (int i = 0; i < restrictions.length; i++) {
					if (restrictions[i] instanceof ISchemaRestriction) {
						((ISchemaRestriction)restrictions[i]).getName();
					}
				}
				restrictions.toString();
			}
		} else if (obj instanceof IPluginExtensionPoint) {
			// provide proposals with all schama files in current plugin?
			if (attr.getAttributeValue().equals(IPluginExtensionPoint.P_SCHEMA)) {
				
			}
		}
		return null;
	}
	
	private boolean canInsertIntoNode(IDocument doc, int offset) {
		try {
			char c = doc.getChar(offset);
			if (Character.isWhitespace(c) || c == '/' || c == '>')
				return true;
			if (offset > 0) {
				c = doc.getChar(offset - 1);
				return Character.isWhitespace(c) || c == '/' || c == '>';
			}
		} catch (BadLocationException e) {
		}
		return false;
	}
	
	private ICompletionProposal[] computeCompletionProposal(IDocumentNode node, int offset, ITextViewer viewer) {
		IPluginObject obj = XMLUtil.getTopLevelParent(node);
		if (obj instanceof IPluginExtension) {
			try {
				if (!canInsertIntoNode(viewer.getDocument(), offset))
					return null;
				int len = node.getLength();
				String eleValue = viewer.getDocument().get(node.getOffset(), len);
				int ind = eleValue.indexOf('>');
				// something is wrong - need to rebuild model
				if (ind == -1)
					return null;
				if (eleValue.charAt(ind - 1) == '/')
					ind -= 1;
				ISchemaElement sElem = XMLUtil.getSchemaElement(node, ((IPluginExtension)obj).getPoint());
				if (offset <= node.getOffset() + ind) { // inside element tag - provide completion for attributes
					ISchemaObject[] sAttrs;
					if (sElem != null)
						sAttrs = sElem.getAttributes();
					else
						sAttrs = new ISchemaObject[] {
								new VirutalSchemaObject(IIdentifiable.P_ID, "The ID of this extension.", F_AT),
								new VirutalSchemaObject(IPluginObject.P_NAME, "The name of this extension.", F_AT),
								new VirutalSchemaObject(IPluginExtension.P_POINT, "The ID of the extension-point this extension will contribute to.", F_AT)
						};
					
					IDocumentAttribute[] attrs = node.getNodeAttributes();
					
					ArrayList list = new ArrayList();
					for (int i = 0; i < sAttrs.length; i++) {
						int k; // if we break early we wont add
						for (k = 0; k < attrs.length; k++)
							if (attrs[k].getAttributeName().equals(sAttrs[i].getName()))
								break;
						if (k == attrs.length)
							list.add(sAttrs[i]);
					}
					ICompletionProposal[] proposals = new ICompletionProposal[list.size()];
					if (proposals.length == 0)
						return null;
					for (int i = 0; i < proposals.length; i++)
						proposals[i] = new XMLCompletionProposal(node, (ISchemaObject)list.get(i), offset, this);
					
					return proposals;
				}
				ind = eleValue.lastIndexOf('<');
				if (ind != 0 && offset > node.getOffset() + ind)
					// inside closing tag
					return null;
				// outside element tag - provide completion for child elements
				if (sElem != null && sElem.getType() instanceof ISchemaComplexType) {
					IDocumentNode[] children = node.getChildNodes();
					ISchemaCompositor comp = ((ISchemaComplexType)sElem.getType()).getCompositor();
					if (comp == null)
						return null;
					ISchemaObject[] sChildren = comp.getChildren();
					
					ArrayList list = new ArrayList();
					for (int i = 0; i < sChildren.length && sChildren[i] instanceof ISchemaElement; i++) {
						int k; // if we break early we wont add
						for (k = 0; k < children.length; k++)
							if (children[k].getXMLTagName().equals(sChildren[i].getName()) &&
									((ISchemaElement)sChildren[i]).getMaxOccurs() == 1)
								break;
						if (k == children.length)
							list.add(sChildren[i]);
					}
					ICompletionProposal[] proposals = new ICompletionProposal[list.size()];
					for (int i = 0; i < proposals.length; i++)
						proposals[i] = new XMLCompletionProposal(node, (ISchemaElement)list.get(i), offset, this);
					
					return proposals;
				}
			} catch (BadLocationException e) {
			}
		} else if (node instanceof IPluginBase) {
			return new ICompletionProposal[] {
					new XMLCompletionProposal(node, new VirutalSchemaObject("extension-point", null, F_EP), offset, this),
					new XMLCompletionProposal(node, new VirutalSchemaObject("extension", null, F_EX), offset, this)
			};
		}
		return null;
	}
	
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] {'<'};
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	public String getErrorMessage() {
		return null;
	}

	protected IBaseModel getModel() {
		return fSourcePage.getInputContext().getModel();
	}

	public void assistSessionEnded(ContentAssistEvent event) {
		// TODO Auto-generated method stub
		
	}

	public void assistSessionStarted(ContentAssistEvent event) {
		// TODO Auto-generated method stub
		
	}

	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
		// TODO Auto-generated method stub
		
	}

}
