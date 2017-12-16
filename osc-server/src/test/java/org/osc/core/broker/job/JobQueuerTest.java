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

import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.internal.InOrderImpl;
import org.mockito.internal.verification.InOrderWrapper;
import org.mockito.internal.verification.Times;
import org.mockito.verification.Timeout;
import org.osc.core.broker.job.Job.JobCompletionListener;
import org.osc.core.broker.job.JobEngineTest.EmptyTask;
import org.osc.core.broker.job.JobQueuer.JobRequest;

public class JobQueuerTest {

    private EmptyTask A = new EmptyTask("A");
    private EmptyTask B = new EmptyTask("B");
    private EmptyTask C = new EmptyTask("C");
    private EmptyTask D = new EmptyTask("D");

    @BeforeClass
    public static void initTests() {
//        Logger hibernateLogger = LoggerFactory.getLogger("org.hibernate");
//        hibernateLogger.setLevel(Level.ERROR);
//
//        Logger broadcastUtilLogger = LoggerFactory.getLogger("org.osc.core.broker.util.TransactionalBroadcastUtil");
//        broadcastUtilLogger.setLevel(Level.ERROR);
//
//        Logger logger = LoggerFactory.getLogger("org.osc.core.broker.job.Job");
//        logger.setLevel(Level.DEBUG);
//        logger = LoggerFactory.getLogger("org.osc.core.broker.job.JobEngine");
//        logger.setLevel(Level.DEBUG);
//        logger = LoggerFactory.getLogger("org.osc.core.broker.job.TaskNode");
//        logger.setLevel(Level.DEBUG);
//        BasicConfigurator.configure();
    }

    @AfterClass
    public static void uninitTests() {
        JobEngine.getEngine().shutdownForTest();
    }

    // TODO : re-enable this once the issue #284 is addressed
    @Test
    @Ignore // this test is unstable
    public void testPutJob() {

        final TaskGraph tg = new TaskGraph();
        tg.addTask(this.A);
        tg.addTask(this.B);
        tg.addTask(this.C, this.A);
        tg.addTask(this.D, this.A);

        final TaskGraph tg2 = new TaskGraph();
        tg2.addTask(this.A);
        tg2.addTask(this.B);
        tg2.addTask(this.C, this.A);
        tg2.addTask(this.D, this.A);

        final TaskGraph tg3 = new TaskGraph();
        tg3.addTask(this.A);
        tg3.addTask(this.B);
        tg3.addTask(this.C, this.A);
        tg3.addTask(this.D, this.A);

        @SuppressWarnings("unchecked")
        final List<String> mockList = mock(List.class);

        JobEngine.getEngine().addJobCompletionListener(new JobCompletionListener() {

            @Override
            public void completed(Job job) {
                mockList.add(job.getName());
            }
        });

        Runnable job1Runnable = new Runnable() {

            @Override
            public void run() {
                JobQueuer.getInstance().putJob(new JobRequest("Job-parallel-nested1", tg, false));
            }
        };

        Runnable job2Runnable = new Runnable() {

            @Override
            public void run() {
                JobQueuer.getInstance().putJob(new JobRequest("Job-parallel-nested2", tg2, false));
            }
        };
        Runnable job3Runnable = new Runnable() {

            @Override
            public void run() {
                JobQueuer.getInstance().putJob(new JobRequest("Job-parallel-nested3", tg3, false));
            }
        };
        new Thread(job1Runnable).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        new Thread(job2Runnable).start();
        new Thread(job3Runnable).start();

        InOrder inOrder = inOrder(mockList);
        verify(mockList, new Timeout(5000, new InOrderWrapper(new Times(1), (InOrderImpl) inOrder))).add(
                "Job-parallel-nested1");
        verify(mockList, new Timeout(5000, new InOrderWrapper(new Times(1), (InOrderImpl) inOrder))).add(
                "Job-parallel-nested2");
        verify(mockList, new Timeout(5000, new InOrderWrapper(new Times(1), (InOrderImpl) inOrder))).add(
                "Job-parallel-nested3");
        verifyNoMoreInteractions(mockList);

    }

}
