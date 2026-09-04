package com.slmanju.ceylonads.infra;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apprunner.CfnAutoScalingConfiguration;
import software.amazon.awscdk.services.apprunner.CfnService;
import software.amazon.awscdk.services.apprunner.CfnVpcConnector;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;

import java.util.List;

public class CeylonAdsApplicationStack extends Stack {

    public CeylonAdsApplicationStack(
            final Construct scope,
            final String id,
            final StackProps props,
            final EnvironmentConfig config,
            final CeylonAdsFoundationStack foundation
    ) {
        super(scope, id, props);

        Role ecrAccessRole = createEcrAccessRole(config);
        Role instanceRole = createInstanceRole(config, foundation);
        CfnVpcConnector vpcConnector = createVpcConnector(config, foundation);
        CfnAutoScalingConfiguration scaling = createScaling(config);
        CfnService service = createAppRunnerService(config, foundation, ecrAccessRole, instanceRole, vpcConnector, scaling);

        CfnOutput.Builder.create(this, "AppRunnerServiceUrl")
                .value(service.getAttrServiceUrl())
                .build();
        CfnOutput.Builder.create(this, "AppRunnerServiceArn")
                .value(service.getAttrServiceArn())
                .build();
    }

    private Role createEcrAccessRole(EnvironmentConfig config) {
        return Role.Builder.create(this, "AppRunnerEcrAccessRole")
                .roleName("ceylonads-" + config.name() + "-apprunner-ecr")
                .assumedBy(new ServicePrincipal("build.apprunner.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSAppRunnerServicePolicyForECRAccess")))
                .build();
    }

    private Role createInstanceRole(EnvironmentConfig config, CeylonAdsFoundationStack foundation) {
        Role role = Role.Builder.create(this, "AppRunnerInstanceRole")
                .roleName("ceylonads-" + config.name() + "-apprunner-instance")
                .assumedBy(new ServicePrincipal("tasks.apprunner.amazonaws.com"))
                .build();

        foundation.getMediaBucket().grantReadWrite(role);
        return role;
    }

    private CfnVpcConnector createVpcConnector(EnvironmentConfig config, CeylonAdsFoundationStack foundation) {
        List<String> subnetIds = foundation.getVpc()
                .selectSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE_WITH_EGRESS).build())
                .getSubnetIds();

        return CfnVpcConnector.Builder.create(this, "AppRunnerVpcConnector")
                .vpcConnectorName("ceylonads-" + config.name())
                .subnets(subnetIds)
                .securityGroups(List.of(foundation.getAppRunnerSecurityGroup().getSecurityGroupId()))
                .build();
    }

    private CfnAutoScalingConfiguration createScaling(EnvironmentConfig config) {
        return CfnAutoScalingConfiguration.Builder.create(this, "AppRunnerScaling")
                .autoScalingConfigurationName("ceylonads-" + config.name())
                .minSize(config.appRunnerMinInstances())
                .maxSize(config.appRunnerMaxInstances())
                .maxConcurrency(config.appRunnerMaxConcurrency())
                .build();
    }

    private CfnService createAppRunnerService(
            EnvironmentConfig config,
            CeylonAdsFoundationStack foundation,
            Role ecrAccessRole,
            Role instanceRole,
            CfnVpcConnector vpcConnector,
            CfnAutoScalingConfiguration scaling
    ) {
        List<CfnService.KeyValuePairProperty> env = List.of(
                keyValue("SPRING_PROFILES_ACTIVE", "aws," + config.name()),
                keyValue("DB_HOST", foundation.getDatabase().getDbInstanceEndpointAddress()),
                keyValue("DB_PORT", foundation.getDatabase().getDbInstanceEndpointPort()),
                keyValue("DB_NAME", "ceylonads"),
                keyValue("DB_USERNAME", "ceylonads"),
                keyValue("DB_PASSWORD", config.databasePassword()),
                keyValue("S3_BUCKET", foundation.getMediaBucket().getBucketName()),
                keyValue("S3_PUBLIC_BASE_URL", foundation.getMediaBucketPublicUrl()),
                keyValue("AWS_REGION", getRegion()),
                keyValue("JWT_SECRET_BASE64", config.jwtSecretBase64()),
                // ceylonads-tuition-ui's own origin (see app.tuition-site-url) - without this, the
                // app falls back to application.yml's localhost default and the ezClass sitemap
                // emits localhost URLs instead of https://ezclass.lk.
                keyValue("EZCLASS_SITE_URL", config.tuitionSiteUrl())
        );

        CfnService.ImageConfigurationProperty imageConfiguration = CfnService.ImageConfigurationProperty.builder()
                .port("8080")
                .runtimeEnvironmentVariables(env)
                .build();

        CfnService.ImageRepositoryProperty imageRepository = CfnService.ImageRepositoryProperty.builder()
                .imageIdentifier(foundation.getAppRepository().getRepositoryUri() + ":" + config.imageTag())
                .imageRepositoryType("ECR")
                .imageConfiguration(imageConfiguration)
                .build();

        CfnService.SourceConfigurationProperty sourceConfiguration = CfnService.SourceConfigurationProperty.builder()
                .authenticationConfiguration(CfnService.AuthenticationConfigurationProperty.builder()
                        .accessRoleArn(ecrAccessRole.getRoleArn())
                        .build())
                .autoDeploymentsEnabled(true)
                .imageRepository(imageRepository)
                .build();

        CfnService.InstanceConfigurationProperty instanceConfiguration = CfnService.InstanceConfigurationProperty.builder()
                .cpu(config.appRunnerCpu())
                .memory(config.appRunnerMemory())
                .instanceRoleArn(instanceRole.getRoleArn())
                .build();

        CfnService.NetworkConfigurationProperty networkConfiguration = CfnService.NetworkConfigurationProperty.builder()
                .egressConfiguration(CfnService.EgressConfigurationProperty.builder()
                        .egressType("VPC")
                        .vpcConnectorArn(vpcConnector.getAttrVpcConnectorArn())
                        .build())
                .ingressConfiguration(CfnService.IngressConfigurationProperty.builder()
                        .isPubliclyAccessible(true)
                        .build())
                .ipAddressType("IPV4")
                .build();

        CfnService.HealthCheckConfigurationProperty healthCheck = CfnService.HealthCheckConfigurationProperty.builder()
                .protocol("TCP")
                .healthyThreshold(1)
                .unhealthyThreshold(5)
                .interval(10)
                .timeout(5)
                .build();

        CfnService service = CfnService.Builder.create(this, "AppRunnerService")
                .serviceName("ceylonads-" + config.name())
                .sourceConfiguration(sourceConfiguration)
                .instanceConfiguration(instanceConfiguration)
                .networkConfiguration(networkConfiguration)
                .healthCheckConfiguration(healthCheck)
                .autoScalingConfigurationArn(scaling.getAttrAutoScalingConfigurationArn())
                .build();

        service.addDependency(vpcConnector);
        service.addDependency(scaling);
        return service;
    }

    private CfnService.KeyValuePairProperty keyValue(String name, String value) {
        return CfnService.KeyValuePairProperty.builder()
                .name(name)
                .value(value)
                .build();
    }
}
