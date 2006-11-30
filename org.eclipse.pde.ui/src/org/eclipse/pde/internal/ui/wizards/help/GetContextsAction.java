package org.eclipse.pde.internal.ui.wizards.help;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

public class GetContextsAction implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;

	public void run(IAction action) {
		GetContextHelpOperation runnable = new GetContextHelpOperation(fSelection);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		} finally {
			final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			final ContextHelpWizard wizard = new ContextHelpWizard(runnable.getContexts());
			final WizardDialog dialog = new WizardDialog(shell, wizard);
			BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
				public void run() {
					dialog.open();
				}
			});
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}
}
