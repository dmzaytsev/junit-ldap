package com.dmzaytsev.junit.ldap;

import com.google.common.io.Resources;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Ldap server rule.
 * @author Dmitry Zaytsev (me@dmzaytsev.com)
 * @version $Id$
 * @since 0.1
 */
@Slf4j
public final class LdapServerRule implements TestRule {
    private final transient InMemoryDirectoryServer server;
    private final transient List<String> ldif;

    public LdapServerRule(final InMemoryDirectoryServerConfig config,
        final List<String> files) {
        try {
            this.server = new InMemoryDirectoryServer(config);
        } catch (final LDAPException ex) {
            throw new IllegalStateException(ex);
        }
        this.ldif = new ArrayList<>(files);
    }

    public LDAPInterface server() {
        return this.server;
    }

    public int port() {
        return this.server.getListenPort();
    }

    private void load() throws LDAPException {
        boolean clear = true;
        for (final String path : this.ldif) {
            LdapServerRule.log.debug("LDIF file {} loading ...", path);
            this.server.importFromLDIF(clear, path);
            LdapServerRule.log.debug("LDIF file {} loaded", path);
            clear = false;
        }
    }

    public Statement apply(final Statement base, final Description desc) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                LdapServerRule.log.debug(
                    "{}.{} ldap server starting...",
                    desc.getTestClass().getName(), desc.getDisplayName()
                );
                LdapServerRule.this.load();
                LdapServerRule.this.server.startListening();
                LdapServerRule.log.debug(
                    "{}.{} ldap server started on port {}",
                    desc.getTestClass().getName(), desc.getDisplayName(),
                    LdapServerRule.this.port()
                );
                try {
                    base.evaluate();
                } finally {
                    LdapServerRule.log.debug(
                        "{}.{} ldap server stopping...",
                        desc.getTestClass().getName(), desc.getDisplayName()
                    );
                    LdapServerRule.this.server.shutDown(true);
                    LdapServerRule.this.server.clear();
                    LdapServerRule.log.debug(
                        "{}.{} ldap server stopped",
                        desc.getTestClass().getName(), desc.getDisplayName()
                    );
                }
            }
        };
    }

    public static final class Builder {
        private final transient InMemoryDirectoryServerConfig config;
        private final transient List<String> ldif;

        public Builder(final String... base) {
            this.ldif = new ArrayList<>(1);
            try {
                this.config = new InMemoryDirectoryServerConfig(base);
            } catch (final LDAPException ex) {
                throw new IllegalStateException(ex);
            }
        }

        public LdapServerRule.Builder listen(final int port)
            throws LDAPException {
            final List<InMemoryListenerConfig> listeners =
                new ArrayList<>(
                    this.config.getListenerConfigs()
                );
            listeners.add(
                InMemoryListenerConfig.createLDAPConfig(
                    String.format("LISTENER-%d", listeners.size()), port
                )
            );
            this.config.setListenerConfigs(listeners);
            return this;
        }

        public LdapServerRule.Builder resource(final String path)
            throws IOException {

            try {
                this.file(
                    new File(Resources.getResource(path).toURI())
                        .getAbsolutePath()
                );
            } catch (final URISyntaxException ex) {
                throw new IOException(ex);
            }
            return this;
        }

        public LdapServerRule.Builder file(final String path) {
            this.ldif.add(path);
            return this;
        }

        public LdapServerRule build() {
            return new LdapServerRule(this.config, this.ldif);
        }
    }
}
