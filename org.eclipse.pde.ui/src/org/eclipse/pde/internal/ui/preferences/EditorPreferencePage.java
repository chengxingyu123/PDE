/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IPDEColorConstants;
import org.eclipse.pde.internal.ui.editor.text.XMLSourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class EditorPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage, IPreferenceConstants {
	
	private class StoreLinkedDisplayItem {
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
			fColor = new Color(getShell().getDisplay(), getColorValue());
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
	
	private class ColorListLabelProvider extends LabelProvider implements IColorProvider {
		public Color getForeground(Object element) {
			return ((StoreLinkedDisplayItem)element).getItemColor();
		}
		public Color getBackground(Object element) {
			return null;
		}
	}
	
	private class ColorListContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ArrayList) {
				return ((ArrayList)inputElement).toArray();
			}
			return null;
		}
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private ColorManager fColorManager;
	private IPreferenceStore fStore;

	private ArrayList fXMLColorData;
	private ArrayList fMFColorData;
	private TableViewer fXMLViewer;
	private TableViewer fMFViewer;
	private String fXMLSample = "XMLSyntaxPreviewCode.txt";
	private String fMFSample = "ManifestSyntaxPreviewCode.txt";
	private String[][] fXMLColorStrings = new String[][] {
	/*		{Display name, IPreferenceStore key}		*/
			{PDEUIMessages.EditorPreferencePage_text, IPDEColorConstants.P_DEFAULT},
			{PDEUIMessages.EditorPreferencePage_proc, IPDEColorConstants.P_PROC_INSTR},
			{PDEUIMessages.EditorPreferencePage_tag, IPDEColorConstants.P_TAG},
			{PDEUIMessages.EditorPreferencePage_string, IPDEColorConstants.P_STRING},
			{PDEUIMessages.EditorPreferencePage_comment, IPDEColorConstants.P_XML_COMMENT}};
	private String[][] fMFColorStrings = new String[][] {
			{"Header Name", IPDEColorConstants.P_HEADER_NAME},
			{"Assignment", IPDEColorConstants.P_HEADER_ASSIGNMENT},
			{"Header Value", IPDEColorConstants.P_HEADER_VALUE}};
	
	public EditorPreferencePage() {
		setDescription(PDEUIMessages.EditorPreferencePage_colorSettings); 
		fStore = new PreferenceStore();
		fColorManager = ColorManager.getDefault();
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		fXMLColorData = loadColorData(store, false, fXMLColorStrings);
		fMFColorData = loadColorData(store, false, fMFColorStrings);
		setPreferenceStore(fStore);
	}

	private ArrayList loadColorData(IPreferenceStore store, boolean def, String[][] colors) {
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
	
	public boolean performOk() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		setPreferences(fXMLColorData, store);
		setPreferences(fMFColorData, store);
		PDEPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
	
	private void setPreferences(ArrayList colors, IPreferenceStore store) {
		for (int i = 0; i < colors.size(); i++) {
			StoreLinkedDisplayItem item = (StoreLinkedDisplayItem)colors.get(i);
			PreferenceConverter.setValue(store, item.getColorKey(), item.getColorValue());
			fColorManager.updateProperty(item.getColorKey());
		}
	}
	
	protected void performDefaults() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		fXMLColorData = loadColorData(store, true, fXMLColorStrings);
		fXMLViewer.refresh();
		fXMLViewer.setSelection(fXMLViewer.getSelection()); // refresh the color selector's color
		fMFColorData = loadColorData(store, true, fMFColorStrings);
		fMFViewer.refresh();
		fMFViewer.setSelection(fMFViewer.getSelection()); // refresh the color selector's color
		super.performDefaults();
	}
	
	public void init(IWorkbench workbench) {
	}
	
	protected Control createContents(Composite parent) {
		final Link link = new Link(parent, SWT.NONE);
		final String target = "org.eclipse.ui.preferencePages.GeneralTextEditor"; //$NON-NLS-1$
		link.setText("See <A>'Text Editors'</A> for the general text editor preferences.");
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(link.getShell(), target, null, null);
			}
		});
		
		final boolean XML = true;
		
		TabFolder folder = new TabFolder(parent, SWT.NONE);
		folder.setLayout(new GridLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText("&XML Highlighting");
		item.setControl(createSyntaxPage(folder, XML));
		
		item = new TabItem(folder, SWT.NONE);
		item.setText("&Manifest Highlighting");
		item.setControl(createSyntaxPage(folder, !XML));
			
		Dialog.applyDialogFont(getControl());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.EDITOR_PREFERENCE_PAGE);
		
		return parent;
	}

	public Control createSyntaxPage(Composite parent, final boolean isXML) {
		
		Composite colorComposite = new Composite(parent, SWT.NONE);
		colorComposite.setLayout(new GridLayout(2, false));
		colorComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(colorComposite, SWT.LEFT);
		label.setText("Elements:");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(colorComposite, SWT.LEFT);
		label.setText("Properties:");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
		final TableViewer viewer = new TableViewer(colorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		viewer.setLabelProvider(new ColorListLabelProvider());
		viewer.setContentProvider(new ColorListContentProvider());
		viewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.getControl().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));

		Composite propertiesComp = new Composite(colorComposite, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		propertiesComp.setLayout(layout);
		propertiesComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		label = new Label(propertiesComp, SWT.LEFT);
		label.setText("&Color:");
		
		final ColorSelector colorSelector = new ColorSelector(propertiesComp);
		Button colorButton = colorSelector.getButton();
		colorButton.setLayoutData(new GridData(GridData.BEGINNING));
		
		Composite previewComp = new Composite(colorComposite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		previewComp.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		previewComp.setLayoutData(gd);
		
		label = new Label(previewComp, SWT.NONE);
		label.setText("Preview:");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	
		Control control = createPreviewer(previewComp, isXML);
		control.setLayoutData(new GridData(GridData.FILL_BOTH));

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
		viewer.setInput(isXML ? fXMLColorData : fMFColorData);
		viewer.setSelection(new StructuredSelection(viewer.getElementAt(0)));
		
		if (isXML)
			fXMLViewer = viewer;
		else
			fMFViewer = viewer;
		
		return colorComposite;
	}
	
	
	private Control createPreviewer(Composite parent, boolean isXML) {
		final SourceViewer previewViewer = new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL);
		
		final ChangeAwareSourceViewerConfiguration config;
		if (isXML)
			config = new XMLSourceViewerConfiguration(null, fColorManager);
		else
			config = null;
		
		IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (config.affectsTextPresentation(event)) {
					config.adaptToPreferenceChange(event);
					previewViewer.invalidateTextPresentation();
				}
			}
		};
		if (config != null) {
			previewViewer.configure(config);
			fStore.addPropertyChangeListener(propertyChangeListener);
		}
		previewViewer.setEditable(false);	
		previewViewer.getTextWidget().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		
		String content = loadPreviewContentFromFile(isXML ? fXMLSample : fMFSample);
		IDocument document = new Document(content);
		
		if (isXML)
			new XMLDocumentSetupParticpant().setup(document);
//		else
//			new ManifestDocumentSetupParticipant().setup(document);
		
		previewViewer.setDocument(document);
		
		return previewViewer.getControl();
	}
	
	public void dispose() {
		super.dispose();
		if (fColorManager != null)
			fColorManager.dispose();
		for (int i = 0; i < fXMLColorData.size(); i++) {
			((StoreLinkedDisplayItem)fXMLColorData.get(i)).disposeColor();
		}
		for (int i = 0; i < fMFColorData.size(); i++)
			((StoreLinkedDisplayItem)fMFColorData.get(i)).disposeColor();
	}
	
	private StoreLinkedDisplayItem getStoreLinkedItem(TableViewer viewer) {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		return (StoreLinkedDisplayItem) selection.getFirstElement();
	}
	
	protected String loadPreviewContentFromFile(String filename) {
		String line;
		String separator = System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer(512);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)));
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
				buffer.append(separator);
			}
		} catch (IOException io) { PDEPlugin.log(io); }
		finally { if (reader != null) { try { reader.close(); } catch (IOException e) {}}}
		
		return buffer.toString();
	}
}
