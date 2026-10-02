package io.github.balbianoluciano.ljbu.runner.web;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Bounds how many child JVMs run at once (docs/SECURITY.md §3, layer 1): a fixed number of workers
 * and a queue with a maximum size. When the queue is full the request is turned away.
 */
public class ExecutionQueue implements AutoCloseable {

  private final ThreadPoolExecutor workers;

  public ExecutionQueue(int maxConcurrent, int queueCapacity) {
    this.workers =
        new ThreadPoolExecutor(
            maxConcurrent,
            maxConcurrent,
            0,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCapacity));
  }

  /**
   * @throws RunnerBusyException if the queue is full
   */
  public <T> T run(Callable<T> execution) throws InterruptedException {
    try {
      return workers.submit(execution).get();
    } catch (RejectedExecutionException e) {
      throw new RunnerBusyException();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof RuntimeException failure) {
        throw failure;
      }
      throw new IllegalStateException("The execution failed", e.getCause());
    }
  }

  /** Executions accepted that have not started yet. */
  public int waiting() {
    return workers.getQueue().size();
  }

  @Override
  public void close() {
    workers.shutdownNow();
  }

  public static class RunnerBusyException extends RuntimeException {}
}
