package ru.samurayrus.smartmodulesystemai.workers;

/**
 * Интерфейс для взаимодействия WorkerEventDataBus с воркерами
 */
public interface WorkerListener {
    /**
     * Пока логика работы - если воркер определяет, что ему нужно дсделать работу, то он её делает,
     * добавляя в процессе работы отчеты в контекст (и в интерфейс если он включен).
     * После завершения работы, если llm нужно узнать результат работы, то нужно вернуть true, тогда
     * глобальный контекст с новыми сообщениями от tool будет переотправлен llm.
     * Если ответ от llm не нужен, то возвращаем false
     *
     * @param content
     * @return boolean
     */
    boolean callWorker(String content);
}
