package org.eclipse.pde.internal.ui.wizards.provisioner;

import java.io.File;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.ui.IBasePluginWizard;

public abstract class ProvisionerWizard extends Wizard implements IBasePluginWizard {
	
	public abstract File[] getDirectories();

}
