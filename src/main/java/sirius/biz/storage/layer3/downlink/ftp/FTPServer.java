/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage.layer3.downlink.ftp;

import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import sirius.biz.storage.util.StorageUtils;
import sirius.kernel.Startable;
import sirius.kernel.Stoppable;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Priorized;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

/**
 * Provides a bridge between the {@link sirius.biz.storage.layer3.VirtualFileSystem} and the Apache FTP server.
 */
@Register(classes = {Startable.class, Stoppable.class})
public class FTPServer implements Startable, Stoppable {

    private FtpServer ftpServer;

    @ConfigValue("storage.layer3.downlink.ftp.port")
    private int ftpPort;

    @ConfigValue("storage.layer3.downlink.ftp.passivePorts")
    private String passivePorts;

    @ConfigValue("storage.layer3.downlink.ftp.bindAddress")
    private String bindAddress;

    @ConfigValue("storage.layer3.downlink.ftp.passiveExternalAddress")
    private String passiveExternalAddress;

    @ConfigValue("storage.layer3.downlink.ftp.idleTimeout")
    private Duration idleTimeout;

    @ConfigValue("storage.layer3.downlink.ftp.keystore")
    private String keystore;

    @ConfigValue("storage.layer3.downlink.ftp.keystorePassword")
    private String keystorePassword;

    @ConfigValue("storage.layer3.downlink.ftp.keyAlias")
    private String keyAlias;

    @ConfigValue("storage.layer3.downlink.ftp.forceSSL")
    private boolean forceSSL;

    @Override
    public int getPriority() {
        return Priorized.DEFAULT_PRIORITY + 100;
    }

    @Override
    public void started() {
        if (ftpPort <= 0) {
            return;
        }

        disableLogging();
        createFTPServer();
        startFTPServer();
    }

    @Override
    public void stopped() {
        if (ftpServer != null) {
            try {
                ftpServer.stop();
            } catch (Exception e) {
                Exceptions.handle()
                          .to(StorageUtils.LOG)
                          .error(e)
                          .withSystemErrorMessage("Layer3/FTP: Failed to stop FTP server on port %s (%s): %s (%s)",
                                                  ftpPort,
                                                  bindAddress)
                          .handle();
            }
        }
    }

    private void startFTPServer() {
        try {
            ftpServer.start();
            StorageUtils.LOG.INFO("Layer3/FTP: Started FTP server on port %s (%s)", ftpPort, bindAddress);
        } catch (FtpException e) {
            Exceptions.handle()
                      .to(StorageUtils.LOG)
                      .error(e)
                      .withSystemErrorMessage("Layer3/FTP: Failed to start FTP server on port %s (%s): %s (%s)",
                                              ftpPort,
                                              bindAddress)
                      .handle();
        }
    }

    private void createFTPServer() {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();
        setupNetwork(factory);
        setupSSL(factory);
        setupFtplets(serverFactory);

        serverFactory.setFileSystem(ignored -> new BridgeFileSystemView());
        serverFactory.setUserManager(new BridgeUserManager());
        serverFactory.setConnectionConfig(new ConfigBasedConnectionConfig());

        serverFactory.addListener("default", factory.createListener());

        ftpServer = serverFactory.createServer();
    }

    private void setupNetwork(ListenerFactory factory) {
        factory.setPort(ftpPort);
        factory.setIdleTimeout((int) idleTimeout.getSeconds());

        if (Strings.isFilled(bindAddress)) {
            factory.setServerAddress(bindAddress);
        }

        DataConnectionConfigurationFactory dataConnectionConfigurationFactory =
                new DataConnectionConfigurationFactory();

        if (Strings.isFilled(passivePorts)) {
            dataConnectionConfigurationFactory.setPassivePorts(passivePorts);
        }

        if (Strings.isFilled(passiveExternalAddress)) {
            dataConnectionConfigurationFactory.setPassiveExternalAddress(passiveExternalAddress);
        }

        factory.setDataConnectionConfiguration(dataConnectionConfigurationFactory.createDataConnectionConfiguration());
    }

    private void setupFtplets(FtpServerFactory serverFactory) {
        Map<String, Ftplet> ftplets = new TreeMap<>();
        ftplets.put("bridge", new BridgeFtplet());
        serverFactory.setFtplets(ftplets);
    }

    private void setupSSL(ListenerFactory factory) {
        if (Strings.isEmpty(keystore)) {
            return;
        }

        SslConfigurationFactory ssl = new SslConfigurationFactory();
        ssl.setKeystoreFile(new File(keystore));
        ssl.setKeystorePassword(keystorePassword);
        ssl.setKeyAlias(keyAlias);
        factory.setSslConfiguration(ssl.createSslConfiguration());
        factory.setImplicitSsl(forceSSL);
    }

    private void disableLogging() {
        // The Apache FTP Server is wayyy too chatty.....
        Logger.getLogger("org.apache.ftpserver").setLevel(Level.ERROR);
    }
}