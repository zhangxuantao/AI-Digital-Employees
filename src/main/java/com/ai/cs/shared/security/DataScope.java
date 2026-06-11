package com.ai.cs.shared.security;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    String ownerField() default "ownerAgentId";
    String agentIdParam() default "agentId";
}
