package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public abstract class GeneralRenameIDWizardPage extends UserInputWizardPage {
	
	protected RefactoringInfo fInfo;
	private Text fNewId;
	private Button fUpdateReferences;
	
	public GeneralRenameIDWizardPage(String title, RefactoringInfo info) {
		super(title);
		fInfo = info;
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		Dialog.applyDialogFont(composite);
		
		createMainControl(composite);
		
		setPageComplete(false);
		setControl(composite);
	}
	
	protected void createMainControl(Composite parent) {
		createNewID(parent);
		createUpdateReferences(parent);
	}
	
	protected void createNewID(Composite composite) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEUIMessages.RenamePluginWizardPage_newId);
		
		fNewId = new Text(composite, SWT.BORDER);
		fNewId.setText(fInfo.getCurrentValue());
		fNewId.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNewId.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fInfo.setNewValue(fNewId.getText());
				validatePage();
			}
		});
	}
	
	protected void createUpdateReferences(Composite composite) {
		fUpdateReferences = new Button(composite, SWT.CHECK);
		fUpdateReferences.setText(PDEUIMessages.RenamePluginWizardPage_updateReferences);
		fUpdateReferences.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, 2, 1));
		fUpdateReferences.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fInfo.setUpdateReferences(fUpdateReferences.getSelection());
			}
		});
		fUpdateReferences.setSelection(true);
	}
	
	protected void validatePage() {
		String text = fNewId.getText();
		String errorMessage = null;
		if (text.length() == 0)
			errorMessage = PDEUIMessages.RenamePluginWizardPage_idNotSet;
		else
			errorMessage = validateId(text);
		if (errorMessage == null && text.equals(fInfo.getCurrentValue()))
			setPageComplete(false);
		else
			setPageComplete(errorMessage == null ? new RefactoringStatus() : RefactoringStatus.createFatalErrorStatus(errorMessage));
	}
	
	/**
	 * Intended to allow subclassing wizard pages validate the id based on custom criteria.
	 * 
	 * @param id
	 * @return null if id is valid, otherwise return corresponding error message
	 */
	protected abstract String validateId(String id);

}