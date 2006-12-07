package org.eclipse.pde.internal.ui.wizards.provisioner;

import java.io.File;

import org.eclipse.pde.internal.ui.PDEPlugin;

public class FileSystemProvisionerWizard extends ProvisionerWizard {
	
	private DirectorySelectionPage fPage = null;
	private File[] fDirs = null;
	
	public FileSystemProvisionerWizard() {
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle("Additional plug-in directories"); 
	}

	public File[] getDirectories() {
		return fDirs;
	}

	public boolean performFinish() {
		fDirs = fPage.getLocations();
		return true;
	}

	public void addPages() {
		fPage = new DirectorySelectionPage("file system");
		addPage(fPage);
		super.addPages();
	}

}
