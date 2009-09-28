/*******************************************************************************
 * Copyright (c) 2009 Anyware Technologies Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anyware Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.editor;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;

/**
 * <p><b>PROVISIONAL:</b> This API is subject to arbitrary change, including renaming or removal.</p>
 * The class that implements this interface can be used to easily create completion proposals for PDE XML documents 
 * @since 3.6
 */
public interface IPDECompletionProposalFactory {
	/**
	 * @param valueOffset
	 * @param name the name of the completion proposal
	 * @param description the description of the completion proposal
	 * @param offset the offset after the completion proposal has been applied
	 * @param image the image of the completion proposal (can be <code>null</code>) 
	 * @return the created completion proposal
	 * @since 3.5
	 */
	ICompletionProposal createCompletionProposal(int valueOffset, String name, String description, int offset, Image image);
}
