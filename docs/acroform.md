# AcroForm Fields

SwingToPDF can export Swing input components as interactive, fillable PDF form fields using the PDF AcroForm standard. The resulting PDF can be filled in by users in any standard PDF viewer (Adobe Acrobat, Preview, Chrome, Firefox, etc.).

---

## Enabling AcroForm

AcroForm generation is off by default. Enable it with a single builder call:

```java
SwingPdfExporter.from(myForm)
    .enableAcroForm()
    .export(Path.of("form.pdf"));
```

When enabled, supported Swing components produce interactive widget annotations **in addition to** their vector rendering. When disabled, all components render as static vector content.

---

## Component-to-Field Mapping

| Swing Component | PDF Field Type | Notes |
|----------------|---------------|-------|
| `JTextField` | Text field (single-line) | Editable in PDF viewer |
| `JFormattedTextField` | Text field (single-line) | Same as JTextField |
| `JPasswordField` | Text field (password) | Password flag set; content cleared in PDF |
| `JTextArea` | Text field (multiline) | Multiline flag set; word-wrap preserved |
| `JCheckBox` | Checkbox | Two states: checked ("Yes") and unchecked ("Off") |
| `JRadioButton` | Radio button | Buttons in the same `ButtonGroup` become a single PDF radio field |
| `JComboBox` | Combo box (dropdown) | All model items listed as options; editable flag preserved |

Components not in this table are unaffected by `enableAcroForm()` -- they render as normal vector content.

---

## Field Naming

Every PDF form field needs a unique name. SwingToPDF determines field names as follows:

1. **`component.getName()`** -- if you've called `setName("fieldName")` on the Swing component, that name is used (sanitized: dots, slashes, and spaces become underscores)
2. **Auto-generated** -- if no name is set, fields are named `field_1`, `field_2`, etc.
3. **Collision handling** -- if two components share the same name, a suffix is appended: `name_2`, `name_3`, etc.

### Best Practice: Name Your Components

```java
JTextField nameField = new JTextField();
nameField.setName("customer_name");

JTextField emailField = new JTextField();
emailField.setName("customer_email");

JCheckBox agreeBox = new JCheckBox("I agree to the terms");
agreeBox.setName("terms_accepted");
```

Named fields are easier to work with when programmatically reading PDF form data (e.g., with PDFBox or iText).

---

## Text Fields

`JTextField`, `JFormattedTextField`, `JPasswordField`, and `JTextArea` all map to PDF text fields.

```java
JTextField name = new JTextField("Jane Doe");
name.setName("full_name");
```

**Behavior:**
- The field's current text becomes the PDF field value
- Font size matches the component's font
- Password fields have the password flag set and their value is cleared in the PDF (the field appears empty but is fillable)
- `JTextArea` fields have the multiline flag set

### Disabled Fields

```java
nameField.setEnabled(false);
```

Disabled components produce **read-only** PDF fields. The value is visible but the user cannot edit it.

---

## Checkboxes

```java
JCheckBox newsletter = new JCheckBox("Subscribe to newsletter");
newsletter.setName("subscribe");
newsletter.setSelected(true);
```

**Behavior:**
- Produces a PDF checkbox with two appearance states: "Yes" (checked) and "Off" (unchecked)
- The checkbox appearance is hand-drawn (vector stroked rectangle with checkmark), not rasterized
- The initial state matches `isSelected()`

---

## Radio Buttons

Radio buttons that share a `ButtonGroup` in Swing become a single PDF radio button field with multiple widgets:

```java
ButtonGroup sizeGroup = new ButtonGroup();

JRadioButton small = new JRadioButton("Small");
small.setName("size");
sizeGroup.add(small);

JRadioButton medium = new JRadioButton("Medium");
medium.setName("size");
sizeGroup.add(medium);

JRadioButton large = new JRadioButton("Large");
large.setName("size");
large.setSelected(true);
sizeGroup.add(large);
```

**Behavior:**
- All buttons in the same `ButtonGroup` produce one PDF radio field
- Each button becomes a widget within that field
- The on-state name for each widget is derived from the button's text (sanitized) or `option_N` as fallback
- Mutual exclusion is enforced in the PDF (the `NoToggleToOff` flag is set)
- The initial selection matches the Swing state

### How Group Discovery Works

SwingToPDF discovers `ButtonGroup` membership via reflection (requires `--add-opens java.desktop/javax.swing`). If reflection is unavailable, it falls back to behavioral testing -- toggling each button and observing which others deselect.

---

## Combo Boxes

```java
JComboBox<String> country = new JComboBox<>(new String[]{"USA", "Canada", "UK", "Germany"});
country.setName("country");
country.setSelectedItem("Canada");
```

**Behavior:**
- All items in the model are listed as PDF dropdown options
- The selected item becomes the field value
- If `isEditable()` returns `true`, the PDF combo box allows custom text entry
- The dropdown always appears closed in the PDF (showing the selected value)

---

## Multi-Page Forms

When a form spans multiple pages (in `DATA_REPORT` mode), each interactive component is emitted **once** on the page where it first appears. If the same component appears in a continuation slice on a subsequent page, it is rendered as static vector content without a duplicate widget.

---

## Complete Example

```java
// Build the form
JPanel form = new JPanel(new GridBagLayout());
GridBagConstraints gbc = new GridBagConstraints();
gbc.insets = new Insets(4, 4, 4, 4);
gbc.anchor = GridBagConstraints.WEST;

// Text fields
JTextField nameField = new JTextField(20);
nameField.setName("name");
JTextField emailField = new JTextField(20);
emailField.setName("email");
JTextArea notesField = new JTextArea(4, 20);
notesField.setName("notes");

// Checkboxes
JCheckBox termsBox = new JCheckBox("I accept the terms");
termsBox.setName("terms");

// Radio group
ButtonGroup priorityGroup = new ButtonGroup();
JRadioButton low = new JRadioButton("Low");
JRadioButton med = new JRadioButton("Medium");
JRadioButton high = new JRadioButton("High");
low.setName("priority");
med.setName("priority");
high.setName("priority");
med.setSelected(true);
priorityGroup.add(low);
priorityGroup.add(med);
priorityGroup.add(high);

// Combo box
JComboBox<String> dept = new JComboBox<>(new String[]{"Sales", "Engineering", "Support"});
dept.setName("department");

// Layout (abbreviated)
gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Name:"), gbc);
gbc.gridx = 1; form.add(nameField, gbc);
// ... add remaining fields ...

// Export
SwingPdfExporter.from(form)
    .pageSize(PageSize.LETTER)
    .enableAcroForm()
    .title("Contact Form")
    .export(Path.of("contact-form.pdf"));
```

---

## Tips

- Always call `setName()` on form components to get predictable field names in the PDF
- Use `setEnabled(false)` to create read-only fields (e.g., pre-filled values the user shouldn't change)
- Radio buttons must share a `ButtonGroup` to behave as a group in the PDF
- Password field values are intentionally cleared in the PDF for security -- the field is present and fillable, but starts empty
- Test your forms in multiple PDF viewers -- AcroForm rendering varies slightly between Adobe Acrobat, Preview, Chrome, and Firefox
