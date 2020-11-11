/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.jobs.params;

import sirius.kernel.commons.Value;
import sirius.kernel.nls.NLS;

import java.util.Map;
import java.util.Optional;

public class Parameter<V> {

    /**
     * Provides a tri-state value indicating in which log the parameter can appear.
     */
    public enum LogVisibility {NORMAL, SYSTEM, NONE}

    private final ParameterBuilder<V, ?> delegate;

    public Parameter(ParameterBuilder<V, ?> delegate) {
        this.delegate = delegate;
    }

    /**
     * Determines if the parameter is currently visible.
     *
     * @param context the context containing all parameter values
     * @return <tt>true</tt> if the parameter is visible, <tt>false</tt> otherwise
     */
    public boolean isVisible(Map<String, String> context) {
        return delegate.isVisible(context);
    }

    /**
     * Returns the name of the template used to render the parameter in the UI.
     *
     * @return the name or path of the template used to render the parameter
     */
    public String getTemplateName() {
        return delegate.getTemplateName();
    }

    /**
     * Returns the name of the template used to render the parameter in the UI.
     * <p>
     * Similar to {@link #getTemplateName()}, but this method considers the visibility
     * of the parameter and delivers an alternative template in case the parameter should be hidden.
     *
     * @param context the context containing all parameter values
     * @return the name or path of the template used to render the parameter
     */
    public String getEffectiveTemplateName(Map<String, String> context) {
        return delegate.getEffectiveTemplateName(context);
    }

    /**
     * Verifies the value given for this parameter
     *
     * @param input the input wrapped as <tt>Value</tt>
     * @return a serialized string version of the given input which can later be resolved using
     * {@link ParameterBuilder#resolveFromString(Value)}
     */
    public String checkAndTransform(Value input) {
        return delegate.checkAndTransform(input);
    }

    /**
     * Reads and resolves the value for this parameter from the given context.
     *
     * @param context the context to read the parameter value from
     * @return the resolved value wrapped as optional or an empty optional if there is no value available
     */
    public Optional<V> get(Map<String, String> context) {
        return delegate.get(context);
    }

    /**
     * Reads and resolves the value for this parameter from the given context.
     * <p>
     * Fails if no value could be resolved from the given context.
     *
     * @param context the context to read the parameter value from
     * @return the resolved value
     * @throws sirius.kernel.health.HandledException if no value for this parameter is available in the given context
     */
    public V require(Map<String, String> context) {
        return delegate.require(context);
    }

    /**
     * Returns the name of the parameter.
     *
     * @return the name of the parameter
     */
    public String getName() {
        return delegate.getName();
    }

    /**
     * Returns the label of the parameter
     *
     * @return the {@link NLS#smartGet(String) auto translated} label of the parameter
     */
    public String getLabel() {
        return delegate.getLabel();
    }

    /**
     * Returns the description of the parameter
     *
     * @return the {@link NLS#smartGet(String) auto translated} description of the parameter
     */
    public String getDescription() {
        return delegate.getDescription();
    }

    /**
     * Determines if this parameter is required.
     *
     * @return <tt>true</tt> if a value has to be present for this parameter, <tt>false</tt> otherwise
     */
    public boolean isRequired() {
        return delegate.isRequired();
    }

    /**
     * Returns a {@link Parameter.LogVisibility} value which indicates in which log this parameter should be logged.
     *
     * @return an enum value indicating the log behavior of this parameter
     */
    public LogVisibility getLogVisibility() {
        return delegate.getLogVisibility();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends ParameterBuilder<?, ?>> getBuilderType() {
        return (Class<? extends ParameterBuilder<?, ?>>) delegate.getClass();
    }
}
