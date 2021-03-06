/**
 * Copyright 2009-2012 the original author or authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.springframework.extensions.jcr.jackrabbit;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.jackrabbit.api.XASession;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.extensions.jcr.JcrCallback;
import org.springframework.extensions.jcr.JcrTemplate;
import org.springframework.extensions.jcr.SessionFactory;
import org.springframework.extensions.jcr.jackrabbit.support.UserTxSessionHolder;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Costin Leau
 * @author Sergio Bossa
 * @author Salvatore Incandela
 * 
 * see {@link LocalTransactionManager2Test}
 */
@Ignore
public class LocalTransactionManagerTest {

    /**
     * see {@link LocalTransactionManager2Test#testTransactionCommit()}
     * @throws Exception
     */
    @Test
    @Ignore
    public void testTransactionCommit() throws Exception {
        final SessionFactory sessionFactory = createMock(SessionFactory.class);
        final XASession xaSession = createMock(XASession.class);
        // create nice mock
        XAResource xaResource = createMock(XAResource.class);

        expect(sessionFactory.getSession()).andReturn(xaSession);
        expect(xaSession.getXAResource()).andReturn(xaResource);

        xaSession.save();
        xaSession.logout();

        Xid xidMock = new XidMock();

        xaResource.start(xidMock, XAResource.TMNOFLAGS);
        expect(xaResource.prepare(xidMock)).andReturn(0);
        xaResource.commit(xidMock, false);
        xaResource.end(xidMock, XAResource.TMSUCCESS);

        replay(sessionFactory, xaSession, xaResource);

        PlatformTransactionManager tm = new LocalTransactionManager(sessionFactory);
        TransactionTemplate tt = new TransactionTemplate(tm);
        assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sessionFactory));
        assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

        tt.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sessionFactory));
                JcrTemplate template = new JcrTemplate(sessionFactory);
                template.save();
            }
        });

        assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sessionFactory));
        assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

        verify(sessionFactory, xaResource, xaSession);
    }

    /**
     * see {@link LocalTransactionManager2Test#testTransactionRollback()}
     * @throws Exception
     */
    @Test
    @Ignore
    public void testTransactionRollback() throws Exception {
        final SessionFactory sessionFactory = createMock(SessionFactory.class);
        final XASession xaSession = createMock(XASession.class);
        XAResource xaResource = createMock(XAResource.class);

        expect(sessionFactory.getSession()).andReturn(xaSession);

        expect(xaSession.getXAResource()).andReturn(xaResource);
        xaSession.save();
        xaSession.logout();

        Xid xidMock = new XidMock();

        xaResource.start(xidMock, XAResource.TMNOFLAGS);
        xaResource.end(xidMock, XAResource.TMFAIL);
        xaResource.rollback(xidMock);

        replay(sessionFactory, xaSession, xaResource);

        PlatformTransactionManager tm = new LocalTransactionManager(sessionFactory);
        TransactionTemplate tt = new TransactionTemplate(tm);
        assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sessionFactory));
        assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

        try {
            tt.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sessionFactory));
                    JcrTemplate template = new JcrTemplate(sessionFactory);
                    template.execute(new JcrCallback<Object>() {
                        @Override
                        public Object doInJcr(Session se) throws RepositoryException {
                            se.save();
                            throw new RuntimeException();
                        }
                    });
                }
            });
        } catch (RuntimeException e) {
            // it's okay
        }

        assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sessionFactory));
        assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

        verify(sessionFactory, xaSession, xaResource);
    }

    @Test
    public void testTransactionRollbackOnly() throws Exception {
        final SessionFactory sessionFactory = createMock(SessionFactory.class);
        final XASession xaSession = createMock(XASession.class);
        XAResource xaResource = createMock(XAResource.class);

        expect(sessionFactory.getSession()).andReturn(xaSession);

        expect(xaSession.getXAResource()).andReturn(xaResource);
        xaSession.save();
        xaSession.logout();

        Xid xidMock = new XidMock();

        xaResource.start(xidMock, XAResource.TMNOFLAGS);
        xaResource.end(xidMock, XAResource.TMFAIL);
        xaResource.rollback(xidMock);

        replay(sessionFactory, xaSession, xaResource);

        PlatformTransactionManager transactionManager = new LocalTransactionManager(sessionFactory);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sessionFactory));
        assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sessionFactory));
                JcrTemplate template = new JcrTemplate(sessionFactory);
                template.execute(new JcrCallback<Object>() {
                    @Override
                    public Object doInJcr(Session se) throws RepositoryException {
                        se.save();
                        return null;
                    }

                });
                status.setRollbackOnly();
            }
        });

        assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sessionFactory));
        assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

        verify(sessionFactory, xaSession, xaResource);
    }

    @Test
    public void testInvalidIsolation() throws Exception {
        final SessionFactory sessionFactory = createMock(SessionFactory.class);

        replay(sessionFactory);

        PlatformTransactionManager transactionManager = new LocalTransactionManager(sessionFactory);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sessionFactory));
        assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sessionFactory));
                    JcrTemplate template = new JcrTemplate(sessionFactory);
                    template.execute(new JcrCallback<Object>() {
                        @Override
                        public Object doInJcr(Session session) throws RepositoryException {
                            return null;
                        }
                    });
                }
            });
            fail("Should have thrown InvalidIsolationLevelException");

        } catch (InvalidIsolationLevelException e) {
            // it's okay
        }

        assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sessionFactory));
        assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

        verify(sessionFactory);
    }

    @Test
    public void testTransactionCommitWithPrebound() throws Exception {
        final SessionFactory sessionFactory = createMock(SessionFactory.class);
        final XASession xaSession = createMock(XASession.class);
        XAResource xaResource = createMock(XAResource.class);

        expect(xaSession.getXAResource()).andReturn(xaResource);
        xaSession.save();

        replay(sessionFactory, xaSession, xaResource);

        PlatformTransactionManager transactionManager = new LocalTransactionManager(sessionFactory);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        UserTxSessionHolder userTransaction = new UserTxSessionHolder(xaSession);
        TransactionSynchronizationManager.bindResource(sessionFactory, userTransaction);

        assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sessionFactory));

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sessionFactory));
                JcrTemplate template = new JcrTemplate(sessionFactory);
                template.save();
            }
        });

        assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sessionFactory));
        TransactionSynchronizationManager.unbindResource(sessionFactory);
        assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

        verify(sessionFactory, xaSession, xaResource);
    }

    @Test
    public void testTransactionRollbackOnlyWithPrebound() throws Exception {
        final SessionFactory sessionFactory = createMock(SessionFactory.class);
        final XASession xaSession = createMock(XASession.class);
        XAResource xaResource = createMock(XAResource.class);

        expect(xaSession.getXAResource()).andReturn(xaResource);
        xaSession.save();

        replay(sessionFactory, xaSession, xaResource);

        PlatformTransactionManager transactionManager = new LocalTransactionManager(sessionFactory);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        UserTxSessionHolder userTransaction = new UserTxSessionHolder(xaSession);
        TransactionSynchronizationManager.bindResource(sessionFactory, userTransaction);

        assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sessionFactory));
        userTransaction.setRollbackOnly();

        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sessionFactory));
                    JcrTemplate template = new JcrTemplate(sessionFactory);
                    template.save();
                }
            });

        } catch (UnexpectedRollbackException e) {
            System.out.println(e);
        }

        assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sessionFactory));
        TransactionSynchronizationManager.unbindResource(sessionFactory);
        assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

        verify(sessionFactory, xaSession, xaResource);
    }

}
