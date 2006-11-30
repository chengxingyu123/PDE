package org.eclipse.pde.internal.core.text.help;

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.PDECore;

public class ContextsNode extends AbstractContextNode {

	private final static String F_DUP = "Cannot add a context node with a duplicate ID: {0} ({0})";
	private final static String F_NUL = "Cannot add a context node with no ID. ({0})";
	
	private static final long serialVersionUID = 1L;
	public static final String F_CONTEXTS = "contexts";
	
	private HashMap fContexts = new HashMap();
	
	protected String getName() {
		return F_CONTEXTS;
	}
	
	/**
	 * Not to be called by clients.
	 * @param id
	 * @param node
	 */
	protected void injectContext(ContextNode node) {
		fContexts.put(node.getId(), node);
	}
	
	/**
	 * Add a Context element to the list of contexts  
	 * @param node the ContextNode to be added (not null)
	 * @return the ContextNode, if it was added
	 * @throws CoreException if the ContextNode has a <code>null</code> ID or if a ContextNode with the same ID already exists in the list
	 */
	public ContextNode addContextNode(ContextNode node) throws CoreException {
		testID(node.getId());
		fContexts.put(node.getId(), node);
		addChildNode(node);
		getModel().fireStructureChanged(node, IModelChangedEvent.INSERT);
		return node;
	}
	
	/**
	 * Get a ContextNode from the list of contexts
	 * @param id
	 * @return the ContextNode with a matching ID, or <code>null</code> if one does not exist
	 */
	public ContextNode getContextNode(String id) {
		return (ContextNode) fContexts.get(id);
	}
	
	/**
	 * Removes a Context element from the list of contexts
	 * @param id the Id of the element to be removed
	 * @return the removed ContextNode, or <code>null</code> if matching node existed
	 */
	public ContextNode removeContextNode(String id) {
		ContextNode node = (ContextNode) fContexts.remove(id);
		removeChildNode(node);
		getModel().fireStructureChanged(node, IModelChangedEvent.REMOVE);
		return node;
	}
	
	/**
	 * Update an existing context id with a new one
	 * @param oldID is the ID to replace
	 * @param newID is the new ID
	 * @return wether the change was successful
	 * @throws CoreException if the <code>newID</code> already coressponds to an existing Context
	 */
	public boolean changeContextID(String oldID, String newID) throws CoreException {
		if (oldID == null || newID == null)
			return false;
		if (oldID.equals(newID))
			return false;
		if (fContexts.get(oldID) == null)
			return false;
		testID(newID);
		ContextNode removed = (ContextNode)fContexts.remove(oldID);
		fContexts.put(newID, removed);
		getModel().fireStructureChanged(removed, IModelChangedEvent.CHANGE, ContextNode.F_ID);
		return removed != null;
	}
	
	private void testID(String id) throws CoreException {
		Status status = null;
		if (id == null)
			status = new Status(
					IStatus.ERROR, 
					PDECore.PLUGIN_ID, 
					NLS.bind(F_NUL, getContextFilename()));
		if (fContexts.get(id) != null)
			status = new Status(
					IStatus.ERROR,
					PDECore.PLUGIN_ID,
					NLS.bind(id, F_DUP, getContextFilename()));
		if (status != null)
			throw new CoreException(status);
	}
	
	protected void clearContexts() {
		fContexts.clear();
	}
	
	private String getContextFilename() {
		if (getModel().getUnderlyingResource() == null)
			return null;
		return getModel().getUnderlyingResource().getName();
	}
	
	public boolean isRoot() {
		return true;
	}

	public String getUnusedContextID() {
		int attempts = 0;
		String id = ContextNode.F_CONTEXT + '_' + Integer.toString(attempts);
		while(true) {
			try {
				testID(id);
				return id;
			} catch (CoreException e) {
				id = ContextNode.F_CONTEXT + '_' + Integer.toString(++attempts);
			}
		}
	}

	public String isValidId(String newText) {
		if (newText == null || newText.length() == 0)
			return "Cannot have an empty ID";
		if (fContexts.get(newText) != null)
			return NLS.bind("{0} is already in use", newText);
		return null;
	}
}
