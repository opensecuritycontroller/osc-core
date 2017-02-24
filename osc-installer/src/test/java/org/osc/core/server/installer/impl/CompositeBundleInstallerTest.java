package org.osc.core.server.installer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class CompositeBundleInstallerTest {

	private TestLogService log;
	private DeploymentInstaller installer;
	
	@Before
	public void setup() {
		log = new TestLogService();
		installer = new DeploymentInstaller();
		installer.log = log;
	}

	@Test
	public void testValidArchiveCanBeHandled() {
		File jar = new File("target/test-classes/valid1.bar");

		assertTrue("test archive not found", jar.isFile());
		assertTrue("test archive not handled", installer.canHandle(jar));
		
		assertEquals(1, log.logged.size());
		assertTrue("missing info message", log.logged.get(0).startsWith("INFO: Detected valid bundle archive"));
	}
	
	@Test
	public void testIndexPathInManifest() {
		File jar = new File("target/test-classes/valid2.bar");

		assertTrue("test archive not found", jar.isFile());
		assertTrue("test archive not handled", installer.canHandle(jar));

		assertEquals(1, log.logged.size());
		assertTrue("missing info message", log.logged.get(0).startsWith("INFO: Detected valid bundle archive"));
	}
	
	@Test
	public void testInvalidExtension() {
		File jar = new File("target/test-classes/invalid-wrong-extension.xxx");

		assertTrue("test archive not found", jar.isFile());
		assertFalse("invalid test archive should not be handled", installer.canHandle(jar));

		assertEquals(1, log.logged.size());
		assertTrue("missing debug message", log.logged.get(0).startsWith("DEBUG: Ignoring"));
	}

	@Test
	public void testMissingRequireHeader() {
		File jar = new File("target/test-classes/invalid-missing-requires.bar");

		assertTrue("test archive not found", jar.isFile());
		assertFalse("invalid test archive should not be handled", installer.canHandle(jar));

		assertEquals(1, log.logged.size());
		assertTrue("missing warning message", log.logged.get(0).startsWith("WARNING: Not a valid bundle archive"));
	}

	@Test
	public void testNoIndex() {
		File jar = new File("target/test-classes/invalid-no-index.bar");

		assertTrue("test archive not found", jar.isFile());
		assertFalse("invalid test archive should not be handled", installer.canHandle(jar));

		assertEquals(1, log.logged.size());
		assertTrue("missing warning message", log.logged.get(0).startsWith("WARNING: Not a valid bundle archive"));
	}

	@Test
	public void testMissingIndexPath() {
		File jar = new File("target/test-classes/invalid-missing-index-path.bar");

		assertTrue("test archive not found", jar.isFile());
		assertFalse("invalid test archive should not be handled", installer.canHandle(jar));

		assertEquals(1, log.logged.size());
		assertTrue("missing warning message", log.logged.get(0).startsWith("WARNING: Not a valid bundle archive"));
	}

}
