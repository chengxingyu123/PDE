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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductTransform;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;
import org.w3c.dom.Node;

/**
 * 
 */
public class ProductTransformFactory {

	/**
	 * @param child
	 * @return
	 */
	public static IProductTransform parse(IProductModel model, Node child) {
		if ("remove".equals(child.getNodeName())) {
			IProductTransform transform = new RemoveTransform(model);
			transform.parse(child);
			return transform;
		}
		return null;
	}

	/**
	 * @param transformedObject
	 * @return
	 */
	public static String toXPath(Object transformedObject, boolean includePlugin) {
		return toXPath(new StringBuffer(), transformedObject, includePlugin);
	}

	private static String toXPath(StringBuffer buffer, Object transformedObject, boolean includePlugin) {
		if (transformedObject instanceof IPluginObject) {

			IPluginObject pluginObject = (IPluginObject) transformedObject;
			if (pluginObject.getParent() != null)
				buffer.append(toXPath(pluginObject
								.getParent(), includePlugin));
			

			if (includePlugin && transformedObject instanceof IPlugin) {
				buffer.append('/');
				IPlugin plugin = (IPlugin) transformedObject;
				buffer.append(plugin.getId());
			} else if (transformedObject instanceof IPluginExtension) {
				buffer.append('/');
				IPluginExtension extension = (IPluginExtension) transformedObject;
				buffer.append("extension[@point='");
				buffer.append(extension.getPoint());

				// buffer.append(((IExtensions) extension.getParent())
				// .getIndexOf(extension)); // fragile - any change in
				// // order will break this
				// // transform
				buffer.append("']");
			} else if (transformedObject instanceof IPluginElement) {
				buffer.append('/');
				IPluginElement element = (IPluginElement) transformedObject;
				buffer.append(PDEXMLHelper.getWritableAttributeString(element
						.getName()));
				IPluginAttribute[] attributes = element.getAttributes();
				if (attributes != null && attributes.length > 0) {
					buffer.append('[');
					for (int i = 0; i < attributes.length; i++) {
						buffer.append('@');
						buffer.append(attributes[i].getName());
						buffer.append("='");
						buffer.append(PDEXMLHelper
								.getWritableAttributeString(attributes[i]
										.getValue()));
						if (i != (attributes.length - 1))
							buffer.append("' and ");
					}
					buffer.append("']");
				}
			}

		}
		return buffer.toString();
	}

	/**
	 * @param buffer
	 * @param name //
	 */
	// private static void appendKeyPart(StringBuffer buffer, String name) {
	// if (name == null)
	// buffer.append("null");
	// else
	// buffer.append(PDEXMLHelper.getWritableAttributeString(name));
	// buffer.append(' ');
	// }
	/**
	 * @param nodeValue
	 * @return
	 */
	public static Object fromXPath(String nodeValue) {
		IPluginBase bundle = null;
		String [] tokens = tokenizeXPath(nodeValue);
		if (tokens.length == 0)
			return null;
		Object model = PDECore.getDefault().getModelManager().findModel(
				tokens[0]);
		if (!(model instanceof IPluginModel))
			return null;
		IPluginModel pluginModel = (IPluginModel) model;
		bundle = pluginModel.getPluginBase();
		if (bundle == null)
			return null;
		return fromXPath(tokens, 1, bundle);
	}

	/**
	 * @param tokenizer
	 * @param bundle
	 * @return
	 */
	private static Object fromXPath(String [] tokens,
			int tokenIndex,
			IPluginObject parentModel) {
		if (tokenIndex >= tokens.length)
			return parentModel;

		String token = tokens[tokenIndex];
		if (parentModel instanceof IPlugin) {
			IPluginBase plugin = (IPluginBase) parentModel;
			Pattern pattern = Pattern
					.compile("extension\\[@point=\\'(\\p{Graph}*)\\'\\]");
			Matcher matcher = pattern.matcher(token);
			if (matcher.matches()) {
				String matchedGroup = matcher.group(1);
				IPluginExtension[] extensions = plugin.getExtensions();
				for (int i = 0; i < extensions.length; i++) {
					// we could fork here = copy the tokenizer at the current
					// position and then if the rest of the search fails we
					// could
					// retry
					if (extensions[i].getPoint().equals(matchedGroup))
						return fromXPath(tokens, tokenIndex + 1, extensions[i]);

				}
			}
		} else if (parentModel instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) parentModel;
			Pattern pattern = Pattern
					.compile("(\\w+)\\[(.*)\\]");
			Matcher matcher = pattern.matcher(token);
			if (matcher.matches()) {
				String tag = matcher.group(1);
				if (tag == null)
					return null;
				String attributes = matcher.group(2);
				Map attributeArray = splitAttributes(attributes);
				IPluginObject[] children = extension.getChildren();
				for (int i = 0; i < children.length; i++) {
					IPluginObject object = children[i];
					if (object instanceof IPluginElement) {
						IPluginElement element = (IPluginElement) object;
						if (elementMatches(tag, attributeArray, element))
							return fromXPath(tokens, tokenIndex + 1, element);
					}
				}
			}
		}
		return null;
	}

	/**
	 * @param tag
	 * @param attributeArray 
	 * @param element
	 * @return
	 */
	private static boolean elementMatches(String tag, Map attributeArray, IPluginElement element) {
		if (tag.equals(element.getName())) {
			if (attributeArray != null) {
				for (Iterator j = attributeArray.entrySet()
						.iterator(); j.hasNext();) {
					Map.Entry entry = (Map.Entry) j.next();
					IPluginAttribute attribute = element
							.getAttribute((String) entry
									.getKey());
					if (attribute == null)
						return false;
					if (!entry.getValue().equals(
							attribute.getValue()))
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * @param attributes
	 * @return
	 */
	private static Map splitAttributes(String attributes) {
		Map result = new HashMap();
		String [] attributePairs = attributes.split(" and ");
		Pattern pattern = Pattern.compile("@(\\w+)=\\'(.+)\\'");
		for (int i = 0; i < attributePairs.length; i++) {
			Matcher matcher = pattern.matcher(attributePairs[i]);
			if (matcher.matches() && matcher.groupCount() == 2) {
				String key = matcher.group(1);
				String value = matcher.group(2);
				result.put(key, value);
			}
		}
		
		return result;
	}
	
	public static String [] tokenizeXPath(String xpath) {
		ArrayList components = new ArrayList(5);
		boolean inQuotes = false;
		StringBuffer currentToken = new StringBuffer();
		for (int i = 0; i < xpath.length(); i++) {
			char charAt = xpath.charAt(i);
			if (!inQuotes && charAt == '/') {
				if (currentToken.length() > 0 ) {
					components.add(currentToken.toString());
					currentToken.setLength(0);
				}			
			}
			else {
				if (charAt == '\'')
					inQuotes = !inQuotes;

				currentToken.append(charAt);
			}
		}
		if (currentToken.length() >0)
			components.add(currentToken.toString());
		return (String[]) components.toArray(new String[components.size()]);
	}
}
