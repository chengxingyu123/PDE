/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;

/**
 * An abstract launch delegate for OSGi-based launch configurations
 * <p>
 * OSGi framework clients are encouraged to subclass this class.
 * </p>
 * @since 3.3
 */
public abstract class AbstractOSGiLaunchConfiguration extends AbstractPDELaunchConfiguration {
	
	public abstract void initialize(ILaunchConfigurationWorkingCopy configuration);
	
	public ISourceContainer[] getSourceContainers() {
		return new ISourceContainer[0];
	}

}
