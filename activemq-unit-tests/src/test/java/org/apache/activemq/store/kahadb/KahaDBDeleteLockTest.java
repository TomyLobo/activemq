/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.store.kahadb;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.util.Wait;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;


import static org.junit.Assert.assertTrue;

public class KahaDBDeleteLockTest {

    protected BrokerService master;

    protected KahaDBPersistenceAdapter masterPersistenceAdapter = new KahaDBPersistenceAdapter();

    private final File testDataDir = new File("target/activemq-data/KahaDBDeleteLockTest");
    private final File kahaDataDir = new File(testDataDir, "kahadb");

    @Before
    public void createMaster() throws Exception{
        master = new BrokerService();
        master.setBrokerName("Master");
        master.setDataDirectoryFile(testDataDir);

        masterPersistenceAdapter.setDirectory(kahaDataDir);
        masterPersistenceAdapter.setLockKeepAlivePeriod(500);

        master.setPersistenceAdapter(masterPersistenceAdapter);
        master.start();
        master.waitUntilStarted();
    }

    @After
    public void stopBrokerJustInCase() throws Exception {
        if (master != null) {
            master.stop();
        }
    }

    /**
     * Deletes the lock file and makes sure that the broken stops.
     * @throws Exception
     */
    @Test
    public void testLockFileDelete() throws Exception {
        assertTrue(master.isStarted());

        //Delete the lock file
        File lockFile = new File(kahaDataDir, "lock");

        if(lockFile.exists()) {
            lockFile.delete();
        }

        assertTrue("Master stops on lock file delete", Wait.waitFor(new Wait.Condition() {
            @Override
            public boolean isSatisified() throws Exception {
                return master.isStopped();
            }
        }));
    }

    /**
     * Modifies the lock file so that the last modified date is not the same when the broker obtained the lock.
     * This should force the broker to stop.
     * @throws Exception
     */
    @Test
    public void testModifyLockFile() throws Exception {
        assertTrue(master.isStarted());

        // ensure modification will be seen, milisecond granularity
        TimeUnit.MILLISECONDS.sleep(1);
        RandomAccessFile file = new RandomAccessFile(new File(kahaDataDir, "lock"), "rw");
        file.write(4);
        file.close();

        assertTrue("Master stops on lock file modification", Wait.waitFor(new Wait.Condition() {
            @Override
            public boolean isSatisified() throws Exception {
                return master.isStopped();
            }
        }, 5000));

    }
}