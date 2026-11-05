package com.stockpilot.infrastructure.persistence;

import com.stockpilot.core.port.out.CurrentUserProvider;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;

/** Stamps created/updated time and user on any entity that has properties with the audit names. */
class AuditInterceptor implements Interceptor {

    private final CurrentUserProvider currentUser;

    AuditInterceptor(CurrentUserProvider currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        Instant now = Instant.now();
        String user = currentUser.currentUserOrSystem();
        boolean changed = setIfNull(state, propertyNames, "createdAt", now);
        changed |= setIfNull(state, propertyNames, "createdBy", user);
        changed |= set(state, propertyNames, "updatedAt", now);
        changed |= set(state, propertyNames, "updatedBy", user);
        return changed;
    }

    @Override
    public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
                                String[] propertyNames, Type[] types) {
        boolean changed = set(currentState, propertyNames, "updatedAt", Instant.now());
        changed |= set(currentState, propertyNames, "updatedBy", currentUser.currentUserOrSystem());
        return changed;
    }

    private static boolean set(Object[] state, String[] names, String name, Object value) {
        int index = List.of(names).indexOf(name);
        if (index < 0) {
            return false;
        }
        state[index] = value;
        return true;
    }

    private static boolean setIfNull(Object[] state, String[] names, String name, Object value) {
        int index = Arrays.asList(names).indexOf(name);
        if (index < 0 || state[index] != null) {
            return false;
        }
        state[index] = value;
        return true;
    }
}
