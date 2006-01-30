package org.eclipse.pde.internal.ui.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.pde.internal.ui.editor.text.XMLPartitionScanner;
import org.eclipse.swt.widgets.Composite;

public class XMLContentMergeViewer extends TextMergeViewer {

	public XMLContentMergeViewer(Composite parent, CompareConfiguration config) {
		super(parent, config);
	}

	protected void configureTextViewer(TextViewer textViewer) {
		if (textViewer instanceof SourceViewer) {
			IColorManager colorManager = ColorManager.getDefault();
			((SourceViewer)textViewer).configure(new XMLConfiguration(colorManager));
			((SourceViewer)textViewer).getTextWidget().setFont(JFaceResources.getTextFont());
		}
	}

	protected IDocumentPartitioner getDocumentPartitioner() {
		return new FastPartitioner(new XMLPartitionScanner(), XMLPartitionScanner.PARTITIONS);
	}
	
	public String getTitle() {
		return XMLStructureCreator.DEFAULT_NAME; 
	}

}
