package com.dmzaytsev.junit.ldap;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.impl.SimpleLogger;

/**
 * Test case for {@LdapServerRule}
 */
public class LdapServerRuleTest {
    @Rule
    public final LdapServerRule rule = new LdapServerRule.Builder("ou=com")
        .build();

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
    }

    @Test
    public void testName() throws Exception {
        MatcherAssert.assertThat(
          this.rule.server().getEntry("ou=unit"),
            CoreMatchers.nullValue());
    }
}
