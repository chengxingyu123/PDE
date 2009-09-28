/*******************************************************************************
 * Copyright (c) 2009 Anyware Technologies Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.customattributes;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.ui.editor.IPDECompletionProposalFactory;

/**
 * <p><b>PROVISIONAL:</b> This API is subject to arbitrary change, including renaming or removal.</p>
 * The class that implements this interface is used to retrieve completion proposals for custom extension point attributes
 * @since 3.6
 */
public interface ICustomAttributeCompletionProvider {
	/**
	 * Computes the completion proposals using the currValue as an indicator of what has already been typed
	 * @param valueOffset the offset of the beginning of the attribute value 
	 * @param offset the current offset
	 * @param currValue the current value of the attribute
	 * @param factory a factory that can be used as an helper to create completion proposals
	 * @return array of the completion proposals
	 * @since 3.6
	 */
	public ICompletionProposal[] computeAttributeProposal(int valueOffset, int offset, String currValue, IPDECompletionProposalFactory factory);
}
