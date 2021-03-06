package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.domain.Stack;

public abstract class StackDependentPollerObject {

    private Stack stack;

    public StackDependentPollerObject(Stack stack) {
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
