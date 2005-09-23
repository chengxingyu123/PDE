package org.eclipse.pde.internal.ui.nls;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ModelChangeTable {

	private Hashtable fChangeTable = new Hashtable();
	private ArrayList fSelectedSet;
	private ArrayList fAllSet;
	
	public void addToChangeTable(IPluginModelBase model, IFile file, Object change, boolean selected) {
		if (change == null) return;
		ModelChange modelChange;
		if (fChangeTable.containsKey(model))
			modelChange = (ModelChange)fChangeTable.get(model);
		else {
			modelChange = new ModelChange(model, selected);
			fChangeTable.put(model, modelChange);
		}
		modelChange.addChange(file, new ModelChangeElement(modelChange, change));
	}
	
	public Set getAllModels() {
		return fChangeTable.keySet();
	}
	
	public ArrayList getAllModelChanges() {
		if (fAllSet == null) {
			fAllSet = new ArrayList(getAllModels().size());
			Iterator iter = getAllModels().iterator();
			while (iter.hasNext())
				fAllSet.add(getModelChange((IPluginModelBase) iter.next()));
		}
		return fAllSet;
	}
	
	public ArrayList getSelectedModelChanges() {
		if (fSelectedSet == null) {
			fSelectedSet = new ArrayList(1);
			Iterator iter = getAllModels().iterator();
			while (iter.hasNext()) {
				ModelChange change = getModelChange((IPluginModelBase) iter.next());
				if (change != null && change.isSelected())
					fSelectedSet.add(change);
			}
		}
		return fSelectedSet;
	}
	
	public boolean selectedNotEmpty() {
		if (fSelectedSet == null)
			getSelectedModelChanges();
		return fSelectedSet.size() > 0;
	}
	
	public ModelChange getModelChange(IPluginModelBase modelKey) {
		if (fChangeTable.containsKey(modelKey))
			return (ModelChange)fChangeTable.get(modelKey);
		return null;
	}
	
	public int getNumberOfChangesInModel(IPluginModelBase modelKey) {
		ModelChange change = getModelChange(modelKey);
		if (change != null)
			return change.getTotalNumberOfChanges();
		return 0;
	}
}
