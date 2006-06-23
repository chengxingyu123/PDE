package org.eclipse.pde.internal.ui.editor.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.util.PDEJavaHelper;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class ManifestContentAssitProcessor implements IContentAssistProcessor, ICompletionListener{
	
	protected PDESourcePage fSourcePage;
	
	private static final String[] fHeader = {
		Constants.BUNDLE_ACTIVATOR,
		Constants.BUNDLE_CATEGORY,
		Constants.BUNDLE_CLASSPATH,
		Constants.BUNDLE_CONTACTADDRESS,
		Constants.BUNDLE_COPYRIGHT,
		Constants.BUNDLE_DESCRIPTION,
		Constants.BUNDLE_DOCURL,
		Constants.BUNDLE_LOCALIZATION,
		Constants.BUNDLE_MANIFESTVERSION,
		Constants.BUNDLE_NAME,
		Constants.BUNDLE_NATIVECODE,
		Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT,
		Constants.BUNDLE_SYMBOLICNAME,
		Constants.BUNDLE_UPDATELOCATION,
		Constants.BUNDLE_VENDOR,
		Constants.BUNDLE_VERSION,
		Constants.DYNAMICIMPORT_PACKAGE,
		Constants.EXPORT_PACKAGE,
		Constants.EXPORT_SERVICE,
		Constants.IMPORT_PACKAGE,
		Constants.IMPORT_SERVICE,
		Constants.REQUIRE_BUNDLE,
		Constants.FRAGMENT_HOST 
	};
	
	HashMap fHeaders;
		
	public ManifestContentAssitProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		IDocument doc = fSourcePage.getDocumentProvider().getDocument(fSourcePage.getInputContext().getInput());
		if (fHeaders == null) {
			parseDocument(doc);
		}
		try {
			int lineNum = doc.getLineOfOffset(offset);
			int lineStart = doc.getLineOffset(lineNum);
			return computeCompletionProposals(doc, lineStart, offset);
		} catch (BadLocationException e) {
		}
		return null;
	}
	
	protected final void parseDocument(IDocument doc) {
		fHeaders = new HashMap();
		int numLines = doc.getNumberOfLines();
		int offset = 0;
		for (int i = 0; i < numLines; i++) {
			try {
				IRegion line = doc.getLineInformation(i);
				String value = 	doc.get(offset, line.getOffset() + line.getLength() - offset);
				if (value.indexOf(':') != value.lastIndexOf(':')|| i == (numLines - 1)) {
					value = doc.get(offset, line.getOffset() - offset - 1).trim();
					int index = value.indexOf(':');
					String header = value.substring(0, index);
					try {
						if (value.endsWith(","))
							value = value.substring(0, value.length() - 1);
						ManifestElement[] elems = ManifestElement.parseHeader(header, value.substring(index + 1));
						if (shouldStoreSet(header)) {
							HashSet set = new HashSet((4/3) * elems.length + 1);
							for (int j = 0; j < elems.length; j++) 
								set.add(elems[j].getValue());
							fHeaders.put(header, set);
						} else 
							fHeaders.put(header, elems);
					} catch (BundleException e) {
						System.err.println(header);
					}
					offset = line.getOffset();
				}
			}  catch (BadLocationException e) {
			}
		}
	}
	
	protected final boolean shouldStoreSet(String header) {
		return header.equalsIgnoreCase(Constants.IMPORT_PACKAGE) || header.equalsIgnoreCase(Constants.EXPORT_PACKAGE) ||
			header.equalsIgnoreCase(Constants.REQUIRE_BUNDLE);
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
				return ch != ',' && ch != ':';
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
			if (fHeader[i].regionMatches(true, 0, currentValue, 0, currentValue.length()) && fHeaders.get(fHeader[i]) == null)
				completions.add(new ManifestCompletionProposal(fHeader[i], startOffset, offset, ManifestCompletionProposal.TYPE_HEADER));
		}
		return (ICompletionProposal[]) completions.toArray(new ICompletionProposal[completions.size()]);
	}
	
	protected ICompletionProposal[] computeValue(IDocument doc, int startOffset, int offset) throws BadLocationException {
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
		if (value.startsWith(Constants.BUNDLE_ACTIVATOR))
			return handleBundleActivatorCompletion(value.substring(Constants.BUNDLE_ACTIVATOR.length() + 1), offset);
		return new ICompletionProposal[0];
	}
	
	protected ICompletionProposal[] handleImportPackageCompletion(String currentValue, int offset) {
		int comma = currentValue.lastIndexOf(',');
		int semicolon = currentValue.lastIndexOf(';');
		if (comma > semicolon || comma == semicolon) {
			HashSet set = (HashSet) fHeaders.get(Constants.IMPORT_PACKAGE);
			if (set == null) set = new HashSet(0);
			String value = comma != -1 ? currentValue.substring(comma + 1) : currentValue.substring(currentValue.indexOf(':') + 1);
			value = value.trim();
			int length = value.length();
			set.remove(value);
			ArrayList completions = new ArrayList();
			ExportPackageDescription[] desc = PDECore.getDefault().getModelManager().getState().getState().getExportedPackages();
			for (int i = 0; i < desc.length; i++) {
				String pkgName = desc[i].getName();
				if (pkgName.regionMatches(true, 0, value, 0, length) && !set.contains(pkgName))
					completions.add(new ManifestCompletionProposal(pkgName, offset- length, offset, ManifestCompletionProposal.TYPE_PACKAGE));
			}
			return (ICompletionProposal[]) completions.toArray(new ICompletionProposal[completions.size()]);
					
		}
		String value = currentValue.substring(semicolon + 1).trim();
		if (Constants.VERSION_ATTRIBUTE.regionMatches(true, 0, value, 0, value.length()))
			return new ICompletionProposal[] {new ManifestCompletionProposal(Constants.VERSION_ATTRIBUTE, offset - value.length(), offset, ManifestCompletionProposal.TYPE_ATTRIBUTE)};
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
						completions.add(new ManifestCompletionProposal(pluginID, offset - length, offset, ManifestCompletionProposal.TYPE_BUNDLE));
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
			HashSet set = (HashSet) fHeaders.get(Constants.REQUIRE_BUNDLE);
			if (set == null) set = new HashSet(0);
			String value = comma != -1 ? currentValue.substring(comma + 1) : currentValue.substring(currentValue.indexOf(':') + 1);
			value = value.trim();
			int length = value.length();
			set.remove(value);
			BundleDescription [] descs = PDECore.getDefault().getModelManager().getState().getState().getResolvedBundles();
			for (int i = 0; i < descs.length; i++) {
				String bundleId = descs[i].getSymbolicName();
				if (descs[i].getHost() == null && bundleId.regionMatches(true, 0, value, 0, value.length()) && 
						!set.contains(bundleId))
					completions.add(new ManifestCompletionProposal(bundleId, offset - length, offset, ManifestCompletionProposal.TYPE_BUNDLE));
			}
		} else {
			String value = currentValue.substring(semicolon + 1).trim();
			if (Constants.VERSION_ATTRIBUTE.regionMatches(true, 0, value, 0, value.length()))
				completions.add(new ManifestCompletionProposal(Constants.VERSION_ATTRIBUTE, offset - value.length(), offset, ManifestCompletionProposal.TYPE_ATTRIBUTE));
			else if (Constants.VISIBILITY_DIRECTIVE.regionMatches(true, 0, value, 0, value.length()))
				completions.add(new ManifestCompletionProposal(Constants.VISIBILITY_DIRECTIVE, offset - value.length(), offset, ManifestCompletionProposal.TYPE_DIRECTIVE));
		}
		return (ICompletionProposal[]) completions.toArray(new ICompletionProposal[completions.size()]);
	}
	
	protected ICompletionProposal[] handleExportPackageCompletion(String currentValue, int offset) {
		int comma = currentValue.lastIndexOf(',');
		int semicolon = currentValue.lastIndexOf(';');
		HashMap map = new HashMap();
		if (comma > semicolon || comma == semicolon) {
			HashSet set = (HashSet) fHeaders.get(Constants.EXPORT_PACKAGE);
			if (set == null) set = new HashSet(0);
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
												new ManifestCompletionProposal(name , offset - length, offset, ManifestCompletionProposal.TYPE_PACKAGE));
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
				map.put(Constants.VERSION_ATTRIBUTE, new ManifestCompletionProposal(Constants.VERSION_ATTRIBUTE, offset - value.length(), offset, ManifestCompletionProposal.TYPE_ATTRIBUTE));
			else if (Constants.VISIBILITY_DIRECTIVE.regionMatches(true, 0, value, 0, value.length()))
				map.put(Constants.VERSION_ATTRIBUTE, new ManifestCompletionProposal(Constants.VISIBILITY_DIRECTIVE, offset - value.length(), offset, ManifestCompletionProposal.TYPE_DIRECTIVE));
		}
		return map.size() > 0 ? (ICompletionProposal[]) map.values().toArray(new ICompletionProposal[map.size()]) :
			new ICompletionProposal[0];
	}
	
	protected ICompletionProposal[] handleBundleActivatorCompletion(final String currentValue, final int offset) {
		char[] valueArray = currentValue.toCharArray();
		int i = 0;
		for (; i < valueArray.length; i++) 
			if (!Character.isWhitespace(valueArray[i])) {
				break;
			}
		final int firstChar = i;
		
		int index = currentValue.lastIndexOf('.');
		String pkg = null, cls = null;
		if (index == -1) 
			cls = (firstChar != 0) ? currentValue.substring(firstChar) : currentValue;
		else {
			if (Character.isUpperCase(currentValue.charAt(index))) {
				pkg = currentValue.substring(firstChar, index);
				cls = currentValue.substring(index+1);
			} else {
				pkg = (firstChar != 0) ? currentValue.substring(firstChar) : currentValue;
				pkg.concat("*");
				cls = "*";
			}
		}
		final ArrayList completions = new ArrayList();
		IProject proj = ((PDEFormEditor)fSourcePage.getEditor()).getCommonProject();
		if (proj != null) {
			TypeNameRequestor req = new TypeNameRequestor() {
				public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) { 
					String fullName = new String(packageName).concat(".").concat(new String(simpleTypeName));
					completions.add(new ManifestCompletionProposal(fullName, offset - currentValue.length() + firstChar, offset, ManifestCompletionProposal.TYPE_CLASS));
				}
			};
			try {
				new SearchEngine().searchAllTypeNames(
						(pkg == null) ? null : pkg.toCharArray(),
						(cls == null) ? null : cls.toCharArray(),
						(cls.charAt(0) == '*') ? SearchPattern.R_PATTERN_MATCH : SearchPattern.R_PREFIX_MATCH,
						IJavaSearchConstants.CLASS,
						PDEJavaHelper.getSearchScope(proj),
						req,
						IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
						null);
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return (ICompletionProposal[]) completions.toArray(new ICompletionProposal[completions.size()]);
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
		return null;
	}

	public String getErrorMessage() {
		return null;
	}

	public void assistSessionEnded(ContentAssistEvent event) {
		fHeaders = null;
	}

	public void assistSessionStarted(ContentAssistEvent event) {
	}

	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
	}

}
