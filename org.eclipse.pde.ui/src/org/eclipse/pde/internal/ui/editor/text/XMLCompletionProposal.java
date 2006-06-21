package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.text.XMLContentAssistProcessor.VSchemaObject;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class XMLCompletionProposal implements ICompletionProposal {
	
	private ISchemaObject fSchemaObject;
	private IDocumentRange fRange;
	private int fOffset, fLen, fSelOffset = -1, fSelLen;
	private XMLContentAssistProcessor fProcessor;
	
	public XMLCompletionProposal(IDocumentRange node, ISchemaObject object, int offset, XMLContentAssistProcessor processor) {
		fRange = node;
		fSchemaObject = object;
		fOffset = offset;
		fProcessor = processor;
	}

	public void apply(IDocument document) {
		StringBuffer sb = new StringBuffer();
		if (fSchemaObject == null && fRange instanceof IDocumentNode) {
			// we are opening up an element
			fSelOffset = fOffset;
			fOffset -= 1;
			fLen = 2;
			sb.append('>');
			sb.append('<');
			sb.append('/');
			sb.append(((IDocumentNode)fRange).getXMLTagName());
			sb.append('>');
		} else if (fSchemaObject.getName().equals(XMLContentAssistProcessor.F_COMMENT)) {
			sb.append('<');
			sb.append(XMLContentAssistProcessor.F_COMMENT);
			sb.append('>');
			fSelOffset = fOffset + 5; // select "comment"
			fSelLen = 7;
		} else if (fSchemaObject instanceof ISchemaAttribute) {
			String attName = ((ISchemaAttribute)fSchemaObject).getName();
			sb.append(attName);
			sb.append('=');
			sb.append('"');
			fSelOffset = fOffset + sb.length();
			String value = " ";
			sb.append(value);
			fSelLen = value.length();
			sb.append('"');
		}
		if (sb.length() == 0)
			return;
		try {
			prepareBuffer(sb, document);
			document.replace(fOffset, fLen, sb.toString());
		} catch (BadLocationException e) {
			PDEPlugin.log(e);
		}
	}
	
	private void prepareBuffer(StringBuffer sb, IDocument document) throws BadLocationException {
		if (fSchemaObject instanceof ISchemaAttribute) {
			// must have whitespace before attribute insert
			if (fOffset > 0 && 
					!Character.isWhitespace(document.getChar(fOffset - 1))) {
				// insert whitespace before
				sb.insert(0, ' ');
				fSelOffset += 1;
			}
			// must have whitespace, tag close or slash after attribute insert
			char c = document.getChar(fOffset);
			if (!Character.isWhitespace(c) && c != '>' && c != '/')
				// insert whitespace after
				sb.append(' ');
		}
	}
	
	public String getAdditionalProposalInfo() {
		if (fSchemaObject == null)
			return null;
		return fSchemaObject.getDescription();
	}

	public IContextInformation getContextInformation() {
		return null;
	}

	public String getDisplayString() {
		if (fSchemaObject instanceof ISchemaAttribute)
			return fSchemaObject.getName();
		if (fSchemaObject != null)
			return '<' + fSchemaObject.getName() + '>';
		if (fRange instanceof IDocumentNode)
			return NLS.bind("open up the <{0}> element", ((IDocumentNode)fRange).getXMLTagName());
		return null;
	}

	public Image getImage() {
		if (fSchemaObject instanceof VSchemaObject)
			return fProcessor.getImage(((VSchemaObject)fSchemaObject).getVType());
		if (fSchemaObject instanceof ISchemaAttribute)
			return fProcessor.getImage(XMLContentAssistProcessor.F_AT);
		if (fSchemaObject instanceof ISchemaElement)
			return fProcessor.getImage(XMLContentAssistProcessor.F_EL);
		return null;
	}

	public Point getSelection(IDocument document) {
		if (fSelOffset == -1)
			return null;
		return new Point(fSelOffset, fSelLen);
	}
	
}
