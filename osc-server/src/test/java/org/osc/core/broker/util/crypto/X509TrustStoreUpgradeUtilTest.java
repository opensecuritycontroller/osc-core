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
package org.osc.core.broker.util.crypto;

import static org.osc.core.broker.util.crypto.X509TrustStoreUpgradeUtil.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class X509TrustStoreUpgradeUtilTest {
    private File pre08TrustStore;
    private File pre08KeyStore;
    private File new08TrustStore;

    @Before
    public void setup() throws IOException {
        this.pre08KeyStore = new File(PRE_0_8_INTERNAL_KEYSTORE_FILE);
        this.pre08TrustStore = new File(PRE_0_8_TRUSTSTORE_FILE);
        this.new08TrustStore = new File(NEW_0_8_TRUSTSTORE_FILE);

    }

    @After
    public void tearDown() {
        if (this.new08TrustStore.exists()) {
            this.new08TrustStore.delete();
        }
        if (this.pre08TrustStore.exists()) {
            this.pre08TrustStore.delete();
        }
        if (this.pre08KeyStore.exists()) {
            this.pre08KeyStore.delete();
        }
    }

    @Test
    public void testUpgrade_Pre08FilesExist_ShouldUpgradeTo08() throws Exception {
        // Arrange.
        InputStream tmpInputStream = getClass().getClassLoader().getResourceAsStream(PRE_0_8_TRUSTSTORE_FILE);
        FileUtils.copyToFile(tmpInputStream, this.pre08TrustStore);

        tmpInputStream = getClass().getClassLoader().getResourceAsStream(PRE_0_8_INTERNAL_KEYSTORE_FILE);
        FileUtils.copyToFile(tmpInputStream, this.pre08KeyStore);

        Assert.assertFalse(this.new08TrustStore.exists());
        Assert.assertTrue(this.pre08KeyStore.exists());
        Assert.assertTrue(this.pre08TrustStore.exists());

        // Act.
        X509TrustStoreUpgradeUtil.upgradeTrustStore();

        // Assert.
        Assert.assertTrue(this.new08TrustStore.exists());
        Assert.assertTrue(this.pre08KeyStore.exists());
        Assert.assertTrue(this.pre08TrustStore.exists());
        // the old files are left for the user to remove.
    }
}
