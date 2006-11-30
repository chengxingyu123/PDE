package org.eclipse.pde.internal.core.text;

public interface IXMLNodeFactory {
	public IDocumentNode createDocumentNode(String name, IDocumentNode parent);
	public IDocumentAttribute createAttribute(String name, String value, IDocumentNode encEl);
}
