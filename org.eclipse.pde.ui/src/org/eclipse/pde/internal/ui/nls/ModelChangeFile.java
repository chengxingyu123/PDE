package org.eclipse.pde.internal.ui.nls;

import org.eclipse.core.resources.IFile;

public class ModelChangeFile {
	private IFile fFile;
	private ModelChange fModel;
	protected ModelChangeFile (IFile file, ModelChange model) {fFile=file;fModel=model;}
	protected int getNumChanges() {return fModel.getNumberOfChangesInFile(fFile);}
	protected IFile getFile() {return fFile;}
	protected ModelChange getModel() {return fModel;}
}
