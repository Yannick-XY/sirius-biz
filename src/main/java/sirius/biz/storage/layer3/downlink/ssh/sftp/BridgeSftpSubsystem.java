/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage.layer3.downlink.ssh.sftp;

import org.apache.sshd.common.util.threads.CloseableExecutorService;
import org.apache.sshd.server.subsystem.sftp.SftpErrorStatusDataHandler;
import org.apache.sshd.server.subsystem.sftp.SftpFileSystemAccessor;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystem;
import org.apache.sshd.server.subsystem.sftp.UnsupportedAttributePolicy;
import sirius.biz.storage.layer3.downlink.ssh.BridgeSession;
import sirius.biz.storage.util.StorageUtils;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;
import sirius.web.security.ScopeDetector;
import sirius.web.security.ScopeInfo;
import sirius.web.security.UserContext;
import sirius.web.security.UserInfo;

import javax.annotation.Nullable;

/**
 * Wraps the SFTP subsystem to execute as the correct {@link UserInfo user}.
 */
class BridgeSftpSubsystem extends SftpSubsystem {

    @Part
    @Nullable
    private static ScopeDetector detector;

    protected BridgeSftpSubsystem(CloseableExecutorService executorService,
                                  UnsupportedAttributePolicy policy,
                                  SftpFileSystemAccessor accessor,
                                  SftpErrorStatusDataHandler errorStatusDataHandler) {
        super(executorService, policy, accessor, errorStatusDataHandler);
    }

    @Override
    public void run() {
        try {
            UserInfo user = ((BridgeSession) getServerSession()).getUser();
            String scopeId = ((BridgeSession) getServerSession()).getScopeId();

            UserContext.get().setCurrentScope(ScopeInfo.DEFAULT_SCOPE);
            if (Strings.isFilled(scopeId) && detector != null) {
                detector.findScopeById(scopeId).ifPresent(UserContext.get()::setCurrentScope);
            }

            UserContext.get().runAs(user, super::run);
        } catch (Exception e) {
            Exceptions.handle()
                      .to(StorageUtils.LOG)
                      .error(e)
                      .withSystemErrorMessage("Layer 3/SFTP: An error occurred: %s (%s)")
                      .handle();
        }
    }
}
