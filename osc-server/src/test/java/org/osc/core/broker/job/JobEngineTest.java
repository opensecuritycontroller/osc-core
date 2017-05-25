/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.model.entities.job.JobRecord;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.service.tasks.BaseTask;
import org.osc.core.broker.service.test.InMemDB;
import org.osc.core.broker.util.StaticRegistry;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.test.util.TestTransactionControl;
import org.osgi.service.transaction.control.TransactionControl;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HibernateUtil.class, StaticRegistry.class})
public class JobEngineTest {

    static class EmptyMetaTask extends EmptyTask implements MetaTask {

        EmptyMetaTask(String name) {
            super(name);
        }

        @Override
        public void execute() throws Exception {
        }

        @Override
        public TaskGraph getTaskGraph() {
            Task A = new EmptyTask("MetaTask-A");
            Task B = new EmptyTask("MetaTask-B");
            Task C = new EmptyTask("MetaTask-C");
            Task D = new EmptyTask("MetaTask-D");
            Task E = new EmptyTask("MetaTask-E");

            TaskGraph tg1 = new TaskGraph();
            tg1.addTask(A);
            tg1.addTask(B, A);
            tg1.addTask(C, A);
            tg1.addTask(D, C, B);
            tg1.addTask(E, A);
            return tg1;
        }
    }

    static class EmptyTask extends BaseTask {

        @TaskInput
        @TaskOutput
        public int taskOrder = 1;

        EmptyTask(String name) {
            super(name);
        }

        @Override
        public void execute() throws Exception {
            try {
                this.taskOrder += 1;
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return "EmptyTask [name=" + this.name + "]";
        }

    }

    JobEngine je;
    TaskGraph tg;
    Job job;

    EmptyTask A = new EmptyTask("A");
    EmptyTask B = new EmptyTask("B");
    EmptyTask C = new EmptyTask("C");
    EmptyTask D = new EmptyTask("D");

    // These tests are highly multithreaded, so we need to provide per-thread instances
    // of non thread safe test resources
    private final ConcurrentMap<Thread, EntityManager> ems =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<Thread, TestTransactionControl> txControls =
            new ConcurrentHashMap<>();

    EntityManagerFactory testEmf;

    @BeforeClass
    public static void initTests() {
        Logger hibernateLogger = Logger.getLogger("org.hibernate");
        hibernateLogger.setLevel(Level.ERROR);

        Logger logger = Logger.getLogger("org.osc.core.broker.job.Job");
        logger.setLevel(Level.DEBUG);
        logger = Logger.getLogger("org.osc.core.broker.job.JobEngine");
        logger.setLevel(Level.DEBUG);
        logger = Logger.getLogger("org.osc.core.broker.job.TaskNode");
        logger.setLevel(Level.DEBUG);
        BasicConfigurator.configure();
    }

    @AfterClass
    public static void uninitTests() {
        JobEngine.getEngine().shutdownForTest();
    }

    @Before
    public void setUp() throws Exception {
        TransactionalBroadcastUtil broadcastUtil = Mockito.mock(TransactionalBroadcastUtil.class);
        PowerMockito.mockStatic(StaticRegistry.class);
        Mockito.when(StaticRegistry.transactionalBroadcastUtil()).thenReturn(broadcastUtil);

        this.testEmf = InMemDB.getEntityManagerFactory();

        PowerMockito.mockStatic(HibernateUtil.class);

        Answer<EntityManager> createEm = i -> this.ems.computeIfAbsent(Thread.currentThread(),
                k -> this.testEmf.createEntityManager());

        Mockito.when(HibernateUtil.getTransactionalEntityManager()).then(
                createEm);

        Answer<TransactionControl> createTxControl = i -> this.txControls
                        .computeIfAbsent(Thread.currentThread(),
                                k -> {
                                    TestTransactionControl testTxControl = Mockito.mock(TestTransactionControl.class,
                                        Mockito.withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS.get()));
                                    try {
                                        testTxControl.setEntityManager(createEm.answer(i));
                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                        Assert.fail(t.getMessage());
                                    }
                                    return testTxControl;
                                });
        Mockito.when(HibernateUtil.getTransactionControl()).then(createTxControl);

        this.je = JobEngine.getEngine();
        this.tg = new TaskGraph();
    }

    @After
    public void tearDown() {
        InMemDB.shutdown();
        this.ems.clear();
        this.txControls.clear();
    }

    @Test
    @Ignore
    public void testTaskDependencyExecutionOrder() throws Exception {
        // Test first level parallel followed by sequential parallel tasks
        this.tg = new TaskGraph();
        this.tg.addTask(this.A);
        this.tg.addTask(this.B);
        this.tg.addTask(this.C, this.A);
        this.tg.addTask(this.D, this.A);
        this.job = this.je.submit("Job-parallel-nested", this.tg, true);
        this.job.waitForCompletion();
        assertTrue(this.C.taskOrder > this.A.taskOrder);
        assertTrue(this.D.taskOrder > this.A.taskOrder);

        // Test two parallel branches of sequential tasks
        this.tg = new TaskGraph();
        this.tg.addTask(this.A);
        this.tg.addTask(this.B);
        this.tg.addTask(this.C, this.A);
        this.tg.addTask(this.D, this.B);
        this.job = this.je.submit("Job-parallel-branches", this.tg, true);
        this.job.waitForCompletion();
        assertTrue(this.C.taskOrder > this.A.taskOrder);
        assertTrue(this.D.taskOrder > this.B.taskOrder);

        // Test all sequential
        this.tg = new TaskGraph();
        this.tg.addTask(this.A);
        this.tg.addTask(this.B, this.A);
        this.tg.addTask(this.C, this.B);
        this.tg.addTask(this.D, this.C);
        this.job = this.je.submit("Job-sequential-tasks", this.tg, true);
        this.job.waitForCompletion();
        assertTrue(this.B.taskOrder > this.A.taskOrder);
        assertTrue(this.C.taskOrder > this.B.taskOrder);
        assertTrue(this.D.taskOrder > this.C.taskOrder);

        // Test all parallel tasks
        this.tg = new TaskGraph();
        this.tg.addTask(this.A);
        this.tg.addTask(this.B);
        this.tg.addTask(this.C);
        this.tg.addTask(this.D);
        this.job = this.je.submit("Job-parallel-tasks", this.tg, true);
        this.job.waitForCompletion();

        verifyJobPersistence(this.job);
    }

    private void verifyJobPersistence(Job job) throws Exception {
        EntityManager em = this.testEmf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            JobRecord jobRecord =  em.find(JobRecord.class, job.getId());
            assertEquals(jobRecord.getName(), job.getName());

            for (TaskRecord ts : jobRecord.getTasks()) {
                TaskNode taskNode = null;
                for (TaskNode tn : job.getTaskGraph().getGraph().topologicalSort()) {
                    if (ts.getId().equals(tn.getId())) {
                        taskNode = tn;
                        break;
                    }
                }
                Assert.assertNotNull(taskNode);

                assertEquals(ts.getName(), taskNode.getTask().getName());
                assertEquals(ts.getState().name(), taskNode.getState().name());
                assertEquals(ts.getStatus().name(), taskNode.getStatus().name());
                assertEquals(ts.getTaskGaurd().name(), taskNode.getTaskGaurd().name());
                assertEquals(ts.getFailReason(), taskNode.getFailReason() != null ? taskNode.getFailReason()
                        .getMessage() : null);
                assertEquals(taskNode.getCompletedTimestamp().toDate(), ts.getCompletedTimestamp());
                assertEquals(taskNode.getQueuedTimestamp().toDate(), ts.getQueuedTimestamp());
                assertEquals(taskNode.getStartedTimestamp().toDate(), ts.getStartedTimestamp());

                // Task nodes always
                Set<TaskNode> successors = taskNode.getSuccessors();
                Set<TaskNode> predecessors = taskNode.getPredecessors();
                if (successors.size() == 1 && successors.iterator().next().isStartOrEndTask()) {
                    assertEquals(ts.getSuccessors().size(), 0);
                } else {
                    assertEquals(ts.getSuccessors().size(), successors.size());
                }
                if (predecessors.size() == 1 && predecessors.iterator().next().isStartOrEndTask()) {
                    assertEquals(ts.getPredecessors().size(), 0);
                } else {
                    assertEquals(ts.getPredecessors().size(), predecessors.size());
                }

                for (TaskRecord tss : ts.getSuccessors()) {
                    boolean found = false;
                    for (TaskNode tns : successors) {
                        if (tss.getId().equals(tns.getId())) {
                            found = true;
                            break;
                        }
                    }
                    Assert.assertTrue(found);
                }
                for (TaskRecord tsp : ts.getPredecessors()) {
                    boolean found = false;
                    for (TaskNode tnp : predecessors) {
                        if (tsp.getId().equals(tnp.getId())) {
                            found = true;
                            break;
                        }
                    }
                    Assert.assertTrue(found);
                }
            }
            tx.commit();

        } catch (Exception e) {
            e.printStackTrace();
            tx.rollback();
            Assert.fail(e.getMessage());
        } finally {
            em.close();
        }
    }

    @Test
    public void testTaskConcurrency() throws Exception {
        // Test large volume of job with large number of parallel tasks
        for (int i = 0; i < 100; i++) {
            this.tg = new TaskGraph();
            this.tg.addTask(this.A);
            this.tg.addTask(this.B);
            this.tg.addTask(this.C);
            this.tg.addTask(this.D);
            this.job = this.je.submit("Job-" + i, this.tg, false);
        }

        waitForJobsCompletion(this.je);
    }

    public class OutputTask extends EmptyTask {

        public OutputTask(String name) {
            super(name);
        }

        @TaskOutput
        public String id;

        @Override
        public void execute() {
            this.id = "output";
        }

        @Override
        public String toString() {
            return super.toString() + " [id=" + this.id + "]";
        }
    }

    public class InputTask extends EmptyTask {

        public InputTask(String name) {
            super(name);
        }

        @TaskInput
        public String id;

        @Override
        public void execute() {
        }

        @Override
        public String toString() {
            return super.toString() + " [id=" + this.id + "]";
        }
    }

    @Test
    public void testTaskInputOutput() throws Exception {
        OutputTask A = new OutputTask("A-OutputTask");
        InputTask B = new InputTask("B-Input");
        InputTask C = new InputTask("C-Input");

        this.tg = new TaskGraph();
        this.tg.addTask(A);
        this.tg.addTask(B, A);
        this.tg.addTask(C, B);

        this.job = this.je.submit("Job-input-output", this.tg, true);
        this.job.waitForCompletion();

        assertEquals(A.id, B.id);
        assertEquals(B.id, C.id);
    }

    private void waitForJobsCompletion(JobEngine je) {
        while (je.isActive()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class FailedTask extends EmptyTask {

        public FailedTask(String name) {
            super(name);
        }

        @Override
        public void execute() {
            throw new RuntimeException("Failure");
        }

        @Override
        public String toString() {
            return "FailedTask [name=" + this.name + "]";
        }
    }

    @Test
    public void testSkippedTasks() throws Exception {
        Task A = new FailedTask("A-Failed");
        Task B = new EmptyTask("B");
        Task C = new EmptyTask("C");
        Task D = new EmptyTask("D");
        Task E = new EmptyTask("E");
        Task F = new EmptyTask("F");
        Task G = new EmptyTask("G");

        this.tg = new TaskGraph();
        this.tg.addTask(A);
        this.tg.addTask(B);
        this.tg.addTask(C);
        /*
         * Task D should be set to skipped since one of its predecessors (A)
         * should fail. It should still allow opportunity for Task E,F to
         * execute.
         */
        this.tg.addTask(D, TaskGuard.ALL_PREDECESSORS_SUCCEEDED, A, B);
        /*
         * Task E depends on D and require successful completion of D. But since
         * D will be skipped, E should also be skipped.
         */
        this.tg.addTask(E, TaskGuard.ALL_PREDECESSORS_SUCCEEDED, D);
        /*
         * Just like E, F also depends on D but only require completion. Even
         * though D will be skipped, F should pass.
         */
        this.tg.addTask(F, TaskGuard.ALL_PREDECESSORS_COMPLETED, C, D);
        /*
         * G should be skipped because task A failed initially
         */
        this.tg.addTask(G, TaskGuard.ALL_ANCESTORS_SUCCEEDED, F);

        this.job = this.je.submit("Job-skipped-task", this.tg, true);
        this.job.waitForCompletion();

        assertEquals(this.job.getTaskGraph().getTaskNode(A).getStatus(), TaskStatus.FAILED);
        assertEquals(this.job.getTaskGraph().getTaskNode(B).getStatus(), TaskStatus.PASSED);
        assertEquals(this.job.getTaskGraph().getTaskNode(C).getStatus(), TaskStatus.PASSED);
        assertEquals(this.job.getTaskGraph().getTaskNode(D).getStatus(), TaskStatus.SKIPPED);
        assertEquals(this.job.getTaskGraph().getTaskNode(E).getStatus(), TaskStatus.SKIPPED);
        assertEquals(this.job.getTaskGraph().getTaskNode(F).getStatus(), TaskStatus.PASSED);
        assertEquals(this.job.getTaskGraph().getTaskNode(G).getStatus(), TaskStatus.SKIPPED);
    }

    class JobCompletionResponder implements JobCompletionListener {
        private boolean isCalled = false;

        @Override
        public void completed(Job job) {
            this.isCalled = true;
            System.out.println("Received completion notification for " + job);
        }

        public boolean isCalled() {
            return this.isCalled;
        }

    }

    @Test
    public void testJobCompletionListener() throws Exception {
        this.tg.addTask(this.A);
        this.tg.addTask(this.B);
        this.tg.addTask(this.C);

        JobCompletionResponder responder = new JobCompletionResponder();

        this.job = this.je.submit("Job-completion-listener", this.tg, responder, false);
        this.job.waitForCompletion();

        assertTrue(responder.isCalled());
    }

    @Test
    public void testAddTaskGraph() throws Exception {
        this.A = new EmptyTask("TG-A");
        this.B = new EmptyTask("TG-B");
        this.C = new EmptyTask("TG-C");
        this.D = new EmptyTask("TG-D");

        this.tg = new TaskGraph();
        this.tg.addTask(this.A);
        this.tg.addTask(this.B, this.A);
        this.tg.addTask(this.C, this.B);
        this.tg.addTask(this.D, this.C);

        Task A = new EmptyTask("TG1-A");
        Task B = new EmptyTask("TG1-B");
        Task C = new EmptyTask("TG1-C");
        Task D = new EmptyTask("TG1-D");

        TaskGraph tg1 = new TaskGraph();
        tg1.addTask(A);
        tg1.addTask(B, A);
        tg1.addTask(C, B);
        tg1.addTask(D, C);

        Task E = new EmptyTask("TG2-E");
        Task F = new EmptyTask("TG2-F");
        Task G = new EmptyTask("TG2-G");
        Task H = new EmptyTask("TG2-H");
        TaskGraph tg2 = new TaskGraph();
        tg2.addTask(E);
        tg2.addTask(F, E);
        tg2.addTask(G, F);
        tg2.addTask(H, G);

        this.tg.addTaskGraph(tg1);
        this.tg.addTaskGraph(tg2, this.D);

        this.job = this.je.submit("Job-task-graph-wiring", this.tg, true);
        this.job.waitForCompletion();
    }

    @Test
    public void testMetaTask() throws Exception {
        EmptyTask A = new EmptyTask("TG-A");
        EmptyMetaTask B = new EmptyMetaTask("TG-B (Meta)");
        EmptyTask C = new EmptyTask("TG-C");
        EmptyTask D = new EmptyTask("TG-D");

        this.tg = new TaskGraph();
        this.tg.addTask(A);
        this.tg.addTask(B, A);
        this.tg.addTask(C, B);
        this.tg.addTask(D, B);

        this.job = this.je.submit("Job-meta-task-wiring", this.tg, true);
        this.job.waitForCompletion();
    }
}
