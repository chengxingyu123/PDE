package org.eclipse.pde.internal.ui.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class FileTreeDialogCellEditor extends DialogCellEditor {

	private String[] fFileExtensions;
	
	public static ElementTreeSelectionDialog getNewDialog(Shell shell, String[] fileExtensions, Object initialSelection) {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
					shell,
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle("Select file");  
		dialog.setMessage("Select a file from the workspace"); 
		dialog.addFilter(new FileExtensionFilter(fileExtensions));
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		dialog.setInitialSelection(initialSelection);
		return dialog;
	}
	
	public FileTreeDialogCellEditor(String[] fileExtensions, Composite parent) {
		super(parent, SWT.NONE);
		fFileExtensions = fileExtensions;
	}
	
	protected Object openDialogBox(Control cellEditorWindow) {
		Object value = getValue();
		ElementTreeSelectionDialog dialog = getNewDialog(cellEditorWindow.getShell(), fFileExtensions, value);
		if (dialog.open() == Window.OK)
			return dialog.getFirstResult();
		return value;
	}
	
	protected void updateContents(Object value) {
		if (value instanceof IResource)
			value = ((IResource)value).getProjectRelativePath().toString();
		super.updateContents(value);
	}
}
