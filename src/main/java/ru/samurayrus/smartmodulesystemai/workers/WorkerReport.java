package ru.samurayrus.smartmodulesystemai.workers;

import lombok.Value;

/**
 * Предлагаемая реазлизация ответов от воркера.
 * Может быть, чтобы не захламлять контекст?
 */
@Value
public class WorkerReport {
    String answerFromWorker;
    boolean waitResponse;
}
