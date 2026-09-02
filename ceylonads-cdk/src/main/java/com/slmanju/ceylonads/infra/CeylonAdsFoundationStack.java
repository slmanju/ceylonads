package com.slmanju.ceylonads.infra;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.GatewayVpcEndpointAwsService;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.IpAddresses;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.rds.StorageType;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class CeylonAdsFoundationStack extends Stack {
    private final EnvironmentConfig config;
    private final Vpc vpc;
    private final SecurityGroup appRunnerSecurityGroup;
    private final DatabaseInstance database;
    private final Bucket mediaBucket;
    private final Repository appRepository;

    public CeylonAdsFoundationStack(
            final Construct scope,
            final String id,
            final StackProps props,
            final EnvironmentConfig config
    ) {
        super(scope, id, props);
        this.config = config;

        this.vpc = createVpc();
        this.appRunnerSecurityGroup = createAppRunnerSecurityGroup();
        SecurityGroup databaseSecurityGroup = createDatabaseSecurityGroup();
        this.database = createDatabase(databaseSecurityGroup);
        this.mediaBucket = createMediaBucket();
        this.appRepository = createAppRepository();

        createOutputs();
    }

    private Vpc createVpc() {
        Vpc vpc = Vpc.Builder.create(this, "Vpc")
                .vpcName("ceylonads-" + config.name())
                .ipAddresses(IpAddresses.cidr(config.name().equals("prod") ? "10.20.0.0/16" : "10.10.0.0/16"))
                .maxAzs(2)
                .natGateways(config.natGateways())
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .name("public")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("application")
                                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                                .cidrMask(24)
                                .build()
                ))
                .build();

        // With NAT disabled, S3 remains reachable privately and without NAT data processing charges.
        vpc.addGatewayEndpoint("S3Endpoint", software.amazon.awscdk.services.ec2.GatewayVpcEndpointOptions.builder()
                .service(GatewayVpcEndpointAwsService.S3)
                .subnets(List.of(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                        .build()))
                .build());

        return vpc;
    }

    private SecurityGroup createAppRunnerSecurityGroup() {
        return SecurityGroup.Builder.create(this, "AppRunnerSecurityGroup")
                .vpc(vpc)
                .securityGroupName("ceylonads-" + config.name() + "-apprunner")
                .description("Outbound security group for CeylonAds App Runner VPC connector")
                .allowAllOutbound(true)
                .build();
    }

    private SecurityGroup createDatabaseSecurityGroup() {
        SecurityGroup sg = SecurityGroup.Builder.create(this, "DatabaseSecurityGroup")
                .vpc(vpc)
                .securityGroupName("ceylonads-" + config.name() + "-rds")
                .description("Allows PostgreSQL from the App Runner VPC connector"
                        + (config.databasePubliclyAccessible() ? " and from the public internet (initial stage)" : ""))
                .allowAllOutbound(true)
                .build();

        sg.addIngressRule(appRunnerSecurityGroup, Port.tcp(5432), "PostgreSQL from App Runner");

        if (config.databasePubliclyAccessible()) {
            // Initial-stage open access so the DB can be reached directly (e.g. to run SQL scripts).
            // Tighten later: remove this rule and set databasePubliclyAccessible=false in EnvironmentConfig.
            sg.addIngressRule(Peer.anyIpv4(), Port.tcp(5432), "PostgreSQL open access (initial stage)");
        }
        return sg;
    }

    private DatabaseInstance createDatabase(SecurityGroup databaseSecurityGroup) {
        InstanceSize size = config.databaseSize();

        DatabaseInstance db = DatabaseInstance.Builder.create(this, "Database")
                .instanceIdentifier("ceylonads-" + config.name())
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_18)
                        .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE4_GRAVITON, size))
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(config.databasePubliclyAccessible() ? SubnetType.PUBLIC : SubnetType.PRIVATE_WITH_EGRESS)
                        .build())
                .securityGroups(List.of(databaseSecurityGroup))
                .databaseName("ceylonads")
                .credentials(Credentials.fromPassword("ceylonads", SecretValue.unsafePlainText(config.databasePassword())))
                .port(5432)
                .allocatedStorage(config.databaseStorageGb())
                .maxAllocatedStorage(config.databaseMaxStorageGb())
                .storageType(StorageType.GP3)
                .multiAz(false)
                .publiclyAccessible(config.databasePubliclyAccessible())
                .backupRetention(Duration.days(config.databaseBackupDays()))
                .deleteAutomatedBackups(!config.retainPersistentData())
                .deletionProtection(config.deletionProtection())
                .autoMinorVersionUpgrade(true)
                .enablePerformanceInsights(false)
                .build();

        db.applyRemovalPolicy(config.retainPersistentData() ? RemovalPolicy.SNAPSHOT : RemovalPolicy.DESTROY);
        return db;
    }

    private Bucket createMediaBucket() {
        // Public read (via bucket policy, not ACLs) so media URLs are directly browsable.
        // Write/modify access stays restricted to the app's instance role (see CeylonAdsApplicationStack).
        Bucket bucket = Bucket.Builder.create(this, "MediaBucket")
                .bucketName("ceylonads-" + config.name() + "-media-" + this.getAccount())
                .blockPublicAccess(BlockPublicAccess.BLOCK_ACLS)
                .publicReadAccess(true)
                .enforceSsl(true)
                .versioned(config.name().equals("prod"))
                .autoDeleteObjects(!config.retainPersistentData())
                .removalPolicy(config.retainPersistentData() ? RemovalPolicy.RETAIN : RemovalPolicy.DESTROY)
                .build();

        return bucket;
    }

    public String getMediaBucketPublicUrl() {
        return "https://" + mediaBucket.getBucketName() + ".s3." + this.getRegion() + ".amazonaws.com";
    }

    private Repository createAppRepository() {
        Repository repository = Repository.Builder.create(this, "AppRepository")
                .repositoryName("ceylonads-" + config.name() + "-app")
                .imageScanOnPush(true)
                .emptyOnDelete(!config.retainPersistentData())
                .removalPolicy(config.retainPersistentData() ? RemovalPolicy.RETAIN : RemovalPolicy.DESTROY)
                .lifecycleRules(List.of(software.amazon.awscdk.services.ecr.LifecycleRule.builder()
                        .description("Keep recent CeylonAds images")
                        .maxImageCount(config.name().equals("prod") ? 20 : 5)
                        .build()))
                .build();

        return repository;
    }

    private void createOutputs() {
        output("Environment", config.name());
        output("VpcId", vpc.getVpcId());
        output("DatabaseEndpoint", database.getDbInstanceEndpointAddress());
        output("DatabasePort", database.getDbInstanceEndpointPort());
        output("DatabaseName", "ceylonads");
        output("DatabaseUsername", "ceylonads");
        output("DatabasePubliclyAccessible", String.valueOf(config.databasePubliclyAccessible()));
        output("MediaBucketName", mediaBucket.getBucketName());
        output("MediaBucketPublicUrl", getMediaBucketPublicUrl());
        output("EcrRepositoryName", appRepository.getRepositoryName());
        output("EcrRepositoryUri", appRepository.getRepositoryUri());
    }

    private void output(String id, String value) {
        CfnOutput.Builder.create(this, id)
                .value(value)
                .exportName(config.stackPrefix() + "-" + id)
                .build();
    }

    public Vpc getVpc() {
        return vpc;
    }

    public SecurityGroup getAppRunnerSecurityGroup() {
        return appRunnerSecurityGroup;
    }

    public DatabaseInstance getDatabase() {
        return database;
    }

    public Bucket getMediaBucket() {
        return mediaBucket;
    }

    public Repository getAppRepository() {
        return appRepository;
    }
}
