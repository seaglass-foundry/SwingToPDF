package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Component;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emits PDF AcroForm widget annotations for interactive Swing form fields.
 *
 * <p>Enabled via {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter#enableAcroForm()}.
 * One instance lives for the entire export; component handlers call its
 * {@code add*} methods during the per-page traversal.
 *
 * <h3>Field type mapping</h3>
 * <ul>
 *   <li>{@link javax.swing.JTextField} / {@code JFormattedTextField}
 *       -&gt; single-line {@link PDTextField}</li>
 *   <li>{@link JPasswordField} -&gt; password-masked {@link PDTextField}</li>
 *   <li>{@link JTextArea} -&gt; multiline {@link PDTextField}</li>
 *   <li>{@link JCheckBox} -&gt; {@link PDCheckBox}</li>
 *   <li>{@link JRadioButton} -&gt; {@link PDRadioButton} group (buttons sharing a
 *       {@link ButtonGroup} become one PDF radio field)</li>
 *   <li>{@link JComboBox} -&gt; {@link PDComboBox}</li>
 * </ul>
 *
 * <h3>Appearance handling</h3>
 * The form is created with {@code NeedAppearances=true} so PDF viewers regenerate
 * visual appearances from the field values. Minimal placeholder appearance sub-dicts
 * are attached to button widgets so that PDFBox validation methods can correctly
 * read on-state names.
 */
final class AcroFormEmitter {

    private static final Logger log = LoggerFactory.getLogger(AcroFormEmitter.class);

    private final PDDocument doc;
    private final PDAcroForm acroForm;
    private int fieldCounter = 0;

    /** Tracks which Swing components have already produced a widget (skip on page 2+). */
    private final Set<Component> emitted = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Tracks emitted field names to avoid collisions. */
    private final Set<String> usedNames = new java.util.HashSet<>();

    /** Ensures the ButtonGroup reflection warning is logged at most once. */
    private static boolean reflectionWarned = false;

    /**
     * Accumulates radio buttons into groups keyed by their Swing {@link ButtonGroup}
     * (found via reflection when {@code --add-opens} is present).
     */
    private final Map<ButtonGroup, RadioGroupBuilder> groupBuilders = new IdentityHashMap<>();

    /**
     * Radio buttons whose {@link ButtonGroup} could not be determined via reflection
     * (typically because {@code --add-opens java.desktop/javax.swing=...} was not passed).
     * At finalization time these are grouped by a behavioural mutual-exclusion test:
     * selecting one button and checking which others the Swing {@code ButtonGroup}
     * mechanism automatically deselects.
     */
    private final List<PendingRadio> pendingRadios = new ArrayList<>();

    // -----------------------------------------------------------------------

    AcroFormEmitter(PDDocument doc) {
        this.doc  = doc;
        acroForm  = new PDAcroForm(doc);
        doc.getDocumentCatalog().setAcroForm(acroForm);
        acroForm.setNeedAppearances(Boolean.TRUE);

        // Provide a default appearance resource so variable-text fields can generate
        // appearances. /Helv = Helvetica from the DR dictionary. Size 0 = auto-size
        // (used only as fallback; individual fields set their own size below).
        PDResources dr = new PDResources();
        dr.put(COSName.getPDFName("Helv"),
               new PDType1Font(Standard14Fonts.FontName.HELVETICA));
        acroForm.setDefaultResources(dr);
        acroForm.setDefaultAppearance("/Helv 0 Tf 0 g");
    }

    // -----------------------------------------------------------------------
    // Text fields (JTextField, JFormattedTextField, JPasswordField, JTextArea)
    // -----------------------------------------------------------------------

    void addTextField(JTextComponent tc, int absX, int absY, int width, int height,
                      PDPage page, PdfPageWriter writer) throws IOException {
        if (!emitted.add(tc) || !isOnPage(absY, height, writer)) return;

        PDTextField field = new PDTextField(acroForm);
        field.setPartialName(nextName(tc));
        if (tc instanceof JTextArea)      field.setMultiline(true);
        if (tc instanceof JPasswordField) field.setPassword(true);

        // Per-field DA using the component's actual font size so the appearance
        // matches the Swing rendering rather than using the form-level auto-size.
        java.awt.Font swingFont = tc.getFont();
        float ptSize = (swingFont != null) ? swingFont.getSize2D() : 12f;
        field.setDefaultAppearance("/Helv " + ptSize + " Tf 0 g");

        String text = (tc instanceof JPasswordField) ? "" : nvl(tc.getText());
        field.setDefaultValue(text);

        if (!tc.isEnabled()) field.setReadOnly(true);

        // Wire the widget first so PDFBox can attach the appearance stream to it
        // when setValue() calls applyChange() internally.
        wireWidget(field, toRect(absX, absY, width, height, writer), page);
        acroForm.getFields().add(field);

        // setValue() generates the appearance stream and sets NeedAppearances=false,
        // which is what we want: all field types will then use their explicit streams.
        try {
            field.setValue(text);
        } catch (Exception e) {
            // Fallback: write V directly; appearance may not render in all viewers
            log.warn("AcroForm appearance generation failed for field '{}'; "
                   + "value set directly (may not render in all viewers): {}",
                     field.getPartialName(), e.getMessage());
            field.getCOSObject().setString(COSName.V, text);
        }
    }

    // -----------------------------------------------------------------------
    // CheckBox
    // -----------------------------------------------------------------------

    void addCheckBox(JCheckBox cb, int absX, int absY,
                     PDPage page, PdfPageWriter writer) throws IOException {
        if (!emitted.add(cb) || !isOnPage(absY, cb.getHeight(), writer)) return;

        PDCheckBox field = new PDCheckBox(acroForm);
        field.setPartialName(nextName(cb));
        if (!cb.isEnabled()) field.setReadOnly(true);

        PDRectangle rect = indicatorRect(cb, absX, absY, writer);
        PDAnnotationWidget widget = buildWidget(rect, page);
        // Draw both states manually so the viewer can render the toggled
        // appearance without relying on PDFBox's save-time appearance generator.
        widget.setAppearance(buildCheckBoxAppearance(rect, doc));
        field.setWidgets(List.of(widget));
        page.getAnnotations().add(widget);
        acroForm.getFields().add(field);

        // Set V and AS directly  -- avoids applyChange() which resets NeedAppearances.
        String state = cb.isSelected() ? "Yes" : "Off";
        field.getCOSObject().setName(COSName.V, state);
        widget.getCOSObject().setName(COSName.AS, state);
    }

    // -----------------------------------------------------------------------
    // RadioButton
    // -----------------------------------------------------------------------

    void addRadioButton(JRadioButton rb, int absX, int absY,
                        PDPage page, PdfPageWriter writer) throws IOException {
        if (!emitted.add(rb) || !isOnPage(absY, rb.getHeight(), writer)) return;

        String onValue = sanitizeOnValue(rb.getText(), fieldCounter + 1);
        PDRectangle rect = indicatorRect(rb, absX, absY, writer);
        PDAnnotationWidget widget = buildWidget(rect, page);
        // Both on/off appearances drawn manually; AS set after group finalization.
        widget.setAppearance(buildRadioAppearance(onValue, rect, doc));
        page.getAnnotations().add(widget);

        // Try to identify the ButtonGroup via reflection (requires --add-opens).
        // If reflection is blocked, queue for behavioural group discovery at
        // finalization time  -- DO NOT fall back to rb.getParent() because a single
        // container can hold buttons from multiple groups (e.g. GridBagLayout form).
        ButtonGroup groupKey = getButtonGroup(rb);
        if (groupKey != null) {
            groupBuilders.computeIfAbsent(groupKey, g -> new RadioGroupBuilder())
                         .add(onValue, rb.isSelected(), !rb.isEnabled(), widget);
        } else {
            pendingRadios.add(new PendingRadio(rb, onValue, widget));
        }
    }

    // -----------------------------------------------------------------------
    // ComboBox
    // -----------------------------------------------------------------------

    void addComboBox(JComboBox<?> cb, int absX, int absY,
                     PDPage page, PdfPageWriter writer) throws IOException {
        if (!emitted.add(cb) || !isOnPage(absY, cb.getHeight(), writer)) return;

        PDComboBox field = new PDComboBox(acroForm);
        field.setPartialName(nextName(cb));
        field.setEdit(cb.isEditable());
        if (!cb.isEnabled()) field.setReadOnly(true);

        java.awt.Font swingFont = cb.getFont();
        float ptSize = (swingFont != null) ? swingFont.getSize2D() : 12f;
        field.setDefaultAppearance("/Helv " + ptSize + " Tf 0 g");

        List<String> options = new ArrayList<>();
        for (int i = 0; i < cb.getItemCount(); i++) {
            Object item = cb.getItemAt(i);
            options.add(item != null ? item.toString() : "");
        }
        if (!options.isEmpty()) field.setOptions(options);

        // Wire the widget before setValue() so PDFBox can attach the appearance
        // stream to the widget annotation when applyChange() runs internally.
        wireWidget(field, toRect(absX, absY, cb.getWidth(), cb.getHeight(), writer), page);
        acroForm.getFields().add(field);

        Object sel = cb.getSelectedItem();
        if (sel != null) {
            try {
                field.setValue(sel.toString());
            } catch (Exception e) {
                // Fallback: write V directly
                field.getCOSObject().setString(COSName.V, sel.toString());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Finalize radio groups  -- called by ExportEngine after all pages rendered
    // -----------------------------------------------------------------------

    void finalizeGroups() throws IOException {
        // 1. Grouped radio buttons (reflection succeeded)  -- one PDRadioButton per ButtonGroup
        for (RadioGroupBuilder builder : groupBuilders.values()) {
            emitRadioGroup(builder);
        }

        // 2. Pending radio buttons (reflection failed, i.e. no --add-opens).
        //    Discover groups by behavioural testing: selecting one button and
        //    checking which others the Swing ButtonGroup automatically deselects.
        if (!pendingRadios.isEmpty()) {
            List<List<PendingRadio>> groups = discoverGroupsByBehavior(pendingRadios);
            for (List<PendingRadio> group : groups) {
                if (group.size() == 1) {
                    // Truly standalone (no ButtonGroup)  -- emits as a single-option
                    // PDRadioButton. This is valid PDF; the button acts as a toggle.
                    PendingRadio pr = group.get(0);
                    PDRadioButton field = new PDRadioButton(acroForm);
                    field.setPartialName(nextName(pr.button));
                    field.setWidgets(List.of(pr.widget));
                    acroForm.getFields().add(field);
                    String state = pr.button.isSelected() ? pr.onValue : "Off";
                    field.setValue(state);
                } else {
                    // Discovered group  -- convert to RadioGroupBuilder and emit
                    RadioGroupBuilder builder = new RadioGroupBuilder();
                    for (PendingRadio pr : group) {
                        builder.add(pr.onValue, pr.button.isSelected(),
                                    !pr.button.isEnabled(), pr.widget);
                    }
                    emitRadioGroup(builder);
                }
            }
        }

        // Appearance streams exist for all field types (text/combo generated by
        // PDFBox's setValue(); checkbox and radio hand-drawn).  Tell viewers to
        // use the explicit streams rather than regenerating from field data.
        acroForm.setNeedAppearances(Boolean.FALSE);
    }

    /** Emit a single PDRadioButton field from a fully populated builder. */
    private void emitRadioGroup(RadioGroupBuilder builder) throws IOException {
        PDRadioButton field = new PDRadioButton(acroForm);
        field.setPartialName("radio_" + (++fieldCounter));
        field.getCOSObject().setFlag(COSName.FF, 1 << 14, true); // NoToggleToOff
        if (builder.disabledCount == builder.widgets.size()) field.setReadOnly(true);
        field.setWidgets(new ArrayList<>(builder.widgets));
        acroForm.getFields().add(field);
        String selectedV = builder.selectedValue != null ? builder.selectedValue : "Off";
        field.setValue(selectedV);
    }

    /**
     * Discover radio button groups without reflection by exploiting Swing's
     * {@link ButtonGroup} mutual-exclusion behaviour.  For each unassigned button,
     * we call {@code setSelected(true)} and observe which other pending buttons
     * the ButtonGroup automatically deselects  -- those belong to the same group.
     *
     * <p>Original selection states are saved and restored after discovery.
     *
     * <p><b>Note:</b> {@code setSelected()} calls may fire {@code ItemListener}
     * and {@code ChangeListener} callbacks on the buttons. This is unavoidable
     * when reflection is blocked; the save/restore in the finally block ensures
     * that the buttons' selected state is returned to its original value even
     * if a listener throws.
     */
    private static List<List<PendingRadio>> discoverGroupsByBehavior(
            List<PendingRadio> pending) {
        int n = pending.size();

        // Save original selection states
        boolean[] origSelected = new boolean[n];
        for (int i = 0; i < n; i++) {
            origSelected[i] = pending.get(i).button.isSelected();
        }

        int[] groupId = new int[n];
        java.util.Arrays.fill(groupId, -1);
        int nextGroup = 0;

        try {
            for (int i = 0; i < n; i++) {
                if (groupId[i] >= 0) continue;        // already assigned
                groupId[i] = nextGroup;

                // Deselect all pending buttons, then select button i
                for (PendingRadio pr : pending) pr.button.setSelected(false);
                pending.get(i).button.setSelected(true);

                // Any other unassigned button whose selection deselects button i
                // must share the same ButtonGroup.
                for (int j = i + 1; j < n; j++) {
                    if (groupId[j] >= 0) continue;
                    pending.get(j).button.setSelected(true);
                    if (!pending.get(i).button.isSelected()) {
                        // j's selection caused i to deselect -&gt; same group
                        groupId[j] = nextGroup;
                        pending.get(i).button.setSelected(true); // re-select for next test
                    }
                    pending.get(j).button.setSelected(false);
                }
                nextGroup++;
            }
        } finally {
            // Restore original selection states even if a listener throws
            for (int i = 0; i < n; i++) {
                pending.get(i).button.setSelected(origSelected[i]);
            }
        }

        // Partition into groups
        Map<Integer, List<PendingRadio>> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            map.computeIfAbsent(groupId[i], k -> new ArrayList<>()).add(pending.get(i));
        }
        return new ArrayList<>(map.values());
    }

    // -----------------------------------------------------------------------
    // Coordinate helpers
    // -----------------------------------------------------------------------

    /** True if the component's top edge falls within the current page slice. */
    private static boolean isOnPage(int absY, int compH, PdfPageWriter writer) {
        float sliceTop = writer.sliceTopPx();
        float sliceBot = sliceTop + writer.pageHeightPx();
        return absY >= sliceTop && absY < sliceBot;
    }

    /** Convert Swing root-space coordinates to a PDF annotation rectangle. */
    private static PDRectangle toRect(int absX, int absY, int w, int h,
                                       PdfPageWriter writer) {
        float left   = writer.pdfX(absX);
        float bottom = writer.pdfY(absY, h);
        float right  = writer.pdfX(absX + w);
        float top    = writer.pdfY(absY, 0);
        return new PDRectangle(left, bottom, right - left, top - bottom);
    }

    /** Rect sized to the indicator area of a check/radio button. */
    private static PDRectangle indicatorRect(AbstractButton btn,
                                              int absX, int absY,
                                              PdfPageWriter writer) {
        final int IND = AbstractButtonHandler.indicatorSize();
        int ix = absX + btn.getInsets().left;
        int iy = absY + (btn.getHeight() - IND) / 2;
        return toRect(ix, iy, IND, IND, writer);
    }

    // -----------------------------------------------------------------------
    // Widget / appearance helpers
    // -----------------------------------------------------------------------

    /** Create a widget annotation with rect and page set. */
    private static PDAnnotationWidget buildWidget(PDRectangle rect, PDPage page) {
        PDAnnotationWidget w = new PDAnnotationWidget();
        w.setRectangle(rect);
        w.setPage(page);
        // Set Print flag (bit 3) so the widget renders both on-screen and in print.
        w.setPrinted(true);
        return w;
    }

    /**
     * Add a single widget to {@code field}, register it with the page.
     * Used for fields that have exactly one widget (text, combo).
     */
    private static void wireWidget(
            org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField field,
            PDRectangle rect, PDPage page) throws IOException {
        PDAnnotationWidget widget = buildWidget(rect, page);
        field.setWidgets(List.of(widget));
        page.getAnnotations().add(widget);
    }

    /**
     * Build a checkbox appearance with hand-drawn PDF content streams for both
     * the "Yes" (checked) and "Off" (unchecked) states.
     * Both streams are proper Form XObjects so the viewer can switch between
     * them when the user clicks without needing to regenerate appearances.
     */
    private static PDAppearanceDictionary buildCheckBoxAppearance(
            PDRectangle rect, PDDocument doc) throws IOException {
        float w = rect.getWidth();
        float h = rect.getHeight();

        COSDictionary normalAp = new COSDictionary();
        normalAp.setItem(COSName.getPDFName("Yes"), buildCheckBoxStream(doc, w, h, true));
        normalAp.setItem(COSName.Off,               buildCheckBoxStream(doc, w, h, false));
        PDAppearanceDictionary ap = new PDAppearanceDictionary();
        ap.setNormalAppearance(new PDAppearanceEntry(normalAp));
        return ap;
    }

    /**
     * Build a radio-button appearance with hand-drawn PDF content streams for
     * both the on-state (filled circle) and "Off" (empty circle) states.
     */
    private static PDAppearanceDictionary buildRadioAppearance(
            String onState, PDRectangle rect, PDDocument doc) throws IOException {
        float w = rect.getWidth();
        float h = rect.getHeight();

        COSDictionary normalAp = new COSDictionary();
        normalAp.setItem(COSName.getPDFName(onState), buildRadioStream(doc, w, h, true));
        normalAp.setItem(COSName.Off,                 buildRadioStream(doc, w, h, false));
        PDAppearanceDictionary ap = new PDAppearanceDictionary();
        ap.setNormalAppearance(new PDAppearanceEntry(normalAp));
        return ap;
    }

    /**
     * Write a Form-XObject stream for a checkbox indicator.
     * Content: white fill, thin black border, optional checkmark (two lines).
     */
    private static COSStream buildCheckBoxStream(
            PDDocument doc, float w, float h, boolean checked) throws IOException {
        StringBuilder sb = new StringBuilder();
        // White background
        sb.append("q 1 g 0 0 ").append(f(w)).append(' ').append(f(h)).append(" re f\n");
        // Black border
        sb.append("0 G 1 w 0.5 0.5 ")
          .append(f(w - 1f)).append(' ').append(f(h - 1f)).append(" re S\n");
        if (checked) {
            // Checkmark: two strokes forming a checkmark
            sb.append("0 g 1.5 w\n");
            sb.append(f(w * 0.15f)).append(' ').append(f(h * 0.45f)).append(" m\n");
            sb.append(f(w * 0.38f)).append(' ').append(f(h * 0.18f)).append(" l\n");
            sb.append(f(w * 0.85f)).append(' ').append(f(h * 0.78f)).append(" l S\n");
        }
        sb.append("Q\n");
        return buildFormXObject(doc, w, h, sb.toString());
    }

    /**
     * Write a Form-XObject stream for a radio-button indicator.
     * Content: white fill, thin black border circle, optional filled inner dot.
     * Uses cubic Bezier approximation (k ~= 0.5523) to draw circles.
     */
    private static COSStream buildRadioStream(
            PDDocument doc, float w, float h, boolean selected) throws IOException {
        float cx = w / 2f, cy = h / 2f;
        float r  = Math.min(w, h) / 2f - 0.5f;  // outer radius with 0.5 margin
        float k  = r * 0.5523f;

        StringBuilder sb = new StringBuilder();
        // White background
        sb.append("q 1 g 0 0 ").append(f(w)).append(' ').append(f(h)).append(" re f\n");
        // Outer circle (black stroke)
        sb.append("0 G 1 w\n");
        appendCircle(sb, cx, cy, r, k);
        sb.append("S\n");
        if (selected) {
            // Inner filled dot
            float ri = r * 0.45f, ki = ri * 0.5523f;
            sb.append("0 g\n");
            appendCircle(sb, cx, cy, ri, ki);
            sb.append("f\n");
        }
        sb.append("Q\n");
        return buildFormXObject(doc, w, h, sb.toString());
    }

    /** Append four cubic Bezier curves approximating a circle centred at (cx,cy). */
    private static void appendCircle(StringBuilder sb,
                                      float cx, float cy, float r, float k) {
        sb.append(f(cx + r)).append(' ').append(f(cy)).append(" m\n");
        sb.append(f(cx + r)).append(' ').append(f(cy + k)).append(' ')
          .append(f(cx + k)).append(' ').append(f(cy + r)).append(' ')
          .append(f(cx)).append(' ').append(f(cy + r)).append(" c\n");
        sb.append(f(cx - k)).append(' ').append(f(cy + r)).append(' ')
          .append(f(cx - r)).append(' ').append(f(cy + k)).append(' ')
          .append(f(cx - r)).append(' ').append(f(cy)).append(" c\n");
        sb.append(f(cx - r)).append(' ').append(f(cy - k)).append(' ')
          .append(f(cx - k)).append(' ').append(f(cy - r)).append(' ')
          .append(f(cx)).append(' ').append(f(cy - r)).append(" c\n");
        sb.append(f(cx + k)).append(' ').append(f(cy - r)).append(' ')
          .append(f(cx + r)).append(' ').append(f(cy - k)).append(' ')
          .append(f(cx + r)).append(' ').append(f(cy)).append(" c\n");
        sb.append("h\n");
    }

    /** Package the given PDF operators into a Form XObject COSStream. */
    private static COSStream buildFormXObject(
            PDDocument doc, float w, float h, String ops) throws IOException {
        COSStream stream = doc.getDocument().createCOSStream();
        stream.setName(COSName.TYPE,    "XObject");
        stream.setName(COSName.SUBTYPE, "Form");
        COSArray bbox = new COSArray();
        bbox.add(COSInteger.ZERO); bbox.add(COSInteger.ZERO);
        bbox.add(new COSFloat(w)); bbox.add(new COSFloat(h));
        stream.setItem(COSName.BBOX, bbox);
        try (OutputStream out = stream.createOutputStream(COSName.FLATE_DECODE)) {
            out.write(ops.getBytes(StandardCharsets.US_ASCII));
        }
        return stream;
    }

    /** Format a float to at most 3 decimal places, trimming trailing zeros. */
    private static String f(float v) {
        if (v == (int) v) return Integer.toString((int) v);
        String s = String.format("%.3f", v);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    private String nextName(Component comp) {
        String base;
        String name = comp.getName();
        if (name != null && !name.isBlank()) {
            base = name.replace('.', '_').replace('/', '_').replace(' ', '_');
        } else {
            base = "field_" + (++fieldCounter);
        }
        String result = base;
        int suffix = 2;
        while (!usedNames.add(result)) {
            result = base + "_" + suffix++;
        }
        return result;
    }

    private static String sanitizeOnValue(String label, int ordinal) {
        if (label != null && !label.isBlank()) {
            String s = label.trim().replace(' ', '_');
            // Limit length and remove PDF special chars
            s = s.replaceAll("[()\\[\\]<>{}/%]", "");
            return s.isEmpty() ? "option_" + ordinal : (s.length() > 32 ? s.substring(0, 32) : s);
        }
        return "option_" + ordinal;
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    /** Reflect the {@link ButtonGroup} from a radio button's model. */
    private static ButtonGroup getButtonGroup(JRadioButton rb) {
        try {
            ButtonModel model = rb.getModel();
            if (model instanceof DefaultButtonModel dm) {
                Field f = DefaultButtonModel.class.getDeclaredField("group");
                f.setAccessible(true);
                return (ButtonGroup) f.get(dm);
            }
        } catch (Exception e) {
            if (!reflectionWarned) {
                reflectionWarned = true;
                log.debug("Could not reflect ButtonGroup from radio button model; "
                        + "falling back to behavioral discovery: {}", e.getMessage());
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Inner types
    // -----------------------------------------------------------------------

    private static final class RadioGroupBuilder {
        final List<PDAnnotationWidget> widgets    = new ArrayList<>();
        final List<String>             onValues   = new ArrayList<>();
        int                            disabledCount = 0;
        String                         selectedValue = null;

        void add(String onValue, boolean selected, boolean disabled,
                 PDAnnotationWidget widget) {
            // Ensure unique on-values within the group  -- duplicate names cause
            // setValue() to select multiple widgets simultaneously.
            String unique = onValue;
            int suffix = 2;
            while (onValues.contains(unique)) {
                unique = onValue + "_" + suffix++;
            }
            if (!unique.equals(onValue)) {
                // Fix widget AP/N key to match the renamed on-value
                COSDictionary apN = widget.getAppearance()
                        .getNormalAppearance().getCOSObject();
                COSBase stream = apN.getDictionaryObject(COSName.getPDFName(onValue));
                apN.removeItem(COSName.getPDFName(onValue));
                apN.setItem(COSName.getPDFName(unique), stream);
            }
            onValues.add(unique);
            widgets.add(widget);
            if (disabled) disabledCount++;
            if (selected) selectedValue = unique;
        }
    }

    private record PendingRadio(
            JRadioButton button, String onValue, PDAnnotationWidget widget) {}
}
