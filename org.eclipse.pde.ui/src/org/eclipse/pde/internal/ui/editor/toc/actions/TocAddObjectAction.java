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

package org.eclipse.pde.internal.ui.editor.toc.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.internal.core.toc.TocObject;

/**
 * TocAddObjectAction - the abstract implementation for
 * adding objects to a TOC object.
 */
public abstract class TocAddObjectAction extends Action {
	//The parent TOC object, which the new object will be
	//a child of.
	TocObject fParentObject;

	/**
	 * Set the parent object that this action will add
	 * objects to.
	 * 
	 * @param parent The new parent object for this action
	 */
	public void setParentObject(TocObject parent)
	{	fParentObject = parent;
	}
	
	/**
	 * @return The names of the children of this TOC object
	 */
	public String[] getChildNames()
	{	
		int numChildren = fParentObject.getChildren().size();
		TocObject[] tocObjects = 
			(TocObject[])fParentObject.getChildren().toArray(new TocObject[numChildren]);
		
		String[] tocObjectNames = new String[tocObjects.length];
		
		for(int i = 0; i < numChildren; ++i)
		{	tocObjectNames[i] = tocObjects[i].getName();
		}
		
		return tocObjectNames;
	}
}
