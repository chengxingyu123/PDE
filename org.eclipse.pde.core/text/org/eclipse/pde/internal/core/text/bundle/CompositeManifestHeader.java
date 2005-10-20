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

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.osgi.framework.BundleException;

public class CompositeManifestHeader extends ManifestHeader {
	
	private static final PDEManifestElement[] NO_ELEMENTS = new PDEManifestElement[0];

	private static final long serialVersionUID = 1L;
	
	private boolean fSort;

	protected ArrayList fManifestElements;
	
	protected Map fElementMap;
	
	public CompositeManifestHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		this(name, value, bundle, lineDelimiter, false);
	}
	
	public CompositeManifestHeader(String name, String value, IBundle bundle, String lineDelimiter, boolean sort) {
        fName = name;
        fBundle = bundle;
        fLineDelimiter = lineDelimiter;
        setModel(fBundle.getModel());
		fSort = sort;
        processValue(value);
	}
	
	protected void processValue(String value) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(fName, value);
			for (int i = 0; i < elements.length; i++)
				addManifestElement(createElement(elements[i]));
		} catch (BundleException e) {
		}
	}
	
	protected PDEManifestElement createElement(ManifestElement element) {
		return new PDEManifestElement(this, element);
	}
	
	public void updateValue() {
		StringBuffer sb = new StringBuffer();
		PDEManifestElement[] elements = getElements();
		for (int i = 0; i < elements.length; i++) {	
			if (sb.length() > 0) {
				sb.append(","); //$NON-NLS-1$
				sb.append(fLineDelimiter);
				sb.append(" ");
			}
			sb.append(elements[i].write());
		}
		String old = fValue;
		fValue = sb.toString();
		fBundle.getModel().fireModelObjectChanged(this, getName(), old, fValue);
	}
	
	protected void addManifestElement(String value) {
		PDEManifestElement element = new PDEManifestElement(this);
		element.setValue(value);
		addManifestElement(element);
	}
	
	protected void addManifestElement(PDEManifestElement element) {
		if (fSort) {
			if (fElementMap == null)
				fElementMap = new TreeMap();
			fElementMap.put(element.getValue(), element);
		} else {
			if (fManifestElements == null)
				fManifestElements = new ArrayList(1);
			fManifestElements.add(element);
		}
		updateValue();
	}
	
	protected Object removeManifestElement(PDEManifestElement element) {
		return removeManifestElement(element.getValue());
	}
	
	protected Object removeManifestElement(String name) {
		Object object = null;
		if (fSort) {
			if (fElementMap != null) {
				object = fElementMap.remove(name);
			}
		} else if (fManifestElements != null){
			for (int i = 0; i < fManifestElements.size(); i++) {
				PDEManifestElement element = (PDEManifestElement)fManifestElements.get(i);
				if (name.equals(element.getValue()))
					object = fManifestElements.remove(i);
			}
		}
		updateValue();
		return object;
	}
	
	protected PDEManifestElement[] getElements() {
		if (fSort && fElementMap != null)
			return (PDEManifestElement[])fElementMap.values().toArray(new PDEManifestElement[fElementMap.size()]);
				
		if (fManifestElements != null)
			return (PDEManifestElement[])fManifestElements.toArray(new PDEManifestElement[fManifestElements.size()]);
		
		return NO_ELEMENTS; 
	}
	
   public boolean isEmpty() {
	   if (fSort)
		   return fElementMap == null || fElementMap.size() == 0;
	   return fManifestElements == null || fManifestElements.size() == 0;
   }
   
   public boolean hasElement(String name) {
	   if (fSort && fElementMap != null)
		   return fElementMap.containsKey(name);
	   
	   if (fManifestElements != null) {
			for (int i = 0; i < fManifestElements.size(); i++) {
				PDEManifestElement element = (PDEManifestElement)fManifestElements.get(i);
				if  (name.equals(element.getValue()))
					return true;
			}	   
	   }
	   return false;
   }
   
   public Vector getElementNames() {
	   PDEManifestElement[] elements = getElements();
       Vector vector = new Vector(elements.length);
       for (int i = 0; i < elements.length; i++) {
           vector.add(elements[i].getValue());
       }
       return vector;
   }
   
   public void swap(String name1, String name2) {
	   if (fSort || fManifestElements == null)
		   return;
	   int index1 = -1;
	   int index2 = -1;
	   for (int i = 0; i < fManifestElements.size(); i++) {
		   PDEManifestElement element = (PDEManifestElement)fManifestElements.get(i);
		   if (name1.equals(element.getValue()))
			   index1 = i;
		   else if (name2.equals(element.getValue()))
			   index2 = i;
		   if (index1 > -1 && index2 > -1)
			   break;
	   }
	   if (index1 > -1 && index2 > -1) {
		   Object object1 = fManifestElements.get(index1);
		   Object object2 = fManifestElements.get(index2);
		   fManifestElements.set(index1, object2);
		   fManifestElements.set(index2, object1);
	   }
	   updateValue();
   }



}
