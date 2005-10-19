/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.bundle;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.bundle.BundleObject;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.ui.model.IDocumentKey;
import org.osgi.framework.BundleException;

public class ManifestHeader extends BundleObject implements IDocumentKey {
    private static final long serialVersionUID = 1L;
    private int fOffset = -1;
	private int fLength = -1;
    
	protected String fName;
    private IBundle fBundle;
	private String fLineDelimiter;
	protected ManifestElementList fManifestElements;
    
    public ManifestHeader(String name, String value, IBundle bundle, String lineDelimiter) {
        fName = name;
        fBundle = bundle;
        fLineDelimiter = lineDelimiter;
        setModel(fBundle.getModel());
        setValue(value);
    }
    
    public String getLineLimiter() {
    	return fLineDelimiter;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setName(java.lang.String)
	 */
	public void setName(String name) {
		fName = name;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getName()
	 */
	public String getName() {
		return fName;
	}
	
	public String getValue() {
		if (fManifestElements == null)
			return null;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < fManifestElements.size(); i++) {	
			if (i != 0) {
				sb.append(","); //$NON-NLS-1$
				sb.append(fLineDelimiter);
				sb.append(" ");
			}
			sb.append(fManifestElements.get(i).write());
		}
		return sb.toString();
	}
	
	public void setValue(String value) {
        try {
        	org.eclipse.osgi.util.ManifestElement[] elements = 
        		org.eclipse.osgi.util.ManifestElement.parseHeader(fName, value);
        	fManifestElements = new ManifestElementList(elements.length);
			for (int i = 0; i < elements.length; i++) {
				new ManifestElement(this, elements[i]);
			}
		} catch (BundleException e) {
		}
	}
	
	protected void addManifestElement(ManifestElement element) {
		if (fManifestElements == null)
			fManifestElements = new ManifestElementList(1);
		fManifestElements.add(element);
	}
	
	protected void removeManifestElement(ManifestElement element) {
		if (!hasElements()) return;
		for (int i = 0; i < fManifestElements.size(); i++) {
			if (fManifestElements.get(i).equals(element))
				fManifestElements.remove(i);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setOffset(int)
	 */
	public void setOffset(int offset) {
		fOffset = offset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getOffset()
	 */
	public int getOffset() {
		return fOffset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setLength(int)
	 */
	public void setLength(int length) {
		fLength = length;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getLength()
	 */
	public int getLength() {
		return fLength;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#write()
	 */
	public String write() {
		StringBuffer sb = new StringBuffer(fName);
		sb.append(": "); //$NON-NLS-1$
		sb.append(getValue());
		sb.append(fLineDelimiter);
		return sb.toString(); 
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}
    
    public void setBundle(IBundle bundle) {
        fBundle = bundle;
    }
    
    public IBundle getBundle() {
        return fBundle;
    }
    
    public void setAttribute(String key, String value) {
    	if (hasElements())
    		getFirstElement().setAttribute(key, new String[] {value});
    }
    public void setDirective(String key, String value) {
    	if (hasElements())
    		getFirstElement().setDirective(key, new String[] {value});
    }
    public String getAttribute(String key) {
    	if (hasElements())
    		return getFirstElement().getAttribute(key);
    	return null;
    }
    public String getDirective(String key) {
    	if (hasElements())
    		return getFirstElement().getDirective(key);
    	return null;
    }
    public String[] getAttributes(String key) {
    	if (hasElements())
    		return getFirstElement().getAttributes(key);
    	return null;
    }
    public String[] getDirectives(String key) {
    	if (hasElements())
    		return getFirstElement().getDirectives(key);
    	return null;
    }
    
    private boolean hasElements() {
    	return fManifestElements != null && fManifestElements.size() > 0;
    }
    private ManifestElement getFirstElement() {
    	return fManifestElements.get(0);
    }
}

