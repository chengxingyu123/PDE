package org.eclipse.pde.internal.core.text.help;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IXMLNodeFactory;
import org.eclipse.pde.internal.core.text.XMLEditingModel;
import org.xml.sax.helpers.DefaultHandler;

public class ContextHelpModel extends XMLEditingModel implements IContextObject {

	private DefaultHandler fHandler;
	private ContextsNode fContexts;
	protected ContextHelpNodeFactory fFactory;
	
	public ContextHelpModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
		fFactory = new ContextHelpNodeFactory(this);
	}

	protected DefaultHandler createDocumentHandler(IModel model, boolean reconciling) {
		if (fHandler == null)
			fHandler = new ContextDocumentHandler(this, reconciling);
		return fHandler;
	}

	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}

	public ContextHelpModel getModel() {
		return this;
	}

	public IContextObject getParent() {
		return null;
	}

	public ContextsNode createContexts() {
		fContexts = new ContextsNode();
		fContexts.setModel(this);
		return fContexts;
	}
	
	public ContextsNode getContexts() {
		return fContexts;
	}

	protected IDocumentNode getRootNode() {
		return getContexts();
	}

	public IXMLNodeFactory getNodeFactory() {
		return fFactory;
	}
	
	public ContextHelpNodeFactory getContextNodeFactory() {
		return fFactory;
	}
	
	public void fireStructureChanged(IContextObject changeObject, int changeType) {
		fireModelChanged(new ModelChangedEvent(this, changeType, new Object[] {changeObject}, null));
	}
	public void fireStructureChanged(IContextObject changeObject, int changeType, String property) {
		fireModelChanged(new ModelChangedEvent(this, changeType, new Object[] {changeObject}, property));
	}
	public void fireAttributeChanged(IContextObject changeObject, int changeType, String attrName, String oldValue, String newValue) {
		fireModelChanged(new ModelChangedEvent(this, changeObject, attrName, oldValue, newValue));
	}
	public void fireTextNodeStructureChanged(IDocumentNode node, int changeType) {
		fireModelChanged(new ModelChangedEvent(this, node, null, node.getTextNode(), null));
	}
	
	
	public void updateContextsIds() {
		if (fContexts == null)
			return;
		fContexts.clearContexts();
		IDocumentNode[] nodes = fContexts.getChildNodes();
		for (int i = 0; i < nodes.length; i++)
			if (nodes[i] instanceof ContextNode)
				fContexts.injectContext((ContextNode)nodes[i]);
	}
	
}
