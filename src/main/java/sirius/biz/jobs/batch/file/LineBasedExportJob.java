/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.jobs.batch.file;

import sirius.biz.jobs.params.EnumParameter;
import sirius.biz.process.ProcessContext;
import sirius.biz.storage.layer3.FileOrDirectoryParameter;
import sirius.biz.storage.layer3.VirtualFile;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Exceptions;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Optional;

/**
 * Provides an export job which writes into a {@link LineBasedExport}.
 */
public abstract class LineBasedExportJob extends FileExportJob {

    /**
     * Used to write the generated rows in a format independent manner
     */
    protected LineBasedExport export;
    protected final ExportFileType fileType;

    /**
     * Creates a new job which writes line based data into the given destination.
     *
     * @param destinationParameter the parameter used to select the destination for the file being written
     * @param fileTypeParameter    the file type to use when writing the line based data
     * @param process              the context in which the process will be executed
     */
    protected LineBasedExportJob(FileOrDirectoryParameter destinationParameter,
                                 EnumParameter<ExportFileType> fileTypeParameter,
                                 ProcessContext process) {
        super(destinationParameter, process);
        this.fileType = process.getParameter(fileTypeParameter).orElse(null);
    }

    @Override
    public void execute() throws Exception {
        createExport();
        executeIntoExport();
    }

    /**
     * Executes the actual work and writes the generated rows into {@link #export}.
     *
     * @throws Exception in case of a severe problem which should terminate the job
     */
    protected abstract void executeIntoExport() throws Exception;

    @Override
    public void close() throws IOException {
        try {
            if (export != null) {
                export.close();
            }
        } catch (Exception e) {
            process.handle(e);
        }

        super.close();
    }

    /**
     * Determines which file type to use.
     * <p>
     * This will either determine the file type of a selected destination file (via its file extension) or use the
     * value provided via {@link #fileType}.
     * </p>
     *
     * @return the effective file type to use
     */
    protected ExportFileType determineEffectiveFileType() {
        return Optional.ofNullable(destination)
                       .map(VirtualFile::fileExtension)
                       .filter(Strings::isFilled)
                       .flatMap(extension -> Value.of(extension.toUpperCase()).getEnum(ExportFileType.class))
                       .orElse(fileType);
    }

    /**
     * Creates the appropriate {@link LineBasedExport} into {@link #export} based on the
     * {@link #determineEffectiveFileType() effective file type}.
     */
    protected void createExport() {
        ExportFileType type = determineEffectiveFileType();
        if (type == ExportFileType.CSV) {
            export = new ExportCSV(new OutputStreamWriter(createOutputStream()));
        } else if (type == ExportFileType.XLS) {
            export = new ExportXLS(this::createOutputStream);
        } else if (type == ExportFileType.XLSX) {
            export = new ExportXLSX(this::createOutputStream);
        } else {
            throw Exceptions.createHandled().withSystemErrorMessage("Unknown export file format: %s", type).handle();
        }
    }
}