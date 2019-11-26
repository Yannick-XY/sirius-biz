/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage.layer3.uplink.ftp;

import org.apache.commons.net.ftp.FTPClient;
import sirius.biz.storage.layer3.uplink.util.UplinkConnectorConfig;
import sirius.biz.storage.util.StorageUtils;
import sirius.kernel.health.Exceptions;
import sirius.kernel.settings.Extension;

import java.io.IOException;

/**
 * Keeps the configuration used to build a FTP connector using the
 * {@link sirius.biz.storage.layer3.uplink.util.UplinkConnectorPool}.
 */
class FTPUplinkConnectorConfig extends UplinkConnectorConfig<FTPClient> {

    private static final int DEFAULT_FTP_PORT = 21;

    protected FTPUplinkConnectorConfig(Extension config) {
        super(config);
    }

    @Override
    protected int getDefaultPort() {
        return DEFAULT_FTP_PORT;
    }

    @Override
    protected FTPClient create() {
        try {
            FTPClient client = new FTPClient();
            client.setConnectTimeout(connectTimeoutMillis);
            client.setDataTimeout(readTimeoutMillis);
            client.setDefaultTimeout(readTimeoutMillis);
            client.connect(host, port);
            client.login(user, password);
            client.enterLocalPassiveMode();

            return client;
        } catch (IOException e) {
            throw Exceptions.handle()
                            .to(StorageUtils.LOG)
                            .error(e)
                            .withSystemErrorMessage(
                                    "Layer 3/FTP: An error occurred while connecting the uplink %s: %s (%s)",
                                    this)
                            .handle();
        }
    }

    @Override
    protected boolean validate(FTPClient connector) {
        return connector.isConnected();
    }

    @Override
    protected void safeClose(FTPClient connector) {
        try {
            connector.disconnect();
        } catch (IOException e) {
            Exceptions.handle()
                      .to(StorageUtils.LOG)
                      .error(e)
                      .withSystemErrorMessage(
                              "Layer 3/FTP: An error occurred while disconnecting the uplink %s: %s (%s)",
                              this)
                      .handle();
        }
    }
}
