/*******************************************************************************
 * Copyright (c) 2009 Anyware Technologies Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anyware Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.customattributes;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * <p><b>PROVISIONAL:</b> This API is subject to arbitrary change, including renaming or removal.</p>
 * The UI representation of a custom extension point attribute editor
 * @since 3.6
 */
public interface ICustomAttributeEditor {
	/**
	 * Creates the SWT controls for this custom attribute editor.
	 * <p>
	 * Clients should not call this method directly.
	 * </p>
	 * <p>
	 * For implementors this is a multi-step process:
	 * <ol>
	 *   <li>Create one or more controls within the parent.</li>
	 *   <li>Register listeners on the created controls which call {@link IExtensionAttributeCallback#markDirty()} on the <code>callback</code> parameter if needed.</li>
	 *   <li>Set the parent layout as needed.</li>
	 * </ol>
	 * </p>
	 *
	 * @param parent the parent control
	 * @param toolkit the form toolkit that can be used as a widget factory
	 * @param listener the listener to notify when the editor value is updated. In 3.5 it is fine to perform a
	 * <code>listener.propertyChange(null)</code> since we don't support property other than the "value" itself
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 3.6
	 */
	void createContents(Composite parent, FormToolkit toolkit, IPropertyChangeListener listener);

	/**
	 * Returns the main SWT control of the custom attribute editor (this control automatically gains focus when the editor becomes active)
	 * @return the SWT control 
	 * @since 3.6
	 */
	Control getMainControl();

	/**
	 * Sets the "editable" state of the editor
	 * @param editable The editor will be enabled if <code>true</code>, and disabled otherwise 
	 * @since 3.6
	 */
	void setEditable(boolean editable);

	/**
	 * Sets the editor's value
	 * @param value the new value of the editor
	 * @since 3.6
	 */
	void setValue(String value);

	/**
	 * Returns the editor's current value
	 * @return the editor's value
	 * @since 3.6
	 */
	String getValue();
}
