package org.osc.core.server.installer.impl;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.osgi.resource.Requirement;

public class RequirementParserTest {

	@Test
	public void testParseRequireBundle() {
		List<Requirement> actual = RequirementParser.parseRequireBundle("foo;bundle-version=1.0.0, bar;bundle-version=\"[1.0,1.1)\", baz;bundle-version=\"[2.0,3.0)\", fnarg");
		
		assertEquals(4, actual.size());
		assertEquals("(&(osgi.wiring.bundle=foo)(bundle-version>=1.0.0))", actual.get(0).getDirectives().get("filter"));
		assertEquals("(&(osgi.wiring.bundle=bar)(bundle-version>=1.0.0)(!(bundle-version>=1.1.0)))", actual.get(1).getDirectives().get("filter"));
		assertEquals("(&(osgi.wiring.bundle=baz)(bundle-version>=2.0.0)(!(bundle-version>=3.0.0)))", actual.get(2).getDirectives().get("filter"));
		assertEquals("(osgi.wiring.bundle=fnarg)", actual.get(3).getDirectives().get("filter"));
	}

	@Test
	public void testParseRequireCapability() {
		List<Requirement> actual = RequirementParser.parseRequireCapability("osgi.extender; filter:=\"(&(osgi.extender=osgi.ds)(version>=1.0))\"; effective:=active, osgi.service; filter:=\"(objectClass=org.example.Foo)\"");
		
		assertEquals(2, actual.size());
		assertEquals("(&(osgi.extender=osgi.ds)(version>=1.0))", actual.get(0).getDirectives().get("filter"));
		assertEquals("active", actual.get(0).getDirectives().get("effective"));
	}

}
