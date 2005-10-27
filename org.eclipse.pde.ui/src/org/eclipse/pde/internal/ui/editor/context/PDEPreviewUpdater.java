/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.context;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class PDEPreviewUpdater {

	private Color fForegroundColor = null;

	public PDEPreviewUpdater(final SourceViewer viewer,
			final SourceViewerConfiguration configuration,
			final IPreferenceStore preferenceStore) {

		initializeViewerColors(viewer, preferenceStore);

		final IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND
						.equals(property)
						|| AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT
								.equals(property)) {
					initializeViewerColors(viewer, preferenceStore);
				}
			}

		};
		preferenceStore.addPropertyChangeListener(propertyChangeListener);
	}

	protected void initializeViewerColors(ISourceViewer viewer,
			IPreferenceStore store) {
		StyledText styledText = viewer.getTextWidget();
		Color color = store
				.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT) ? null
				: createColor(store,
						AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND,
						styledText.getDisplay());
		styledText.setForeground(color);
		if (fForegroundColor != null) {
			fForegroundColor.dispose();
		}
		fForegroundColor = color;
	}

	private Color createColor(IPreferenceStore store, String key,
			Display display) {
		RGB rgb = null;
		if (store.contains(key)) {
			if (store.isDefault(key))
				rgb = PreferenceConverter.getDefaultColor(store, key);
			else
				rgb = PreferenceConverter.getColor(store, key);
			if (rgb != null)
				return new Color(display, rgb);
		}
		return null;
	}

	public void dispose() {
		if (fForegroundColor != null) {
			fForegroundColor.dispose();
			fForegroundColor = null;
		}
	}
}
