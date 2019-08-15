/*
 * Stork
 * Copyright (C) 2019 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package org.tigase.messenger.phone.pro.dynaform;

import android.content.Context;
import android.support.design.widget.TextInputLayout;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import cz.destil.settleup.gui.MultiSpinner;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.*;

import java.util.*;

public class DynamicForm
		extends LinearLayout {

	private final HashMap<String, View> fields = new HashMap<>();
	private JabberDataElement form;

	public DynamicForm(Context context) {
		super(context);
	}

	public DynamicForm(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DynamicForm(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void clear() {
		removeAllViews();
		form = null;
		this.fields.clear();
	}

	public JabberDataElement getJabberDataElement() throws XMLException {
		for (Map.Entry<String, View> entry : this.fields.entrySet()) {
			final AbstractField<?> field = form.getField(entry.getKey());
			if (field instanceof ListMultiField) {
				MultiSpinner editor = (MultiSpinner) entry.getValue();
				((ListMultiField) field).setFieldValue(editor.getCheckedItems().toArray(new String[]{}));
				throw new RuntimeException("Unsupported field: " + field);
			} else if (field instanceof ListSingleField) {
				Spinner editor = (Spinner) entry.getValue();
				String value = editor.getSelectedItem() != null ? editor.getSelectedItem().toString() : null;
				((ListSingleField) field).setFieldValue(value);
			} else if (field instanceof TextMultiField) {
				EditText editor = (EditText) entry.getValue();
				((TextMultiField) field).setFieldValue(getTextValues(editor));
			} else if (field instanceof JidMultiField) {
				EditText editor = (EditText) entry.getValue();
				((JidMultiField) field).setFieldValue(getJidValues(editor));
			} else if (field instanceof TextSingleField) {
				EditText editor = (EditText) entry.getValue();
				((TextSingleField) field).setFieldValue(editor.getText().toString());
			} else if (field instanceof TextPrivateField) {
				EditText editor = (EditText) entry.getValue();
				((TextPrivateField) field).setFieldValue(editor.getText().toString());
			} else if (field instanceof JidSingleField) {
				EditText editor = (EditText) entry.getValue();
				((JidSingleField) field).setFieldValue(JID.jidInstance(editor.getText().toString()));
			} else if (field instanceof BooleanField) {
				Switch editor = (Switch) entry.getValue();
				((BooleanField) field).setFieldValue(editor.isChecked());
			} else {
				throw new RuntimeException("Unsupported field: " + field);
			}
		}

		return form;
	}

	public void setJabberDataElement(final JabberDataElement form) {
		clear();
		try {
			this.form = form;
			if (form.getInstructions() != null) {
				addInstruction(form.getInstructions());
			}
			final ArrayList<AbstractField<?>> fields = form.getFields();
			for (AbstractField<?> field : fields) {
				if (field instanceof ListMultiField) {
					addListMultiField((ListMultiField) field);
				} else if (field instanceof ListSingleField) {
					addListSingleField((ListSingleField) field);
				} else if (field instanceof FixedField) {
					addFixedField((FixedField) field);
				} else if (field instanceof TextMultiField) {
					addTextMultiField((TextMultiField) field);
				} else if (field instanceof JidMultiField) {
					addJidMultiField((JidMultiField) field);
				} else if (field instanceof TextSingleField) {
					addTextSingleField((TextSingleField) field);
				} else if (field instanceof TextPrivateField) {
					addTextPrivateField((TextPrivateField) field);
				} else if (field instanceof JidSingleField) {
					addJidSingleField((JidSingleField) field);
				} else if (field instanceof BooleanField) {
					addBooleanField((BooleanField) field);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			forceLayout();
			refreshDrawableState();
		}
	}

	private void addBooleanField(final BooleanField field) throws XMLException {
		Switch editor = new Switch(getContext());
		editor.setTextAppearance(getContext(), android.R.style.TextAppearance_Medium);
		Boolean v = field.getFieldValue();
		editor.setChecked(v != null && v.booleanValue());
		editor.setHint(field.getLabel());
		editor.setText(field.getLabel());
		editor.setPadding(14, 35, 14, 38);

		this.fields.put(field.getVar(), editor);
		addView(wrap(editor));
	}

	private void addFixedField(final FixedField field) throws XMLException {
		TextView view = new TextView(getContext());
		view.setPadding(14, 35, 14, 38);
		view.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		view.setText(field.getFieldValue());
		addView(view);
	}

	private void addInstruction(final String instructions) throws XMLException {
		TextView view = new TextView(getContext());
		view.setPadding(14, 35, 14, 38);
		view.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		view.setText(instructions);
		addView(view);
	}

	private void addJidMultiField(final JidMultiField field) throws XMLException {
		EditText editor = new EditText(getContext());
		String v = "";
		JID[] jids = field.getFieldValue();
		if (jids != null) {
			for (int i = 0; i < jids.length; i++) {
				if (i > 0) {
					v += "\n";
				}
				v += jids[i];
			}
		}
		editor.setText(v);
		editor.setHint(field.getLabel());
//					editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		editor.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		editor.setMaxLines(Integer.MAX_VALUE);
		editor.setWidth(42);

		this.fields.put(field.getVar(), editor);
		addView(wrap(editor));
	}

	private void addJidSingleField(final JidSingleField field) throws XMLException {
		EditText editor = new EditText(getContext());
		JID v = field.getFieldValue();
		editor.setText(v == null ? "" : v.toString());
		editor.setMaxLines(1);
		editor.setSingleLine();
		editor.setHint(field.getLabel());
		editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		editor.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		this.fields.put(field.getVar(), editor);
		addView(wrap(editor));
	}

	private void addListMultiField(final ListMultiField field) throws XMLException {
		LinearLayout layout = new LinearLayout(getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		MultiSpinner spinner = new MultiSpinner(getContext());
		spinner.setPadding(14, 5, 14, 38);

		List<String> values = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		try {

			for (Element element : field.getChildren("option")) {
				String ll = element.getAttribute("label");
				String vv = element.getFirstChild("value").getValue();

				labels.add(ll == null ? vv : ll);
				values.add(vv);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		spinner.setItems(labels, "All", new MultiSpinner.MultiSpinnerListener() {
			@Override
			public void onItemsSelected(boolean[] selected) {
				System.out.println(Arrays.toString(selected));
			}
		});

		try {
			for (Element element : field.getChildren("value")) {
				String x = element.getValue();
				int p = values.indexOf(x);
				spinner.setChecked(p, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		TextView label = new TextView(getContext());
		label.setPadding(14, 35, 14, 0);
		label.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		label.setText(field.getLabel());
		// @style/TextAppearance.AppCompat.Medium
		label.setTextAppearance(getContext(), android.support.design.R.styleable.TextInputLayout_hintTextAppearance);
		layout.addView(label);

		layout.addView(spinner);

		this.fields.put(field.getVar(), spinner);
		addView(wrap(layout));
	}

	private void addListSingleField(final ListSingleField field) throws XMLException {
		LinearLayout layout = new LinearLayout(getContext());

		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		Spinner spinner = new Spinner(getContext());
		spinner.setPadding(14, 5, 14, 0);
		spinner.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		List<Element> options = field.getChildren("option");
		String[] opts = new String[options.size()];
		for (int i = 0; i < options.size(); i++) {
			Element e = options.get(i);
			opts[i] = e.getAttribute("label");
		}

		ArrayAdapter aa = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item, opts);
		aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spinner.setAdapter(aa);

		TextView label = new TextView(getContext());
		label.setPadding(14, 35, 14, 38);
		label.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		label.setText(field.getLabel());
		// @style/TextAppearance.AppCompat.Medium
		label.setTextAppearance(getContext(), android.support.design.R.styleable.TextInputLayout_hintTextAppearance);
		layout.addView(label);

		layout.addView(spinner);

		this.fields.put(field.getVar(), spinner);
		addView(wrap(layout));
	}

	private void addTextMultiField(final TextMultiField field) throws XMLException {
		EditText editor = new EditText(getContext());
		String v = "";
		String[] lines = field.getFieldValue();
		if (lines != null) {
			for (int i = 0; i < lines.length; i++) {
				if (i > 0) {
					v += "\n";
				}
				v += lines[i];
			}
		}
		editor.setText(v);
		editor.setHint(field.getLabel());
		editor.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		editor.setMaxLines(Integer.MAX_VALUE);
		editor.setWidth(42);

		this.fields.put(field.getVar(), editor);
		addView(wrap(editor));
	}

	private void addTextPrivateField(final TextPrivateField field) throws XMLException {
		EditText editor = new EditText(getContext());
		editor.setText(field.getFieldValue());
		editor.setHint(field.getLabel());
		editor.setMaxLines(1);
		editor.setSingleLine();
		editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		editor.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		this.fields.put(field.getVar(), editor);
		addView(wrap(editor));
	}

	private void addTextSingleField(final TextSingleField field) throws XMLException {
		EditText editor = new EditText(getContext());
		editor.setText(field.getFieldValue());
		editor.setHint(field.getLabel());
		editor.setMaxLines(1);
		editor.setSingleLine();
		editor.setLayoutParams(
				new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		this.fields.put(field.getVar(), editor);
		addView(wrap(editor));
	}

	private JID[] getJidValues(EditText editor) {
		String[] b = getTextValues(editor);
		if (b == null) {
			return null;
		}
		JID[] result = new JID[b.length];
		for (int i = 0; i < b.length; i++) {
			result[i] = JID.jidInstance(b[i]);
		}
		return result;
	}

	private String[] getTextValues(EditText editor) {
		String t = editor.getText().toString();
		if (t == null) {
			return null;
		}
		ArrayList<String> result = new ArrayList<>();
		String[] lines = t.trim().split("\n");
		return lines;
	}

	private TextInputLayout wrap(View view) {
		android.support.design.widget.TextInputLayout l = new TextInputLayout(getContext());
		l.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		l.addView(view);

		return l;
	}

}