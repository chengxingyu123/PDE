package org.eclipse.pde.internal.ui.editor.toc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.toc.TocObject;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;

public class TocDragAdapter extends DragSourceAdapter {
	private ISelectionProvider fSelectionProvider;
	private TocTreeSection fSection;
	private ArrayList fDraggedItems;
	
	public TocDragAdapter(ISelectionProvider provider, TocTreeSection section) {
		fSelectionProvider = provider;
		fSection = section;
	}

	public void dragStart(DragSourceEvent event) {
		if(event.doit)
		{	event.doit = !fSelectionProvider.getSelection().isEmpty();
		}
	}

	public void dragSetData(DragSourceEvent event) {
		if(event.doit)
		{	IStructuredSelection sel = (IStructuredSelection)fSelectionProvider.getSelection();

			if(TextTransfer.getInstance().isSupportedType(event.dataType)) {
				StringWriter sw = new StringWriter();
				PrintWriter writer = new PrintWriter(sw);

				for(Iterator iter = sel.iterator(); iter.hasNext();)
				{	Object obj = iter.next();
					if(obj instanceof TocObject)
					{	((TocObject)obj).write("", writer); //$NON-NLS-1$
					}
				}

				event.data = sw.toString();
				fDraggedItems = null;
			}
			else if (ModelDataTransfer.getInstance().isSupportedType(event.dataType)) {
				fDraggedItems = getSelectedObjects(sel); 
				TocObject[] selectedObjects = (TocObject[])fDraggedItems.toArray(new TocObject[fDraggedItems.size()]);
				if(selectedObjects.length == 0)
				{	event.doit = false;
				}
				else
				{	event.data = selectedObjects;
				}
			}
		}	
	}

	private ArrayList getSelectedObjects(IStructuredSelection selection) {
		ArrayList objects = new ArrayList();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof TocObject && ((TocObject)obj).canBeRemoved())
			{	objects.add(obj);
			}
			else
			{	return new ArrayList();
			}
		}

		return objects;
	}

	public void dragFinished(DragSourceEvent event)
	{	if(event.detail == DND.DROP_MOVE && fDraggedItems != null)
		{	fSection.handleRemove(fDraggedItems);
		}
	}
}
