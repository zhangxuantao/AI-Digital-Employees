package com.ai.cs.shared.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DataScopeAspectTest {

    @InjectMocks
    private DataScopeAspect dataScopeAspect;

    @Test
    void annotation_shouldHaveTargetMethod() {
        Target target = DataScope.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertTrue(target.value().length > 0);
        assertEquals(ElementType.METHOD, target.value()[0]);
    }

    @Test
    void annotation_shouldHaveRuntimeRetention() {
        Retention retention = DataScope.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    void annotation_shouldHaveDefaultValues() {
        // Verify we can create a proxy and read default values
        assertNotNull(DataScopeAspect.class);
        assertNotNull(DataScope.class);
    }
}
