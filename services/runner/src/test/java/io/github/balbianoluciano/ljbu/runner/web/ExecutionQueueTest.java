package io.github.balbianoluciano.ljbu.runner.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ExecutionQueueTest {

  @Test
  void turnsAwayAnExecutionWhenTheQueueIsFull() throws Exception {
    CountDownLatch running = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    try (ExecutionQueue queue = new ExecutionQueue(1, 1);
        ExecutorService callers = Executors.newFixedThreadPool(2)) {
      Future<String> first =
          callers.submit(
              () ->
                  queue.run(
                      () -> {
                        running.countDown();
                        release.await();
                        return "first";
                      }));
      assertThat(running.await(5, TimeUnit.SECONDS)).isTrue();
      Future<String> waiting = callers.submit(() -> queue.run(() -> "waiting"));
      awaitQueued(queue);

      assertThatThrownBy(() -> queue.run(() -> "one too many"))
          .isInstanceOf(ExecutionQueue.RunnerBusyException.class);

      release.countDown();
      assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("first");
      assertThat(waiting.get(5, TimeUnit.SECONDS)).isEqualTo("waiting");
    }
  }

  @Test
  void passesOnTheFailureOfAnExecution() {
    try (ExecutionQueue queue = new ExecutionQueue(1, 1)) {
      assertThatThrownBy(
              () ->
                  queue.run(
                      () -> {
                        throw new IllegalArgumentException("boom");
                      }))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("boom");
    }
  }

  private static void awaitQueued(ExecutionQueue queue) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (queue.waiting() == 0 && System.nanoTime() < deadline) {
      Thread.sleep(5);
    }
    assertThat(queue.waiting()).isEqualTo(1);
  }
}
