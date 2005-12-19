package org.eclipse.pde.internal.core;

import java.util.HashSet;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;

public class TargetProfileManager implements IRegistryChangeListener{
	
	IConfigurationElement[] elements;
	private static String[] attributes;
	{
		attributes = new String[] {"id", "name" }; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void registryChanged(IRegistryChangeEvent event) {
		IExtensionDelta[] deltas = event.getExtensionDeltas();
		for (int i = 0; i < deltas.length; i++) {
			IExtension extension = deltas[i].getExtension();
			String extensionId = extension.getExtensionPointUniqueIdentifier();
			if (extensionId.equals("org.eclipse.pde.core.targetProfiles")) { //$NON-NLS-1$
				IConfigurationElement[] elems = extension.getConfigurationElements();
				if (deltas[i].getKind() == IExtensionDelta.ADDED)
					add(elems);
				else
					remove(elems);
			}
		}
	}
	
	public IConfigurationElement[] getTargets() {
		if (elements == null)
			loadElements();
		return elements;
	}
	
	private void loadElements() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		registry.addRegistryChangeListener(this);
		elements = registry.getConfigurationElementsFor("org.eclipse.pde.core.targetProfiles"); //$NON-NLS-1$
	}
	
	public IConfigurationElement[] getValidTargets() {
		if (elements == null)
			loadElements();
		HashSet result = new HashSet();
		for (int i = 0; i < elements.length; i++) {
			if (isValid(elements[i]))
				result.add(elements[i]);
		}
		return (IConfigurationElement[])result.toArray(new IConfigurationElement[result.size()]) ;
	}
	
	private boolean isValid (IConfigurationElement elem) {
		String value;
		for (int i = 0; i < attributes.length; i++) {
			value = elem.getAttribute(attributes[i]);
			if (value == null || value.equals("")) //$NON-NLS-1$
				return false;
		}
		return true;
	}
	
	private void add(IConfigurationElement[] elems) {
		IConfigurationElement[] newArray = new IConfigurationElement[elements.length + elems.length];
		System.arraycopy(elements, 0, newArray, 0, elements.length);
		System.arraycopy(elems, 0, newArray, elements.length, elems.length);
		elements = newArray;
	}
	
	private void remove(IConfigurationElement[] elems) {
		HashSet set = new HashSet((4/3) * elements.length + 1);
		for (int i = 0; i < elements.length; i++) 
			set.add(elements[i]);
		for (int i = 0; i < elems.length; i++)
			set.remove(elems[i]);
		elements = (IConfigurationElement[]) set.toArray(new IConfigurationElement[set.size()]);
	}
	
	public void shutdown() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		registry.removeRegistryChangeListener(this);
	}

}
