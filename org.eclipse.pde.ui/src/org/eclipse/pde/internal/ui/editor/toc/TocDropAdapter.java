package org.eclipse.pde.internal.ui.editor.toc;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;

public class TocDropAdapter extends ViewerDropAdapter {
	private TocTreeSection fSection;
	
	public TocDropAdapter(TreeViewer tocTree, TocTreeSection section)
	{	super(tocTree);
		fSection = section;
	}

	public void dragEnter(DropTargetEvent event) {
		validateFileDrop(event);
		setDNDMode(event);
	}

	public void dragOver(DropTargetEvent event) {
		int currentLocation = determineLocation(event);
		
		switch (currentLocation) {
        case LOCATION_BEFORE:
            event.feedback = DND.FEEDBACK_INSERT_BEFORE;
            break;
        case LOCATION_AFTER:
            event.feedback = DND.FEEDBACK_INSERT_AFTER;
            break;
        case LOCATION_ON:
        default:
            event.feedback = DND.FEEDBACK_SELECT;
            break;
        }
		
		event.feedback |= DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
	}

	public void dragOperationChanged(DropTargetEvent event) {
		setDNDMode(event);
	}

	private void setDNDMode(DropTargetEvent event) {
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType))
		{	if(event.detail == DND.DROP_DEFAULT)
			{	//If no modifier key is pressed
				//set the operation to DROP_COPY if available
				//DROP_NONE otherwise
				event.detail = (event.operations & DND.DROP_COPY);
			}
			else
			{	//If a modifier key is pressed for a file and the operation isn't a copy,
				//disallow it
				event.detail &= DND.DROP_COPY;
			}
		}
		//The only other transfer type allowed is a Model Data Transfer
		else if(!ModelDataTransfer.getInstance().isSupportedType(event.currentDataType))
		{	//disallow drag if the transfer is not Model Data or Files
			event.detail = DND.DROP_NONE;
		}
	}

	private void validateFileDrop(DropTargetEvent event) {
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType))
		{	String[] fileNames = (String[])FileTransfer.getInstance().nativeToJava(event.currentDataType);
			for(int i = 0; i < fileNames.length; i++)
			{	IPath path = new Path(fileNames[i]);
				if(!TocExtensionUtil.hasValidPageExtension(path)
					&& !TocExtensionUtil.hasValidTocExtension(path))
				{	event.detail = DND.DROP_NONE;
					return;
				}
			}
		}
	}

	public void drop(DropTargetEvent event)
	{	fSection.handleDrop(event.item, event.data, event.detail);
	}

	
	public boolean performDrop(Object data) {
		return true;
	}

	public boolean validateDrop(Object target, int operation,
			TransferData transferType) {
		return true;
	}
	
	public void dragLeave(DropTargetEvent event) 
	{
	}

	public void dropAccept(DropTargetEvent event)
	{
	}
}
