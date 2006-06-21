package org.eclipse.pde.internal.ui.editor.text;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
import org.osgi.framework.Constants;

public class ManifestContentAssitProcessor implements IContentAssistProcessor {
	
	protected PDESourcePage fSourcePage;
	
	private static final String[] fHeader = {
		Constants.BUNDLE_CATEGORY,
		Constants.BUNDLE_CLASSPATH,
		Constants.BUNDLE_COPYRIGHT,
		Constants.BUNDLE_DESCRIPTION,
		Constants.BUNDLE_NAME,
		Constants.BUNDLE_NATIVECODE,
		Constants.EXPORT_PACKAGE,
		Constants.EXPORT_SERVICE,
		Constants.IMPORT_PACKAGE,
		Constants.DYNAMICIMPORT_PACKAGE,
		Constants.IMPORT_SERVICE,
		Constants.BUNDLE_VENDOR,
		Constants.BUNDLE_VERSION,
		Constants.BUNDLE_DOCURL,
		Constants.BUNDLE_CONTACTADDRESS,
		Constants.BUNDLE_ACTIVATOR,
		Constants.BUNDLE_UPDATELOCATION,
		Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT,
		Constants.BUNDLE_SYMBOLICNAME,
		Constants.BUNDLE_LOCALIZATION,
		Constants.REQUIRE_BUNDLE,
		Constants.BUNDLE_MANIFESTVERSION,
		Constants.FRAGMENT_HOST //?
	};
	
	class ManifestCompletionProposal implements ICompletionProposal {

		String fValue; 
		int fOffset;
		int fStartOffset;
		
		protected ManifestCompletionProposal(String value, int startOffset, int currentOffset) {
			fValue = value;
			fOffset = currentOffset;
			fStartOffset = startOffset;
		}

		public void apply(IDocument document) {
			ReplaceEdit edit = new ReplaceEdit(fStartOffset, fOffset-fStartOffset, fValue);
			try {
				edit.apply(document);
			} catch (MalformedTreeException e) {
				e.printStackTrace();
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			fSourcePage.getInputContext().flushEditorInput();
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
			return null;
		}

		public Point getSelection(IDocument document) {
			return new Point(fOffset, 0);
		}
		
	}
	
	public ManifestContentAssitProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		IDocument doc = fSourcePage.getDocumentProvider().getDocument(fSourcePage.getInputContext().getInput());
		try {
			int lineNum = doc.getLineOfOffset(offset);
			int lineStart = doc.getLineOffset(lineNum);
			return computeCompletionProposals(doc, lineStart, offset);
		} catch (BadLocationException e) {
		}
		return null;
	}
	
	protected ICompletionProposal[] computeCompletionProposals(IDocument doc, int startOffset, int offset) {
		try {
			if (!isHeader(doc, startOffset, offset))
				return computeValue(doc, startOffset, offset);
			return computeHeader(doc.get(startOffset, offset - startOffset), startOffset, offset);
		} catch (BadLocationException e) {
		}
		return new ICompletionProposal[0];
	}
	
	protected final boolean isHeader(IDocument doc, int startOffset, int offset) throws BadLocationException {
		String value = doc.get(startOffset, offset - startOffset);
		if (value.indexOf(':') != -1)
			return false;
		for (--startOffset; startOffset >= 0; --startOffset) {
			char ch = doc.getChar(startOffset);
			if (!Character.isWhitespace(ch))
				return ch != ',';
		}
		return true;
	}
	
	protected ICompletionProposal[] computeHeader(String currentValue, int startOffset, int offset) {
		ArrayList completions = new ArrayList();
		IBaseModel model = fSourcePage.getInputContext().getModel();
		int length = fHeader.length;
		if (model instanceof IBundleModel && !((IBundleModel)model).isFragmentModel()) 
			--length;
		for (int i = 0; i < fHeader.length; i++) {
			if (fHeader[i].regionMatches(true, 0, currentValue, 0, currentValue.length()))
				completions.add(new ManifestCompletionProposal(fHeader[i] + ": ", startOffset, offset));
		}
		return (ICompletionProposal[]) completions.toArray(new ICompletionProposal[completions.size()]);
	}
	
	protected ICompletionProposal[] computeValue(IDocument doc, int startOffset, int offset) throws BadLocationException {
		System.out.println("Compute value");
		String value = doc.get(startOffset, offset - startOffset);
		int lineNum = doc.getLineOfOffset(startOffset) - 1;
		while(value.indexOf(':') == -1) {
			int startLine = doc.getLineOffset(lineNum);
			value = doc.get(startLine, offset-startLine);
			lineNum--;
		}
				
		if (value.startsWith(Constants.IMPORT_PACKAGE))
			return handleImportPackageCompletion(value.substring(Constants.IMPORT_PACKAGE.length()), offset);
		if (value.startsWith(Constants.FRAGMENT_HOST))
			return handleFragmentHostCompletion(value.substring(Constants.FRAGMENT_HOST.length()), offset);
		if (value.startsWith(Constants.REQUIRE_BUNDLE))
			return handleRequireBundleCompletion(value.substring(Constants.REQUIRE_BUNDLE.length()), offset);
		if (value.startsWith(Constants.EXPORT_PACKAGE))
			return handleExportPackageCompletion(value.substring(Constants.EXPORT_PACKAGE.length()), offset);
		return new ICompletionProposal[0];
	}
	
	protected ICompletionProposal[] handleImportPackageCompletion(String currentValue, int offset) {
		int comma = currentValue.lastIndexOf(',');
		int semicolon = currentValue.lastIndexOf(';');
		if (comma > semicolon || comma == semicolon) {
			String value = comma != -1 ? currentValue.substring(comma + 1) : currentValue.substring(currentValue.indexOf(':') + 1);
			value = value.trim();
			int length = value.length();
			ArrayList completions = new ArrayList();
			ExportPackageDescription[] desc = PDECore.getDefault().getModelManager().getState().getState().getExportedPackages();
			for (int i = 0; i < desc.length; i++) 
				if (desc[i].getName().regionMatches(true, 0, value, 0, length))
					completions.add(new ManifestCompletionProposal(desc[i].getName(), offset- length, offset));
			return (ICompletionProposal[]) completions.toArray(new ICompletionProposal[completions.size()]);
					
		}
		String value = currentValue.substring(semicolon + 1).trim();
		if (Constants.VERSION_ATTRIBUTE.regionMatches(true, 0, value, 0, value.length()))
			return new ICompletionProposal[] {new ManifestCompletionProposal(Constants.VERSION_ATTRIBUTE + ":=", offset - value.length(), offset)};
		return new ICompletionProposal[0];
	}
	
	protected ICompletionProposal[] handleFragmentHostCompletion(String currentValue, int offset) {
		int colon = currentValue.indexOf(':');
		if (colon != -1) {
			ArrayList completions = new ArrayList();
			String pluginStart = currentValue.substring(colon + 1).trim();
			int length = pluginStart.length();
			IPluginModelBase [] bases = PDECore.getDefault().getModelManager().getPlugins();
			for (int i = 0; i < bases.length; i++) {
				if (bases[i].getBundleDescription().getHost() == null) {
					String pluginID = bases[i].getBundleDescription().getSymbolicName();
					if (pluginID.regionMatches(true, 0, pluginStart, 0, length))
						completions.add(new ManifestCompletionProposal(pluginID, offset - length, offset));
				}
			}
			return (ICompletionProposal[]) completions.toArray(new ICompletionProposal[completions.size()]);
		}
		return new ICompletionProposal[0];
	}
	
	protected ICompletionProposal[] handleRequireBundleCompletion(String currentValue, int offset) {
		int comma = currentValue.lastIndexOf(',');
		int semicolon = currentValue.lastIndexOf(';');
		ArrayList completions = new ArrayList();
		if (comma > semicolon || comma == semicolon) {
			String value = comma != -1 ? currentValue.substring(comma + 1) : currentValue.substring(currentValue.indexOf(':') + 1);
			value = value.trim();
			int length = value.length();
			BundleDescription [] descs = PDECore.getDefault().getModelManager().getState().getState().getResolvedBundles();
			for (int i = 0; i < descs.length; i++) {
				if (descs[i].getHost() == null && descs[i].getSymbolicName().regionMatches(true, 0, value, 0, value.length()))
					completions.add(new ManifestCompletionProposal(descs[i].getSymbolicName(), offset - length, offset));
			}
		} else {
			String value = currentValue.substring(semicolon + 1).trim();
			if (Constants.VERSION_ATTRIBUTE.regionMatches(true, 0, value, 0, value.length()))
				completions.add(new ManifestCompletionProposal(Constants.VERSION_ATTRIBUTE + ":=", offset - value.length(), offset));
			else if (Constants.VISIBILITY_DIRECTIVE.regionMatches(true, 0, value, 0, value.length()))
				completions.add(new ManifestCompletionProposal(Constants.VISIBILITY_DIRECTIVE + ":=", offset - value.length(), offset));
		}
		return (ICompletionProposal[]) completions.toArray(new ICompletionProposal[completions.size()]);
	}
	
	protected ICompletionProposal[] handleExportPackageCompletion(String currentValue, int offset) {
		int comma = currentValue.lastIndexOf(',');
		int semicolon = currentValue.lastIndexOf(';');
		HashMap map = new HashMap();
		if (comma > semicolon || comma == semicolon) {
			String value = comma != -1 ? currentValue.substring(comma + 1) : currentValue.substring(currentValue.indexOf(':') + 1);
			value = value.trim();
			int length = value.length();
			IProject proj = ((PDEFormEditor)fSourcePage.getEditor()).getCommonProject();
			if (proj != null) {
				IJavaProject jp = JavaCore.create(proj);
				try {
					IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
					for (int i = 0; i < roots.length; i++) {
						if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE
								|| proj.equals(roots[i].getCorrespondingResource())
								|| (roots[i].isArchive() && !roots[i].isExternal())) {
							IJavaElement[] children = roots[i].getChildren();
							for (int j = 0; j < children.length; j++) {
								IPackageFragment fragment = (IPackageFragment)children[j];
								String name = fragment.getElementName();
								if (fragment.hasChildren()) {
									if (!name.equals("java") || !name.startsWith("java.") /*|| allowJava)*/ //$NON-NLS-1$ //$NON-NLS-2$
											&& (name.regionMatches(true, 0, value, 0, length)))
										map.put(name, 
												new ManifestCompletionProposal(name, offset - length, offset));
								}
							}
						}
					}
				} catch (JavaModelException e) {
				}
			}
		} else {
			String value = currentValue.substring(semicolon + 1).trim();
			if (Constants.VERSION_ATTRIBUTE.regionMatches(true, 0, value, 0, value.length()))
				map.put(Constants.VERSION_ATTRIBUTE, new ManifestCompletionProposal(Constants.VERSION_ATTRIBUTE + ":=", offset - value.length(), offset));
			else if (Constants.VISIBILITY_DIRECTIVE.regionMatches(true, 0, value, 0, value.length()))
				map.put(Constants.VERSION_ATTRIBUTE, new ManifestCompletionProposal(Constants.VISIBILITY_DIRECTIVE + ":=", offset - value.length(), offset));
		}
		return map.size() > 0 ? (ICompletionProposal[]) map.values().toArray(new ICompletionProposal[map.size()]) :
			new ICompletionProposal[0];
	}
	
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getErrorMessage() {
		return null;
	}

}
