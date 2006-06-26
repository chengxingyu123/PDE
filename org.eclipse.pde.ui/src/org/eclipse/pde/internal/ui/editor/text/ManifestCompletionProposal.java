package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class ManifestCompletionProposal implements ICompletionProposal {
	
	static final Image[] fImages = new Image[7];
	static final int TYPE_HEADER = 0;
	static final int TYPE_PACKAGE = 1;
	static final int TYPE_CLASS = 2;
	static final int TYPE_ATTRIBUTE = 3;
	static final int TYPE_DIRECTIVE = 4;
	static final int TYPE_BUNDLE = 5;
	static final int TYPE_VALUE = 6;

	String fValue; 
	int fOffset;
	int fStartOffset;
	int fType;
	
	protected ManifestCompletionProposal(String value, int startOffset, int currentOffset, int type) {
		fValue = value;
		fOffset = currentOffset;
		fStartOffset = startOffset;
		fType = type;
	}

	public void apply(IDocument document) {
		try {
			document.replace(fStartOffset, fOffset-fStartOffset, getValue());
		} catch (BadLocationException e) {
			return;
		}
	}

	public String getAdditionalProposalInfo() {
		return fValue;
	}

	public IContextInformation getContextInformation() {
		return null;
	}

	public String getDisplayString() {
		return fValue;
	}

	public Image getImage() {
		return getImage(fType);
	}

	public Point getSelection(IDocument document) {
		return new Point(fStartOffset + getValue().length(), 0);
	}
	
	Image getImage(int type) {
		Image image = fImages[type];
		if (image == null) {
			switch (type) {
			case TYPE_HEADER:
				image = PDEPluginImages.DESC_BUILD_VAR_OBJ.createImage();
				fImages[TYPE_HEADER] = image;
				return image;
			case TYPE_PACKAGE:
				image = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
				fImages[TYPE_PACKAGE] = image;
				return image;
			case TYPE_BUNDLE:
				image = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
				fImages[TYPE_BUNDLE] = image;
				return image;
			}
		}
		return image;
	}
	
	String getValue() {
		switch (fType) {
		case TYPE_HEADER:
			return fValue + ": ";
		case TYPE_ATTRIBUTE:
			return fValue + "= ";
		case TYPE_DIRECTIVE:
			return fValue + ":= ";
		}
		return fValue;
	}

}
