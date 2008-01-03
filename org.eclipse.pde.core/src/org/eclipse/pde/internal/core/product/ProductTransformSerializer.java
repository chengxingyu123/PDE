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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.iproduct.IProductTransform;
import org.eclipse.pde.internal.core.iproduct.IProductTransformSerializer;

/**
 * 
 */
public class ProductTransformSerializer implements IProductTransformSerializer {

	private IFolder fFolder;
	private CustomizationInfo fCustomizationInfo;

	/**
	 * @param customizationInfo
	 * @param folder
	 */
	public ProductTransformSerializer(CustomizationInfo customizationInfo,
			IFolder folder) {
		fFolder = folder;
		fCustomizationInfo = customizationInfo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.iproduct.IProductTransformSerializer#serialize()
	 */
	public void serialize(IProgressMonitor monitor) {
		IProductTransform[] allTransforms = fCustomizationInfo.getTransforms();
		// you can complicate this logic in the future for more types of
		// transforms. For now we know they're all going to be XSLT removals.
		

		monitor.setTaskName("Saving transformations");
		
		Map transformsForBundle = new HashMap();
		// sort the transforms by bundle.
		for (int i = 0; i < allTransforms.length; i++) {
			IProductTransform productTransform = allTransforms[i];
			Object transformed = productTransform.getTransformedObject();
			if (transformed instanceof IPluginObject) {
				IPluginObject element = (IPluginObject) transformed;
				String pluginId = element.getPluginBase().getId();
				List transforms = (List) transformsForBundle.get(pluginId);
				if (transforms == null) {
					transforms = new ArrayList();
					transformsForBundle.put(pluginId, transforms);
				}
				transforms.add(productTransform);
			}
		}
		
		serializeCSV(transformsForBundle, monitor);
		
		serializeXSLTs(transformsForBundle, monitor);
		monitor.worked(1);
	}

	/**
	 * @param transformsForBundle
	 * @param monitor 
	 */
	private void serializeXSLTs(Map transformsForBundle, IProgressMonitor monitor) {
		monitor.subTask("Saving XSLT transforms");
		Set touchedFiles =  new HashSet();
		for (Iterator i = transformsForBundle.entrySet().iterator(); i.hasNext();) {
			Map.Entry entry = (Map.Entry) i.next();
			IFile xsltFile = fFolder.getFile(xmlFilename((String) entry.getKey()));
			touchedFiles.add(xsltFile);
			List transforms = (List) entry.getValue();
			serializeXSLT(xsltFile, transforms);
		}
		try {
			Set allFiles = new HashSet();
			IResource [] members  = fFolder.members();
			for (int i = 0; i < members.length; i++) {
				IResource resource = members[i];
				if (resource.getType() == IResource.FILE) {
					if ("xml".equals(((IFile)resource).getFileExtension())) 
						allFiles.add(resource);
				}
			}
			allFiles.removeAll(touchedFiles);
			for (Iterator i = allFiles.iterator(); i.hasNext();) {
				IFile xsltFile = (IFile) i.next();
				serializeXSLT(xsltFile, Collections.EMPTY_LIST);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param xsltFile
	 * @param transforms
	 */
	private void serializeXSLT(IFile xsltFile, List transforms) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(baos);
		BufferedReader reader = null;
		
		String indent = "    ";
		
		if (xsltFile.exists()) {
			// read it until we hit our marker
			try {
				reader = new BufferedReader(new InputStreamReader(xsltFile.getContents()));
			} catch (CoreException e) {
				e.printStackTrace();
				return;
			}
			String line = null;
			try {
				while((line = reader.readLine()) != null) {
					if (line.indexOf("<?pde-autogen-start?>") == -1)
						writer.println(line);
					else
						break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		else {
			writer.println("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">");
		}
		
		writer.println(indent + "<?pde-autogen-start?>");
		for (Iterator i = transforms.iterator(); i.hasNext();) {
			RemoveTransform transform = (RemoveTransform) i.next();
			String xpath = ProductTransformFactory.toXPath(transform.getTransformedObject(), false);
			writer.print(indent + "<xsl:template match=\"");
			writer.print("/plugin" + xpath);
			writer.println("\" />");
		}		
		writer.println(indent + "<?pde-autogen-end?>");		
		
		if (xsltFile.exists()) {
			String line = null;
			boolean foundEndToken = false;
			try {
				while((line = reader.readLine()) != null) {
					if (line.indexOf("<?pde-autogen-end?>") != -1)
						foundEndToken = true;
					else if (foundEndToken)
						writer.println(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		else {
			writer.println();
			writer.println(indent + "<xsl:template match=\"node()|@*\">");
			writer.println(indent + "<xsl:copy>");
			writer.println(indent + "<xsl:apply-templates select=\"node()|@*\"/>");
	        writer.println(indent + "</xsl:copy>");
	        writer.println(indent + "</xsl:template>");
			writer.println("</xsl:stylesheet>");
		}
		
		writer.close();
		try {
			if (xsltFile.exists())
				xsltFile.setContents(new ByteArrayInputStream(baos.toByteArray()), true, true, new NullProgressMonitor());
			else
				xsltFile.create(new ByteArrayInputStream(baos.toByteArray()), true, new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param transformsForBundle
	 * @param monitor 
	 */
	private void serializeCSV(Map transformsForBundle, IProgressMonitor monitor) {
		monitor.subTask("Saving CSV map");
		IFile csv = fFolder.getFile("xslt_transforms.csv");
		BufferedReader reader = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(baos);
		if (csv.exists()) {
			// read it until we hit our marker
			try {
				reader = new BufferedReader(new InputStreamReader(csv.getContents()));
			} catch (CoreException e) {
				e.printStackTrace();
				return;
			}
			String line = null;
			try {
				while((line = reader.readLine()) != null) {
					if (line.indexOf("pde autogen start") == -1)
						writer.println(line);
					else
						break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		else {
			writer.println("#  This file is autogenerated by PDE. Any edits to this file should not occur between the 'pde autogen' comments below.");
			writer.println();
		}
		
		writer.println("# pde autogen start - do not remove");
		for (Iterator i = transformsForBundle.keySet().iterator(); i.hasNext();) {
			String pluginId = (String) i.next();
			writer.print(regexEscape(pluginId));
			writer.print(',');
			writer.print("plugin\\.xml");
			writer.print(',');
			writer.println("/" + fFolder.getName() + "/" + xmlFilename(pluginId));
		}
		writer.println("# pde autogen end - do not remove");
		
		if (csv.exists()) {
			String line = null;
			boolean foundEndToken = false;
			try {
				while((line = reader.readLine()) != null) {
					if (line.indexOf("pde autogen end") != -1)
						foundEndToken = true;
					else if (foundEndToken)
						writer.println(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			// read it until we hit our end marker then spit out the rest to the stream
		}
		writer.close();
		try {
			if (csv.exists())
				csv.setContents(new ByteArrayInputStream(baos.toByteArray()), true, true, new NullProgressMonitor());
			else
				csv.create(new ByteArrayInputStream(baos.toByteArray()), true, new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param pluginId
	 * @return
	 */
	private String xmlFilename(String pluginId) {
		return pluginId.replaceAll("\\.", "_") + ".xml";
	}

	/**
	 * @param pluginId
	 * @return
	 */
	private String regexEscape(String pluginId) {
		return pluginId.replaceAll("\\.", "\\\\\\.");
	}

}
