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
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.launcher.OSGiBundleBlock;
import org.eclipse.pde.internal.ui.launcher.OSGiLauncherTabGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.PlatformUI;

/**
 * A launch configuration tab that lets the user customize the list of bundles to launch with,
 * their start level and their auto-start attributes.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed by clients.
 * </p>
 * @since 3.2
 */
public class OSGiBundlesTab extends AbstractLauncherTab {

	private Image fImage;
	private OSGiBundleBlock fPluginBlock;
	private Listener fListener = new Listener();
	private Combo fDefaultAutoStart;
	private Spinner fDefaultStartLevel;
	private Combo fLauncherCombo;
	private IConfigurationElement[] fConfigElements;
	private boolean fRefreshing;
	private OSGiLauncherTabGroup fGroup;
	private boolean fUpdateRequired;
	private boolean fInitializing = false;

	class Listener extends SelectionAdapter implements ModifyListener{
		public void widgetSelected(SelectionEvent e) {
			updateLaunchConfigurationDialog();
		}

		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}
	}


	public OSGiBundlesTab(OSGiLauncherTabGroup group) {
		this();
		fGroup = group;
	}

	/*
	 * Constructor
	 */
	public OSGiBundlesTab() {
		fImage = PDEPluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
		fPluginBlock = new OSGiBundleBlock(this);
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		fConfigElements = registry.getConfigurationElementsFor("org.eclipse.pde.ui.osgiLauncher"); //$NON-NLS-1$

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		fPluginBlock.dispose();
		fImage.dispose();
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		createFrameworkGroup(composite);

		createDefaultsGroup(composite);
		fPluginBlock.createControl(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.LAUNCHER_ADVANCED);
	}

	private void createFrameworkGroup(Composite container) {
//		Group group = new Group(container, SWT.NONE);
//		group.setLayout(new GridLayout(2, false));
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEUIMessages.OSGiBundlesTab_frameworkLabel);

		fLauncherCombo = new Combo(composite, SWT.READ_ONLY);
		for (int i = 0; i < fConfigElements.length; i++) 
			fLauncherCombo.add(fConfigElements[i].getAttribute("name")); //$NON-NLS-1$
		fLauncherCombo.select(0);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			fInitializing = true;
			initializeFramework(config);
			boolean auto = config.getAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
			fDefaultAutoStart.setText(Boolean.toString(auto));
			int level = config.getAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
			fDefaultStartLevel.setSelection(level);
			fPluginBlock.initializeFrom(config);
			fInitializing = false;
			if (fUpdateRequired) {
				fUpdateRequired = false;
				updateLaunchConfigurationDialog();
			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	private void initializeFramework(ILaunchConfiguration config) {
		try {
			String id = config.getAttribute(OSGiLaunchConfiguration.OSGI_ENV_ID, (String) null);
			if (id == null)
				fLauncherCombo.select(0);
			else {
				for (int i = 0; i < fConfigElements.length; i++) {
					if (id.equals(fConfigElements[i].getAttribute("id"))){ //$NON-NLS-1$
						fLauncherCombo.select(i);
						return;
					}
				}
			}
		} catch (CoreException e) {

		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
		config.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
		fPluginBlock.setDefaults(config);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		if (fRefreshing)
			return;
		config.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, 
				Boolean.toString(true).equals(fDefaultAutoStart.getText()));
		config.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, fDefaultStartLevel.getSelection());
		setLauncher(config);
		fPluginBlock.performApply(config);
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
					if (fGroup != null) 
						fGroup.initializeFrom(config);
					else 
						// if we can't initialize all tabs, we can initialize the current one.
						initializeFramework(config);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return PDEUIMessages.AdvancedLauncherTab_name; 
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/**
	 * Returns the default start level for the launch configuration
	 * 
	 * @return the default start level
	 */
	public int getDefaultStartLevel() {
		return fDefaultStartLevel.getSelection();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy config) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractLauncherTab#validateTab()
	 */
	public void validateTab() {
	}

	public void updateLaunchConfigurationDialog() {
		if (!fInitializing)
			super.updateLaunchConfigurationDialog();
		else 
			fUpdateRequired = true;
	}

}
