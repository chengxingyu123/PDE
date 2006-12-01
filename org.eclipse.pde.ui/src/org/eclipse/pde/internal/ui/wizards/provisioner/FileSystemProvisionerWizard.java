package org.eclipse.pde.internal.ui.wizards.provisioner;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class FileSystemProvisionerWizard extends ProvisionerWizard {
	
	private FileSelectionPage fPage = null;
	private File[] fDirs = null;
	
	class FileSelectionPage extends WizardPage {
		
		Text fDir = null;

		protected FileSelectionPage(String pageName) {
			super(pageName);
			setTitle("File System Provisioner");
			setDescription("Add plug-ins from your file system");
			setPageComplete(false);
		}

		public void createControl(Composite parent) {
			Composite comp = new Composite(parent, SWT.NONE);
			comp.setLayoutData(new GridData());
			comp.setLayout(new GridLayout(3, false));
			
			Label label = new Label(comp, SWT.NONE);			
			label.setText("&Location:");
			label.setLayoutData(new GridData());
			
			fDir = new Text(comp, SWT.BORDER);
			fDir.setText("");
			fDir.setFocus();
			fDir.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fDir.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					if (validatePage()) {
						setPageComplete(true);
						setErrorMessage(null);
					}
					else {
						setPageComplete(false);
						setErrorMessage("The path specified does not exist");
					}
				}
			});
			
			Button browse = new Button(comp, SWT.PUSH);
			browse.setText("B&rowse");
			browse.setLayoutData(new GridData());
			browse.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleBrowse();
				}
			});
			
			setControl(comp);
		}
		
		private void handleBrowse() {
			DirectoryDialog dialog = new DirectoryDialog(getShell());
			dialog.setMessage(PDEUIMessages.TargetPlatformPreferencePage_chooseInstall);
			if (fDir.getText().length() > 0)
				dialog.setFilterPath(fDir.getText());
			String newPath = dialog.open();
			if (newPath != null
					&& !ExternalModelManager.arePathsEqual(new Path(fDir.getText()), new Path(newPath))) {
				fDir.setText(newPath);
			}
		}
		
		private boolean validatePage() {
			return new File(fDir.getText()).exists();
		}
		
		private String getLocation() {
			return fDir.getText();
		}
		
	}

	public File[] getDirectories() {
		return fDirs;
	}

	public boolean performFinish() {
		if (fPage.getLocation().length() > 0)
			fDirs  = new File[] { new File(fPage.getLocation())} ;
		return true;
	}

	public void addPages() {
		fPage = new FileSelectionPage("file system");
		addPage(fPage);
		super.addPages();
	}

}
