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
package org.eclipse.pde.internal.ui.preferences;

import java.util.ArrayList;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public abstract class SyntaxColorTab {

	protected ColorManager fColorManager;
	private IPreferenceStore fStore;

	class StoreLinkedDisplayItem {
		private String fDisplayName;
		private String fColorKey;
		private Color fColor;
		
		public StoreLinkedDisplayItem(String displayName, String colorKey) {
			fDisplayName = displayName;
			fColorKey = colorKey;
		}
		public String getColorKey() {
			return fColorKey;
		}
		public String getDisplayName() {
			return fDisplayName;
		}
		public Color getItemColor() {
			if (fColor != null)
				fColor.dispose();
			fColor = new Color(PDEPlugin.getActiveWorkbenchShell().getDisplay(), getColorValue());
			return fColor;
		}
		public RGB getColorValue() {
			return PreferenceConverter.getDefaultColor(fStore, fColorKey);
		}
		public void setColorValue(RGB rgb) {
			RGB oldrgb = getColorValue();
			PreferenceConverter.setDefault(fStore, fColorKey, rgb);
			fStore.firePropertyChangeEvent(fColorKey, oldrgb, rgb);
		}
		public void disposeColor() {
			if (fColor != null) {
				fColor.dispose();
				fColor = null;
			}
		}
		public String toString() { // called by the label provider
			return fDisplayName;
		}
	}
	
	class ColorListLabelProvider extends LabelProvider implements IColorProvider {
		public Color getForeground(Object element) {
			return ((StoreLinkedDisplayItem)element).getItemColor();
		}
		public Color getBackground(Object element) {
			return null;
		}
	}
	
	public SyntaxColorTab(ColorManager manager) {
		fColorManager = manager;
	}

	protected ArrayList loadColorData(IPreferenceStore store, boolean def, String[][] colors) {
		ArrayList list = new ArrayList(colors.length);
		for (int i = 0; i < colors.length; i++) {
			StoreLinkedDisplayItem item = new StoreLinkedDisplayItem(colors[i][0], colors[i][1]);
			RGB rgb = def ?
					PreferenceConverter.getDefaultColor(store, item.getColorKey()) : 
					PreferenceConverter.getColor(store, item.getColorKey());
			fStore.setDefault(item.getColorKey(), StringConverter.asString(rgb));
			list.add(item);
		}
		return list;
	}
	
	public Control createContents(Composite parent) {	
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		createElementTable(container);
		createPreviewer(container);
		return container;
	}
	
	private void createElementTable(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
			
		Label label = new Label(container, SWT.LEFT);
		label.setText("Elements:");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(container, SWT.LEFT);
		label.setText("Properties:");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
		final TableViewer viewer = new TableViewer(container, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		viewer.setLabelProvider(new ColorListLabelProvider());
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.getControl().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));

		Composite colorComposite = new Composite(container, SWT.NONE);
		colorComposite.setLayout(new GridLayout(2, false));
		colorComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		label = new Label(colorComposite, SWT.LEFT);
		label.setText("&Color:");
		
		final ColorSelector colorSelector = new ColorSelector(colorComposite);
		Button colorButton = colorSelector.getButton();
		colorButton.setLayoutData(new GridData(GridData.BEGINNING));

		colorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				StoreLinkedDisplayItem item = getStoreLinkedItem(viewer);
				item.setColorValue(colorSelector.getColorValue());
				viewer.update(item, null);
			}
		});
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				StoreLinkedDisplayItem item = getStoreLinkedItem(viewer);
				colorSelector.setColorValue(item.getColorValue());
			}
		});
		viewer.setInput(getViewerInput());
		viewer.setSelection(new StructuredSelection(viewer.getElementAt(0)));
	}	
	
	private void createPreviewer(Composite parent) {
		Composite previewComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		previewComp.setLayout(layout);
		previewComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = new Label(previewComp, SWT.NONE);
		label.setText("Preview:");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final SourceViewer previewViewer = new SourceViewer(previewComp, null, SWT.BORDER|SWT.V_SCROLL);	
		final ChangeAwareSourceViewerConfiguration config = getSourceViewerConfiguration();
		
		if (config != null) {
			previewViewer.configure(config);
			IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					if (config.affectsTextPresentation(event)) {
						config.adaptToPreferenceChange(event);
						previewViewer.invalidateTextPresentation();
					}
				}
			};
			fStore.addPropertyChangeListener(propertyChangeListener);
		}
		previewViewer.setEditable(false);	
		previewViewer.getTextWidget().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));	
		previewViewer.setDocument(getDocument());
		
		Control control = previewViewer.getControl();
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
	}
	
	protected abstract ChangeAwareSourceViewerConfiguration getSourceViewerConfiguration();
	
	public void performOk() {
		/*for (int i = 0; i < colors.size(); i++) {
			StoreLinkedDisplayItem item = (StoreLinkedDisplayItem)colors.get(i);
			PreferenceConverter.setValue(store, item.getColorKey(), item.getColorValue());
			fColorManager.updateProperty(item.getColorKey());
		}*/
	}
	
	public abstract void performDefaults();
	
	public abstract void dispose();
	
	protected abstract IDocument getDocument();
	
	protected abstract ArrayList getViewerInput();
	
	private StoreLinkedDisplayItem getStoreLinkedItem(TableViewer viewer) {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		return (StoreLinkedDisplayItem) selection.getFirstElement();
	}

}
