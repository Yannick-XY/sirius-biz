/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.analytics.events;

import sirius.db.mixing.Mapping;
import sirius.db.mixing.annotations.BeforeSave;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Strings;
import sirius.web.controller.ControllerDispatcher;
import sirius.web.http.WebContext;

/**
 * Records a page view or page impression.
 * <p>
 * This can be used either by the backend (aka {@link sirius.web.security.ScopeInfo#DEFAULT_SCOPE}) or within
 * other scopes.
 * <p>
 * Recording a page impression is as easy as:
 * <pre>{@code
 *  @Part
 *  EventRecorder recorder;
 *
 *  void doStuff() {
 *      recorder.record(new PageImpressionEvent().withAggregationUrl("/my/lovely/page"));
 *  }
 *
 * }</pre>
 *
 * @see EventRecorder
 * @see #withAggregationUrl(String)
 */
public class PageImpressionEvent extends Event {

    /**
     * Contains a generic or shortened URI which can be used to aggregate on.
     * <p>
     * If, for example a web shop would record views of items with urls like "/item/0815" and "/item/0816", these
     * would end up in {@link WebData#URL}. However, to sum up the total view of items one could use "/item/:1" as
     * <b>aggregationUri</b>.
     * <p>
     * If no explicit value is given, but a controller {@link sirius.web.controller.Routed route} is hit,
     * is pattern will be used.
     */
    public static final Mapping AGGREGATION_URI = Mapping.named("aggregationUri");
    private String aggregationUri;

    /**
     * Contains the effectively requested URI.
     * <p>
     * If not explicit value is given, we use the {@link WebContext#getRequestedURI() requested URI}.
     */
    public static final Mapping URI = Mapping.named("uri");
    private String uri;

    /**
     * Contains the current user, tenant and scope if available.
     */
    public static final Mapping USER_DATA = Mapping.named("userData");
    private final UserData userData = new UserData();

    /**
     * Contains metadata about the HTTP request (user-agent, url).
     */
    public static final Mapping WEB_DATA = Mapping.named("webData");
    private final WebData webData = new WebData();

    @BeforeSave
    protected void fillAndCheck() {
        if (Strings.isEmpty(uri) || Strings.isEmpty(aggregationUri)) {
            WebContext webContext = CallContext.getCurrent().get(WebContext.class);
            if (webContext.isValid()) {
                if (Strings.isEmpty(uri)) {
                    uri = webContext.getRequestedURI();
                }
                if (Strings.isEmpty(aggregationUri)) {
                    aggregationUri = webContext.get(ControllerDispatcher.ATTRIBUTE_MATCHED_ROUTE).getString();
                }
            }
        }

        if (Strings.isEmpty(uri)) {
            throw new IllegalArgumentException("Please provide an URI");
        }
        if (Strings.isEmpty(aggregationUri)) {
            throw new IllegalArgumentException("Please provide an aggregation URI");
        }
    }

    /**
     * Specifies the aggregation URL to use.
     *
     * @param aggregationUri a shortened or generic URI
     * @return the event itself for fluent method calls
     * @see #aggregationUri
     */
    public PageImpressionEvent withAggregationUrl(String aggregationUri) {
        this.aggregationUri = aggregationUri;
        return this;
    }

    public String getAggregationUri() {
        return aggregationUri;
    }

    public UserData getUserData() {
        return userData;
    }

    public WebData getWebData() {
        return webData;
    }
}
