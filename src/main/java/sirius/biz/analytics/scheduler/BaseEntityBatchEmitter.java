/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.analytics.scheduler;

import com.alibaba.fastjson.JSONObject;
import sirius.db.mixing.BaseEntity;
import sirius.db.mixing.BaseMapper;
import sirius.db.mixing.Mixing;
import sirius.db.mixing.query.Query;
import sirius.db.mixing.query.constraints.Constraint;
import sirius.kernel.async.TaskContext;
import sirius.kernel.commons.ValueHolder;
import sirius.kernel.di.std.Part;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Provides a facility to execute a query into a set of batch descriptions.
 * <p>
 * These batches are described using JSON and can be evaluated into an iterator of entities using
 * {@link #evaluateBatch(JSONObject, Consumer, Consumer)}.
 *
 * @param <I> the id type of the entities being processed by this emitter
 * @param <C> the constraint type to be applied on entities processed by this emitter
 * @param <B> the entity type being processed by this emitter
 * @param <Q> the query type used to retrieve entities being processed by this emitter
 */
public abstract class BaseEntityBatchEmitter<I, C extends Constraint, B extends BaseEntity<I>, Q extends Query<Q, B, C>> {

    /**
     * Contains the type of entities being batched.
     */
    public static final String TYPE = "type";

    /**
     * Contains the first id in the batch.
     */
    public static final String START_ID = "startId";

    /**
     * Contains the last id in the batch.
     */
    public static final String END_ID = "endId";

    @Part
    protected Mixing mixing;

    /**
     * Creates a query for the given type of entities and yields a number of batch descriptions.
     *
     * @param type          the type of entities being queried
     * @param queryExtender an extender which can further narrow down the entities being queried
     * @param batchSize     the size of each batch
     * @param batchConsumer a consumer which processes the batch descriptions
     */
    @SuppressWarnings("unchecked")
    public <E extends B> void computeBatches(Class<E> type,
                                             @Nullable Consumer<Q> queryExtender,
                                             int batchSize,
                                             Predicate<JSONObject> batchConsumer) {
        TaskContext taskContext = TaskContext.get();
        ValueHolder<I> lastLimit = ValueHolder.of(null);
        while (taskContext.isActive()) {
            Q query = (Q) getMapper().select(type);
            if (lastLimit.get() != null) {
                query.where(getMapper().filters().gt(BaseEntity.ID, lastLimit.get()));
            }
            if (queryExtender != null) {
                queryExtender.accept(query);
            }

            ValueHolder<I> nextLimit = ValueHolder.of(null);
            query.orderAsc(BaseEntity.ID).limit(batchSize).iterateAll(e -> {
                nextLimit.set(e.getId());
            });

            if (nextLimit.get() == null) {
                return;
            }

            JSONObject batch = new JSONObject();
            batch.put(TYPE, Mixing.getNameForType(type));
            batch.put(START_ID, lastLimit.get());
            batch.put(END_ID, nextLimit.get());
            if (!batchConsumer.test(batch)) {
                return;
            }

            lastLimit.set(nextLimit.get());
        }
    }

    protected abstract BaseMapper<B, C, ?> getMapper();

    /**
     * Resolves a JSON batch description and supplies the given consumer with all associated entities.
     *
     * @param batchDescription the batch description as generated by
     *                         {@link #computeBatches(Class, Consumer, int, Predicate)}
     * @param queryExtender    the query extender which was also passed into <tt>computeBatches</tt>
     * @param entityConsumer   the consumer to be supplid with all entities in the batch
     */
    @SuppressWarnings("unchecked")
    public <E extends B> void evaluateBatch(JSONObject batchDescription,
                                            @Nullable Consumer<Q> queryExtender,
                                            Consumer<E> entityConsumer) {
        String startId = batchDescription.getString(START_ID);
        String endId = batchDescription.getString(END_ID);
        String typeName = batchDescription.getString(TYPE);

        Class<E> type = (Class<E>) mixing.getDescriptor(typeName).getType();
        Q query = (Q) getMapper().select(type);
        query.where(getMapper().filters().gte(BaseEntity.ID, startId))
             .where(getMapper().filters().lte(BaseEntity.ID, endId));
        if (queryExtender != null) {
            queryExtender.accept(query);
        }

        ((Query<?, E, C>) query).iterateAll(entityConsumer);
    }
}