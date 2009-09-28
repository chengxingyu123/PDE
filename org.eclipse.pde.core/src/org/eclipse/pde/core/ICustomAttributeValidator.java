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
package org.eclipse.pde.core;

/**
 * <p><b>PROVISIONAL:</b> This API is subject to arbitrary change, including renaming or removal.</p>
 * @since 3.6
 */
public interface ICustomAttributeValidator {
	/**
	 * This method is called by the PDE error reporter to validate a custom attribute value
	 * @param value the custom attribute value to validate
	 * @return computed error messages, or <code>null</code>
	 * @since 3.5
	 */
	public String[] validateAttribute(String value);
}
