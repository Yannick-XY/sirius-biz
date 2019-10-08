/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage.layer3.downlink.ssh.scp;

import org.apache.sshd.common.scp.ScpTargetStreamResolver;
import org.apache.sshd.common.scp.ScpTimestamp;
import org.apache.sshd.common.session.Session;
import sirius.biz.storage.layer3.VirtualFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

class BridgeScpTargetStreamResolver implements ScpTargetStreamResolver {
    private final Path path;
    private final VirtualFile virtualFile;

    protected BridgeScpTargetStreamResolver(Path path, VirtualFile virtualFile) {
        this.path = path;
        this.virtualFile = virtualFile;
    }

    @Override
    public OutputStream resolveTargetStream(Session session,
                                            String name,
                                            long length,
                                            Set<PosixFilePermission> perms,
                                            OpenOption... options) throws IOException {
        return virtualFile.createOutputStream();
    }

    @Override
    public Path getEventListenerFilePath() {
        return path;
    }

    @Override
    public void postProcessReceivedData(String name,
                                        boolean preserve,
                                        Set<PosixFilePermission> perms,
                                        ScpTimestamp time) throws IOException {

    }
}
