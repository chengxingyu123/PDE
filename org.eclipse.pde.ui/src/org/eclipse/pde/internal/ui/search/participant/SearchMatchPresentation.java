package org.eclipse.pde.internal.ui.search.participant;

import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.model.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PartInitException;

public class SearchMatchPresentation implements IMatchPresentation {

	private ILabelProvider fLabelProvider;
	
	private class LabelProvider extends SharedLabelProvider {
		private Image fClassImage;
		
		private LabelProvider() {
			fClassImage = PDEPluginImages.DESC_ATT_CLASS_OBJ.createImage();
		}
		
		public Image getImage(Object element) {
			if (element instanceof PluginAttribute)
				return fClassImage;
			return super.getImage(element);
		}

		public String getText(Object element) {
			if (element instanceof PluginAttribute)
				return ((PluginAttribute)element).getAttributeValue()
					+ ": "
					+ ((PluginAttribute)element).getPluginModel().getUnderlyingResource();
			return super.getText(element);
		}
		public void dispose() {
			if (fClassImage != null && !fClassImage.isDisposed())
				fClassImage.dispose();
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
		ClassSearchEditorOpener.open(match, activate);
		return;
	}

}
