package org.eclipse.pde.internal.ui.nls;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

public class ExternalizeStringsLabelProvider  extends LabelProvider implements ITableLabelProvider, IFontProvider {

	private FontRegistry fFontRegistry;
	private Image fTrans;
	private Image fNoTrans;
	
	public ExternalizeStringsLabelProvider() {
		fFontRegistry = JFaceResources.getFontRegistry();
		fTrans = PDEPluginImages.DESC_OK_TRANSLATE_OBJ.createImage();
		fNoTrans = PDEPluginImages.DESC_NO_TRANSLATE_OBJ.createImage();
	}

	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof ModelChangeElement) {
			ModelChangeElement changeElement = (ModelChangeElement)element;
			if (columnIndex == ExternalizeStringsWizardPage.VALUE) {
				return StringWinder.unwindEscapeChars(changeElement.getValue());
			} else if (columnIndex == ExternalizeStringsWizardPage.KEY) {
				return StringWinder.unwindEscapeChars(changeElement.getKey());
			}
		}
		return "";
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == ExternalizeStringsWizardPage.EXTERN && element instanceof ModelChangeElement)
			return getImage((ModelChangeElement)element);
		return null;
	}

	private Image getImage(ModelChangeElement sub) {
		if (sub.getExternalized() == true)
			return fTrans;
		return fNoTrans;
	}
	
	public Font getFont(Object element) {
		if (element instanceof ModelChangeElement) {
			ModelChangeElement changeElement = (ModelChangeElement) element;
			if (changeElement.getExternalized()) {
				return fFontRegistry.getBold(JFaceResources.DIALOG_FONT);
			}
		}
		return null;
	}
	
	public void dispose() {
		fTrans.dispose();
		fNoTrans.dispose();
		super.dispose();
	}
	
}
