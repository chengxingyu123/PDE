package org.eclipse.pde.internal.ui.wizards.help;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.help.ContextNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ChangeContextIDDialog extends InputDialog {

	private String fJavaFile;
	private boolean fUpdateJavaFile;
	
	public ChangeContextIDDialog(Shell parentShell, final ContextNode contextNode) {
		super(parentShell, 
				"Change Context ID", 
				"New ID:",
				contextNode.getId(),
				new IInputValidator() {
					public String isValid(String newText) {
						if (newText.equals(contextNode.getId()))
							return new String();
						return contextNode.getContexts().isValidId(newText);
					}
		});
	}

	public boolean updateJavaFile() {
		return fUpdateJavaFile;
	}
	
	protected Control createDialogArea(Composite parent) {
		Control comp = super.createDialogArea(parent);
		final Button updateJavaContext = new Button((Composite)comp, SWT.CHECK);
		updateJavaContext.setText(NLS.bind("Update context id in {0}", fJavaFile));
		updateJavaContext.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fUpdateJavaFile = updateJavaContext.getSelection();
			}
		});
		updateJavaContext.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		return comp;
	}
	
}
