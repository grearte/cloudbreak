package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;

import reactor.event.Event;

@Component
public class StackUpscaleHandler extends AbstractFlowHandler<StackScalingContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpscaleHandler.class);

    @Override
    protected Object execute(Event<StackScalingContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        FlowContext context = getFlowFacade().upscaleStack(event.getData());
        LOGGER.info("Upscale of stack is finished. Context: {}", context);
        return context;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, StackScalingContext data) throws Exception {
        LOGGER.info("handleErrorFlow() for phase: {}", getClass());
        data.setErrorReason(throwable.getMessage());
        return getFlowFacade().handleStackScalingFailure(data);
    }
}
