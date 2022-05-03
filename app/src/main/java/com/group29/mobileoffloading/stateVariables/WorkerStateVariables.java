package com.group29.mobileoffloading.stateVariables;

public class WorkerStateVariables {
    static {
        FAILED = "WORK_FAILED";
        DISCONNECTED = "WORKER_DISCONNECTED";
        WORKING = "WORKING";
        FINISHED = "WORK_FINISHED";
    }

    public static final String WORKING;
    public static final String FINISHED;
    public static final String FAILED;
    public static final String DISCONNECTED;
}
