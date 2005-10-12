package org.eclipse.pde.internal.ui.search.participant;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PartInitException;

public class SearchMatchPresentation implements IMatchPresentation {

	private ILabelProvider fLabelProvider;
	
	private class LabelProvider extends SharedLabelProvider {
		private Image fImage;
		
		private LabelProvider() {
			fImage = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
		}
		
		public Image getImage(Object element) {
			if (element instanceof SearchHit)
				return fImage;
			return super.getImage(element);
		}

		public String getText(Object element) {
			String value = null;
			IResource resource = null;
			if (element instanceof SearchHit) {
				value = ((SearchHit)element).getValue();
				resource = ((SearchHit)element).getResource();
			}
			if (resource != null) {
				return value + " - " + resource.getFullPath().toOSString().substring(1);
			}
			return super.getText(element);
		}
		
		public void dispose() {
			if (fImage != null && !fImage.isDisposed())
				fImage.dispose();
			super.dispose();
		}
	}
	
	public ILabelProvider createLabelProvider() {
		if (fLabelProvider == null)
			fLabelProvider = new LabelProvider();
		return fLabelProvider;
	}

	public void showMatch(Match match, int currentOffset, int currentLength,
			boolean activate) throws PartInitException {
		ClassSearchEditorOpener.open(match);
	}

}
