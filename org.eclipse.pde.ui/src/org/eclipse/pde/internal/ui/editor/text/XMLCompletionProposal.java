package org.eclipse.pde.internal.ui.editor.text;

import java.util.HashSet;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
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
		ITextSelection sel = fProcessor.getCurrentSelection();
		if (sel == null)
			return;
		
		fLen = sel.getLength() + sel.getOffset() - fOffset;
		StringBuffer sb = new StringBuffer();
		boolean doInternalWork = false;
		if (fSchemaObject == null && fRange instanceof IDocumentNode) {
			// we are opening up an element
			fSelOffset = fOffset;
			fOffset -= 1;
			fLen = 2;
			sb.append("></");
			sb.append(((IDocumentNode)fRange).getXMLTagName());
			sb.append('>');
		} else if (fSchemaObject instanceof ISchemaAttribute) {
			String attName = ((ISchemaAttribute)fSchemaObject).getName();
			sb.append(attName);
			sb.append("=\"");
			fSelOffset = fOffset + sb.length();
			String value = generateDefaultAttributeValue((ISchemaAttribute)fSchemaObject);
			sb.append(value);
			fSelLen = value.length();
			sb.append('"');
		} else if (fSchemaObject instanceof ISchemaElement) {
			sb.append('<');
			sb.append(((ISchemaElement)fSchemaObject).getName());
			sb.append(" />");
			doInternalWork = true;
		} else if (fSchemaObject instanceof VSchemaObject) {
			if (((VSchemaObject)fSchemaObject).getVType() == XMLContentAssistProcessor.F_CL) {
				fOffset = sel.getOffset();
				fLen = 0;
				sb.append(" />");
			} else if (((VSchemaObject)fSchemaObject).getVType() == XMLContentAssistProcessor.F_EX) {
				String point = null;
				sb.append("<extension");
				if (point != null)
					sb.append(" point=\"" + point + "\"");
				sb.append(">");
				sb.append(TextUtilities.getDefaultLineDelimiter(document));
				try {
					// add indentation
					int line = document.getLineOfOffset(fOffset);
					int lineOffset = document.getLineOffset(line); 
					int indent = fOffset - lineOffset;
					char[] indentChars = document.get(lineOffset, indent).toCharArray();
					for (int i = 0; i < indentChars.length; i++) {
						if (indentChars[i] == '\t')
							sb.append('\t');
						else
							sb.append(' ');
					}
				} catch (BadLocationException e) {
				}
				sb.append("</extension>");
			} else if (((VSchemaObject)fSchemaObject).getVType() == XMLContentAssistProcessor.F_EP) {
				String id = null;
				String name = null;
				sb.append("<extension-point id=\"" + id + "\" name=\"" + name + "\" />");
			}
		}
		if (sb.length() == 0)
			return;
		try {
			prepareBuffer(sb, document);
			document.replace(fOffset, fLen, sb.toString());
		} catch (BadLocationException e) {
			PDEPlugin.log(e);
		}
		
		if (doInternalWork) {
			IBaseModel model = fProcessor.getModel();
			// TODO shouldn't need to reconcile every time - can we check state?
			if (model instanceof IReconcilingParticipant)
				((IReconcilingParticipant)model).reconciled(document);
			
			if (model instanceof IPluginModelBase) {
				IPluginBase base = ((IPluginModelBase)model).getPluginBase();
				Stack s = new Stack();
				
				if (fRange instanceof IDocumentNode && base instanceof IDocumentNode) {
					IDocumentNode node = (IDocumentNode)fRange;
					IDocumentNode newSearch = (IDocumentNode)base;
					while (node != null && !(node instanceof IPluginBase)) {
						s.push(node);
						node = node.getParentNode();
					}
					
					while (!s.isEmpty()) {
						node = (IDocumentNode)s.pop();
						int nodeIndex = -1;
						while (node != null) {
							nodeIndex += 1;
							node = node.getPreviousSibling();
						}
						newSearch = newSearch.getChildAt(nodeIndex);
					}
					IDocumentNode[] children = newSearch.getChildNodes();
					for (int i = 0; i < children.length; i++) {
						if (children[i].getOffset() == fOffset && 
								children[i] instanceof IPluginElement) {
							computeInsertionElement(
									(ISchemaElement)fSchemaObject,
									(IPluginElement)children[i]);
							fProcessor.flushDocument();
							break;
						}
					}
				}
			}
		}
	}
	
	private String generateDefaultAttributeValue(ISchemaAttribute sAttr) {
		return " ";
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
		if (fSchemaObject instanceof VSchemaObject && 
				((VSchemaObject)fSchemaObject).getVType() == XMLContentAssistProcessor.F_CL)
			return "... />";
		if (fSchemaObject instanceof ISchemaAttribute)
			return fSchemaObject.getName();
		if (fSchemaObject != null)
			return '<' + fSchemaObject.getName() + '>';
		if (fRange instanceof IDocumentNode)
			return "...> </" + ((IDocumentNode)fRange).getXMLTagName() + ">";
		return null;
	}

	public Image getImage() {
		if (fSchemaObject instanceof VSchemaObject)
			return fProcessor.getImage(((VSchemaObject)fSchemaObject).getVType());
		if (fSchemaObject instanceof ISchemaAttribute)
			return fProcessor.getImage(XMLContentAssistProcessor.F_AT);
		if (fSchemaObject instanceof ISchemaElement || fSchemaObject == null)
			return fProcessor.getImage(XMLContentAssistProcessor.F_EL);
		return null;
	}

	public Point getSelection(IDocument document) {
		if (fSelOffset == -1)
			return null;
		return new Point(fSelOffset, fSelLen);
	}
	
	public void computeInsertionElement(ISchemaElement sElement, 
			IPluginElement pElement) {
		computeInsertionElement(sElement, pElement, new HashSet());
	}
	
	
	public void computeInsertionElement(ISchemaElement sElement, 
			IPluginElement pElement, HashSet visited) {
	
		// TODO:  VERY IMPORTANT:  DETECT CYCLES - track visited using a 
		// HashSet
		
		if (sElement == null) {
			// If there is no corresponding schema information, then there is 
			// nothing to augment
			return;
		} else if (pElement == null) {
			// This shouldn't happen
			return;
		} else if (sElement.getType() instanceof ISchemaSimpleType) {
			// If the corresponding schema information is not complex, then
			// there is nothing to augment
			// TODO:  MP:  Augment with simple text contents? YES
			return;
		}
		// We have a complex type
		ISchemaComplexType type = (ISchemaComplexType)sElement.getType();
		// TODO:  Determine if we should always ignore mixed content types
		//type.isMixed();

		ISchemaAttribute[] attributes = type.getAttributes();
		for (int i = 0; i < type.getAttributeCount(); i++) {
			// TODO:  MP:  Check if attributes deprecated ?
			attributes[i].isDeprecated();

			// TODO: MP:  Check for enumerated values and pick one
			if (attributes[i].getUse() == ISchemaAttribute.DEFAULT) {
				// TODO:  MP:  Debug
				System.out.println(
					"ATTRIBUTE DEFAULT:  " + 
					attributes[i].getName() +
					"=\"" +
					attributes[i].getValue() +
					"\""
				);
				
				// Update Model
				try {
					pElement.setAttribute(attributes[i].getName(), attributes[i].getValue().toString());
				} catch (CoreException e) {
					// TODO: MP: Debug
					e.printStackTrace();
				}
			} else if (attributes[i].getUse() == ISchemaAttribute.REQUIRED) {
				// TODO:  MP:  Debug
				System.out.println(
					"ATTRIBUTE REQUIRED:  " + 
					attributes[i].getName() +
					"=\"TODO\""
				);
				
				// Update Model
				try {
					pElement.setAttribute(attributes[i].getName(), "TODO");
				} catch (CoreException e) {
					// TODO: MP: Debug
					e.printStackTrace();
				}
				
			} else {
				// TODO:  MP: Debug
				System.out.println(
					"ATTRIBUTE OPTIONAL:  " + 
					attributes[i].getName() +
					"=\"TODO\""
				);
			}
		}
		
		ISchemaCompositor compositor = type.getCompositor();

		// TODO:  MP:  Determine if this could be null
		if (compositor == null) {
			return;
		}
		
		// Note:  Don't care about min occurences for root node
		
		if (compositor.getKind() == ISchemaCompositor.CHOICE) {
			// Do not process - too presumptious to choose for the
			// user
			// - let plugin manifest editor flag an error
			// - could insert a comment indicating as such - phase #2
			return;
		} else if (compositor.getKind() == ISchemaCompositor.ALL) {
			// Not supported by PDE - should never get here
			return;
		} else if (compositor.getKind() == ISchemaCompositor.GROUP) {
			// Not supported by PDE - should never get here
			return;
		} 
		
		// Assume SEQUENCE if got this far
		
		// TODO: MP: Probably a more efficent way to do this
   		// Just insert the previously created node multiple times
		for (int k = 0; k < compositor.getMinOccurs(); k++) {
		
			ISchemaObject[] schemaObject = compositor.getChildren();
			for (int i = 0; i < compositor.getChildCount(); i++) {
				// TODO: MP:  Do we really need this check? YES
				if (schemaObject[i] instanceof ISchemaElement) {
					ISchemaElement schemaElement = (ISchemaElement) schemaObject[i];
					// TODO:  MP:  Check if elements deprecated ?
					schemaElement.isDeprecated();
					// TODO: MP:  Probably a more efficient way to do this
					// Just insert the previously created node multiple times
					for (int j = 0; j < schemaElement.getMinOccurs(); j++) {
						System.out.println("ELEMENT:  "
								+ schemaElement.getName());
						// TODO: MP:  Recursion handles 
						// SchemaType type = element.getType();
		
						// Update Model
						IPluginElement childElement = null;
						try {
							childElement = pElement.getModel().getFactory()
									.createElement(pElement);
							childElement.setName(schemaElement.getName());
							pElement.add(childElement);
						} catch (CoreException e) {
							// TODO: MP: Debug
							e.printStackTrace();
						}
						// TODO: MP: DO RECURSION HERE
						
						// Track visited
						HashSet newSet = (HashSet)visited.clone();
						// TODO: IMPORTANT: MERGE COMMON CODe
						// TODO:  Will fix bug of immediate detection of cycle if merge
						if (newSet.add(schemaElement.getName()))
							computeInsertionElement(schemaElement, childElement, newSet);
					}
				} else if (schemaObject[i] instanceof ISchemaCompositor) {
					ISchemaCompositor sCompositor = (ISchemaCompositor)schemaObject[i];
					computeInsertionSequence(sCompositor, pElement, (HashSet)visited.clone());
				} else {
					// TODO: MP: Debug
					System.out.println("UNKNOWN:  " + schemaObject[i].getName());
				}
	
			}
			
		}
	
	}
	
	public void computeInsertionSequence(ISchemaCompositor compositor, 
			IPluginElement pElement, HashSet visited) {

		
		// TODO:  MP:  Determine if this could be null
		if (compositor == null) {
			return;
		}
		
		// Note:  Don't care about min occurences for root node
		
		if (compositor.getKind() == ISchemaCompositor.CHOICE) {
			// Do not process - too presumptious to choose for the
			// user
			// - let plugin manifest editor flag an error
			// - could insert a comment indicating as such - phase #2
			return;
		} else if (compositor.getKind() == ISchemaCompositor.ALL) {
			// Not supported by PDE - should never get here
			return;
		} else if (compositor.getKind() == ISchemaCompositor.GROUP) {
			// Not supported by PDE - should never get here
			return;
		} 
		
		// Assume SEQUENCE if got this far
		
		// TODO: MP: Probably a more efficent way to do this
   		// Just insert the previously created node multiple times
		for (int k = 0; k < compositor.getMinOccurs(); k++) {
		
			ISchemaObject[] schemaObject = compositor.getChildren();
			for (int i = 0; i < compositor.getChildCount(); i++) {
				// TODO: MP:  Do we really need this check? YES
				if (schemaObject[i] instanceof ISchemaElement) {
					ISchemaElement schemaElement = (ISchemaElement) schemaObject[i];
					// TODO:  MP:  Check if elements deprecated ?
					schemaElement.isDeprecated();
					// TODO: MP:  Probably a more efficient way to do this
					// Just insert the previously created node multiple times
					for (int j = 0; j < schemaElement.getMinOccurs(); j++) {
						System.out.println("ELEMENT:  "
								+ schemaElement.getName());
						// TODO: MP:  Recursion handles 
						// SchemaType type = element.getType();
		
						// Update Model
						IPluginElement childElement = null;
						try {
							childElement = pElement.getModel().getFactory()
									.createElement(pElement);
							childElement.setName(schemaElement.getName());
							pElement.add(childElement);
						} catch (CoreException e) {
							// TODO: MP: Debug
							e.printStackTrace();
						}
						// TODO: MP: DO RECURSION HERE
						// Track visited
						HashSet newSet = (HashSet)visited.clone();
						// TODO: IMPORTANT: MERGE COMMON CODe
						// TODO:  Will fix bug of immediate detection of cycle if merge
						if (newSet.add(schemaElement.getName()))
							computeInsertionElement(schemaElement, childElement, newSet);
					}
				} else if (schemaObject[i] instanceof ISchemaCompositor) {
					ISchemaCompositor sCompositor = (ISchemaCompositor)schemaObject[i];
					computeInsertionSequence(sCompositor, pElement, (HashSet)visited.clone());
				} else {
					// TODO: MP: Debug
					System.out.println("UNKNOWN:  " + schemaObject[i].getName());
				}
			}
		}
	}
	
}
