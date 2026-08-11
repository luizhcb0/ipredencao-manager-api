package org.ipredencao.ipredencao_manager.service;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FirebaseAuthServiceEmailExistsTest {

    @Test
    void detectsEmailExistsFromAuthErrorCode() {
        FirebaseAuthException ex = mock(FirebaseAuthException.class);
        when(ex.getAuthErrorCode()).thenReturn(AuthErrorCode.EMAIL_ALREADY_EXISTS);

        assertTrue(FirebaseAuthService.isEmailAlreadyExists(ex));
    }

    @Test
    void detectsEmailExistsFromWrappedLambdaMessage() {
        assertTrue(FirebaseAuthService.isEmailAlreadyExists(new RuntimeException(
            new IllegalStateException("Firebase Lambda error: ... already exists (EMAIL_EXISTS)."))));
        assertTrue(FirebaseAuthService.isEmailAlreadyExists(
            new RuntimeException("EMAIL_ALREADY_EXISTS")));
    }

    @Test
    void ignoresUnrelatedErrors() {
        assertFalse(FirebaseAuthService.isEmailAlreadyExists(new RuntimeException("INVALID_PASSWORD")));
        assertFalse(FirebaseAuthService.isEmailAlreadyExists(new RuntimeException((String) null)));
    }
}
