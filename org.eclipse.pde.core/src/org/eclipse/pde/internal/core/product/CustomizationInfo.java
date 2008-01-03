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

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.iproduct.ICustomizationInfo;
import org.eclipse.pde.internal.core.iproduct.IProductCustomizationConfigurer;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductTransform;
import org.eclipse.pde.internal.core.iproduct.IProductTransformSerializer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 */
public class CustomizationInfo extends ProductObject implements
		ICustomizationInfo {

	/**
	 * 
	 */
	private static final String INDENT = "   ";

	private static final long serialVersionUID = 1L;

	private boolean fUseCustomizations = false;
	private Set fTransforms = new HashSet();
	private String fTargetPlugin;

	/**
	 * @param model
	 */
	public CustomizationInfo(IProductModel model) {
		super(model);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		Node useNode = node.getAttributes().getNamedItem(P_USE);
		if (useNode != null) {
			String useValue = useNode.getNodeValue();
			fUseCustomizations = useValue != null
					&& "true".equals(useValue.toLowerCase());
		}
		Node projectNode = node.getAttributes().getNamedItem(P_TRANSFORM_HOST);
		if (projectNode != null) {			
			String projectValue = projectNode.getNodeValue();
			
			if (PDECore.getDefault().getModelManager().findEntry(projectValue) != null) {
				fTargetPlugin = projectValue;
			}
		}

		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node child = nodeList.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				IProductTransform transform = ProductTransformFactory.parse(
						getModel(), child);
				if (transform != null)
					fTransforms.add(transform);

			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String,
	 *      java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<customizationInfo ");
		writer.print(P_USE);
		writer.print("=\"");
		writer.print(Boolean.toString(fUseCustomizations));
		writer.print("\"");
		if (fTargetPlugin != null) {
			writer.print(' ');
			writer.print(P_TRANSFORM_HOST);
			writer.print("=\"");
			writer.print(fTargetPlugin);
			writer.print("\"");
		}
		writer.println(">");
		{
			String indent2 = indent + INDENT;
			for (Iterator i = fTransforms.iterator(); i.hasNext();) {
				IProductTransform entry = (IProductTransform) i.next();
				entry.write(indent2, writer);
			}
		}
		writer.print(indent);
		writer.print("</customizationInfo>");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.iproduct.ICustomizationInfo#getUseCustomizations()
	 */
	public boolean getUseCustomizations() {
		return fUseCustomizations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.iproduct.ICustomizationInfo#setUseCustomizations(boolean)
	 */
	public void setUseCustomizations(boolean b) {
		if (fUseCustomizations == b)
			return;

		fUseCustomizations = b;
		// if (isEditable()) {
		firePropertyChanged(P_USE, fUseCustomizations ? Boolean.FALSE
				: Boolean.TRUE, fUseCustomizations ? Boolean.TRUE
				: Boolean.FALSE);
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.iproduct.ICustomizationInfo#addTransform(java.lang.Object,
	 *      org.eclipse.pde.internal.core.iproduct.IProductTransform)
	 */
	public void addTransform(IProductTransform transform) {
		if (fTransforms.add(transform))
			fireStructureChanged(transform, IModelChangedEvent.INSERT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.iproduct.ICustomizationInfo#removeTransform(java.lang.Object)
	 */
	public void removeTransform(IProductTransform transform) {
		if (fTransforms.remove(transform))
			fireStructureChanged(transform, IModelChangedEvent.REMOVE);
	}

	public IProductTransform[] getTransforms() {
		return (IProductTransform[]) fTransforms
				.toArray(new IProductTransform[fTransforms.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.iproduct.ICustomizationInfo#getTargetProject()
	 */
	public String getTargetPlugin() {
		return fTargetPlugin;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.iproduct.ICustomizationInfo#setTargetProject(org.eclipse.core.resources.IProject)
	 */
	public void setTargetPlugin(String pluginId) {
		if (fTargetPlugin == null ? pluginId == null
				: ((pluginId != null) && fTargetPlugin.equals(pluginId)))
			return;

		String oldPlugin = fTargetPlugin;
		fTargetPlugin = pluginId;
		firePropertyChanged(P_TRANSFORM_HOST, oldPlugin, fTargetPlugin);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.ICustomizationInfo#createTransformSerializer(org.eclipse.core.resources.IFolder)
	 */
	public IProductTransformSerializer createTransformSerializer(
			IFolder folder) {
		return new ProductTransformSerializer(this, folder);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.ICustomizationInfo#createConfigurer(org.eclipse.pde.internal.core.ibundle.IBundlePluginModel)
	 */
	public IProductCustomizationConfigurer createConfigurer(
			IBundlePluginModel bundleModel) {
		return new ProductCustomizationConfigurer(bundleModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.ICustomizationInfo#getFrameworkExtensions()
	 */
	public String[] getFrameworkExtensions() {
		// hard coded for XSLT at the moment.
		return new String[] {"org.eclipse.equinox.transforms", "org.eclipse.equinox.transforms.xslt"}; //$NON-NLS-1$
	}
}
