package $packageName$;

import org.eclipse.jface.resource.StringConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.ICustomAttributeValidator;

public class $validatorClassName$ implements ICustomAttributeValidator {
	public String[] validateAttribute(String value) {
		try {
			StringConverter.asRGB(value);
		} catch (Exception dfe) {
			return new String[] { NLS.bind("{0} is not a valid color string.",
					value) };
		}
		return null;
	}
}