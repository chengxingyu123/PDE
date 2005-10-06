package org.eclipse.pde.internal.ui.search.participant;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jdt.ui.search.IQueryParticipant;
import org.eclipse.jdt.ui.search.ISearchRequestor;
import org.eclipse.jdt.ui.search.PatternQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.ui.model.plugin.FragmentModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.model.plugin.PluginModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginModelBase;
import org.eclipse.search.ui.text.Match;

public class ClassSearchParticipant implements IQueryParticipant {

	private IMatchPresentation fMatchPresentation;
	
	public ClassSearchParticipant() {
		fMatchPresentation = new SearchMatchPresentation();
	}
	
	public void search(ISearchRequestor requestor,
			QuerySpecification querySpecification, IProgressMonitor monitor)
			throws CoreException {
		
		if (querySpecification.getLimitTo() != 2 && querySpecification.getLimitTo() != 3) 
			return;
		IPluginModelBase[] pluginModels = PDECore.getDefault().getModelManager().getWorkspaceModels();
		monitor.beginTask("Searching for classes and packages in manifest files", pluginModels.length);
		for (int i = 0; i < pluginModels.length; i++) {
			IProject project = pluginModels[i].getUnderlyingResource().getProject();
			if (!monitor.isCanceled()) {
				searchProject(project, requestor, querySpecification, monitor);
			}
		}
	}

	private void searchProject(IProject project, ISearchRequestor requestor, QuerySpecification query, IProgressMonitor monitor) throws CoreException {
		String[] files = new String[] {"plugin.xml", "fragment.xml" };
		for (int i = 0; i < files.length; i++) {
			IFile file = project.getFile(files[i]);
			if (!file.exists()) continue;
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			try {
				manager.connect(file.getFullPath(), monitor);
				ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
				IDocument document = buffer.getDocument();
				PluginModelBase loadModel;
				if ("fragment.xml".equals(files[i])) //$NON-NLS-1$
					loadModel = new FragmentModel(document, false);
				else
					loadModel = new PluginModel(document, false);
				loadModel.setUnderlyingResource(file);
				
				SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
				IPluginExtension[] extensions = loadModel.getPluginBase().getExtensions();
				for (int j = 0; j < extensions.length; j++) {
					ISchema schema = registry.getSchema(extensions[j].getPoint());
					if (schema != null)
						inspectExtension(schema, extensions[j], requestor, query);
				}
			} finally {
				manager.disconnect(file.getFullPath(), monitor);
			}
		}
	}

	private void inspectExtension(ISchema schema, IPluginParent extension, ISearchRequestor requestor, QuerySpecification query) {
		IPluginObject[] children = extension.getChildren();
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
						String search = null;
						if (query instanceof PatternQuerySpecification) {
							search = ((PatternQuerySpecification)query).getPattern();
						} else if (query instanceof ElementQuerySpecification) {
							search = ((ElementQuerySpecification)query).getElement().getElementName();
						}
						if (attr.getValue().indexOf(search) != -1) { 
							int offset = ((PluginAttribute)attr).getValueOffset();
							int length = ((PluginAttribute)attr).getValueLength();
							requestor.reportMatch(new Match(attr, Match.UNIT_CHARACTER, offset, length));
						}
					}
				}
			}
			inspectExtension(schema, child, requestor, query);
		}
	}

	public int estimateTicks(QuerySpecification specification) {
		return 100;
	}

	public IMatchPresentation getUIParticipant() {
		return fMatchPresentation;
	}

}
