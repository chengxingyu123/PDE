package org.eclipse.pde.internal.core.schema;

import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaRootElement;

public class SchemaRootElement extends SchemaElement implements
		ISchemaRootElement {

	private static final long serialVersionUID = 1L;
	public static final String P_DEP_SUGGESTION = "deprecatedSuggestion";
	private String fDeperecatedSuggestion;
	
	public SchemaRootElement(ISchemaObject parent, String name) {
		super(parent, name);
	}

	public void setDeprecatedSuggestion(String value) {
		Object oldValue = fDeperecatedSuggestion;
		fDeperecatedSuggestion = value;
		getSchema().fireModelObjectChanged(this, P_DEP_SUGGESTION, oldValue, fDeperecatedSuggestion);
	}

	public String getDeprecatedSuggestion() {
		return fDeperecatedSuggestion;
	}

	public String getExtenderProperies() {
		return " " + P_DEP_SUGGESTION + "=\"" + fDeperecatedSuggestion + "\" ";
	}
}
