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
package org.eclipse.pde.internal.ui.launcher;

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

public class EquinoxPluginBlock extends AbstractPluginBlock {
	
	class EquinoxLabelProvider extends PDELabelProvider {
		
		public Image getColumnImage(Object obj, int index) {
			return index == 0 ? super.getColumnImage(obj, index) : null;
		}
		public String getColumnText(Object obj, int index) {
			switch (index) {
			case 0:
				return super.getColumnText(obj, index);
			case 1:
				return getStartLevel(obj);
			default:
				return getAutoStart(obj);
			}
		}
	}

	private Map fWorkspaceMap;
	private Map fTargetMap;
	
	private String getStartLevel(Object obj) {
		if (fTargetMap == null || fWorkspaceMap == null)
			return "";
		String value = null;
		if (obj instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase)obj;
			String id = model.getPluginBase().getId();
			if ("org.eclipse.osgi".equals(id))
				return "";
			
			if (model.getUnderlyingResource() != null) {
				if (fWorkspaceMap.containsKey(id)) {
					value = (String)fWorkspaceMap.get(id);
				}
			} else if (fTargetMap.containsKey(id)) {
				value = (String)fTargetMap.get(id);
			}
		}
		return value == null ? "" : value.substring(0, value.indexOf(':'));
	}

	private String getAutoStart(Object obj) {
		if (fTargetMap == null || fWorkspaceMap == null)
			return "";
		String value = null;
		if (obj instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase)obj;
			String id = model.getPluginBase().getId();
			if ("org.eclipse.osgi".equals(id))
				return "";
			
			if (model.getUnderlyingResource() != null) {
				if (fWorkspaceMap.containsKey(id)) {
					value = (String)fWorkspaceMap.get(id);
				}
			} else if (fTargetMap.containsKey(id)) {
				value = (String)fTargetMap.get(id);
			}
		}
		return value == null ? "" : value.substring(value.indexOf(':') + 1);
	}

	public EquinoxPluginBlock(AbstractLauncherTab tab) {
		super(tab);
	}
	
	protected void createPluginViewer(Composite composite) {
		super.createPluginViewer(composite);
    	Tree tree = fPluginTreeViewer.getTree();
 
    	TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
    	column1.setText("Plug-ins"); 
    	column1.setWidth(300);

    	TreeColumn column2 = new TreeColumn(tree, SWT.CENTER);
    	column2.setText("Auto-Start"); 
    	column2.setWidth(80);
         
        TreeColumn column3 = new TreeColumn(tree, SWT.CENTER);
        column3.setText("Start Level");
        column3.setWidth(80);      
        tree.setHeaderVisible(true);

		createEditors();
	}
	
	private void createEditors() {
		final Tree tree = fPluginTreeViewer.getTree();
		final TreeEditor editor1 = new TreeEditor(tree);
		editor1.horizontalAlignment = SWT.CENTER;
		editor1.grabHorizontal = true;
		editor1.minimumWidth = 60;

		final TreeEditor editor2 = new TreeEditor(tree);
		editor2.horizontalAlignment = SWT.CENTER;
		editor2.minimumWidth = 60;

		tree.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Clean up any previous editor control
				Control oldEditor = editor1.getEditor();
				if (oldEditor != null)
					oldEditor.dispose();

				oldEditor = editor2.getEditor();
				if (oldEditor != null)
					oldEditor.dispose();

				// Identify the selected row
				TreeItem item = (TreeItem) e.item;
				if (!isEditable(item))
					return;

				CCombo combo = new CCombo(tree, SWT.BORDER | SWT.READ_ONLY);
				combo.setItems(new String[] { "default", Boolean.toString(true), Boolean.toString(false) });
				combo.select(0);
				combo.pack();
				editor1.setEditor(combo, item, 1);

				Spinner spinner = new Spinner(tree, SWT.BORDER);
				spinner.setMinimum(1);
				editor2.setEditor(spinner, item, 2);
			}
		});			
	}
	
	private boolean isEditable(TreeItem item) {
		Object obj = item.getData();
		if (obj instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase)obj;
			if (!"org.eclipse.osgi".equals(model.getPluginBase().getId()))
				return fPluginTreeViewer.getChecked(model);
		}
		return false;
	}
	
	protected ILabelProvider getLabelProvider() {
		return new EquinoxLabelProvider();
	}
	
	protected void savePluginState(ILaunchConfigurationWorkingCopy config) {		
		config.setAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, 
							saveMap(fWorkspaceMap));
		
		config.setAttribute(IPDELauncherConstants.TARGET_BUNDLES, 
							saveMap(fTargetMap));
		
		StringBuffer buffer = new StringBuffer();
		if (fAddWorkspaceButton.getSelection()) {
			for (int i = 0; i < fWorkspaceModels.length; i++) {
				if (!fPluginTreeViewer.getChecked(fWorkspaceModels[i])) {
					if (buffer.length() > 0)
						buffer.append(",");
					buffer.append(fWorkspaceModels[i].getPluginBase().getId());
				}
			}
		} 		
		config.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, 
							buffer.length() > 0 ? buffer.toString() : (String)null);
	}
	
	private String saveMap(Map map) {
		StringBuffer buffer = new StringBuffer();
		if (map.size() > 0) {
			Iterator iter = map.keySet().iterator();
			while (iter.hasNext()) {
				if (buffer.length() > 0)
					buffer.append(",");
				String key = iter.next().toString();
				buffer.append(key);
				buffer.append("@");
				buffer.append(map.get(key));
			}		
		}
		return buffer.length() > 0 ? buffer.toString() : null;
	}
	
	public static Map retrieveMap(ILaunchConfiguration configuration, String attribute) {
		Map map = new TreeMap();
		try {
			String value = configuration.getAttribute(attribute, "");
			StringTokenizer tok = new StringTokenizer(value, ",");
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				int index = token.indexOf('@');
				map.put(token.substring(0, index), token.substring(index + 1));
			}
		} catch (CoreException e) {
		}	
		return map;
	}

	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		super.initializeFrom(configuration);
		initWorkspacePluginsState(configuration);
		initExternalPluginsState(configuration);
		updateCounter();
	}
		
	private void initExternalPluginsState(ILaunchConfiguration configuration)
			throws CoreException {
		fNumExternalChecked = 0;
		fPluginTreeViewer.setSubtreeChecked(fExternalPlugins, false);
		
		fTargetMap = retrieveMap(configuration, IPDELauncherConstants.TARGET_BUNDLES);
		Iterator iter = fTargetMap.keySet().iterator();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		while (iter.hasNext()) {
			String key = iter.next().toString();
			IPluginModelBase model = manager.findModel(key);
			if (model != null && model.getUnderlyingResource() == null) {
				fPluginTreeViewer.setChecked(model, true);
				fNumExternalChecked += 1;
				fPluginTreeViewer.refresh(model);
			} else {
				fTargetMap.remove(key);
			}
		}
		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(fExternalPlugins, fNumExternalChecked > 0
				&& fNumExternalChecked < fExternalModels.length);
	}

	private void initWorkspacePluginsState(ILaunchConfiguration configuration)
			throws CoreException {
		fWorkspaceMap = retrieveMap(configuration, IPDELauncherConstants.WORKSPACE_BUNDLES);
		fNumWorkspaceChecked = 0;
		if (configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true)) {
			TreeSet deselectedPlugins = LaunchPluginValidator.parsePlugins(configuration, IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS);
			for (int i = 0; i < fWorkspaceModels.length; i++) {
				String id = fWorkspaceModels[i].getPluginBase().getId();
				if (!fWorkspaceMap.containsKey(id) && deselectedPlugins.contains(id)) {
					fWorkspaceMap.put(id, "default:default");
				}
			}
		}

		Iterator iter = fWorkspaceMap.keySet().iterator();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		while (iter.hasNext()) {
			String key = iter.next().toString();
			IPluginModelBase model = manager.findModel(key);
			if (model != null && model.getUnderlyingResource() != null) {
				fPluginTreeViewer.setChecked(model, true);
				fNumWorkspaceChecked += 1;
				fPluginTreeViewer.refresh(model);
			} else {
				fWorkspaceMap.remove(key);
			}
		}
		fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
		fPluginTreeViewer.setGrayed(
			fWorkspacePlugins,
			fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
	}
	
	protected void handleGroupStateChanged(Object group, boolean checked) {
		if (!checked) {
			if (group == fWorkspacePlugins) {
				fWorkspaceMap.clear();
			} else if (group == fExternalPlugins) {
				fTargetMap.clear();
			}
		} else {
			if (group == fWorkspacePlugins) {
				for (int i = 0; i < fWorkspaceModels.length; i++) {
					String id = fWorkspaceModels[i].getPluginBase().getId();
					fWorkspaceMap.put(id, "default:default");
				}
			} else if (group == fExternalPlugins) {
				for (int i = 0; i < fExternalModels.length; i++) {
					String id = fExternalModels[i].getPluginBase().getId();
					fTargetMap.put(id, "default:default");
				}
			}
		}
		fPluginTreeViewer.refresh(group);
		super.handleGroupStateChanged(group, checked);
	}
	
	protected void handleCheckStateChanged(IPluginModelBase model, boolean checked) {
		super.handleCheckStateChanged(model, checked);
		adjustState(model, checked);
		fPluginTreeViewer.refresh(model);
	}
	
	protected void setChecked(IPluginModelBase model, boolean checked) {
		adjustState(model, checked);
		fPluginTreeViewer.refresh(model);
		super.setChecked(model, checked);
	}
	
	private void adjustState(IPluginModelBase model, boolean checked) {
		String id = model.getPluginBase().getId();
		if (checked) {
			if (model.getUnderlyingResource() != null)
				fWorkspaceMap.put(id, "default:default");
			else
				fTargetMap.put(id, "default:default");
		} else {
			if (model.getUnderlyingResource() != null)
				fWorkspaceMap.remove(id);
			else
				fTargetMap.remove(id);			
		}		
	}
	
	protected void setCheckedElements(Object[] checked) {
		super.setCheckedElements(checked);
		for (int i = 0; i < fWorkspaceModels.length; i++) {
			String id = fWorkspaceModels[i].getPluginBase().getId();
			if (fPluginTreeViewer.getChecked(fWorkspaceModels[i])) {
				if (!fWorkspaceMap.containsKey(id)) {
					fWorkspaceMap.put(id, "default:default");
					fPluginTreeViewer.refresh(fWorkspaceModels[i]);
				}			
			} else if (fWorkspaceMap.containsKey(id)){
				fWorkspaceMap.remove(id);
				fPluginTreeViewer.refresh(fWorkspaceModels[i]);
			}
		}
		for (int i = 0; i < fExternalModels.length; i++) {
			String id = fExternalModels[i].getPluginBase().getId();
			if (fPluginTreeViewer.getChecked(fExternalModels[i])) {
				if (!fTargetMap.containsKey(id)) {
					fTargetMap.put(id, "default:default");
					fPluginTreeViewer.refresh(fExternalModels[i]);
				} 
			} else if (fTargetMap.containsKey(id)) {
				fTargetMap.remove(id);
				fPluginTreeViewer.refresh(fExternalModels[i]);
			}
		}
	}
	
	protected void handleRestoreDefaults() {
		Object[] selected = fPluginTreeViewer.getCheckedElements();
		for (int i = 0; i < selected.length; i++) {
			if (selected[i] instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase)selected[i];
				String id = model.getPluginBase().getId();
				if (model.getUnderlyingResource() == null) {
					String value = (String)fTargetMap.get(id);
					if (!"default:default".equals(value)) {
						fTargetMap.put(id, "default:default");
					}
				} else {
					String value = (String)fWorkspaceMap.get(id);
					if (!"default:default".equals(value)) {
						fWorkspaceMap.put(id, "default:default");
					}					
				}
				fPluginTreeViewer.refresh(model);
			}
		}
	}

}
