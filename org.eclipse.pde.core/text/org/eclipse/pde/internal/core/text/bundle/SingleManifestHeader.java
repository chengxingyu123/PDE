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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.BundleException;

public class SingleManifestHeader extends ManifestHeader {

	private static final long serialVersionUID = 1L;
	
	private String fMainComponent;
	
	private Map fDirectives;
	private Map fAttributes;

	public SingleManifestHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}
	
	protected void processValue(String value) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(getName(), value);
			if (elements.length > 0) {
				fMainComponent = elements[0].getValue();
			}
		} catch (BundleException e) {
		}
		fValue = value;
	}
	
    public void setAttribute(String key, String value) {
    	setTableValue(fAttributes, key, value);
    }
    
    public void setDirective(String key, String value) {
    	setTableValue(fDirectives, key, value);
    }
    
    private void setTableValue(Map table, String key, String value) {
       	if (table == null && value == null)
    		return;
    	
    	if (table == null)
    		table = new HashMap();
    	
    	if (value != null)
    		table.put(key, value);
    	else
    		table.remove(key);
    	
    	updateValue();	
    }
    
    public void setMainComponent(String value) {
    	fMainComponent = value;
    	updateValue();
    }
    
    public String getAttribute(String key) {
    	return (fAttributes != null) ? (String)fAttributes.get(key) : null;
    }
    
    public String getDirective(String key) {
    	return (fDirectives != null) ? (String)fDirectives.get(key) : null;
    }
    
    public String getMainComponent() {
    	return fMainComponent;
    }
    
    private void updateValue() {
    	StringBuffer buffer = new StringBuffer();
    	if (fMainComponent != null) {
    		buffer.append(fMainComponent);
    		
    		if (fDirectives != null) {
    			Iterator iter = fDirectives.keySet().iterator();
    			while (iter.hasNext()) {
    				String key = iter.next().toString();
    				buffer.append("; ");
    				buffer.append(key);
    				buffer.append(":=\"");
    				buffer.append(fDirectives.get(key));
    			}
    		}
    		
    		if (fAttributes != null) {
    			Iterator iter = fAttributes.keySet().iterator();
    			while (iter.hasNext()) {
    				String key = iter.next().toString();
    				buffer.append("; ");
    				buffer.append(key);
    				buffer.append("=");
     				buffer.append(fAttributes.get(key));
    				buffer.append("\"");
    			}    			
    		}  	
    	}
    	fValue = buffer.toString();
    }

}
