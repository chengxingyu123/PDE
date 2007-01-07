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
package org.eclipse.pde.core;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;

/**
 * The central class for the plug-in development target platform. This class cannot
 * be instantiated or subclassed by clients; all functionality is provided 
 * by static methods.  Features include:
 * <ul>
 * <li>the target platform's OS/WS/ARCH</li>
 * <li>the default application and product</li>
 * <li>the available applications and products</li>
 * </ul>
 * <p>
 * @since 3.3
 * </p>
 */
public class TargetPlatform {

	/**
	 * Returns the target operating system as specified on the <b>Environment</b>
	 * tab of the <b>Plug-in Development > Target Platform</b> preference page.
	 *  
	 * @return the target operating system
	 */
	public static String getOS() {
		return getProperty(ICoreConstants.OS, Platform.getOS());
	}

	/**
	 * Returns the target windowing system as specified on the <b>Environment</b>
	 * tab of the <b>Plug-in Development > Target Platform</b> preference page.
	 *  
	 * @return the target windowing system
	 */
	public static String getWS() {
		return getProperty(ICoreConstants.WS, Platform.getWS());
	}

	/**
	 * Returns the target locale as specified on the <b>Environment</b>
	 * tab of the <b>Plug-in Development > Target Platform</b> preference page.
	 *  
	 * @return the target locale
	 */
	public static String getNL() {
		return getProperty(ICoreConstants.NL, Platform.getNL());
	}

	/**
	 * Returns the target system architecture as specified on the <b>Environment</b>
	 * tab of the <b>Plug-in Development > Target Platform</b> preference page.
	 *  
	 * @return the target system architecture
	 */
	public static String getOSArch() {
		return getProperty(ICoreConstants.ARCH, Platform.getOSArch());
	}

	private static String getProperty(String key, String defaultValue) {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String value = preferences.getString(key);
		return value.equals("") ? defaultValue : value; //$NON-NLS-1$
	}

}
