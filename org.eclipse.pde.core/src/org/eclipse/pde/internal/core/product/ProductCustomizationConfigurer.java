/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.iproduct.IProductCustomizationConfigurer;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * 
 */
public class ProductCustomizationConfigurer implements
		IProductCustomizationConfigurer {

	/**
	 * 
	 */
	private static final String SOURCE_REMOVE_SERVICE_REFERENCE = "//pde-autogen-start - do not delete\n    if (transformServiceReference != null) transformServiceReference.unregister();\n//pde-autogen-end\n";
	/**
	 * 
	 */
	private static final String SOURCE_CREATE_SERVICE_REFERENCE = "//pde-autogen-start - do not delete\n    transformServiceReference = CSVTransformingBundleFile.register(context, context.getBundle().getEntry(\"/transforms/xslt_transforms.csv\"), XSLTTransformingBundleFileWrapperFactoryHook.class.getName());\n//pde-autogen-end\n";

	private IBundlePluginModel bundleModel;

	/**
	 * @param bundleModel
	 */
	public ProductCustomizationConfigurer(IBundlePluginModel bundleModel) {
		this.bundleModel = bundleModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.iproduct.IProductCustomizationConfigurer#configureBundle()
	 */
	public void configureBundle(IProgressMonitor monitor) throws CoreException {
		configureDependencies(monitor);
		configureActivator(monitor);
	}

	private void configureDependencies(IProgressMonitor monitor) throws CoreException {
		addDependency("system.bundle");
		addDependency("org.eclipse.equinox.transforms.xslt");
		bundleModel.save();
	}

	/**
	 * @param targetId
	 * @throws CoreException 
	 */
	private void addDependency(String targetId) throws CoreException {
		IPluginImport [] imports = bundleModel.getPluginBase().getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport pluginImport = imports[i];
			if (pluginImport.getId().equals(targetId))
				return; // we have found it
		}

		IPluginBase model = bundleModel.createPluginBase();
		IPluginImport importNode = bundleModel.createImport();
		importNode.setId(targetId);
		model.add(importNode);
	}

	private void configureActivator(IProgressMonitor monitor,
			IType activatorType, String methodName, String methodBody)
			throws CoreException {

		String error = MessageFormat
				.format(
						"Unable to parse {0}(BundleContext) in activator for transform bundle.",
						new Object[] { methodName });
		IMethod method = findMethod(activatorType, methodName,
				"org.eclipse.osgi", "BundleContext");
		if (method == null || !method.exists()) {
			String methodSignature = MessageFormat.format(
					"public void {0} (BundleContext context) throws Exception {{",
					new Object[] { methodName });
			activatorType.getCompilationUnit().createImport(
					"org.osgi.framework.BundleContext", null, monitor);
			StringBuffer contents = new StringBuffer();
			contents.append(methodSignature);
			contents.append('\n');
			contents.append(methodBody);
			contents.append("}");
			activatorType.createMethod(contents.toString(), null, false,
					monitor);
		} else {
			String existingSource = method.getSource();
			StringBuffer buffer = new StringBuffer();
			BufferedReader reader = new BufferedReader(new StringReader(
					existingSource));
			String line = null;
			boolean foundStartToken = false;
			try {
				while ((line = reader.readLine()) != null) {
					if (line.indexOf("pde-autogen-start") == -1)
						buffer.append(line).append('\n');
					else {
						foundStartToken = true;
						break;
					}
				}
				if (foundStartToken)  // good news - we found our token
					buffer.append(methodBody);					
				else { // we have a virgin method here. We've exhausted the
						// buffer without finding our token. This means we've
						// actually closed the method body. We need to go back
						// and find the right spot.
					
					int insertIndex = buffer.length();
					while (insertIndex >= 0) {
						insertIndex--;
						if (buffer.charAt(insertIndex) == '}') {
							insertIndex--;
							break;
						}
					}
					if (insertIndex < 0) // we didn't find a brace either?
						createCoreException(error);
					else 
						buffer.insert(insertIndex, methodBody);
				}
				boolean foundEndToken = false;

				while ((line = reader.readLine()) != null) {
					if (line.indexOf("pde-autogen-end") != -1)
						foundEndToken = true;
					else if (foundEndToken)
						buffer.append(line).append('\n');
				}
				IDocument document = new Document(buffer.toString());
				TextEdit methodText = ToolFactory.createCodeFormatter(JavaCore.getDefaultOptions()).format(CodeFormatter.K_UNKNOWN, buffer.toString(), 0, buffer.length(), 0, null);
				try {
					if (methodText != null) methodText.apply(document);
				} catch (MalformedTreeException e) {
				} catch (BadLocationException e) {
				}
				
				method.delete(true, monitor);
				method = activatorType.createMethod(document.get(), null, true, monitor);
			} catch (IOException e) {
				throw createCoreException(error);
			
			}
		}
	}

	/**
	 * @param monitor
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private void configureActivator(IProgressMonitor monitor)
			throws CoreException, JavaModelException {
		IProject project = bundleModel.getUnderlyingResource().getProject();
		String activatorName = bundleModel.getPlugin().getClassName();
		if (activatorName == null) {
			// TODO create the activator in this (rarish) case
			throw createCoreException("Transforms cannot be applied to a bundle that does not have an activator.");
		}

		IJavaProject jProject = JavaCore.create(project);
		IType activatorType = jProject.findType(activatorName);
		ICompilationUnit compilationUnit = activatorType.getCompilationUnit();
		compilationUnit.becomeWorkingCopy(monitor);
		compilationUnit.getType(activatorName);
		
		IField field = activatorType.getField("transformServiceReference");
		compilationUnit.createImport(
				"org.osgi.framework.ServiceRegistration", null, monitor);
		compilationUnit.createImport(
				"org.eclipse.equinox.transforms.CSVTransformingBundleFile",
				null, monitor);
		compilationUnit
		.createImport(
				"org.eclipse.equinox.transforms.xslt.XSLTTransformingBundleFileWrapperFactoryHook",
				null, monitor);
		if (field == null || !field.exists()) {

			field = activatorType.createField(
					"private ServiceRegistration transformServiceReference;",
					null, false, monitor);
		}
		configureActivator(monitor, activatorType, "start",
				SOURCE_CREATE_SERVICE_REFERENCE);

		configureActivator(monitor, activatorType, "stop",
				SOURCE_REMOVE_SERVICE_REFERENCE);
		
		compilationUnit.commitWorkingCopy(true, monitor);
	}

	private IMethod findMethod(IType type, String methodName,
			String packageName, String className) {
		IMethod method = type.getMethod(methodName, new String[] { Signature
				.createTypeSignature(packageName + "." + className, true) });
		if (method == null || !method.exists())
			method = type.getMethod(methodName, new String[] { Signature
					.createTypeSignature(className, false) });
		return method;
	}

	protected CoreException createCoreException(String message) {
		IStatus status = new Status(IStatus.ERROR,
				"org.eclipse.pde.ui", IStatus.ERROR, message, null); //$NON-NLS-1$
		return new CoreException(status);
	}
}
