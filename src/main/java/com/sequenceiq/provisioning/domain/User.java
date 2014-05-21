package com.sequenceiq.provisioning.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

@Entity
@NamedQuery(
        name = "User.findOneWithLists",
        query = "SELECT u FROM User u "
                + "LEFT JOIN FETCH u.azureInfraList "
                + "LEFT JOIN FETCH u.awsInfraList "
                + "LEFT JOIN FETCH u.cloudInstances "
                + "WHERE u.id= :id")
public class User implements ProvisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty
    private String firstName;

    @NotEmpty
    private String lastName;

    @Email
    @NotEmpty
    @Column(unique = true, nullable = false)
    private String email;

    private String roleArn;

    private String subscriptionId;

    private String jks;

    @NotEmpty
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AzureInfra> azureInfraList = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AwsInfra> awsInfraList = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CloudInstance> cloudInstances = new HashSet<>();

    public User() {
    }

    public User(User user) {
        this.id = user.id;
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.email = user.email;
        this.password = user.password;
        this.awsInfraList = user.awsInfraList;
        this.azureInfraList = user.azureInfraList;
        this.jks = user.jks;
        this.subscriptionId = user.subscriptionId;
        this.roleArn = user.roleArn;
        this.cloudInstances = user.cloudInstances;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<AzureInfra> getAzureInfraList() {
        return azureInfraList;
    }

    public void setAwsInfraList(Set<AwsInfra> awsInfraList) {
        this.awsInfraList = awsInfraList;
    }

    public Set<AwsInfra> getAwsInfraList() {
        return awsInfraList;
    }

    public void setAzureInfraList(Set<AzureInfra> azureInfraList) {
        this.azureInfraList = azureInfraList;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getJks() {
        return jks;
    }

    public void setJks(String jks) {
        this.jks = jks;
    }

    public Set<CloudInstance> getCloudInstances() {
        return cloudInstances;
    }

    public void setCloudInstances(Set<CloudInstance> cloudInstances) {
        this.cloudInstances = cloudInstances;
    }

    public String emailAsFolder() {
        return email.replaceAll("@", "_").replace(".", "_");
    }
}