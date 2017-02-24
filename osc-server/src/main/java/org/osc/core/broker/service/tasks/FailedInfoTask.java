package org.osc.core.broker.service.tasks;


public class FailedInfoTask extends BaseTask {
    protected String errorStr;
    private Exception exception;

    public FailedInfoTask(String name, String errorStr) {
        super(name);
        this.errorStr = errorStr;
    }

    public FailedInfoTask(String name, Exception exception) {
        super(name);
        this.exception = exception;
    }

    @Override
    public void execute() throws Exception {
        if (exception != null) {
            throw exception;
        } else {
            throw new Exception(errorStr);
        }
    }

    @Override
    public String toString() {
        return "FailedInfoTask [name=" + name + "]";
    }

}
