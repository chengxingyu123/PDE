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

import org.eclipse.core.resources.IFolder;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;

/**
 * 
 */
public interface ICustomizationInfo extends IProductObject {

	public static final String P_USE = "use"; //$NON-NLS-1$
	public static final String P_TRANSFORM_HOST = "transformHost"; //$NON-NLS-1$

	boolean getUseCustomizations();

	void setUseCustomizations(boolean b);

	void addTransform(IProductTransform transform);

	void removeTransform(IProductTransform transform);

	IProductTransform[] getTransforms();

	void setTargetPlugin(String pluginId);

	String getTargetPlugin();

	IProductTransformSerializer createTransformSerializer(IFolder resource);

	IProductCustomizationConfigurer createConfigurer(
			IBundlePluginModel bundleModel);

	String[] getFrameworkExtensions();
}
