package ru.samurayrus.smartmodulesystemai.workers;

import org.springframework.stereotype.Service;

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

    public boolean callActivityWorkers(String contentFromLlm) {
        Set<Boolean> workersReports = workerListeners.stream().map(x -> x.callWorker(contentFromLlm)).collect(Collectors.toSet());
        if (workersReports.contains(true))
            return true;
        else
            return false;
    }
}
