package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.ui.editor.text.XMLContentAssistProcessor.VirutalSchemaObject;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;

public class XMLCompletionProposal implements ICompletionProposal {
	
	private ISchemaObject fSchemaObject;
	private IDocumentRange fNode;
	private int fOffset, fSelOffset, fSelLen;
	private XMLContentAssistProcessor fProcessor;
	
	public XMLCompletionProposal(IDocumentRange node, ISchemaObject attribute, int offset, XMLContentAssistProcessor processor) {
		fNode = node;
		fSchemaObject = attribute;
		fOffset = offset;
		fProcessor = processor;
	}

	public void apply(IDocument document) {
		IBaseModel baseModel = fProcessor.getModel();
		StringBuffer sb = new StringBuffer();
		if (baseModel instanceof IPluginModelBase) {
			if (fSchemaObject instanceof ISchemaAttribute) {
				if (fNode instanceof IPluginElement) {
					String attName = ((ISchemaAttribute)fSchemaObject).getName();
					sb.append(attName);
					sb.append('=');
					sb.append('"');
					fSelOffset = fOffset + sb.length();
					String value = "name";
					sb.append(value);
					fSelLen = value.length();
					sb.append('"');
				}
			}
		}
		if (sb.length() == 0)
			return;
		try {
			if (fOffset > 0 && 
					!Character.isWhitespace(document.getChar(fOffset - 1))) {
				// insert whitespace before
				sb.insert(0, ' ');
				fSelOffset += 1;
			}
			char c = document.getChar(fOffset);
			if (!Character.isWhitespace(c) && c != '>' && c != '/')
				// insert whitespace after
				sb.append(' ');
			new InsertEdit(fOffset, sb.toString()).apply(document);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
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
			return fProcessor.getImage(((VirutalSchemaObject)fSchemaObject).getType());
		if (fSchemaObject instanceof ISchemaAttribute)
			return fProcessor.getImage(XMLContentAssistProcessor.F_AT);
		if (fSchemaObject instanceof ISchemaElement)
			return fProcessor.getImage(XMLContentAssistProcessor.F_EL);
		return null;
	}

	public Point getSelection(IDocument document) {
		return new Point(fSelOffset, fSelLen);
	}
	
}
