package com.sequenceiq.cloudbreak.service.aws;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudFormationTemplate;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.UserRepository;

@Service
public class CloudFormationStackCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFormationStackCreator.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private CloudFormationTemplate cfTemplate;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private UserRepository userRepository;

    public void createCloudFormationStack(Stack stack, AwsCredential awsCredential, SnsTopic notificationTopic) {
        try {
            // TODO: this method should be thread safe and insert another check
            // here if the stack is still requested
            stack.setStatus(Status.CREATE_IN_PROGRESS);
            stack = stackRepository.save(stack);
            AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
            AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(awsTemplate.getRegion(), awsCredential);
            createStack(stack, awsTemplate, notificationTopic, client);
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while creating stack on AWS.", e);
            awsStackUtil.createFailed(stack);
        }
    }

    public void startAllRequestedStackCreationForTopic(SnsTopic notificationTopic) {
        AwsCredential awsCredential = notificationTopic.getCredential();
        List<Stack> requestedStacks = stackRepository.findRequestedStacksWithCredential(awsCredential.getId());
        for (Stack stack : requestedStacks) {
            createCloudFormationStack(stack, awsCredential, notificationTopic);
        }
    }

    private void createStack(Stack stack, AwsTemplate awsTemplate, SnsTopic snstopic, AmazonCloudFormationClient client) {
        String stackName = String.format("%s-%s", stack.getName(), stack.getId());
        CreateStackRequest createStackRequest = new CreateStackRequest()
                .withStackName(stackName)
                .withTemplateBody(cfTemplate.getBody())
                .withNotificationARNs(snstopic.getTopicArn())
                .withParameters(new Parameter().withParameterKey("SSHLocation").withParameterValue(awsTemplate.getSshLocation()));
        CreateStackResult createStackResult = client.createStack(createStackRequest);
        stack.setCfStackId(createStackResult.getStackId());
        stack.setCfStackName(stackName);
        stackRepository.save(stack);
        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", stackName, stack.getId());
    }

}
