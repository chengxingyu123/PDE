package org.eclipse.pde.internal.ui.nls;

import java.util.Properties;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class ExternalizeStringsWizardPage extends WizardPage {

	public static final String PAGE_NAME = "ExternalizeStringsWizardPage"; //$NON-NLS-1$
	
	public static final int EXTERN = 0;
	public static final int VALUE = 1;
	public static final int KEY = 2;
	private static final int SIZE = 3; // column counter
	private static final String[] TABLE_PROPERTIES = new String[SIZE];
	private static final String[] TABLE_COLUMNS = new String[SIZE];
	
	static {
		TABLE_PROPERTIES[EXTERN] = "extern"; //$NON-NLS-1$
		TABLE_PROPERTIES[VALUE] = "value"; //$NON-NLS-1$
		TABLE_PROPERTIES[KEY] = "key"; //$NON-NLS-1$
		TABLE_COLUMNS[EXTERN] = ""; //$NON-NLS-1$
		TABLE_COLUMNS[VALUE] = "Value"; //$NON-NLS-1$
		TABLE_COLUMNS[KEY] = "Substitution Key"; //$NON-NLS-1$
	}

	public class ModelChangeContentProvider implements ITreeContentProvider, IContentProvider {
		
		public Object[] getElements(Object parent) {
			return fModelChangeTable.getAllModelChanges().toArray();
		}

		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof ModelChange))
				return new Object[0];
			return ((ModelChange)parentElement).getModelChangeFiles();
		}

		public Object getParent(Object element) {
			if (element instanceof ModelChangeFile) {
				return ((ModelChangeFile)element).getModel();
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			return element instanceof ModelChange;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private class CellModifier implements ICellModifier {

		/**
		 * @see ICellModifier#canModify(Object, String)
		 */
		public boolean canModify(Object element, String property) {
			return (property != null &&
					(element instanceof ModelChangeElement) &&
					!TABLE_PROPERTIES[VALUE].equals(property) &&
					(isPageComplete() || element.equals(fErrorElement)) &&
					(TABLE_PROPERTIES[KEY].equals(property) && ((ModelChangeElement)element).getExtern()));

		}

		/**
		 * @see ICellModifier#getValue(Object, String)
		 */
		public Object getValue(Object element, String property) {
			if (element instanceof ModelChangeElement) {
				ModelChangeElement changeElement = (ModelChangeElement) element;
				if (TABLE_PROPERTIES[KEY].equals(property)) {
					return StringWinder.unwindEscapeChars(changeElement.getKey());
				}
			}
			return ""; //$NON-NLS-1$
		}

		/**
		 * @see ICellModifier#modify(Object, String, Object)
		 */
		public void modify(Object element, String property, Object value) {
			if (element instanceof TableItem) {
				Object data = ((TableItem) element).getData();
				if (data instanceof ModelChangeElement) {
					ModelChangeElement changeElement = (ModelChangeElement) data;
					if (TABLE_PROPERTIES[KEY].equals(property)) {
						String newKey = StringWinder.windEscapeChars((String)value);
						validateKey(newKey, changeElement);
						changeElement.setKey(newKey);
						fPropertiesViewer.update(data, null);
					}
				}
			}
		}
	}
	
	private ModelChangeTable fModelChangeTable;
	
	private ContainerCheckedTreeViewer fInputViewer;
	private Button fUnselectedFilterButton;
	private Label fPropertiesLabel;
	private CheckboxTableViewer fPropertiesViewer;
	private Table fTable;
	private SourceViewer fSourceViewer;
	private ViewerFilter fPreSelectFilter;
	private ViewerFilter fErrorElementFilter;
	
	private Object fCurrSelection;
	private ModelChangeElement fErrorElement;
	private String fPreErrorKey;
	
	
	protected ExternalizeStringsWizardPage(ModelChangeTable changeTable) {
		super("ExternalizeTranslationPage");
		setTitle("Externalize Strings");
		setDescription("Externalize strings in manifest files.");
		fModelChangeTable = changeTable;
		fPreSelectFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (!(element instanceof ModelChange) && !(parentElement instanceof ModelChange))
					return false;
				ModelChange change = (element instanceof ModelChange) ? 
						(ModelChange) element :
						(ModelChange) parentElement;
				return change.wasPreSelected();
			}
		};
		fErrorElementFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (!(element instanceof ModelChangeElement))
					return false;
				ModelChangeElement change = (ModelChangeElement) element;
				return change.equals(fErrorElement);
			}
		};
	}
	
	public void createControl(Composite parent) {

		SashForm superSash = new SashForm(parent, SWT.HORIZONTAL);
		superSash.setFont(parent.getFont());
		superSash.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createInputContents(superSash);
		
		SashForm sash = new SashForm(superSash, SWT.VERTICAL);
		sash.setFont(superSash.getFont());
		sash.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTableViewer(sash);
		createSourceViewer(sash);
		
		superSash.setWeights(new int[] {4,7});
		setControl(superSash);
		Dialog.applyDialogFont(superSash);

		// TODO ADD HELP
		// PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
		// IHelpContextIds.UPDATE_CLASSPATH);
	}

	private void createInputContents(Composite composite) {

		Composite fileComposite = new Composite(composite, SWT.NONE);
		fileComposite.setLayout(new GridLayout());
		fileComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(fileComposite, SWT.NONE);
		label.setText("Resources with non-externalized strings:");
		fInputViewer = new ContainerCheckedTreeViewer(fileComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.BORDER);
		fInputViewer.setContentProvider(new ModelChangeContentProvider());
		fInputViewer.setLabelProvider(new ModelChangeLabelProvider());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 250;
		fInputViewer.getTree().setLayoutData(gd);
		fInputViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged(event);
			}
		});
		
		
		Composite buttonComposite = new Composite(fileComposite, SWT.NONE);
		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = layout.marginWidth = 0;
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button selectAll = new Button(buttonComposite, SWT.PUSH);
		selectAll.setText("Select All");
		selectAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Button selectNone = new Button(buttonComposite, SWT.PUSH);
		selectNone.setText("Select None");
		selectNone.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Composite checkButtonComp = new Composite(buttonComposite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		checkButtonComp.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		checkButtonComp.setLayoutData(gd);
		fUnselectedFilterButton = new Button(checkButtonComp, SWT.CHECK);
		fUnselectedFilterButton.setText("Show selected resources only");
		fUnselectedFilterButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fUnselectedFilterButton.setSelection(fModelChangeTable.hasPreSelected());
		fUnselectedFilterButton.setEnabled(fModelChangeTable.enableFilter());
		fUnselectedFilterButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fUnselectedFilterButton.getSelection())
					fInputViewer.addFilter(fPreSelectFilter);
				else
					fInputViewer.removeFilter(fPreSelectFilter);
			}
		});
		if (fModelChangeTable.hasPreSelected())
			fInputViewer.addFilter(fPreSelectFilter);
		
		Composite infoComposite = new Composite(fileComposite, SWT.NONE);
		infoComposite.setLayout(new GridLayout());
		infoComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label properties = new Label(infoComposite, SWT.NONE);
		properties.setText("Properties file:");
		fPropertiesLabel = new Label(infoComposite, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 10;
		fPropertiesLabel.setLayoutData(gd);
		fPropertiesLabel.setText("No underlying resource selected");
		
		fInputViewer.setInput(PDEPlugin.getDefault());
	}

	private void createTableViewer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout());
				
		Label label = new Label(composite, SWT.NONE);
		label.setText("Strings to externalize:"); 
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		
		fPropertiesViewer = CheckboxTableViewer.newCheckList(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.BORDER);
		
		fTable = fPropertiesViewer.getTable();
		fTable.setFont(composite.getFont());
		fTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		fTable.setLayout(new GridLayout());
		fTable.setLinesVisible(true);
		fTable.setHeaderVisible(true);

		for (int i= 0; i < TABLE_COLUMNS.length; i++) {
			TableColumn tc = new TableColumn(fTable, SWT.NONE);
			tc.setText(TABLE_COLUMNS[i]);
			tc.setResizable(i != 0);
			tc.setWidth(i == 0 ? 20 : 200);
		}
		
		fPropertiesViewer.setUseHashlookup(true);

		final CellEditor[] editors = createCellEditors();
		fPropertiesViewer.setCellEditors(editors);
		fPropertiesViewer.setColumnProperties(TABLE_PROPERTIES);
		fPropertiesViewer.setCellModifier(new CellModifier());

		fPropertiesViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				if (fInputViewer.getSelection() instanceof IStructuredSelection) {
					Object selection = ((IStructuredSelection)fInputViewer.getSelection()).getFirstElement();
					if (selection instanceof ModelChangeFile) {
						ModelChangeFile cf = (ModelChangeFile)selection;
						return (cf).getModel().getChangesInFile(cf.getFile()).toArray();
					}
				}
				return new Object[0];
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		fPropertiesViewer.setLabelProvider(new ExternalizeStringsLabelProvider());
		fPropertiesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handlePropertySelection();
			}
		});
		fPropertiesViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof ModelChangeElement) {
					((ModelChangeElement)element).setExternalized(event.getChecked());
					fPropertiesViewer.update(element, null);
				}
			}
		});
		
		fPropertiesViewer.setInput(new Object());
	}

	private void createSourceViewer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout());

		Label label = new Label(composite, SWT.NONE);
		label.setText("Source:");
		label.setLayoutData(new GridData());

		fSourceViewer = new SourceViewer(composite, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		fSourceViewer.getControl().setFont(JFaceResources.getTextFont());
		fSourceViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		fSourceViewer.setEditable(false);
	}
	
	private void handleSelectionChanged(SelectionChangedEvent event) {
		if (!(event.getSelection() instanceof IStructuredSelection))
			return;
		Object selection = (((IStructuredSelection)event.getSelection()).getFirstElement());
		if (selection == null) {
			fSourceViewer.setDocument(null);
			fCurrSelection = null;
		} else if (selection.equals(fCurrSelection)) {
			return;
		} else if (selection instanceof ModelChangeFile) {
			fCurrSelection = selection;
			IFile file = ((ModelChangeFile)fCurrSelection).getFile();
			NullProgressMonitor monitor = new NullProgressMonitor();
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			try {
				try {
					manager.connect(file.getFullPath(), monitor);
					updateSourceViewer(manager, file);
				} catch (MalformedTreeException e) {
				} finally {
					manager.disconnect(file.getFullPath(), monitor);
				}
			} catch (CoreException e) {
			}
		} else if (selection instanceof ModelChange) {
			fCurrSelection = selection;
			updatePropertiesLabel(((ModelChange)fCurrSelection).getParentModel());
		} 
		refreshPropertiesViewer(false);
	}
	
	private void refreshPropertiesViewer(boolean updateLabels) {
		fPropertiesViewer.refresh();
		TableItem[] items = fTable.getItems();
		for (int i = 0; i < items.length; i++) {
			if (!(items[i].getData() instanceof ModelChangeElement)) continue;
			ModelChangeElement element = (ModelChangeElement)items[i].getData();
			fPropertiesViewer.setChecked(element, element.getExtern());
		}
	}
	
	private void updateSourceViewer(ITextFileBufferManager manager, IFile sourceFile) {
		IDocument document = manager.getTextFileBuffer(sourceFile.getFullPath()).getDocument();
		TreeItem item = fInputViewer.getTree().getSelection()[0];
		IPluginModelBase model = ((ModelChange)item.getParentItem().getData()).getParentModel();
		
//		if (fSourceViewer.getDocument() != null)
//			fSourceViewer.unconfigure();
//		
//		if (sourceFile.getFileExtension().equalsIgnoreCase("xml")) {
//			if (fColorManager != null)
//				fColorManager.dispose();
//			fColorManager = ColorManager.getDefault();
//			fSourceViewer.configure(new XMLConfiguration(fColorManager));
//		}
		fSourceViewer.setDocument(document);
		updatePropertiesLabel(model);
	}
	
	
	private void updatePropertiesLabel(IPluginModelBase model) {
		IFile propertiesFile = fModelChangeTable.getModelChange(model).getPropertiesFile();
		fPropertiesLabel.setText(model.getBundleDescription().getName() + "\\"
				+ propertiesFile.getProjectRelativePath().toOSString());
	}
	
	protected void handlePropertySelection() {
		if (!(fPropertiesViewer.getSelection() instanceof IStructuredSelection)) return;
		Object selection = (((IStructuredSelection)fPropertiesViewer.getSelection()).getFirstElement());
		if (selection instanceof ModelChangeElement && fSourceViewer.getDocument() != null) {
			ModelChangeElement element = (ModelChangeElement) selection;
			int offset = element.getOffset();
			int length = element.getLength();
			fSourceViewer.setSelectedRange(offset, length);
			fSourceViewer.revealRange(offset, length);
		}
	}
	
	private CellEditor[] createCellEditors() {
		final CellEditor editors[] = new CellEditor[SIZE];
		editors[EXTERN] = null;
		editors[VALUE] = null;
		editors[KEY] = new TextCellEditor(fTable);
		return editors;
	}

	protected void validateKey(String key, ModelChangeElement element) {
		ModelChange modelChange = ((ModelChangeFile)fCurrSelection).getModel();
		Properties properties = modelChange.getProperties();
		String error = null;
		String oldKey = (fPreErrorKey != null) ? fPreErrorKey : element.getKey();
		if (key.equals(fPreErrorKey)) {
			error = null;
		} else if (key.trim().length() < 1) {
			error = getErrorMessage("New key is too short", oldKey);
		} else if (key.charAt(0) == '#' || key.charAt(0) == '!' || key.charAt(0) == '%') {
			error = getErrorMessage("New key may not begin with #, ! or %", oldKey);
		} else if ((key.indexOf(":") != -1 && key.indexOf("\\:") == -1) ||
				   (key.indexOf("=") != -1 && key.indexOf("\\=") == -1) ) {
			error = getErrorMessage("New key may not contain : or =", oldKey);
		} else if ((!key.equals(oldKey) || !isPageComplete()) &&
				properties.containsKey(key)) {
			error = getErrorMessage("Duplicate key found", oldKey);
		}

		setErrorMessage(error);
		setPageComplete(error == null);
		if (error == null) {
			fErrorElement = null;
			fPreErrorKey = null;
			fInputViewer.getControl().setEnabled(true);
			fPropertiesViewer.removeFilter(fErrorElementFilter);
			refreshPropertiesViewer(true);
			properties.setProperty(key, element.getValue());
		} else if (fPreErrorKey == null) {
			fErrorElement = element;
			fPreErrorKey = oldKey;
			fInputViewer.getControl().setEnabled(false);
			fPropertiesViewer.addFilter(fErrorElementFilter);
		}
	}
	
	private String getErrorMessage(String error, String suggestion) {
		StringBuffer sb = new StringBuffer(error);
		if (suggestion != null) {
			sb.append("\n\tsuggested key value: ");
			sb.append(suggestion);
		}
		return sb.toString();
	}
	
	public Object[] getChangeFiles() {
		return fInputViewer.getCheckedElements();
	}
}
