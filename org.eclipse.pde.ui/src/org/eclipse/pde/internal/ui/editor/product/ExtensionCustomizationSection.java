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
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.IExtensionDeltaEvent;
import org.eclipse.pde.internal.core.IExtensionDeltaListener;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.iproduct.ICustomizationInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductTransform;
import org.eclipse.pde.internal.core.product.RemoveTransform;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.editor.plugin.ExtensionContentProvider;
import org.eclipse.pde.internal.ui.editor.plugin.ExtensionLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * First customization section - merely asks if you wish to use customizations.
 */
public class ExtensionCustomizationSection extends TreeSection {

	private class RemovedExtensionLabelProvider extends ExtensionLabelProvider
			implements IColorProvider {

		private ICustomizationInfo fCustomizationInfo;
		private IModelChangedListener fListener;
		private Color fDisabledColor;
		private ResourceManager fResourceManager = new LocalResourceManager(
				JFaceResources.getResources());
		//private Map fDisabledImageDescriptors = new HashMap();

		/**
		 * @param customizationInfo
		 */
		public RemovedExtensionLabelProvider(
				ICustomizationInfo customizationInfo) {
			this.fCustomizationInfo = customizationInfo;
			fListener = new IModelChangedListener() {

				public void modelChanged(IModelChangedEvent event) {
					Object[] affected = event.getChangedObjects();

					for (int i = 0; i < affected.length; i++) {
						if (affected[i] instanceof IProductTransform) {
							IProductTransform transform = (IProductTransform) affected[i];
							fireLabelProviderChanged(new LabelProviderChangedEvent(
									RemovedExtensionLabelProvider.this,
									transform.getTransformedObject()));
						}
					}

				}
			};
			customizationInfo.getModel().addModelChangedListener(fListener);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
		 */
		public void dispose() {
			if (fDisabledColor != null)
				fDisabledColor.dispose();
			fResourceManager.dispose();
			fCustomizationInfo.getModel().removeModelChangedListener(fListener);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
		 */
		public Color getBackground(Object element) {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
		 */
		public Color getForeground(Object element) {

			IProductTransform[] transforms = fCustomizationInfo.getTransforms();

			for (int i = 0; i < transforms.length; i++) {
				IProductTransform productTransform = transforms[i];
				if (productTransform.getTransformedObject().equals(element)) {
					// this is hard coded for the remove behavior
					return getDisabledColor();
				}
			}
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.pde.internal.ui.editor.plugin.ExtensionLabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object obj) {
//			IProductTransform[] transforms = fCustomizationInfo.getTransforms();
//
//			for (int i = 0; i < transforms.length; i++) {
//				IProductTransform productTransform = transforms[i];
//				if (productTransform.getTransformedObject().equals(obj)) {
//
//					return getDisabledImage(obj);
//				}
//			}
			return super.getImage(obj);
		}

		/**
		 * @param image
		 * @return
		 */
//		private Image getDisabledImage(Object object) {
//			ImageDescriptor descriptor = (ImageDescriptor) fDisabledImageDescriptors
//					.get(object);
//			if (descriptor == null) {
//				final Image superImage = super.getImage(object);
//				descriptor = new CompositeImageDescriptor() {
//
//					protected void drawCompositeImage(int width, int height) {
//						Rectangle bounds = superImage.getBounds();
////						ImageData data = (ImageData) superImage.getImageData().clone();
//						ImageData data = superImage.getImageData();
//						int[] pixels = new int[bounds.width];
//						for (int i = 0; i < bounds.height; i++) {
//							data.getPixels(0, i, bounds.width, pixels, 0);
//							for (int j = 0; j < pixels.length; j++) {
//								System.out.print(j + "," + i + " : ");
//								System.out.print(pixels[j] + " = ");
//								float r = (pixels[j] & 0xFF) / 255f, g = ((pixels[j] >> 8) & 0xFF) / 255f, b = ((pixels[j] >> 16) & 0xFF) / 255f;
//								System.out.print(r + "," + g + "," + b + " -> ");
//								r *= 1.2f;
//								g *= 1.2f;
//								b *= 1.2f; 
//								int rInt = ((int)(Math.min(r * 255, 255)));
//								int gInt = ((int)(Math.min(r * 255, 255))) << 8;
//								int bInt = ((int)(Math.min(r * 255, 255))) << 16;
//								pixels[j] = rInt + gInt + bInt;
//								System.out.println(r + "," + g + "," + b);
////								pixels[j] = ((int) (((pixels[j] & 0xFF) / 255) * 1.5) * 255)
////										| ((int) (((((pixels[j] >> 8) & 0xFF) / 255) * 1.5) * 255) << 8)
////										| ((int) (((((pixels[j] >> 16) & 0xFF) / 255) * 1.5) * 255) << 16);
//								// pixels[j] = ((int)((pixels[j] & 0xFF)/255) *
//								// 1.5) * 255)) | ((int)(([j] & 0xFF)/255)
//								// * 1.5) * 255)) | ((int)((pixels[j] &
//								// 0xFF)/255) * 1.5) * 255));
//								
//							}
//							data.setPixels(0, i, bounds.width, pixels, 0);
//						}
//						//drawImage(superImage.getImageData(), 0, 0);
//						drawImage(data, 0, 0);
//					}
//
//					protected Point getSize() {
//						return new Point(superImage.getBounds().width,
//								superImage.getBounds().height);
//					}
//				};
//			}
//			return fResourceManager.createImage(descriptor);
//		}

		/**
		 * @return
		 */
		private Color getDisabledColor() {
			if (fDisabledColor == null) {
				// fDisabledColor = new
				// Color(fTreeViewer.getTree().getDisplay(), 255, 255, 255);
				fDisabledColor = new Color(fTreeViewer.getTree().getDisplay(),
						FormColors.blend(fToolkit.getColors().getSystemColor(
								SWT.COLOR_WIDGET_FOREGROUND), fToolkit
								.getColors().getSystemColor(SWT.COLOR_WHITE),
								50));
			}

			return fDisabledColor;
		}
	}

	private class RefreshListener implements IModelChangedListener,
			IExtensionDeltaListener, IPluginModelListener {

		private StructuredViewer fViewer;

		/**
		 * 
		 */
		public RefreshListener(StructuredViewer viewer) {
			this.fViewer = viewer;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
		 */
		public void modelChanged(IModelChangedEvent event) {
			refresh();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.pde.internal.core.IExtensionDeltaListener#extensionsChanged(org.eclipse.pde.internal.core.IExtensionDeltaEvent)
		 */
		public void extensionsChanged(IExtensionDeltaEvent event) {
			refresh();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.pde.internal.core.IPluginModelListener#modelsChanged(org.eclipse.pde.internal.core.PluginModelDelta)
		 */
		public void modelsChanged(PluginModelDelta delta) {
			refresh();
		}

		/**
		 * 
		 */
		private void refresh() {
			IProductModel productModel = getProductModel();
			if (productModel == null)
				return;
			fViewer.setInput(productModel.getProduct());
			fViewer.refresh();
		}

	}

	private RefreshListener fRefreshListener;
	private RemovedExtensionLabelProvider fLabelProvider;
	private TreeViewer fTreeViewer;
	private FormToolkit fToolkit;

	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public ExtensionCustomizationSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {
				"Restore To Configuration", "Remove From Configuration" });
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		fToolkit = toolkit;
		section.setText("Extensions");
		GridData sectionData = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(sectionData);
		Composite container = createClientContainer(section, 3, toolkit);
		createViewerPartControl(container, SWT.MULTI, 3, toolkit);
		fTreeViewer = getTreePart().getTreeViewer();
		fTreeViewer.setContentProvider(new ExtensionContentProvider());
		fLabelProvider = new RemovedExtensionLabelProvider(
				getCustomizationInfo());
		fTreeViewer.setLabelProvider(fLabelProvider);
		fTreeViewer.setInput((getProductModel()).getProduct());

		toolkit.paintBordersFor(container);
		section.setClient(container);
		fRefreshListener = new RefreshListener(fTreeViewer);

		PDECore.getDefault().getModelManager().addExtensionDeltaListener(
				fRefreshListener);
		PDECore.getDefault().getModelManager().addPluginModelListener(
				fRefreshListener);
		getProductModel().addModelChangedListener(fRefreshListener);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		getProductModel().removeModelChangedListener(fRefreshListener);
		PDECore.getDefault().getModelManager().removeExtensionDeltaListener(
				fRefreshListener);
		PDECore.getDefault().getModelManager().removePluginModelListener(
				fRefreshListener);
		super.dispose();
	}

	/**
	 * The customization info for this product.
	 * 
	 * @return the info
	 */
	protected ICustomizationInfo getCustomizationInfo() {
		ICustomizationInfo customizationInfo = (getProductModel()).getProduct()
				.getCustomizationInfo();
		if (customizationInfo == null) {
			IProductModel productModel = getProductModel();
			customizationInfo = productModel.getFactory()
					.createCustomizationInfo();
			productModel.getProduct().setCustomizationInfo(customizationInfo);
		}
		return customizationInfo;
	}

	/**
	 * @return
	 */
	private IProductModel getProductModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
		if (index == 0) {

		} else {
			IStructuredSelection selection = (IStructuredSelection) fTreeViewer
					.getSelection();
			Object selected = selection.getFirstElement();
			getCustomizationInfo().addTransform(new RemoveTransform(getProductModel(), selected));
		}
	}

}
