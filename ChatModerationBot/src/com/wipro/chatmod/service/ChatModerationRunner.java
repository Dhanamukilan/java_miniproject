package com.wipro.chatmod.service;

import com.wipro.chatmod.bean.ChatMessage;
import com.wipro.chatmod.util.ModerationException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ChatModerationRunner
 * ---------------------
 * Application entry point. Wires together the Producer-Consumer pipeline:
 *
 *   Producer Thread --> BlockingQueue<ChatMessage> --> Consumer Thread Pool
 *
 * The producer simulates an incoming chat stream by pushing test messages
 * onto a LinkedBlockingQueue. A fixed pool of consumer threads (managed by
 * an ExecutorService) concurrently pull messages off the queue and hand
 * them to MessageModeratorService for classification. Thread-safe
 * AtomicInteger counters track the running totals, and a final summary is
 * printed once every message has been processed.
 */
public class ChatModerationRunner {

    /** Number of consumer threads processing the queue concurrently. */
    private static final int CONSUMER_COUNT = 3;

    /**
     * Sentinel "poison pill" message used to tell each consumer thread that
     * there is no more work and it should stop polling the queue. One pill
     * is enqueued per consumer thread once the producer has finished.
     */
    private static final ChatMessage POISON_PILL = new ChatMessage("SYSTEM", "POISON_PILL", -1L);

    private final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>();
    private final MessageModeratorService moderatorService = new MessageModeratorService();

    // Thread-safe counters shared across all consumer threads.
    private final AtomicInteger allowedCount = new AtomicInteger(0);
    private final AtomicInteger flaggedCount = new AtomicInteger(0);
    private final AtomicInteger blockedCount = new AtomicInteger(0);
    private final AtomicInteger exceptionCount = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        new ChatModerationRunner().run();
    }

    /**
     * Orchestrates the full pipeline: builds test data, starts the consumer
     * pool, starts the producer, waits for everything to finish, and prints
     * the final summary.
     */
    private void run() throws InterruptedException {
        List<ChatMessage> testMessages = buildTestMessages();
        int totalMessages = testMessages.size();

        ExecutorService consumerPool = Executors.newFixedThreadPool(CONSUMER_COUNT);
        startConsumers(consumerPool);

        // Producer runs on its own dedicated thread so main() can wait on it.
        Thread producerThread = new Thread(() -> produce(testMessages), "Producer-Thread");
        producerThread.start();
        producerThread.join();

        // No more work will be submitted; let queued tasks finish, then wait.
        consumerPool.shutdown();
        consumerPool.awaitTermination(30, TimeUnit.SECONDS);

        printSummary(totalMessages);
    }

    /**
     * Submits CONSUMER_COUNT long-running consumer tasks to the pool.
     * Each task loops, blocking on queue.take(), until it receives the
     * POISON_PILL sentinel.
     */
    private void startConsumers(ExecutorService consumerPool) {
        for (int i = 1; i <= CONSUMER_COUNT; i++) {
            consumerPool.submit(this::consume);
        }
    }

    /**
     * Producer logic: pushes every test message onto the blocking queue
     * (blocking automatically if the queue implementation were bounded and
     * full), then enqueues one poison pill per consumer thread so that each
     * consumer knows to terminate cleanly once all real work is done.
     */
    private void produce(List<ChatMessage> messages) {
        for (ChatMessage msg : messages) {
            try {
                messageQueue.put(msg);
                System.out.println("Producer Added:");
                System.out.println("User " + (msg.getUserId() == null ? "N/A" : msg.getUserId()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        for (int i = 0; i < CONSUMER_COUNT; i++) {
            try {
                messageQueue.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Consumer logic: continuously takes messages from the queue and
     * moderates them, until the poison pill signals it is time to stop.
     * take() blocks the thread when the queue is empty instead of busy
     * polling, which is the correct, efficient way to consume from a
     * BlockingQueue.
     */
    private void consume() {
        while (true) {
            try {
                ChatMessage msg = messageQueue.take();
                if (msg == POISON_PILL) {
                    break;
                }
                processMessage(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Runs a single message through the moderation service, updates the
     * relevant AtomicInteger counter, and prints the result. Any
     * ModerationException (null/empty content) is caught here so that a
     * single bad message never kills the consumer thread - it is logged
     * and simply skipped.
     */
    private void processMessage(ChatMessage msg) {
        System.out.println("Consumer Processing...");
        try {
            String result = moderatorService.moderate(msg);

            switch (result) {
                case MessageModeratorService.ALLOWED:
                    allowedCount.incrementAndGet();
                    break;
                case MessageModeratorService.FLAGGED:
                    flaggedCount.incrementAndGet();
                    break;
                case MessageModeratorService.BLOCKED:
                    blockedCount.incrementAndGet();
                    break;
                default:
                    break;
            }
            System.out.println("Result : " + result);

        } catch (ModerationException e) {
            exceptionCount.incrementAndGet();
            System.out.println(e.toString());
            System.out.println("Message skipped");
        }
        System.out.println("--------------------------------");
    }

    /** Prints the final thread-safe counts once all messages are processed. */
    private void printSummary(int totalMessages) {
        System.out.println();
        System.out.println("FINAL SUMMARY");
        System.out.println("=====================");
        System.out.println("Allowed Messages : " + allowedCount.get());
        System.out.println("Flagged Messages : " + flaggedCount.get());
        System.out.println("Blocked Messages : " + blockedCount.get());
        System.out.println("Exceptions : " + exceptionCount.get());
        System.out.println("Total Messages : " + totalMessages);
    }

    /**
     * Builds the test data set (23 messages) covering every scenario
     * required by the assignment: normal messages, banned words (including
     * upper/mixed case), repeated characters, repeated URLs, a null
     * message, an empty message, and a blank (whitespace-only) message.
     */
    private List<ChatMessage> buildTestMessages() {
        List<ChatMessage> list = new ArrayList<>();
        long ts = System.currentTimeMillis();

        list.add(new ChatMessage("U101", "Hello everyone, good morning!", ts));
        list.add(new ChatMessage("U102", "heyyyyyyyy buddy how are you", ts));
        list.add(new ChatMessage("U103", "www.spam.com www.spam.com", ts));
        list.add(new ChatMessage("U104", "This is a badword you used", ts));
        list.add(new ChatMessage("U105", "I really hate this situation", ts));
        list.add(new ChatMessage("U106", "Please stop the abuse now", ts));
        list.add(new ChatMessage("U107", "There was violence in the news", ts));
        list.add(new ChatMessage("U108", "This looks like a scam offer", ts));
        list.add(new ChatMessage("U109", "noooooooo way that happened", ts));
        list.add(new ChatMessage("U110", "whyyyyyyyy did you do that", ts));
        list.add(new ChatMessage("U111", "Check this out https://abc.com https://abc.com", ts));
        list.add(new ChatMessage("U112", "Visit http://xyz.com for details", ts));
        list.add(new ChatMessage("U113", null, ts));                       // null content
        list.add(new ChatMessage("U114", "", ts));                         // empty content
        list.add(new ChatMessage("U115", "   ", ts));                      // blank content
        list.add(new ChatMessage("U116", "BADWORD should be blocked too", ts));   // uppercase banned word
        list.add(new ChatMessage("U117", "HaTe mixed case banned word", ts));     // mixed-case banned word
        list.add(new ChatMessage("U118", "The weather is nice today", ts));
        list.add(new ChatMessage("U119", "Let's meet at the coffee shop", ts));
        list.add(new ChatMessage("U120", "Great job team, keep it up!", ts));
        list.add(new ChatMessage("U121", "sooooo cooool right now", ts));
        list.add(new ChatMessage("U122", "www.test.com is a nice site", ts));      // single URL, not spam
        list.add(new ChatMessage("U123", "Random sentence with no issues at all", ts));

        return list;
    }
}
