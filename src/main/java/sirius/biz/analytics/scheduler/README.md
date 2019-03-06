# Analytics Scheduling System

This framework is responsible for executing 
[analytical tasks](AnalyticalTask.java) on all kinds of entities. To 
distribute the execution across a cluster of machines and to manage the execution of
these tasks the [distributed tasks framework](../../cluster/work) is used.

To maintain high efficiency, entities for which tasks are to be executed, are grouped
into batches which are then managed as single distributed task. The creating of
appropriate batches is performed by [schedulers](AnalyticsScheduler.java). There
are two base implementations readily available, one for [JDBC](SQLEntityBatchScheduler.java)-
and one for [MongoDB](MongoEntityBatchScheduler.java) entities.

Schedulers can one of two different flavors: **Guaranteed execution** or **best effort
execution**. The former will always be executed in their desired interval where the latter
will only be scheduled for execution if the underlying job queue (*analytics-best-effort*)
is empty. An example using this execution model would be monthly computed metrics. For this
scenario we'd have **guaranteed executing scheduler** which performs the execution once
per month and an additional **best effort** one which tries to compute the computations each
day in the current month, so that the relected values are up to date (as long as the system 
isn't overloaded).

