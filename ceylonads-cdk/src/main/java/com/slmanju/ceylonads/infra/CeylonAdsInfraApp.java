package com.slmanju.ceylonads.infra;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;

public final class CeylonAdsInfraApp {
    private CeylonAdsInfraApp() {
    }

    public static void main(final String[] args) {
        App app = new App();

        String account = envOrDefault("CDK_DEFAULT_ACCOUNT", System.getenv("AWS_ACCOUNT_ID"));
        if (account == null || account.isBlank()) {
            throw new IllegalStateException(
                    "AWS account is not available. Run through the CDK CLI after 'aws configure', " +
                    "or set AWS_ACCOUNT_ID."
            );
        }

        String region = envOrDefault("CDK_DEFAULT_REGION", "ap-south-1");
        Environment awsEnvironment = Environment.builder()
                .account(account)
                .region(region)
                .build();

        createEnvironment(app, awsEnvironment, EnvironmentConfig.dev());
        createEnvironment(app, awsEnvironment, EnvironmentConfig.prod());

        app.synth();
    }

    private static void createEnvironment(App app, Environment awsEnvironment, EnvironmentConfig config) {
        StackProps props = StackProps.builder().env(awsEnvironment).build();

        CeylonAdsFoundationStack foundation = new CeylonAdsFoundationStack(
                app,
                config.stackPrefix() + "Foundation",
                props,
                config
        );

        CeylonAdsApplicationStack application = new CeylonAdsApplicationStack(
                app,
                config.stackPrefix() + "App",
                props,
                config,
                foundation
        );
        application.addDependency(foundation);

        Tags.of(foundation).add("Application", "CeylonAds");
        Tags.of(foundation).add("Environment", config.name());
        Tags.of(application).add("Application", "CeylonAds");
        Tags.of(application).add("Environment", config.name());
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
