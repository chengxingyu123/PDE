/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text.bundle;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.bundle.BundleObject;
import org.osgi.framework.BundleException;

public class PDEManifestElement extends BundleObject {

	private static final long serialVersionUID = 1L;
	
	protected String[] fValueComponents;
	protected Hashtable fAttributes;
	protected Hashtable fDirectives;
	protected ManifestHeader fHeader;
	
	public PDEManifestElement(ManifestHeader header) {
		fHeader = header;
	}
	protected PDEManifestElement(ManifestHeader header,
			org.eclipse.osgi.util.ManifestElement manifestElement)  {
		this(header);
		init(manifestElement);
	}
	
	public String[] getValueComponents() {
		return fValueComponents;
	}

	protected void setValueComponents(String[] valueComponents) {
		fValueComponents = valueComponents;
	}
	
	public String[] getAttributes(String key) {
		return getTableValues(fAttributes, key);
	}
	
	public String getAttribute(String key) {
		return getTableValue(fAttributes, key);
	}
	
	public Enumeration getKeys() {
		return getTableKeys(fAttributes);
	}

	protected void addAttribute(String key, String value) {
		fAttributes = addTableValue(fAttributes, key, value);
	}
	
	protected void setAttribute(String key, String value) {
		setTableValue(fAttributes, key, value);
	}
	
	protected void setAttributes(Hashtable attributes) {
		fAttributes = attributes;
	}
	
	public String getDirective(String key) {
		return getTableValue(fDirectives, key);
	}
	
	public String[] getDirectives(String key) {
		return getTableValues(fDirectives, key);
	}

	public Enumeration getDirectiveKeys() {
		return getTableKeys(fDirectives);
	}

	protected void addDirective(String key, String value) {
		fDirectives = addTableValue(fDirectives, key, value);
	}
	
	protected void setDirective(String key, String value) {
		setTableValue(fDirectives, key, value);
	}
	
	protected void setDirectives(Hashtable directives) {
		fDirectives = directives;
	}

	private String getTableValue(Hashtable table, String key) {
		if (table == null)
			return null;
		Object result = table.get(key);
		if (result == null)
			return null;
		if (result instanceof String)
			return (String) result;

		ArrayList valueList = (ArrayList) result;
		//return the last value
		return (String) valueList.get(valueList.size() - 1);
	}
	
	private String[] getTableValues(Hashtable table, String key) {
		if (table == null)
			return null;
		Object result = table.get(key);
		if (result == null)
			return null;
		if (result instanceof String)
			return new String[] {(String) result};
		ArrayList valueList = (ArrayList) result;
		return (String[]) valueList.toArray(new String[valueList.size()]);
	}

	private Enumeration getTableKeys(Hashtable table) {
		if (table == null)
			return null;
		return table.keys();
	}

	private Hashtable addTableValue(Hashtable table, String key, String value) {
		if (table == null) {
			table = new Hashtable(7);
		}
		Object curValue = table.get(key);
		if (curValue != null) {
			ArrayList newList;
			// create a list to contain multiple values
			if (curValue instanceof ArrayList) {
				newList = (ArrayList) curValue;
			} else {
				newList = new ArrayList(5);
				newList.add(curValue);
			}
			newList.add(value);
			table.put(key, newList);
		} else {
			table.put(key, value);
		}
		return table;
	}
	
    private void setTableValue(Hashtable table, String key, String value) {
    	if (table == null) {
			table = new Hashtable(7);
		}
    	if (value == null || value.trim().length() == 0)
    		table.remove(key);
    	else {
    		String[] tokens = ManifestElement.getArrayFromList(value);
    		ArrayList values = new ArrayList(tokens.length);
    		for (int i = 0; i < tokens.length; i++)
    			values.add(tokens[i]);
    		table.put(key, values);
    	}
    	fHeader.updateValue();
    }
    
    public void setValue(String value) {
    	if (value == null)
    		fHeader.removeManifestElement(this);
    	try {
			org.eclipse.osgi.util.ManifestElement[] elements = 
				org.eclipse.osgi.util.ManifestElement.parseHeader(fHeader.fName, value);
			if (elements != null && elements.length > 0)
				init(elements[0]);
		} catch (BundleException e) {
		}
		fHeader.updateValue();
    }
    
    private void init(org.eclipse.osgi.util.ManifestElement manifestElement) {
		setValueComponents(manifestElement.getValueComponents());
		Hashtable attributes = new Hashtable();
		Enumeration attKeys = manifestElement.getKeys();
		while (attKeys != null && attKeys.hasMoreElements()) {
			String attKey = (String)attKeys.nextElement();
			attributes.put(attKey, manifestElement.getAttributes(attKey));
		}
		Hashtable directives = new Hashtable();
		Enumeration dirKeys = manifestElement.getDirectiveKeys();
		while (dirKeys != null && dirKeys.hasMoreElements()) {
			String dirKey = (String)dirKeys.nextElement();
			directives.put(dirKey, manifestElement.getDirective(dirKey));
		}
		setAttributes(attributes);
		setDirectives(directives);
    }
    
    public String write() {
    	StringBuffer sb = new StringBuffer(getValue());
    	appendValuesToBuffer(sb, fAttributes);
    	appendValuesToBuffer(sb, fDirectives);
    	return sb.toString();
    }
    
    public String getValue() {
    	StringBuffer sb = new StringBuffer();
    	if (fValueComponents == null)
    		return "";
    	for (int i = 0; i < fValueComponents.length; i++) {
    		if (i != 0) sb.append("; "); //$NON-NLS-1$
    		sb.append(fValueComponents[i]);
    	}
    	return sb.toString();
    }
    
    private void appendValuesToBuffer(StringBuffer sb, Hashtable table) {
    	if (table == null)
    		return;
    	Enumeration dkeys = table.keys();
    	while (dkeys.hasMoreElements()) {
    		String dkey = (String)dkeys.nextElement();
    		Object value = table.get(dkey);
    		if (value == null)
    			continue;
    		sb.append("; "); //$NON-NLS-1$
			sb.append(dkey);
			sb.append(table.equals(fDirectives) ? ":=" : "="); //$NON-NLS-1$ //$NON-NLS-2$
    		if (value instanceof String) {
    			sb.append(value);
    		} else if (value instanceof ArrayList) {
    			ArrayList values = (ArrayList)value;
	    		if (values.size() > 0) sb.append("\""); //$NON-NLS-1$
	    		for (int i = 0; i < values.size(); i++) {
	    			if (i != 0) sb.append(", "); //$NON-NLS-1$
	    			sb.append(values.get(i));
	    		}
	    		if (values.size() > 0) sb.append("\""); //$NON-NLS-1$
    		}
    	}
    }
}
