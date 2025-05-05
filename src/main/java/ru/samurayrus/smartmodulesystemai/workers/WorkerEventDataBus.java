package ru.samurayrus.smartmodulesystemai.workers;

import org.springframework.stereotype.Service;
import ru.samurayrus.smartmodulesystemai.workers.fileeditor.Command;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkerEventDataBus {

    private static final Set<WorkerListener> workerListeners = new HashSet<>();

    public boolean registerWorker(WorkerListener workerListener) {
        workerListeners.add(workerListener);
        return true;
    }

    public boolean callActivityWorkers(String contentFromLlm, boolean toolMode) {
        //TODO: возможно сделать через CompletableFuture, если архитектура будет исключать взаимодействие между разными воркерами
        Set<Boolean> workersReports = workerListeners.stream().map(x -> x.callWorker(contentFromLlm, toolMode)).collect(Collectors.toSet());
        return workersReports.contains(true);
    }

    public boolean callActivityWorkers(Command command) {
        //TODO: возможно сделать через CompletableFuture, если архитектура будет исключать взаимодействие между разными воркерами
        Set<Boolean> workersReports = workerListeners.stream().map(x -> x.callWorker(command)).collect(Collectors.toSet());
        return workersReports.contains(true);
    }
}
