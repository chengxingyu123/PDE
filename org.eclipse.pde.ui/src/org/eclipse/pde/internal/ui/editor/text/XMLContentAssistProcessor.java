package org.eclipse.pde.internal.ui.editor.text;

import java.io.PrintWriter;
import java.util.ArrayList;

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
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.swt.graphics.Image;

public class XMLContentAssistProcessor implements IContentAssistProcessor {

	protected static final String
		F_COMMENT = "!-- comment --";
	protected static final int 
		F_EP = 0,
		F_EX = 1,
		F_EL = 2,
		F_AT = 3,
		F_CO = 4;
	private static final int 
		F_NO_ASSIST = 0,
		F_ADD_ATTRIB = 1,
		F_ADD_CHILD = 2,
		F_OPEN_TAG = 3,
		F_BROKEN_MODEL = 4;
	
	// temp
	private static final boolean DEBUG = false;
	
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
			case F_CO:
				return F_IMAGES[F_CO] = null;
			}
		}
		return F_IMAGES[type];
	}
	
	public void disposeImages() {
		for (int i = 0; i < F_IMAGES.length; i++)
			if (F_IMAGES[i] != null && !F_IMAGES[i].isDisposed())
				F_IMAGES[i].dispose();
	}
	
	class VSchemaAttribute extends VSchemaObject implements ISchemaAttribute {
		public VSchemaAttribute(String name, String description) {
			super(name, description, F_AT);
		}
		public int getUse() {return 0;}
		public Object getValue() {return null;}
		public String getBasedOn() {return null;}
		public int getKind() {return 0;}
		public boolean isDeprecated() {return false;}
		public boolean isTranslatable() {return false;}
		public ISchemaSimpleType getType() {return null;}
	}
	
	class VSchemaObject implements ISchemaObject {
		String vName, vDesc; int vType;
		public VSchemaObject(String name, String description, int type)
			{ vName = name; vDesc = description; vType = type; }
		public String getDescription() {return vDesc;}
		public String getName() {return vName;}
		public ISchemaObject getParent() {return null;}
		public ISchema getSchema() {return null;}
		public void setParent(ISchemaObject parent) {}
		public Object getAdapter(Class adapter) {return null;}
		public void write(String indent, PrintWriter writer) {}
		public int getVType() {return vType;}
	}
	
	private PDESourcePage fSourcePage;
	private final Image[] F_IMAGES = new Image[5];
	
	public XMLContentAssistProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IBaseModel model = getModel();
		long time = System.currentTimeMillis();
		// TODO shouldn't need to reconcile every time - can we check state?
		if (model instanceof IReconcilingParticipant)
			((IReconcilingParticipant)model).reconciled(viewer.getDocument());
		System.out.println("time to reconcile (ms): " + (System.currentTimeMillis() - time));
		
		IDocumentRange range = fSourcePage.getRangeElement(offset, true);
		range = verifyPosition(range, offset);
		
		if (range instanceof IDocumentAttribute) 
			return computeCompletionProposal((IDocumentAttribute)range, offset);
		if (range instanceof IDocumentNode)
			return computeCompletionProposal((IDocumentNode)range, offset, viewer.getDocument());
		return null;
	}

	private IDocumentRange verifyPosition(IDocumentRange range, int offset) {
		// if we are rigth AT (cursor before) the range, we want to contribute
		// to its parent
		if (range instanceof IDocumentAttribute) {
			if (((IDocumentAttribute)range).getNameOffset() == offset)
				return ((IDocumentAttribute)range).getEnclosingElement();
		} else if (range instanceof IDocumentNode) {
			if (((IDocumentNode)range).getOffset() == offset)
				return ((IDocumentNode)range).getParentNode();
		} else if (range instanceof IDocumentTextNode) {
			if (((IDocumentTextNode)range).getOffset() == offset)
				return ((IDocumentTextNode)range).getEnclosingElement();
		}
		return range;
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
	
	private ICompletionProposal[] computeCompletionProposal(IDocumentNode node, int offset, IDocument doc) {
		int prop_type = determineAssistType(node, doc, offset);
		if (DEBUG) {
			switch (prop_type) {
			case F_NO_ASSIST:
				System.out.println(System.currentTimeMillis() + " no assist");
				break;
			case F_ADD_ATTRIB:
				System.out.println(System.currentTimeMillis() + " add attrib");
				break;
			case F_OPEN_TAG:
				System.out.println(System.currentTimeMillis() + " opening a tag");
				break;
			case F_ADD_CHILD:
				System.out.println(System.currentTimeMillis() + " add child");
				break;
			case F_BROKEN_MODEL:
				System.out.println(System.currentTimeMillis() + " broken model");
				break;
			}
		}
		switch (prop_type) {
		case F_NO_ASSIST:
			return null;
		case F_ADD_ATTRIB:
			return computeAddAttributeProposal(node, offset, doc);
		case F_OPEN_TAG:
			return computeOpenTagProposal(node, offset, doc);
		case F_ADD_CHILD:
			return computeAddChildProposal(node, offset, doc);
		case F_BROKEN_MODEL:
			return computeBrokenModelProposal(node, offset, doc);
		}
		return null;
	}
	
	private int determineAssistType(IDocumentNode node, IDocument doc, int offset) {
		int len = node.getLength();
		int off = node.getOffset();
		if (len == -1 || off == -1)
			return F_BROKEN_MODEL;
		
		offset = offset - off; // look locally
		if (offset > node.getXMLTagName().length()) { // +1 for '<' open tag char
			try {
				String eleValue = doc.get(off, len);
				int ind = eleValue.indexOf('>');
				if (eleValue.charAt(ind - 1) == '/')
					ind -= 1;
				if (offset <= ind)
					return F_ADD_ATTRIB;
				ind = eleValue.lastIndexOf('<');
				if (ind == 0 && offset == len - 1)
					return F_OPEN_TAG; // childless node - check if it can be cracked open
				if (eleValue.charAt(ind + 1) == '/' && offset <= ind)
					return F_ADD_CHILD;
			} catch (BadLocationException e) {
			}
		}
		return F_NO_ASSIST;		
	}
	
	private ICompletionProposal[] computeAddChildProposal(IDocumentNode node, int offset, IDocument doc) {
		if (node instanceof IPluginBase) {
			return new ICompletionProposal[] {
					new XMLCompletionProposal(node, new VSchemaObject("extension-point", null, F_EP), offset, this),
					new XMLCompletionProposal(node, new VSchemaObject("extension", null, F_EX), offset, this),
					new XMLCompletionProposal(node, new VSchemaObject(F_COMMENT, null, F_CO), offset, this) };
		} else if (node instanceof IPluginExtensionPoint) {
			return null;
		} else {
			IPluginObject obj = XMLUtil.getTopLevelParent(node);
			if (obj instanceof IPluginExtension) {
				ISchemaElement sElem = XMLUtil.getSchemaElement(node, ((IPluginExtension)obj).getPoint());
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
					ICompletionProposal[] prop = new ICompletionProposal[list.size() + 1];
					for (int i = 0; i < prop.length; i++) {
						if (i != prop.length - 1)
							prop[i] = new XMLCompletionProposal(node, (ISchemaElement)list.get(i), offset, this);
						else
							prop[i] = new XMLCompletionProposal(node, new VSchemaObject(F_COMMENT, null, F_CO), offset, this);
					}
					return prop;
				}
			}
		}
		return null;
	}

	private ICompletionProposal[] computeOpenTagProposal(IDocumentNode node, int offset, IDocument doc) {
		IPluginObject obj = XMLUtil.getTopLevelParent(node);
		if (obj instanceof IPluginExtension) {
			ISchemaElement sElem = XMLUtil.getSchemaElement(node, ((IPluginExtension)obj).getPoint());
			if (sElem == null)
				return null;
			ISchemaCompositor comp = ((ISchemaComplexType)sElem.getType()).getCompositor();
			if (comp != null)
				return new ICompletionProposal[] { new XMLCompletionProposal(node, null, offset, this) };
			
		}
		return null;
	}

	private ICompletionProposal[] computeAddAttributeProposal(IDocumentNode node, int offset, IDocument doc) {
		if (node instanceof IPluginExtension) {
			ISchemaElement sElem = XMLUtil.getSchemaElement(node, ((IPluginExtension)node).getPoint());
			ISchemaObject[] sAttrs = sElem != null ?
					sElem.getAttributes() :
					new ISchemaObject[] {
						new VSchemaAttribute(IIdentifiable.P_ID, "The ID of this extension."),
						new VSchemaAttribute(IPluginObject.P_NAME, "The name of this extension."),
						new VSchemaAttribute(IPluginExtension.P_POINT, "The ID of the extension-point this extension will contribute to.")
					};
			return generateAttributeProposals(sAttrs, node, offset);
		} else if (node instanceof IPluginExtensionPoint) {
			ISchemaObject[] sAttrs = new ISchemaObject[] {
						new VSchemaAttribute(IIdentifiable.P_ID, "The ID of this extension-point."),
						new VSchemaAttribute(IPluginObject.P_NAME, "The name of this extension-point."),
						new VSchemaAttribute(IPluginExtensionPoint.P_SCHEMA, "The location of this extension-point's schema file.")
					};
			return generateAttributeProposals(sAttrs, node, offset);
		} else {
			IPluginObject obj = XMLUtil.getTopLevelParent(node);
			if (obj instanceof IPluginExtension) {
				ISchemaElement sElem = XMLUtil.getSchemaElement(node, ((IPluginExtension)obj).getPoint());
				ISchemaObject[] sAttrs = sElem != null ? sElem.getAttributes() : null;
				return generateAttributeProposals(sAttrs, node, offset);
			}
		}
		return null;
	}

	private ICompletionProposal[] computeBrokenModelProposal(IDocumentNode node, int offset, IDocument doc) {
		// malformed xml encountered - give proposals based on document contents
		return null;
	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset)
		{ return null; }
	public char[] getCompletionProposalAutoActivationCharacters() 
		{ return new char[] {'<'}; }
	public char[] getContextInformationAutoActivationCharacters()
		{ return null; }
	public IContextInformationValidator getContextInformationValidator()
		{ return null; }
	public String getErrorMessage()
		{ return null; }
	protected IBaseModel getModel()
		{ return fSourcePage.getInputContext().getModel(); }
	
	private ICompletionProposal[] generateAttributeProposals(ISchemaObject[] sAttrs, IDocumentNode node, int offset) {
		if (sAttrs == null || sAttrs.length == 0)
			return null;
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

}
