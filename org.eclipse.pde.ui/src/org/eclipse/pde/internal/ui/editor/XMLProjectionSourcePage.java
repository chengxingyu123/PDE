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
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.swt.widgets.Composite;

public abstract class XMLProjectionSourcePage extends XMLSourcePage implements IProjectionListener, IModelChangedListener {

	private ProjectionSupport fProjectionSupport;
	private PDEFoldingStructureProvider fFoldingStructureProvider;
	

	public XMLProjectionSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);		
		if (isFoldingEnabled()) {
			fFoldingStructureProvider = new PDEFoldingStructureProvider(this);
		}
	}
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();
		createFoldingSupport(projectionViewer);
		
		((AbstractEditingModel) getInputContext().getModel()).addModelChangedListener(this);
		
        if (isFoldingEnabled()) {
        	projectionViewer.doOperation(ProjectionViewer.TOGGLE);
        }
	}

	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		ISourceViewer viewer = new ProjectionViewer(
				parent, 
				ruler,
				getOverviewRuler(), 
				isOverviewRulerVisible(), 
				styles);
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}
	
	public void dispose() {
		((ProjectionViewer) getSourceViewer()).removeProjectionListener(this);
		if (fProjectionSupport != null) {
			fProjectionSupport.dispose();
			fProjectionSupport= null;
		}
		super.dispose();
	}
	
	private void createFoldingSupport(ProjectionViewer projectionViewer) {
		fProjectionSupport= new ProjectionSupport(
				projectionViewer,
				getAnnotationAccess(),
				getSharedColors());

        fProjectionSupport.install();
		((ProjectionViewer)getSourceViewer()).addProjectionListener(this);
        
	}

	public void projectionEnabled() {
		fFoldingStructureProvider = new PDEFoldingStructureProvider(this);
		fFoldingStructureProvider.setDocument(getDocumentProvider().getDocument(getEditorInput()));
		fFoldingStructureProvider.updateFoldingRegions(getInputContext().getModel());
	}
	
	public void projectionDisabled() {
        fFoldingStructureProvider = null;
	}
	
	private boolean isFoldingEnabled() {
		IPreferenceStore store = getPreferenceStore();
		return store.getBoolean(IPreferenceConstants.EDITOR_FOLDING_ENABLED);
	}
	
	public Object getAdapter(Class key) { 
		if (fProjectionSupport != null) { 
			Object adapter= fProjectionSupport.getAdapter(getSourceViewer(), key); 
			if (adapter != null) {
				return adapter;
			}
		}
		return super.getAdapter(key);
	}

	public void modelChanged(IModelChangedEvent event) {
		if (fFoldingStructureProvider != null) {
			fFoldingStructureProvider.updateFoldingRegions(getInputContext().getModel());
		}
	}
	
}
