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

import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.core.plugin.IPluginModelBase;
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
				return getAutoStart(obj);
			default:
				return getStartLevel(obj);
			}
		}
	}

	private Map fWorkspaceMap;
	private Map fTargetMap;
	
	private String getAutoStart(Object obj) {
		if (obj instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase)obj;
			if (!"org.eclipse.osgi".equals(model.getPluginBase().getId())) 
				return fPluginTreeViewer.getChecked(model) ? "default" : "";
		}
		return "";
	}

	private String getStartLevel(Object obj) {
		if (obj instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase)obj;
			if (!"org.eclipse.osgi".equals(model.getPluginBase().getId())) 
				return fPluginTreeViewer.getChecked(model) ? "default" : "";
		}
		return "";
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
	
	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		super.initializeFrom(config);
		fWorkspaceMap = config.getAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, new Properties());
		fTargetMap = config.getAttribute(IPDELauncherConstants.TARGET_BUNDLES, new Properties());
		updateCounter();
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
		if (fWorkspaceMap != null && fWorkspaceMap.size() > 0) {
			config.setAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, fWorkspaceMap);
		} else {
			config.setAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, (Map)null);
		}
		
		if (fTargetMap != null && fTargetMap.size() > 0) {
			config.setAttribute(IPDELauncherConstants.TARGET_BUNDLES, fTargetMap);
		} else {
			config.setAttribute(IPDELauncherConstants.TARGET_BUNDLES, (Map)null);
		}
	}

	protected void initExternalPluginsState(ILaunchConfiguration config)
			throws CoreException {
	}

	protected void initWorkspacePluginsState(ILaunchConfiguration config)
			throws CoreException {
	}
	
	protected void handleCheckStateChanged(IPluginModelBase model, boolean checked) {
		super.handleCheckStateChanged(model, checked);
	}
	
	protected void handleGroupStateChanged(Object group, boolean checked) {
		super.handleGroupStateChanged(group, checked);
	}
	
	protected void setChecked(IPluginModelBase model, boolean checked) {
		super.setChecked(model, checked);
	}
	
	protected void setCheckedElements(Object[] checked) {
		super.setCheckedElements(checked);
	}
	
	protected void handleRestoreDefaults() {
		super.handleRestoreDefaults();
	}

}
