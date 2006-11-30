package org.eclipse.pde.internal.ui.wizards.help;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.internal.core.text.help.ContextHelpModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

public class ContextJavaUnitManagmentDialog extends Dialog {

	private ICompilationUnit fCU;
	//private ContextHelpModel fModel;
	private Hashtable fTypeTable = new Hashtable();
	
	protected ContextJavaUnitManagmentDialog(Shell parentShell, ICompilationUnit cu, ContextHelpModel model) {
		super(parentShell);
		fCU = cu;
	//	fModel = model;
	}

	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite)super.createDialogArea(parent);
		Table table = new Table(comp, SWT.BORDER);
		table.setLayout(new GridLayout());
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		TableViewer viewer = new TableViewer(table);
		viewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				ArrayList list = null;
				try {
					IJavaElement[] children = fCU.getChildren();
					IType type = null;
					for (int i = 0; i < children.length; i++)
						if (children[i].getElementType() == IJavaElement.TYPE) {
							type = (IType)children[i];
							break;
						}
					if (type != null) {
						list = new ArrayList();
						IField[] fields = type.getFields();
						for (int i = 0; i < fields.length; i++) {
							if (fields[i].getTypeSignature().equals("QString;")) {
								Object constant = fields[i].getConstant();
								if (constant != null) {
									list.add(fields[i]);
									fTypeTable.put(fields[i], constant);
								}
							}
						}
						
					}
				} catch (JavaModelException e) {}
				return list != null ? list.toArray() : new Object[0];
			}

			public void dispose() {}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		});
		viewer.setLabelProvider(new ITableLabelProvider() {
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			public String getColumnText(Object element, int columnIndex) {
				if (columnIndex == 0)
					return ((IField)element).getElementName();
				return fTypeTable.get(element).toString();
			}

			public void addListener(ILabelProviderListener listener) {}
			public void removeListener(ILabelProviderListener listener) {}
			public void dispose() {}

			public boolean isLabelProperty(Object element, String property) {
				return false;
			}
		});
		viewer.setInput(fCU);
		
		return comp; 
	}
	
	public void applyChanges() {
		// TODO Auto-generated method stub
		
	}


}
