/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.pde.internal.build.Config;
import org.eclipse.pde.internal.build.Policy;
import org.eclipse.pde.internal.build.builder.FeatureBuildScriptGenerator;

public class ExportFeatureBuildScriptGenerator extends FeatureBuildScriptGenerator {
	protected void generateZipDistributionWholeTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_ZIP_DISTRIBUTION, TARGET_INIT, null, null, Policy.bind("build.feature.zips", featureIdentifier)); //$NON-NLS-1$
		script.printMkdirTask(featureTempFolder);
		Map params = new HashMap(1);
		params.put(PROPERTY_FEATURE_BASE, featureTempFolder);
		params.put(PROPERTY_INCLUDE_CHILDREN, "true"); //$NON-NLS-1$
		params.put(PROPERTY_OS, feature.getOS() == null ? Config.ANY : feature.getOS());
		params.put(PROPERTY_WS, feature.getWS() == null ? Config.ANY : feature.getWS());
		params.put(PROPERTY_ARCH, feature.getOSArch() == null ? Config.ANY : feature.getOSArch());
		params.put(PROPERTY_NL, feature.getNL() == null ? Config.ANY : feature.getNL());

		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, null, params);
		script.printTargetEnd();
	}
	
	protected void generateZipSourcesTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_ZIP_SOURCES, TARGET_INIT, null, null, null);
		script.printMkdirTask(featureTempFolder);
		Map params = new HashMap(1);
		params.put(PROPERTY_TARGET, TARGET_GATHER_SOURCES);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, featureTempFolder + "/plugins"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		script.printAntCallTask(TARGET_ALL_CHILDREN, null, params);
		script.printTargetEnd();
	}

}

