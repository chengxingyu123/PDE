package org.eclipse.pde.internal.ui.preferences;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class TargetImplicitPluginsTab {
	
	private TableViewer fElementViewer;
	private String ROOT;
	protected Set fElements;
	
	private Button fAddButton;
	private Button fRemoveButton;
	
	public TargetImplicitPluginsTab() {
		ROOT = "Wassim";
	}
	
	class SourceProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			if (inputElement.equals(ROOT))
				return fElements.toArray();
			return null;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}		
	}
	
	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createLabel(container);
		createTable(container);
		createButtons(container);
		
		return container;
	}
	
	private void createLabel(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText("The plug-ins selected will be included as required bundles during dependency checking:");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
	}
	
	private void createTable(Composite container) {
		fElementViewer = new TableViewer(container, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		fElementViewer.getControl().setLayoutData(gd);
		fElementViewer.setContentProvider(new ArrayContentProvider());
		fElementViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fElementViewer.setInput(ROOT);
		fElementViewer.setSorter(new ViewerSorter());
		loadTable();
	}
	
	protected void loadTable() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String value = preferences.getString(ICoreConstants.IMPLICIT_DEPENDENCIES);
		StringTokenizer tokens = new StringTokenizer(value,",");
		fElements = new HashSet((4/3) * tokens.countTokens() + 1);
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		while (tokens.hasMoreElements()) {
			IPluginModelBase base = manager.findModel(tokens.nextToken());
			if (base != null) {
				fElements.add(base);
				fElementViewer.add(base);
			}
		}
	}
	
	private void createButtons(Composite container) {
		Composite buttonContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonContainer.setLayout(layout);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		fAddButton = new Button(buttonContainer, SWT.PUSH);
		fAddButton.setText(PDEUIMessages.SourceBlock_add); 
		fAddButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		
		fRemoveButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.SourceBlock_remove); 
		fRemoveButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		if (fElements.size() == 0)
			fRemoveButton.setEnabled(false);
	}
	
	private void handleAdd() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(), 
				PDEPlugin.getDefault().getLabelProvider());
		
		dialog.setElements(getValidBundles());
		dialog.setTitle(PDEUIMessages.PluginSelectionDialog_title); 
		dialog.setMessage(PDEUIMessages.PluginSelectionDialog_message);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {
			Object[] bundles = dialog.getResult();
			//TODO this is incorrect.
			// the list of available plugins should be those on the Plugins tab, whatever they may be
			// they are not necessarily the plugins in the PluginModelManager
			PluginModelManager manager = PDECore.getDefault().getModelManager();
			for (int i = 0; i < bundles.length; i++) {
				IPluginModelBase base = manager.findModel((BundleDescription)bundles[i]);
				if (base != null) {
					fElementViewer.add(base);
					fElements.add(base);
				}
			}
			if (fElements.size() > 0)
				fRemoveButton.setEnabled(true);
		}
	}
	
	protected Object[] getValidBundles() {
		Set currentPlugins = new HashSet((4/3) * fElements.size() + 1);
		Iterator it = fElements.iterator();
		while (it.hasNext()) {
			IPluginModelBase base = (IPluginModelBase)it.next();
			currentPlugins.add(getSymbolicName(base));
		}
		
		BundleDescription[] bundles = TargetPlatform.getState().getBundles();
		Set result = new HashSet((4/3) * bundles.length + 1);
		for (int i = 0; i < bundles.length; i++) {
			if (!currentPlugins.contains(bundles[i].getSymbolicName()))
				result.add(bundles[i]);
		}
		return result.toArray();
	}
	
	private void handleRemove() {
		//TODO remove button should only be selected if there is a selection in the table
		// TODO since this is a flat table, a "Remove All" button wouldn't hurt
		IStructuredSelection ssel = (IStructuredSelection)fElementViewer.getSelection();
		Iterator it = ssel.iterator();
		while (it.hasNext()) {
			Object item = it.next();
			fElements.remove(item);
			fElementViewer.remove(item);
		}
		if (fElements.size() == 0) 
			fRemoveButton.setEnabled(false);
	}

	public void performDefauls() {
		fElementViewer.remove(fElements.toArray());
		fElements.clear();
		fRemoveButton.setEnabled(false);
	}

	public void performOk() {
		StringBuffer buffer = new StringBuffer();
		Iterator it = fElements.iterator();
		while (it.hasNext()) {
			if (buffer.length() > 0)
				buffer.append(",");
			IPluginModelBase base = (IPluginModelBase) it.next();
			buffer.append(getSymbolicName(base));
		}
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		preferences.setValue(ICoreConstants.IMPLICIT_DEPENDENCIES, buffer.toString());
	}
	
	private String getSymbolicName(IPluginModelBase base) {
		BundleDescription desc = base.getBundleDescription();
		if (desc != null) 
			return desc.getSymbolicName();
		return base.getPluginBase().getId();
	}

}
