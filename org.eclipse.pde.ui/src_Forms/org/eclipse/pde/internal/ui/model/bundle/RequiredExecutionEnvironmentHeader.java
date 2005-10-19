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
package org.eclipse.pde.internal.ui.model.bundle;

import java.util.ArrayList;

import org.eclipse.pde.internal.core.ibundle.IBundle;

public class RequiredExecutionEnvironmentHeader extends ManifestHeader {
    
    private static final long serialVersionUID = 1L;
    public static final int TOTAL_JRES = 7;
    public static final int TOTAL_J2MES = 2;
    public static final ArrayList JRES = new ArrayList(TOTAL_JRES);
    public static final ArrayList J2MES = new ArrayList(TOTAL_J2MES);
    static {
    	JRES.add("OSGi/Minimum-1.0"); //$NON-NLS-1$
    	JRES.add("OSGi/Minimum-1.1"); //$NON-NLS-1$
    	JRES.add("JRE-1.1"); //$NON-NLS-1$
    	JRES.add("J2SE-1.2"); //$NON-NLS-1$
    	JRES.add("J2SE-1.3"); //$NON-NLS-1$
    	JRES.add("J2SE-1.4"); //$NON-NLS-1$
    	JRES.add("J2SE-1.5"); //$NON-NLS-1$
    	
    	J2MES.add("CDC-1.0/Foundation-1.0"); //$NON-NLS-1$
    	J2MES.add("CDC-1.1/Foundation-1.1"); //$NON-NLS-1$
    }
    
    public static String[] getJRES() {
    	return (String[])JRES.toArray(new String[JRES.size()]);
    }
    public static String[] getJ2MES() {
    	return (String[])J2MES.toArray(new String[J2MES.size()]);
    }
    
    private ManifestElement fMinJRE;
    private ManifestElement fMinJ2ME;
    
    public RequiredExecutionEnvironmentHeader(String name, String value, IBundle bundle,
			String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
		processValue();
	}
    
    // possibly bring this behaviour down to ManifestHeader and give a subclass the ability to
    // pass an ArrayList of valid element values to get looked at
    private void processValue() {
    	if (fManifestElements == null)
    		return;
        if (fManifestElements.size() > 0) {
            for (int i = 0; i < TOTAL_JRES; i++) {
            	for (int j = 0; j < fManifestElements.size(); j++) {
            		String value = fManifestElements.get(j).getValue();
            		if (value.equals(JRES.get(i))) {
            			fMinJRE = fManifestElements.get(j);
            			break;
            		}
            	}
            }
           	for (int i = 0; i < TOTAL_J2MES; i++) {
            	for (int j = 0; j < fManifestElements.size(); j++) {
            		String value = fManifestElements.get(j).getValue();
            		if (value.equals(J2MES.get(i))) {
            			fMinJ2ME = fManifestElements.get(j);
            			break;
            		}
            	}
            }
        }
    }
    
    public String getMinimumJRE() {
        return (fMinJRE != null) ? fMinJRE.getValue() : "";
    }
    
    public String getMinimumJ2ME() {
        return (fMinJ2ME != null) ? fMinJ2ME.getValue() : "";
    }
    
    public void updateJRE(String newValue) {
    	update(fMinJRE, newValue);
    }
    
    public void updateJ2ME(String newValue) {
    	update(fMinJ2ME, newValue);
    }
    
    private void update(ManifestElement element, String newValue) {
    	if (element != null && newValue.equals(element.getValue()))
    		return;
    	String old = null;
    	if (element != null) {
    		if (newValue == null || newValue.equals("")) {
    			element.setValue(null);
    			return;
    		}
    		old = getValue();
    	} else
	    	element = new ManifestElement(this);
    	element.setValue(newValue); //$NON-NLS-1$
    	firePropertyChanged(this, fName, old, getValue());
    }
}
