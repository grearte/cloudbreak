package com.sequenceiq.cloudbreak.core.flow.containers;

import static com.sequenceiq.cloudbreak.service.cluster.flow.DockerContainer.MUNCHAUSEN;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;

public class MunchausenBootstrap {

    private final DockerClient docker;
    private final String privateIp;
    private final String consulServers;
    private final String consulJoinIps;

    public MunchausenBootstrap(DockerClient docker, String privateIp, String consulServers, String consulJoinIps) {
        this.docker = docker;
        this.privateIp = privateIp;
        this.consulJoinIps = consulJoinIps;
        this.consulServers = consulServers;
    }

    public Boolean call() {
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        CreateContainerResponse response = docker.createContainerCmd(MUNCHAUSEN.getContainer().get())
                .withEnv(String.format("BRIDGE_IP=%s", privateIp))
                .withName(MUNCHAUSEN.getName())
                .withHostConfig(hostConfig)
                .withCmd("--debug", "bootstrap", "--consulServers", consulServers, consulJoinIps)
                .exec();
        docker.startContainerCmd(response.getId())
                .withBinds(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock")))
                .exec();
        return true;
    }
}
