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
package org.springframework.extensions.jcr;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.fail;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.Invocation;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Costin Leau
 * @author Sergio Bossa
 * @author Salvatore Incandela
 */
public class JcrInterceptorTest {

    @Test
    public void testInterceptor() throws RepositoryException {

        SessionFactory sessionFactory = createMock(SessionFactory.class);
        Session session = createMock(Session.class);
        expect(sessionFactory.getSession()).andReturn(session);
        session.logout();
        expectLastCall().once();
        expect(sessionFactory.getSessionHolder(session)).andReturn(new SessionHolder(session));

        replay(sessionFactory, session);

        JcrInterceptor interceptor = new JcrInterceptor();
        interceptor.setSessionFactory(sessionFactory);
        interceptor.afterPropertiesSet();
        try {
            interceptor.invoke(new TestInvocation(sessionFactory));
        } catch (Throwable t) {
            fail("Should not have thrown Throwable: " + t);
        }

        verify(sessionFactory, session);
    }

    @Test
    public void testInterceptorWithPrebound() {
        SessionFactory sessionFactory = createMock(SessionFactory.class);
        Session session = createMock(Session.class);

        replay(sessionFactory, session);

        TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
        JcrInterceptor interceptor = new JcrInterceptor();
        interceptor.setSessionFactory(sessionFactory);
        interceptor.afterPropertiesSet();
        try {
            interceptor.invoke(new TestInvocation(sessionFactory));
        } catch (Throwable t) {
            fail("Should not have thrown Throwable: " + t.getMessage());
        } finally {
            TransactionSynchronizationManager.unbindResource(sessionFactory);
        }

        verify(sessionFactory, session);
    }

    private static class TestInvocation implements MethodInvocation {

        private final SessionFactory sessionFactory;

        public TestInvocation(SessionFactory sessionFactory) {
            this.sessionFactory = sessionFactory;
        }

        @Override
        public Object proceed() throws Throwable {
            if (!TransactionSynchronizationManager.hasResource(this.sessionFactory)) {
                throw new IllegalStateException("Session not bound");
            }
            return null;
        }

        @Override
        public Object[] getArguments() {
            return null;
        }

        public int getCurrentInterceptorIndex() {
            return 0;
        }

        public int getNumberOfInterceptors() {
            return 0;
        }

        public Interceptor getInterceptor(int i) {
            return null;
        }

        @Override
        public Method getMethod() {
            return null;
        }

        @Override
        public AccessibleObject getStaticPart() {
            return getMethod();
        }

        public Object getArgument(int i) {
            return null;
        }

        public void setArgument(int i, Object handler) {
        }

        public int getArgumentCount() {
            return 0;
        }

        @Override
        public Object getThis() {
            return null;
        }

        public Object getProxy() {
            return null;
        }

        public Invocation cloneInstance() {
            return null;
        }

        public void release() {
        }
    }

}
