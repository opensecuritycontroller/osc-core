package org.osc.core.broker.job;

/**
 * A {@link Task} that constructs other Tasks (in the form of a {@link TaskGraph}) instead of executing business logic.
 * 
 * The <tt>TaskGraph</tt> constructed by this <tt>MetaTask</tt> is merged into
 * the {@link Job}'s <tt>TaskGraph</tt> subsequent to the <tt>MetaTask</tt>'s
 * execution.
 * 
 * This provides a basic facility for adding {@link Task}s dynamically to a <tt>TaskGraph</tt> during job execution.
 */
public interface MetaTask extends Task {

    /**
     * Get the {@link TaskGraph} created by this {@link MetaTask}.
     * 
     * The returned {@link TaskGraph} will be merged into the {@link Job}'s <tt>TaskGraph</tt> as a successor to this
     * {@link MetaTask}. All edges
     * inbound to the <tt>FINISH</tt> node of the returned <tt>TaskGraph</tt> will be rewired to point to all the
     * successor nodes of this {@link MetaTask}.
     * 
     * The getTaskGraph() method will called right after {@link Task#execute()} had complete.
     * 
     */
    TaskGraph getTaskGraph();

}
