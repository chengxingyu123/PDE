package org.eclipse.pde.internal.ui.search.participant;

import java.util.regex.Pattern;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.PatternQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.model.AbstractEditingModel;
import org.eclipse.pde.internal.ui.model.bundle.Bundle;
import org.eclipse.pde.internal.ui.model.bundle.BundleModel;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;
import org.eclipse.pde.internal.ui.model.plugin.FragmentModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.model.plugin.PluginModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginModelBase;
import org.eclipse.search.ui.text.Match;
import org.eclipse.text.edits.MalformedTreeException;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class ClassSearchParticipant implements IQueryParticipant {

	private static final int PLUGIN = 0;
	private static final int FRAGMENT = 1;
	private static final int MANIFEST = 2;
	private static final int TOTAL_FILES = 3;
	private static final String[] SEARCH_FILES = new String[TOTAL_FILES];
	static {
		SEARCH_FILES[PLUGIN] = "plugin.xml"; //$NON-NLS-1$
		SEARCH_FILES[FRAGMENT] = "fragment.xml"; //$NON-NLS-1$
		SEARCH_FILES[MANIFEST] = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
	}
	private static final int TOTAL_HEADERS = 4;
	private static final String[] SEARCH_HEADERS = new String[TOTAL_HEADERS];
	static {
		SEARCH_HEADERS[0] = Constants.IMPORT_PACKAGE;
		SEARCH_HEADERS[1] = Constants.EXPORT_PACKAGE;
		SEARCH_HEADERS[2] = Constants.BUNDLE_ACTIVATOR;
		SEARCH_HEADERS[3] = ICoreConstants.PLUGIN_CLASS;
	}
	
	private SearchMatchPresentation fMatchPresentation;
	private ISearchRequestor fSearchRequestor;
	private Pattern fSearchPattern;
	
	public ClassSearchParticipant() {
		fMatchPresentation = new SearchMatchPresentation();
	}
	
	public void search(ISearchRequestor requestor,
			QuerySpecification querySpecification, IProgressMonitor monitor)
			throws CoreException {
		
		if (querySpecification.getLimitTo() != 2 && querySpecification.getLimitTo() != 3) 
			return;
		
		String search;
		if (querySpecification instanceof ElementQuerySpecification) {
			search = ((ElementQuerySpecification)querySpecification).getElement().getElementName();
		} else {
			int searchFor = ((PatternQuerySpecification)querySpecification).getSearchFor();
			if (searchFor != 0 && searchFor != 2)
				return;
			search = ((PatternQuerySpecification)querySpecification).getPattern();
		}
		fSearchPattern = PatternConstructor.createPattern("*" + search + "*", true);
		fSearchRequestor = requestor;
		
		IPluginModelBase[] pluginModels = PDECore.getDefault().getModelManager().getWorkspaceModels();
		monitor.beginTask("Searching for classes and packages in manifest files", pluginModels.length);
		for (int i = 0; i < pluginModels.length; i++) {
			IProject project = pluginModels[i].getUnderlyingResource().getProject();
			if (!monitor.isCanceled()) {
				searchProject(project, monitor);
			}
		}
	}

	private void searchProject(IProject project, IProgressMonitor monitor) throws CoreException {
		for (int i = 0; i < SEARCH_FILES.length; i++) {
			IFile file = project.getFile(SEARCH_FILES[i]);
			if (!file.exists()) continue;
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			try {
				manager.connect(file.getFullPath(), monitor);
				ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
				IDocument document = buffer.getDocument();
				AbstractEditingModel loadModel = null;
				switch(i) {
				case PLUGIN:
					loadModel = new PluginModel(document, false);
					break;
				case FRAGMENT:
					loadModel = new FragmentModel(document, false);
					break;
				case MANIFEST:
					loadModel = new BundleModel(document, false);
				}	
				if (loadModel == null) continue;
				loadModel.load();
				if (!loadModel.isLoaded()) continue;
				
				if ((i == FRAGMENT || i == PLUGIN) && loadModel instanceof IPluginModelBase) {
					loadModel.setUnderlyingResource(file);
					PluginModelBase modelBase = (PluginModelBase)loadModel;
					SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
					IPluginExtension[] extensions = modelBase.getPluginBase().getExtensions();
					for (int j = 0; j < extensions.length; j++) {
						ISchema schema = registry.getSchema(extensions[j].getPoint());
						if (schema != null)
							inspectExtension(schema, extensions[j], extensions[j]);
					}
				} else if (i == MANIFEST && loadModel instanceof IBundleModel) {
					loadModel.setUnderlyingResource(file);
					Bundle bundle = (Bundle)((IBundleModel)loadModel).getBundle();
					if (bundle != null)
						inspectBundle(bundle);
				}
			} finally {
				manager.disconnect(file.getFullPath(), monitor);
			}
		}
	}

	private void inspectExtension(ISchema schema, IPluginParent parent, IPluginExtension extension) {
		IPluginObject[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement)children[i];
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				IPluginAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					IPluginAttribute attr = attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null 
							&& attInfo.getKind() == IMetaAttribute.JAVA
							&& attr instanceof PluginAttribute) {
						String search = attr.getValue();
						if (fSearchPattern.matcher(search.subSequence(0, search.length())).matches()) { 
							int offset = ((PluginAttribute)attr).getValueOffset();
							int length = ((PluginAttribute)attr).getValueLength();
							fSearchRequestor.reportMatch(new Match(attr, Match.UNIT_CHARACTER, offset, length));
						}
					}
				}
			}
			inspectExtension(schema, child, extension);
		}
	}

	private void inspectBundle(Bundle bundle) {
		for (int i = 0; i < SEARCH_HEADERS.length; i++) {
			ManifestHeader header = bundle.getManifestHeader(SEARCH_HEADERS[i]);
			if (header != null) {
				try {
					ManifestElement[] elements = ManifestElement.parseHeader(header.getName(), header.getValue());
					if (elements == null) continue;
					int initOff = 0;
					for (int j = 0; j < elements.length; j++) {
						String search = elements[j].getValue();
						if (fSearchPattern.matcher(search.subSequence(0, search.length())).matches()) { 
							int[] offlen;
							try {
								offlen = getOffsetOfElement(header, search, initOff);
								initOff = offlen[0] - header.getOffset() - header.getLineDelimiter().length();
							} catch (CoreException e) {
								offlen = new int[]{header.getOffset(), header.getLength()};
							}
							fSearchRequestor.reportMatch(new Match(new HeaderElementHit(header), Match.UNIT_CHARACTER, offlen[0], offlen[1]));
						}
					}
				} catch (BundleException e) {
				}
			}
		}
	}
	
	private int[] getOffsetOfElement(ManifestHeader header, String value, int initOff) throws CoreException {
		int offset = 0;
		int length = 0;
		IResource res = header.getModel().getUnderlyingResource();
		if (res instanceof IFile) {
			IFile file = (IFile)res;
			IProgressMonitor monitor = new NullProgressMonitor();
			ITextFileBufferManager pManager = FileBuffers.getTextFileBufferManager();
			try {
				pManager.connect(file.getFullPath(), monitor);
				ITextFileBuffer pBuffer = pManager.getTextFileBuffer(file.getFullPath());
				IDocument pDoc = pBuffer.getDocument();
				int headerOffset = header.getOffset();
				String headerString = pDoc.get(headerOffset, header.getLength());
				int internalOffset = headerString.indexOf(value, initOff);
				if (internalOffset != -1) {
					offset = headerOffset + internalOffset + header.getLineDelimiter().length();
				} else {
					offset = headerOffset + header.getValue().indexOf(value);
				}
				length = value.length();
			} catch (MalformedTreeException e) {
			} catch (BadLocationException e) {
			} finally {
				pManager.disconnect(file.getFullPath(), monitor);
			}
		}
		return new int[]{offset, length};
	}
	
	public int estimateTicks(QuerySpecification specification) {
		return 100;
	}

	public IMatchPresentation getUIParticipant() {
		return fMatchPresentation;
	}
}
