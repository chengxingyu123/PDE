/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.pde.internal.core.ibundle.IBundle;

public class ExportPackageHeader extends BasePackageHeader {
    
    private static final long serialVersionUID = 1L;
   
    public ExportPackageHeader(String name, String value, IBundle bundle,
			String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}
    
    protected void processValue() {
        for (int i = 0; i < fManifestElements.size(); i++) {
            ExportPackageObject p = 
            	new ExportPackageObject(this, fManifestElements.get(i), getVersionAttribute());
            fPackages.put(p.getName(), p);
        }
    }
    
    public Vector getPackageNames() {
        Vector vector = new Vector(fPackages.size());
        Iterator iter = fPackages.keySet().iterator();
        for (int i = 0; iter.hasNext(); i++) {
            vector.add(iter.next().toString());
        }
        return vector;
    }
    
    public ExportPackageObject[] getPackages() {
        return (ExportPackageObject[])fPackages.values().toArray(new ExportPackageObject[fPackages.size()]);
    }
    
}
