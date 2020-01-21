/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.jobs.batch.file;

import sirius.biz.importer.ImportContext;
import sirius.biz.importer.Importer;
import sirius.biz.importer.format.ImportDictionary;
import sirius.biz.jobs.infos.JobInfoCollector;
import sirius.biz.jobs.params.Parameter;
import sirius.biz.process.ProcessContext;
import sirius.biz.storage.layer3.FileParameter;
import sirius.db.mixing.BaseEntity;
import sirius.db.mixing.query.Query;
import sirius.kernel.commons.Explain;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.util.List;
import java.util.function.Consumer;

/**
 * Provides a base implementation for batch jobs which export entities into line based files using a
 * {@link EntityExportJob}.
 *
 * @param <E> the type of entities being exported
 * @param <Q> the query type used to select entities if all are to be exported
 */
public abstract class EntityExportJobFactory<E extends BaseEntity<?>, Q extends Query<Q, E, ?>>
        extends LineBasedExportJobFactory {

    protected final FileParameter templateFileParameter;

    protected EntityExportJobFactory() {
        templateFileParameter = new FileParameter("templateFile", "$EntityExportJobFactory.templateFile");
        templateFileParameter.withDescription("$EntityExportJobFactory.templateFile.help");
        templateFileParameter.withBasePath("/work");
    }

    @Override
    protected void collectParameters(Consumer<Parameter<?, ?>> parameterCollector) {
        super.collectParameters(parameterCollector);
        parameterCollector.accept(templateFileParameter);
    }

    @SuppressWarnings("squid:S2095")
    @Explain("The job must not be closed here as it is returned and managed by the caller.")
    @Override
    protected EntityExportJob<E, Q> createJob(ProcessContext process) {
        // We only resolve the parameters once and keep the final values around in a local context...
        ImportContext paramterContext = new ImportContext();
        transferParameters(paramterContext, process);

        return new EntityExportJob<E, Q>(templateFileParameter,
                                         destinationParameter,
                                         fileTypeParameter,
                                         getExportType(),
                                         getDictionary(),
                                         getDefaultMapping(),
                                         process).withQueryExtender(query -> extendSelectQuery(query, process))
                                                 .withContextExtender(context -> context.putAll(paramterContext))
                                                 .withFileName(setCustomFileName());
    }

    /**
     * Permits to transfer parameters into the import context.
     * <p>
     * This is required as an export can process partial template files by looking up the matching entities and then
     * completing the rows.
     *
     * @param context        the context to enrich. This will be transferred to the underlying {@link Importer} and
     *                       {@link sirius.biz.importer.ImportHandler import handlers}
     * @param processContext the process context used to resolve parameter values
     */
    protected void transferParameters(ImportContext context, ProcessContext processContext) {
        // nothing to transfer by default
    }

    /**
     * Permits to provide a custom file name when exporting the entity.
     *
     * Otherwise the "end user friendly" plural of the entity is used as target file name
     */
    protected String setCustomFileName() {
        return null;
    }

    /**
     * Permits to add additional constraints on the query used to select all exportable entities.
     *
     * @param query          the query to extend
     * @param processContext the current process which can be used to extract parameters
     */
    protected void extendSelectQuery(Q query, ProcessContext processContext) {
        // Nothing to add by default
    }

    /**
     * Creates the dictionary used by the export.
     *
     * @return the dictionary being used
     */
    protected ImportDictionary getDictionary() {
        try (Importer importer = new Importer("getDictionary")) {
            ImportDictionary dictionary = importer.getExportDictionary(getExportType());
            enhanceDictionary(dictionary);
            return dictionary;
        } catch (Exception e) {
            throw Exceptions.handle(Log.BACKGROUND, e);
        }
    }

    /**
     * Defines the default list of export columns.
     *
     * @return the columns to export unless a custom list is given via a template file
     */
    protected List<String> getDefaultMapping() {
        try (Importer importer = new Importer("getDefaultMapping")) {
            return importer.findHandler(getExportType()).getDefaultExportMapping();
        } catch (Exception e) {
            throw Exceptions.handle(Log.BACKGROUND, e);
        }
    }

    /**
     * Returns the main type being exported by this job.
     *
     * @return the type of entities being imported
     */
    protected abstract Class<E> getExportType();

    /**
     * Adds the possibility to enhance a dicitonary during the setup of the job
     *
     * @param dictionary the dictionary to enhance
     */
    @SuppressWarnings("squid:S1186")
    @Explain("Do nothing by default since we only need this for exports which contain more than one Entity")
    protected void enhanceDictionary(ImportDictionary dictionary) {
    }

    @Override
    protected void collectJobInfos(JobInfoCollector collector) {
        super.collectJobInfos(collector);
        collector.addTranslatedWell("EntityExportJobFactory.templateModes");
        getDictionary().emitJobInfos(collector);
    }
}
