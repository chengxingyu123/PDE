/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Instances of this interface are capable of serializing the transformation
 * files associated with a given product.
 */
public interface IProductTransformSerializer {

	/**
	 * @param monitor
	 * 
	 */
	void serialize(IProgressMonitor monitor);

}
