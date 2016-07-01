/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.protocol;

import sirius.biz.web.DateRange;
import sirius.biz.web.PageHelper;
import sirius.db.mixing.OMA;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.nls.NLS;
import sirius.web.controller.BasicController;
import sirius.web.controller.Controller;
import sirius.web.controller.DefaultRoute;
import sirius.web.controller.Routed;
import sirius.web.http.WebContext;
import sirius.web.security.Permission;

/**
 * Created by aha on 10.03.16.
 */
@Register(classes = Controller.class, framework = Protocols.FRAMEWORK_PROTOCOLS)
public class LogsController extends BasicController {

    @Part
    private OMA oma;

    @Permission(Protocols.PERMISSION_VIEW_PROTOCOLS)
    @DefaultRoute
    @Routed("/system/logs")
    public void logs(WebContext ctx) {
        PageHelper<LogEntry> ph = PageHelper.withQuery(oma.select(LogEntry.class).orderDesc(LogEntry.TOD));
        ph.withContext(ctx);
        ph.addQueryFacet(LogEntry.CATEGORY.getName(),
                         NLS.get("LogEntry.category"),
                         q -> q.copy().distinctFields(LogEntry.CATEGORY, LogEntry.CATEGORY).asSQLQuery());
        ph.addQueryFacet(LogEntry.LEVEL.getName(),
                         NLS.get("LogEntry.level"),
                         q -> q.copy().distinctFields(LogEntry.LEVEL, LogEntry.LEVEL).asSQLQuery());
        ph.addTimeFacet(LogEntry.TOD.getName(),
                        NLS.get("LogEntry.tod"),
                        DateRange.lastFiveMinutes(),
                        DateRange.lastFiveteenMinutes(),
                        DateRange.lastTwoHours(),
                        DateRange.today(),
                        DateRange.yesterday(),
                        DateRange.thisWeek(),
                        DateRange.lastWeek());
        ph.withSearchFields(LogEntry.CATEGORY, LogEntry.LEVEL, LogEntry.MESSAGE);

        ctx.respondWith().template("view/protocol/logs.html", ph.asPage());
    }
}
