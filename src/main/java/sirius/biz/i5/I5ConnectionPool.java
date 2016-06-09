package sirius.biz.i5;

import com.ibm.as400.access.AS400;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import sirius.kernel.commons.Watch;
import sirius.kernel.extensions.Extension;
import sirius.kernel.health.Exceptions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides access to a pool of connections to an IBM iSeries.
 */
class I5ConnectionPool implements PooledObjectFactory<I5Connection> {

    private String name;
    private String host;
    private String username;
    private String password;
    protected I5Connector i5Connector;
    private final Consumer<I5Connection> initializer;

    protected GenericObjectPool<I5Connection> connectionPool;
    protected List<WeakReference<I5Connection>> openConnections =
            Collections.synchronizedList(new ArrayList<WeakReference<I5Connection>>());

    /**
     * Creates  a new pool with the given name, configuration and initializer
     *
     * @param name        the name of the pool
     * @param i5Connector the shared connector instance used to report statistics
     * @param ext         the extension used to configure the pool
     * @param initializer the initializer method used to setup a new connection
     */
    I5ConnectionPool(String name, I5Connector i5Connector, Extension ext, Consumer<I5Connection> initializer) {
        super();
        this.name = name;
        this.i5Connector = i5Connector;
        this.initializer = initializer;
        this.host = ext.get("host").asString();
        this.username = ext.get("username").asString();
        this.password = ext.get("password").asString();

        connectionPool = new GenericObjectPool<>(this);
        connectionPool.setTestOnBorrow(true);
        connectionPool.setMaxTotal(ext.get("maxActive").asInt(10));
        connectionPool.setMaxIdle(ext.get("maxIdle").asInt(1));
    }

    @Override
    public PooledObject<I5Connection> makeObject() throws Exception {
        try {
            Watch w = Watch.start();
            I5Connection result = new I5Connection();
            result.pool = this;
            result.i5 = new AS400(host, username, password);
            result.initialize();
            w.submitMicroTiming("UPOS", "I5ConnectionPool.makeObject");
            openConnections.add(new WeakReference<I5Connection>(result));
            return new DefaultPooledObject<>(result);
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(I5Connector.LOG)
                            .error(e)
                            .withSystemErrorMessage("Cannot create connection to %s as %s: %s (%s)", host, username)
                            .handle();
        }
    }

    @Override
    public void destroyObject(PooledObject<I5Connection> pooledObject) throws Exception {
        if (pooledObject != null && pooledObject.getObject() != null) {
            try {
                try {
                    openConnections.remove(new WeakReference<I5Connection>(pooledObject.getObject()));
                } catch (Throwable t) {
                    I5Connector.LOG.WARN(t);
                }
                pooledObject.getObject().release();
            } catch (Throwable e) {
                throw Exceptions.handle()
                                .to(I5Connector.LOG)
                                .error(e)
                                .withSystemErrorMessage("Error while closing connection to %s as %s: %s (%s)",
                                                        host,
                                                        username)
                                .handle();
            }
        }
    }

    @Override
    public boolean validateObject(PooledObject<I5Connection> pooledObject) {
        if (pooledObject != null && pooledObject.getObject() != null) {
            return pooledObject.getObject().check();
        } else {
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<I5Connection> pooledObject) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<I5Connection> pooledObject) throws Exception {

    }

    /**
     * Releases all connections
     */
    void release() {
        try {
            connectionPool.close();
        } catch (Throwable e) {
            Exceptions.handle()
                      .to(I5Connector.LOG)
                      .error(e)
                      .withSystemErrorMessage("Error while releasing all connections to %s as %s: %s (%s)",
                                              host,
                                              username)
                      .handle();
        }
    }

    /**
     * Retrieves a new connection.
     *
     * @return a new connection to the configured machine
     */
    I5Connection getConnection() {
        try {
            i5Connector.borrows.inc();
            I5Connection result = connectionPool.borrowObject();
            result.borrowed = true;
            return result;
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(I5Connector.LOG)
                            .error(e)
                            .withSystemErrorMessage("Error acquiring a connection to %s as %s: %s (%s)", host, username)
                            .handle();
        }
    }

    /**
     * Releases / returns the given connection into the pool
     *
     * @param con the connection to return
     */
    void returnConnection(I5Connection con) {
        try {
            con.borrowed = false;
            connectionPool.returnObject(con);
        } catch (Throwable e) {
            Exceptions.handle()
                      .to(I5Connector.LOG)
                      .error(e)
                      .withSystemErrorMessage("Error while returning a pooled connection to %s as %s: %s (%s)",
                                              host,
                                              username)
                      .handle();
        }
    }

    /**
     * Initializes the connection if required
     *
     * @param i5Connection the connection to initialize
     */
    void initConnection(I5Connection i5Connection) {
        if (initializer != null) {
            initializer.accept(i5Connection);
        }
    }

    @Override
    public String toString() {
        return name + " (" + host + ")";
    }
}
