/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;

public class PDEFoldingStructureProvider {

	private XMLSourcePage fEditor;
	private IDocument fDocument;

	private Map fPositionToElement = new HashMap();

	public PDEFoldingStructureProvider(XMLSourcePage editor) {
		super();
		this.fEditor = editor;
	}

	public void setDocument(IDocument document) {
		this.fDocument = document;
	}

	public void updateFoldingRegions(IBaseModel model) {
		fPositionToElement = new HashMap();
		ProjectionAnnotationModel annotationModel = 
			(ProjectionAnnotationModel) fEditor.getAdapter(ProjectionAnnotationModel.class);
		if (annotationModel == null)
			return;

		if(model instanceof PluginModel) {
			updateFoldingRegions(annotationModel, (PluginModel) model);
		}
	}
	
	public void updateFoldingRegions(ProjectionAnnotationModel annotationModel, PluginModel model) {
		IExtensions extensions = model.getExtensions();
		IPluginExtension[] pluginExtensions = extensions.getExtensions();
		
		Set currentRegions = new HashSet();
		try {
			addFoldingRegions(currentRegions, pluginExtensions);
			updateFoldingRegions(annotationModel, currentRegions);
		} catch (BadLocationException e) {}
	}
	
	private void addFoldingRegions(Set regions, IPluginExtension[] nodes) throws BadLocationException  {
		for(int i = 0; i < nodes.length; i++) {
			IDocumentNode element = (IDocumentNode) nodes[i];
			int startLine= fDocument.getLineOfOffset(element.getOffset());
			int endLine= fDocument.getLineOfOffset(element.getOffset() + element.getLength());
			if (startLine < endLine) {
				int start= fDocument.getLineOffset(startLine);
				int end= fDocument.getLineOffset(endLine) + fDocument.getLineLength(endLine);
				Position position= new Position(start, end - start);
				regions.add(position);
				fPositionToElement.put(position, element);
			}
//			children = element.getChildNodes();
//			if (children != null) {
//			addFoldingRegions(regions, children);
//			}

		}
	}
	
	private void updateFoldingRegions(ProjectionAnnotationModel model, Set currentRegions) {
		Annotation[] deletions = computeDifferences(model, currentRegions);

		Map additionsMap = new HashMap();
		for (Iterator iter = currentRegions.iterator(); iter.hasNext();) {
			Object position= iter.next();
			additionsMap.put(new ProjectionAnnotation(false), position);
		}

		if ((deletions.length != 0 || additionsMap.size() != 0)) {
			model.modifyAnnotations(deletions, additionsMap, new Annotation[] {});
		}
	}

	private Annotation[] computeDifferences(ProjectionAnnotationModel model, Set additions) {
		List deletions = new ArrayList();
		for (Iterator iter = model.getAnnotationIterator(); iter.hasNext();) {
			Object annotation = iter.next();
			if (annotation instanceof ProjectionAnnotation) {
				Position position = model.getPosition((Annotation) annotation);
				if (additions.contains(position)) {
					additions.remove(position);
				} else {
					deletions.add(annotation);
				}
			}
		}
		return (Annotation[]) deletions.toArray(new Annotation[deletions.size()]);
	}

}
