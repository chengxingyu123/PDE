package $packageName$;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.ui.customattributes.ICustomAttributeCompletionProvider;
import org.eclipse.pde.ui.editor.IPDECompletionProposalFactory;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.FrameworkUtil;

public class $completionProviderClassName$ implements
		ICustomAttributeCompletionProvider {

	public ICompletionProposal[] computeAttributeProposal(int valueOffset, int offset, String currValue,
			IPDECompletionProposalFactory proc) {
		ICompletionProposal[] proposals = new ICompletionProposal[2];
		
		ImageDescriptor id = ImageDescriptor
				.createFromURL(FrameworkUtil.getBundle(this.getClass()).getResource(
						"/icons/color_wheel.png"));
		Image img = id.createImage(); // TODO this image should be disposed when
										// no longer used....
		proposals[0] = proc
				.createCompletionProposal(
						valueOffset,
						"0,0,255",
						"The sky is <strong><font color=\"blue\">blue</font></strong>!",
						offset, img);
		proposals[1] = proc
				.createCompletionProposal(
						valueOffset,
						"0,255,0",
						"The grass is <strong><font color=\"green\">green</font></strong>!",
						offset, img);
		return proposals;
	}
}