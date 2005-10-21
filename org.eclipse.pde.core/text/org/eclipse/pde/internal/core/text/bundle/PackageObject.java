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

import org.eclipse.osgi.util.ManifestElement;

public class PackageObject extends PDEManifestElement {

    private static final long serialVersionUID = 1L;

    private String fVersionAttribute;
    private String fName;
    private String fVersion;
    
    public PackageObject(ManifestHeader header, ManifestElement element, String versionAttribute) {
    	super(header, element);
    	fVersionAttribute = versionAttribute;
        fName = element.getValue();
        fVersion = element.getAttribute(fVersionAttribute);
        setModel(fHeader.getBundle().getModel());
    }
    
    public PackageObject(ManifestHeader header, String name, String version, String versionAttribute) {
        super(header);
        fVersion = version;
        fVersionAttribute = versionAttribute;
        fName = name.length() > 0 ? name : "."; //$NON-NLS-1$
        setModel(fHeader.getBundle().getModel());
    }
    
     public String toString() {
        StringBuffer buffer = new StringBuffer(fName);
        if (fVersion != null && fVersion.length() > 0) {
            buffer.append(" "); //$NON-NLS-1$
            boolean wrap = Character.isDigit(fVersion.charAt(0));
            if (wrap)
                buffer.append("("); //$NON-NLS-1$
            buffer.append(fVersion);
            if (wrap)
                buffer.append(")"); //$NON-NLS-1$
        }
        return buffer.toString();
    }
    
    public String getVersion() {
        return fVersion;
    }

    public String getName() {
        return fName;
    }
    
    public void setName(String name) {
    	fName = name;
    }

    public void setVersion(String version) {
        String old = fVersion;
        fVersion = version;
        firePropertyChanged(this, fVersionAttribute, old, version);
    }

}
