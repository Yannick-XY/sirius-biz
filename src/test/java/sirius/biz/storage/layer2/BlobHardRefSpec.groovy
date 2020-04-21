/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.storage.layer1

import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import sirius.biz.storage.layer1.replication.ReplicationBackgroundLoop
import sirius.biz.storage.layer1.replication.ReplicationManager
import sirius.kernel.BaseSpecification
import sirius.kernel.Scope
import sirius.kernel.async.BackgroundLoop
import sirius.kernel.di.std.Part

import java.time.Duration

@Scope(Scope.SCOPE_NIGHTLY)
class ReplicationSpec extends BaseSpecification {

    @Part
    private static ObjectStorage storage

    @Part
    private static ReplicationManager replicationManager

    def awaitReplication() {
        BackgroundLoop.nextExecution(ReplicationBackgroundLoop.class).await(Duration.ofMinutes(1))
    }

    def "updates are replicated correctly"() {
        given:
        def testData = "test".getBytes(Charsets.UTF_8)
        when:
        storage.getSpace("repl-primary").upload("repl-update-test", new ByteArrayInputStream(testData), testData.length)
        and:
        awaitReplication()
        def downloaded = storage.getSpace("reply-secondary").download("repl-update-test")
        then:
        downloaded.isPresent()
        and:
        CharStreams.toString(new InputStreamReader(downloaded.get().getInputStream(), Charsets.UTF_8)) == "test"
    }

    def "deletes are replicated correctly"() {
        given:
        def testData = "test".getBytes(Charsets.UTF_8)
        when:
        storage.getSpace("repl-primary").upload("repl-delete-test", new ByteArrayInputStream(testData), testData.length)
        and:
        awaitReplication()
        and:
        storage.getSpace("repl-primary").delete("repl-delete-test")
        and:
        awaitReplication()
        def downloaded = storage.getSpace("reply-secondary").download("repl-delete-test")
        then:
        !downloaded.isPresent()
    }

}
