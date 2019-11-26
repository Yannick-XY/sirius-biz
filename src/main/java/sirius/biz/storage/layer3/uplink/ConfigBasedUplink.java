/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage.layer3.uplink;

import sirius.biz.storage.layer3.ChildProvider;
import sirius.biz.storage.layer3.FileSearch;
import sirius.biz.storage.layer3.MutableVirtualFile;
import sirius.biz.storage.layer3.VirtualFile;
import sirius.kernel.commons.Strings;
import sirius.kernel.settings.Extension;
import sirius.web.security.ScopeInfo;
import sirius.web.security.UserContext;
import sirius.web.security.UserInfo;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Represents an uplink which has been defined in the system config.
 * <p>
 * All uplinks are collected by the {@link ConfigBasedUplinksRoot}.
 */
public abstract class ConfigBasedUplink {

    protected String description;
    protected String permission;
    protected String name;
    protected Extension config;
    protected boolean readonly;
    protected VirtualFile file;

    protected ChildProvider innerChildProvider = new ChildProvider() {
        @Override
        public Optional<VirtualFile> findChild(VirtualFile directory, String name) {
            return findChildInDirectory(directory, name);
        }

        @Override
        public void enumerate(VirtualFile parent, FileSearch search) {
            enumerateDirectoryChildren(parent, search);
        }
    };

    /**
     * Creates a new instance based on the config section.
     *
     * @param config the configuration of this uplink
     */
    protected ConfigBasedUplink(Extension config) {
        this.readonly = config.get("readonly").asBoolean();
        this.description = config.get("description").asString();
        this.permission = config.get("permission").asString();
        this.name = config.get("name").asString(config.getId());
        this.config = config;
    }

    /**
     * Returns the root directory of this uplink.
     *
     * @param parent the parent to use
     * @return the root directory of this uplink
     */
    public VirtualFile getFile(VirtualFile parent) {
        if (file == null) {
            file = makeDirectory(parent);
        }

        return file;
    }

    /**
     * Determines if the current user may access this uplink.
     *
     * @return <tt>true</tt> if all checks were successful, <tt>false</tt> otherwise
     */
    public boolean checkPermission() {
        if (!Strings.areEqual(ScopeInfo.DEFAULT_SCOPE.getScopeId(), UserContext.getCurrentScope().getScopeId())) {
            return false;
        }

        UserInfo currentUser = UserContext.getCurrentUser();
        if (!currentUser.isLoggedIn()) {
            return false;
        }

        return currentUser.hasPermission(permission);
    }

    /**
     * Returns the effective directory name to use.
     *
     * @return the directory name of the virtual root directory
     */
    @Nonnull
    protected String getDirectoryName() {
        return name;
    }

    /**
     * Creates the virtual root directory.
     *
     * @param parent the parent to use
     * @return the virtual root directory
     */
    protected VirtualFile makeDirectory(@Nonnull VirtualFile parent) {
        return createDirectoryFile(parent).withDescription(description)
                                          .markAsExistingDirectory()
                                          .withChildren(innerChildProvider);
    }

    /**
     * Instantiates the virtual root directory.
     *
     * @param parent the parent to use
     * @return the virtual root directory
     */
    protected MutableVirtualFile createDirectoryFile(@Nonnull VirtualFile parent) {
        return new MutableVirtualFile(parent, getDirectoryName());
    }

    /**
     * Resolves the child with the given name in the given parent directory.
     *
     * @param parent the parent directory
     * @param name   the name of the child
     * @return the resolved child (existing or not) or an empty optional if the child cannot be resolved
     */
    protected abstract Optional<VirtualFile> findChildInDirectory(VirtualFile parent, String name);

    /**
     * Enumerates all children in the given parent matching the given search.
     *
     * @param parent the parent directory
     * @param search the search which defines all filters and collects results
     */
    protected abstract void enumerateDirectoryChildren(VirtualFile parent, FileSearch search);

    /**
     * Creates a {@link MutableVirtualFile} with the given parameters and attaches the default handlers known to this
     * class.
     *
     * @param parent   the parent file
     * @param filename the name of the file
     * @return a new virtual file which can be provided with additional handlers
     */
    protected MutableVirtualFile createVirtualFile(VirtualFile parent, String filename) {
        return new MutableVirtualFile(parent, filename).withCanDeleteHandler(this::canDeleteHandler)
                                                       .withCanCreateChildren(this::canCreateChildrenHandler)
                                                       .withCanCreateDirectoryHandler(this::canCreateDirectoryHandler)
                                                       .withCanProvideInputStream(this::canProvideInputStreamHandler)
                                                       .withCanProvideOutputStream(this::canProvideOutputStreamHandler)
                                                       .withCanRenameHandler(this::canRenameHandler)
                                                       .withChildren(innerChildProvider);
    }

    /**
     * Provides a base implementation which can be passed into
     * {@link MutableVirtualFile#withCanProvideOutputStream(Predicate)}.
     *
     * @param file the file to check
     * @return <tt>true</tt> if an output stream can be supplied, <tt>false</tt> otherwise
     */
    protected boolean canProvideOutputStreamHandler(VirtualFile file) {
        return !readonly && !file.isDirectory();
    }

    /**
     * Provides a base implementation which can be passed into
     * {@link MutableVirtualFile#withCanProvideInputStream(Predicate)}.
     *
     * @param file the file to check
     * @return <tt>true</tt> if an input stream can be supplied, <tt>false</tt> otherwise
     */
    protected boolean canProvideInputStreamHandler(VirtualFile file) {
        return file.exists() && !file.isDirectory();
    }

    /**
     * Provides a base implementation which can be passed into
     * {@link MutableVirtualFile#withCanCreateDirectoryHandler(Predicate)}.
     *
     * @param file the file to check
     * @return <tt>true</tt> if the file can be created as child directory, <tt>false</tt> otherwise
     */
    protected boolean canCreateDirectoryHandler(VirtualFile file) {
        return !readonly && (!file.exists() || file.isDirectory());
    }

    /**
     * Provides a base implementation which can be passed into
     * {@link MutableVirtualFile#withCanCreateChildren(Predicate)}.
     *
     * @param file the file to check
     * @return <tt>true</tt> if child files can be created, <tt>false</tt> otherwise
     */
    protected boolean canCreateChildrenHandler(VirtualFile file) {
        return file.exists() && file.isDirectory() && !readonly;
    }

    /**
     * Provides a base implementation which can be passed into
     * {@link MutableVirtualFile#withCanDeleteHandler(Predicate)}.
     *
     * @param file the file to check
     * @return <tt>true</tt> if the file can be deleted, <tt>false</tt> otherwise
     */
    protected boolean canDeleteHandler(VirtualFile file) {
        return !readonly && file.exists();
    }

    /**
     * Provides a base implementation which can be passed into
     * {@link MutableVirtualFile#withCanRenameHandler(Predicate)}.
     *
     * @param file the file to check
     * @return <tt>true</tt> if the file can be renamed, <tt>false</tt> otherwise
     */
    protected boolean canRenameHandler(VirtualFile file) {
        return !readonly && file.exists();
    }
}
