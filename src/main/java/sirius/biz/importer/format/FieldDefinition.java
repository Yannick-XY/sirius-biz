/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.importer.format;

import com.google.common.collect.ImmutableList;
import sirius.kernel.commons.Lambdas;
import sirius.kernel.commons.Value;
import sirius.kernel.commons.Values;
import sirius.kernel.nls.NLS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Describes a field which is part of a {@link ImportDictionary}.
 * <p>
 * This can be used to import and check datasets (mostly row based ones like CSV or MS Excel).
 */
public class FieldDefinition {

    private static final ValueInListCheck BOOLEAN_VALUES_CHECK =
            new ValueInListCheck(ImmutableList.of("true", "false"));

    protected String name;
    protected String type;
    protected Supplier<String> label;
    protected Set<String> aliases = new HashSet<>();
    protected List<ValueCheck> checks = new ArrayList<>();

    /**
     * Creates a new field with the given name and type.
     *
     * @param name the name of the field
     * @param type the type description. Use helpers like {@link #typeString(Integer)} to generate one
     */
    public FieldDefinition(String name, String type) {
        this.name = name;
        this.type = type;
        addAlias(name);
    }

    /**
     * Helper to create a type description for a string field with a given length.
     *
     * @param maxLength the max length of the given field
     * @return a description to be shown to the user
     */
    public static String typeString(Integer maxLength) {
        if (maxLength == null || maxLength == 0) {
            return NLS.get("FieldDefinition.typeString.plain");
        }

        return NLS.fmtr("FieldDefinition.typeString.length").set("length", maxLength).format();
    }

    /**
     * Boilerplate to create a new string field with the given <tt>maxLength</tt> which is also enforced by a
     * {@link LengthCheck}.
     *
     * @param name      the name of the field
     * @param maxLength the maximal length of the string
     * @return the newly created field
     */
    public static FieldDefinition stringField(String name, int maxLength) {
        return new FieldDefinition(name, typeString(maxLength)).withCheck(new LengthCheck(maxLength));
    }

    /**
     * Boilerplate to create a new string field with a given list of permitted values.
     *
     * @param name       then name of the field
     * @param enumValues the list of permitted values
     * @return the newly created field
     */
    public static FieldDefinition enumStringField(String name, List<String> enumValues) {
        return new FieldDefinition(name, typeString(null)).withCheck(new ValueInListCheck(enumValues));
    }

    /**
     * Helper to create a type description for a numeric field with a given precision and scale.
     *
     * @param precision the precision of the field
     * @param scale     the scale of the field
     * @return a description to be shown to the user
     */
    public static String typeNumber(int precision, int scale) {
        if (precision == 0) {
            return NLS.get("FieldDefinition.typeNumber.plain");
        }

        return NLS.fmtr("FieldDefinition.typeNumber.length").set("precision", precision).set("scale", scale).format();
    }

    /**
     * Boilerplate to create a new numeric field with the given <tt>precision</tt> and <tt>scale</tt>}.
     *
     * @param name      the name of the field
     * @param precision the precision of the field
     * @param scale     the scale of the field
     * @return the newly created field
     */
    public static FieldDefinition numericField(String name, int precision, int scale) {
        return new FieldDefinition(name, typeNumber(precision, scale)).withCheck(new AmountScaleCheck(precision,
                                                                                                      scale));
    }

    /**
     * Helper to create a type description for a boolean field.
     *
     * @return a description to be shown to the user
     */
    public static String typeBoolean() {
        return NLS.get("FieldDefinition.typeBoolean");
    }

    /**
     * Boilerplate to create a new boolean field.
     *
     * @param name the name of the field
     * @return the newly created field
     */
    public static FieldDefinition booleanField(String name) {
        return new FieldDefinition(name, typeBoolean()).withCheck(BOOLEAN_VALUES_CHECK);
    }

    /**
     * Helper to create a type description for a date field.
     *
     * @return a description to be shown to the user
     */
    public static String typeDate() {
        return NLS.get("FieldDefinition.typeDate");
    }

    /**
     * Boilerplate to create a new date field.
     *
     * @param name   the name of the field
     * @param format the date format of the field
     * @return the newly created field
     */
    public static FieldDefinition dateField(String name, String format) {
        return new FieldDefinition(name, typeDate()).withCheck(new DateTimeFormatCheck(format));
    }

    /**
     * Helper to create a type description for a field with an unknown type.
     *
     * @return a description to be shown to the user
     */
    public static String typeOther() {
        return NLS.get("FieldDefinition.typeOther");
    }

    /**
     * Specifies the label to use for this field.
     *
     * @param constantLabel the label to show
     * @return the field itself for fluent method calls
     */
    public FieldDefinition withLabel(String constantLabel) {
        this.label = () -> constantLabel;
        return this;
    }

    /**
     * Specifies the label to use for this field.
     *
     * @param labelFunction the function used to determine the effective label
     * @return the field itself for fluent method calls
     */
    public FieldDefinition withLabel(Supplier<String> labelFunction) {
        this.label = labelFunction;
        return this;
    }

    /**
     * Adds a check for the field.
     *
     * @param check the check to add
     * @return the field itself for fluent method calls
     */
    public FieldDefinition withCheck(ValueCheck check) {
        if (check != null) {
            this.checks.add(check);
        }
        return this;
    }

    /**
     * Boilerplate to add a {@link RequiredCheck} to this field.
     *
     * @return the field itself for fluent method calls
     */
    public FieldDefinition markRequired() {
        return withCheck(new RequiredCheck());
    }

    /**
     * Adds an alias for the field.
     * <p>
     * Aliases are used by {@link ImportDictionary#determineMappingFromHeadings(Values, boolean)} to "learn" which
     * field is provided in which column.
     *
     * @param alias the alias to add
     * @return the field itself for fluent method calls
     */
    public FieldDefinition addAlias(String alias) {
        aliases.add(ImportDictionary.normalize(NLS.smartGet(alias)));

        return this;
    }

    /**
     * Verifies that a given value passes all provided checks.
     *
     * @param value the value to verify
     * @throws IllegalArgumentException if a check fails
     */
    public void verify(Value value) {
        this.checks.forEach(check -> check.perform(value));
    }

    /**
     * Returns the effective label for this field.
     *
     * @return the label to use for this field
     */
    public String getLabel() {
        if (label == null) {
            return name;
        }

        return label.get();
    }

    /**
     * Returns the name of the field.
     *
     * @return the "technical" name of the field
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a set of aliases for this field.
     *
     * @return the aliases of this field
     */
    public Set<String> getAliases() {
        return aliases == null ? Collections.emptySet() : Collections.unmodifiableSet(aliases);
    }

    /**
     * Lists all remarks to show for this field.
     *
     * @return a list of all remarks
     */
    public List<String> getRemarks() {
        List<String> result = new ArrayList<>();
        this.checks.stream().map(ValueCheck::generateRemark).filter(Objects::nonNull).collect(Lambdas.into(result));
        if (!aliases.isEmpty()) {
            result.add(NLS.fmtr("FieldDefinition.aliasRemark")
                          .set("aliases", aliases.stream().map(NLS::smartGet).collect(Collectors.joining(", ")))
                          .format());
        }

        return result;
    }

    /**
     * Returns the type description of the field.
     *
     * @return the type description to be shown to the user
     */
    public String getType() {
        return type;
    }
}