package $packageName$;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.pde.ui.customattributes.ICustomAttributeEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class $editorClassName$ implements ICustomAttributeEditor {
	private Label _label;
	private Color _color;
	private Button _button;

	private FormToolkit _toolkit;

	public void createContents(final Composite parent,
			final FormToolkit toolkit,
			final IPropertyChangeListener listener) {
		_toolkit = toolkit;
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parent);
		_label = toolkit.createLabel(parent, "", SWT.NULL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true,
				false).applyTo(_label);
		_button = toolkit.createButton(parent, "Select color...", SWT.PUSH);
		_button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ColorDialog dialog = new ColorDialog(parent.getShell());
				if (dialog.open() != null) {
					refresh(StringConverter.asString(dialog.getRGB()));
					listener.propertyChange(null) ;
				}
			}
		});
	}

	public Control getMainControl() {
		return _label;
	}

	public String getValue() {
		return StringConverter.asString(_color.getRGB());
	}

	public void setEditable(boolean editable) {
		_label.setEnabled(editable);
	}

	public void setValue(String value) {
		refresh(value);
	}

	private void refresh(String newValue) {
		String labelText = "";
		_color = _toolkit.getColors().getBackground();

		if ("".equals(newValue)) {
			labelText = "<empty>";
		} else {
			try {
				_color = new Color(Display.getDefault(), StringConverter
						.asRGB(newValue));
			} catch (Exception ex) {
				labelText = "Invalid color!";
			}
		}
		_label.setText(labelText);
		_label.setBackground(_color);
	}
}
