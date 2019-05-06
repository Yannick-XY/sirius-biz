/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.analytics.offheap.memory;

import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class LargeMemoryPool {

    private ConcurrentHashMap<String, Allocator> allocators = new ConcurrentHashMap<>();
    private String name;

    public LargeMemoryPool(String name) {
        this.name = name;
    }

    public void execute(Consumer<Function<String, Allocator>> job) {
        try {
            job.accept(this::createAllocator);
        } catch(Exception e) {
            throw Exceptions.handle(Log.BACKGROUND, e);
        } finally {
            release();
        }
    }

    private Allocator createAllocator(String name) {
        allocators.put(name, new Allocator(name));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("\n-------------------\n");
        for (Allocator alloc : allocators.values()) {
            sb.append(alloc).append("\n");
        }
        sb.append("-------------------\n");

        return sb.toString();
    }

    public void release() {
        allocators.values().forEach(Allocator::release);
    }
}
