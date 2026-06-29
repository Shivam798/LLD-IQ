package com.taskscheduler.enums;

/**
 * Lifecycle state of a scheduled task.
 *
 * <pre>
 *   SCHEDULED -> sitting in the priority queue, waiting for its fire time
 *   RUNNING   -> handed to a worker thread and currently executing
 *   COMPLETED -> a one-time (or finite) task finished and will not run again
 *   FAILED    -> a one-time task threw and will not run again
 *   CANCELLED -> removed by the caller; will be skipped if already dequeued
 * </pre>
 *
 * <p>A recurring task cycles SCHEDULED -> RUNNING -> SCHEDULED for each tick;
 * it only reaches COMPLETED/FAILED when its {@code Schedule} returns no next
 * fire time (e.g. a one-time schedule, or a finite one that is exhausted).
 */
public enum TaskStatus {
    SCHEDULED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
