package org.eclipse.pde.internal.ui.editor.text;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
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
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class XMLContentAssistProcessor implements IContentAssistProcessor {

	protected static final int F_EP = 0;
	protected static final int F_EX = 1;
	protected static final int F_EL = 2;
	protected static final int F_AT = 3;
	
	private final Image[] F_IMAGES = new Image[4];
	
	private Image getImage(int type) {
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
	
	class XMLCompletionProposal implements ICompletionProposal {
		
		private ISchemaObject fSchemaObject;
		private IDocumentRange fNode;
		private int fOffset;
		
		public XMLCompletionProposal(IDocumentRange node, ISchemaObject attribute, int offset) {
			fNode = node;
			fSchemaObject = attribute;
			fOffset = offset;
		}

		public void apply(IDocument document) {
			IBaseModel baseModel = fSourcePage.getInputContext().getModel();
			if (baseModel instanceof IPluginModelBase) {
				if (fSchemaObject instanceof ISchemaAttribute) {
					if (fNode instanceof IPluginElement) {
						try {
							((IPluginElement)fNode).setAttribute(
									((ISchemaAttribute)fSchemaObject).getName(),
									"name");
						} catch (CoreException e) {
							PDEPlugin.log(e);
						}
					}
				}
			}
			fSourcePage.getInputContext().flushEditorInput();
		}

		public String getAdditionalProposalInfo() {
			return fSchemaObject.getDescription();
		}

		public IContextInformation getContextInformation() {
			return null;
		}

		public String getDisplayString() {
			return fSchemaObject.getName();
		}

		public Image getImage() {
			if (fSchemaObject instanceof VirutalSchemaObject)
				return XMLContentAssistProcessor.this.getImage(((VirutalSchemaObject)fSchemaObject).getType());
			if (fSchemaObject instanceof ISchemaAttribute)
				return XMLContentAssistProcessor.this.getImage(F_AT);
			if (fSchemaObject instanceof ISchemaElement)
				return XMLContentAssistProcessor.this.getImage(F_EL);
			return null;
		}

		public Point getSelection(IDocument document) {
			return new Point(fOffset, 0);
		}
		
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
			if (((IDocumentAttribute)range).getNameOffset() == offset)
				range = ((IDocumentAttribute)range).getEnclosingElement();
			else
				return computeCompletionProposal((IDocumentAttribute)range, offset);
			
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
				String eleValue = viewer.getDocument().get(node.getOffset(), node.getLength());
				int beforeClose = eleValue.indexOf('>');
				//TODO beforeClose should never be -1 - model needs to be reconciled sooner 
				if (beforeClose <= 0)
					return null;
				if (eleValue.charAt(beforeClose - 1) == '/')
					beforeClose -= 1;
				ISchemaElement sElem = XMLUtil.getSchemaElement(node, ((IPluginExtension)obj).getPoint());
				if (offset <= node.getOffset() + beforeClose) { // inside element tag - provide completion for attributes
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
						proposals[i] = new XMLCompletionProposal(node, (ISchemaObject)list.get(i), offset);
					
					return proposals;
				}
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
						proposals[i] = new XMLCompletionProposal(node, (ISchemaElement)list.get(i), offset);
					
					return proposals;
				}
			} catch (BadLocationException e) {
			}
		} else if (node instanceof IPluginBase) {
			return new ICompletionProposal[] {
					new XMLCompletionProposal(node, new VirutalSchemaObject("extension-point", null, F_EP), offset),
					new XMLCompletionProposal(node, new VirutalSchemaObject("extension", null, F_EX), offset)
			};
		}
		return null;
	}
	
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
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

}
