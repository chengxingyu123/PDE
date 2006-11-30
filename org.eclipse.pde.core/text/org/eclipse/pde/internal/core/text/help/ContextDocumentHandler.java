package org.eclipse.pde.internal.core.text.help;

import org.eclipse.pde.internal.core.text.XMLDocumentHandler;
import org.xml.sax.SAXException;

public class ContextDocumentHandler extends XMLDocumentHandler {

	public ContextDocumentHandler(ContextHelpModel model, boolean reconciling) {
		super(model, reconciling);
	}

	public void endDocument() throws SAXException {
		super.endDocument();
		((ContextHelpModel)getModel()).updateContextsIds();
	}
}
