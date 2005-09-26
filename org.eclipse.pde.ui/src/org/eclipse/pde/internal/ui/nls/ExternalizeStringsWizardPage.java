package org.eclipse.pde.internal.ui.nls;

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
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
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
			return ((ModelChange)parentElement).getChangeFileCoupling();
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
			if (property == null)
				return false;

			if (!(element instanceof ModelChangeElement))
				return false;
			
			if (TABLE_PROPERTIES[VALUE].equals(property))
				return false;
			
			ModelChangeElement changeElement = (ModelChangeElement) element;
			if (TABLE_PROPERTIES[KEY].equals(property) && !changeElement.getExternalized())
				return false;
			
			return true;
		}

		/**
		 * @see ICellModifier#getValue(Object, String)
		 */
		public Object getValue(Object element, String property) {
			if (element instanceof ModelChangeElement) {
				ModelChangeElement changeElement = (ModelChangeElement) element;
				String res = null;
				if (TABLE_PROPERTIES[KEY].equals(property)) {
					res = changeElement.getKey();
				} else if (TABLE_PROPERTIES[VALUE].equals(property)) {
					res = changeElement.getValue();
				} else if (TABLE_PROPERTIES[EXTERN].equals(property)) {
					return new Boolean(changeElement.getExternalized());
				}
				if (res != null) {
					return StringWinder.unwindEscapeChars(res);
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
					if (TABLE_PROPERTIES[EXTERN].equals(property)) {
						changeElement.setExternalized(((Boolean) value).booleanValue());
					}
					if (TABLE_PROPERTIES[VALUE].equals(property)) {
						changeElement.setValue(StringWinder.windEscapeChars((String)value));
					}
					if (TABLE_PROPERTIES[KEY].equals(property)) {
						String string = (String)value;
						string = StringWinder.windEscapeChars(string);
						changeElement.setKey(string);
					}
//					validateKeys(false);
				}
				fPropertiesViewer.update(data, null);
			}
		}
	}
	
	private ModelChangeTable fModelChangeTable;
	
	private ContainerCheckedTreeViewer fInputViewer;
	private Button fHideUnselectedButton;
	private Label fPropertiesLabel;
	private TableViewer fPropertiesViewer;
	private Table fTable;
	private SourceViewer fSourceViewer;
	private ViewerFilter fViewerFilter;
	
	private ColorManager fColorManager;
	private Object fCurrSelection;
	
	protected ExternalizeStringsWizardPage(ModelChangeTable changeTable) {
		super("ExternalizeTranslationPage");
		setTitle("Externalize Strings");
		setDescription("Externalize strings in manifest files.");
		fModelChangeTable = changeTable;
		fViewerFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (!(element instanceof ModelChange) && !(parentElement instanceof ModelChange))
					return false;
				ModelChange change = (element instanceof ModelChange) ? 
						(ModelChange) element :
						(ModelChange) parentElement;
				return change.wasPreSelected();
			}
		};
	}
	
	public void dispose() {
		super.dispose();
		if (fColorManager != null)
			fColorManager.dispose();
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
		label.setText("Resources with un-externalized strings:");
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
		fInputViewer.addFilter(fViewerFilter);
		
		Composite buttonComposite = new Composite(fileComposite, SWT.NONE);
		buttonComposite.setLayout(new GridLayout());
		buttonComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fHideUnselectedButton = new Button(buttonComposite, SWT.CHECK);
		fHideUnselectedButton.setText("Hide unselected projects");
		fHideUnselectedButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fHideUnselectedButton.setSelection(true);
		fHideUnselectedButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fHideUnselectedButton.getSelection())
					fInputViewer.addFilter(fViewerFilter);
				else
					fInputViewer.removeFilter(fViewerFilter);
			}
		});
		
		
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
		
		
		fPropertiesViewer = new TableViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.BORDER);
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
			IPluginModelBase model = ((ModelChange)fCurrSelection).getParentModel();
			updatePropertiesLabel(fModelChangeTable.getModelChange(model).getPropertiesFile(), model);
		} 
		fPropertiesViewer.refresh();
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
		updatePropertiesLabel(fModelChangeTable.getModelChange(model).getPropertiesFile(), model);
	}
	
	
	private void updatePropertiesLabel(IFile propertiesFile, IPluginModelBase model) {
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
		editors[EXTERN] = new CheckboxCellEditor(fTable);
		editors[KEY] = new TextCellEditor(fTable);
		editors[VALUE] = new TextCellEditor(fTable);
		return editors;
	}

	protected void validateKeys(String key, boolean refreshTable) {
		RefactoringStatus status = new RefactoringStatus();
		checkInvalidKey(key, status);
		checkDuplicateKey(key, status);
		checkMissingKey(key, status);
		if (!status.isOK())
			setErrorMessage(status.getEntryWithHighestSeverity().getMessage());
		else
			setErrorMessage(null);
		setPageComplete(status.isOK());
		if (refreshTable)
			fPropertiesViewer.refresh(true);
	}
	
	private void checkInvalidKey(String key, RefactoringStatus status) {
		if (key.length() < 1) {
			status.addError("Key is too short");
			return;
		}
		char first = key.charAt(0);
		if (first == '#' || first == '!') {
			status.addError("Keys may not begin with # or !");
			return;
		}
		if ( (key.indexOf(":") != -1 && key.indexOf("\\:") == -1) ||
				(key.indexOf("=") != -1 && key.indexOf("\\=") == -1) ) {
			status.addError("Keys may not contain : or =");
			return;
		}
		
	}
	
	private void checkDuplicateKey(String key, RefactoringStatus status) {
		
	}

	private void checkMissingKey(String key, RefactoringStatus status) {
	}
	
	public Object[] getChangeFiles() {
		return fInputViewer.getCheckedElements();
	}
}
