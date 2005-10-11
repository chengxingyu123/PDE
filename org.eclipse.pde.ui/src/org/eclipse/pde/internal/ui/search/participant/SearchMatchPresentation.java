package org.eclipse.pde.internal.ui.search.participant;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PartInitException;

public class SearchMatchPresentation implements IMatchPresentation {

	private ILabelProvider fLabelProvider;
	
	private class LabelProvider extends SharedLabelProvider {
//		private Image fImage;
		
		private LabelProvider() {
//			fImage = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
		}
		
		public Image getImage(Object element) {
//			if (element instanceof IPluginAttribute || element instanceof ManifestHeader)
//				return fImage;
			return super.getImage(element);
		}

		public String getText(Object element) {
			String name = null;
			IResource resource = null;
			if (element instanceof ManifestHeader) {
				name = ((ManifestHeader)element).getName();
				resource = ((ManifestHeader)element).getModel().getUnderlyingResource();
			}
			if (element instanceof IPluginAttribute) {
				name = ((IPluginAttribute)element).getValue();
				resource = ((IPluginAttribute)element).getModel().getUnderlyingResource();
			}
			if (resource != null) {
				return name + " - " + resource.getFullPath().toOSString().substring(1);
			}
			return super.getText(element);
		}
		
//		public void dispose() {
//			if (fImage != null && !fImage.isDisposed())
//				fImage.dispose();
//			super.dispose();
//		}
	}
	
	public ILabelProvider createLabelProvider() {
		if (fLabelProvider == null)
			fLabelProvider = new LabelProvider();
		return fLabelProvider;
	}

	public void showMatch(Match match, int currentOffset, int currentLength,
			boolean activate) throws PartInitException {
		ClassSearchEditorOpener.open(match, activate);
	}

}
