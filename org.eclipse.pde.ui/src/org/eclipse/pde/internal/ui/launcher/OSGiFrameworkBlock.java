/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.AbstractOSGiLaunchConfiguration;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.OSGiLaunchConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class OSGiFrameworkBlock {
	
	private Combo fDefaultAutoStart;
	private Spinner fDefaultStartLevel;
	private IConfigurationElement[] fConfigElements;
	private Combo fLauncherCombo;
	private Listener fListener;
	private AbstractLauncherTab fTab;
	
	class Listener extends SelectionAdapter implements ModifyListener{
		
		public void widgetSelected(SelectionEvent e) {
			fTab.updateLaunchConfigurationDialog();
		}

		public void modifyText(ModifyEvent e) {
			fTab.updateLaunchConfigurationDialog();
		}
	}
	
	public OSGiFrameworkBlock(AbstractLauncherTab tab) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		fConfigElements = orderElements(registry.getConfigurationElementsFor("org.eclipse.pde.ui.osgiLauncher")); //$NON-NLS-1$
		fTab = tab;
		fListener = new Listener();
	}
	
	private IConfigurationElement[] orderElements(IConfigurationElement[] elems) {
		Arrays.sort(elems, new Comparator() {
			public int compare(Object o1, Object o2) {
				String name1 = ((IConfigurationElement)o1).getAttribute("name"); //$NON-NLS-1$
				String name2 = ((IConfigurationElement)o2).getAttribute("name"); //$NON-NLS-1$
				if (name1 != null)
					return name1.compareToIgnoreCase(name2);
				return 1;
			}
		});
		return elems;
	}
	
	public void createControl(Composite parent) {
		createFrameworkGroup(parent);
		createDefaultsGroup(parent);
	}
	
	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		initializeFramework(config);
		boolean auto = config.getAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
		fDefaultAutoStart.setText(Boolean.toString(auto));
		int level = config.getAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
		fDefaultStartLevel.setSelection(level);
	}
	
	private void initializeFramework(ILaunchConfiguration config) throws CoreException {
		String id = config.getAttribute(OSGiLaunchConfiguration.OSGI_ENV_ID, (String) null);
		if (id == null)
			id = EquinoxLauncher.ID;
		
		for (int i = 0; i < fConfigElements.length; i++) {
			if (id.equals(fConfigElements[i].getAttribute("id"))){ //$NON-NLS-1$
				fLauncherCombo.select(i);
				return;
			}
		}
	}
	
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, 
				Boolean.toString(true).equals(fDefaultAutoStart.getText()));
		config.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, fDefaultStartLevel.getSelection());
		setLauncher(config);
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
		config.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
	}
	
	private void createFrameworkGroup(Composite container) {
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEUIMessages.OSGiBundlesTab_frameworkLabel);

		fLauncherCombo = new Combo(composite, SWT.READ_ONLY);
		for (int i = 0; i < fConfigElements.length; i++) 
			fLauncherCombo.add(fConfigElements[i].getAttribute("name")); //$NON-NLS-1$
		fLauncherCombo.addModifyListener(fListener);
	}
	
	private void createDefaultsGroup(Composite container) {
		Composite defaults = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(5, false);
		defaults.setLayout(layout);
		defaults.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		Label startLevelLabel = new Label(defaults, SWT.NONE);
		startLevelLabel.setText(PDEUIMessages.EquinoxPluginsTab_defaultStart);

		fDefaultStartLevel = new Spinner(defaults, SWT.BORDER);
		fDefaultStartLevel.setMinimum(1);
		fDefaultStartLevel.addModifyListener(fListener);

		Label label = new Label(defaults, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.minimumWidth = 50;
		label.setLayoutData(gd);

		Label autoStartLabel = new Label(defaults, SWT.NONE);
		autoStartLabel.setText(PDEUIMessages.EquinoxPluginsTab_defaultAuto);

		fDefaultAutoStart = new Combo(defaults, SWT.BORDER | SWT.READ_ONLY);
		fDefaultAutoStart.setItems(new String[] {Boolean.toString(true), Boolean.toString(false)});
		fDefaultAutoStart.select(0);
		fDefaultAutoStart.addSelectionListener(fListener);
	}

	private void setLauncher(ILaunchConfigurationWorkingCopy config) {
		try {
			String oldId = config.getAttribute(OSGiLaunchConfiguration.OSGI_ENV_ID, ""); //$NON-NLS-1$
			String newId = fConfigElements[fLauncherCombo.getSelectionIndex()].getAttribute("id"); //$NON-NLS-1$
			if (!newId.equals(oldId)) {
				AbstractOSGiLaunchConfiguration launcher = (AbstractOSGiLaunchConfiguration) fConfigElements[fLauncherCombo.getSelectionIndex()].createExecutableExtension("class"); //$NON-NLS-1$
				if (launcher != null) {
					launcher.initialize(config);
					config.setAttribute(OSGiLaunchConfiguration.OSGI_ENV_ID, fConfigElements[fLauncherCombo.getSelectionIndex()].getAttribute("id")); //$NON-NLS-1$
					fTab.initializeFrom(config);
				}
			}
		} catch (CoreException e) {
		}
	}
	
	public int getDefaultStartLevel() { 
		return fDefaultStartLevel.getSelection();
	}
}
