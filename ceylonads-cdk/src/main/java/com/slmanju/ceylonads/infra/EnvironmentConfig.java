package com.slmanju.ceylonads.infra;

import software.amazon.awscdk.services.ec2.InstanceSize;

public record EnvironmentConfig(
        String name,
        String stackPrefix,
        InstanceSize databaseSize,
        int databaseStorageGb,
        int databaseMaxStorageGb,
        int databaseBackupDays,
        boolean deletionProtection,
        boolean retainPersistentData,
        int natGateways,
        String appRunnerCpu,
        String appRunnerMemory,
        int appRunnerMinInstances,
        int appRunnerMaxInstances,
        int appRunnerMaxConcurrency,
        String imageTag,
        boolean databasePubliclyAccessible,
        String databasePassword,
        String jwtSecretBase64
) {
    public static EnvironmentConfig dev() {
        return new EnvironmentConfig(
                "dev",
                "CeylonAdsDev",
                InstanceSize.MICRO,
                20,
                50,
                1,
                false,
                false,
                0,
                "0.25 vCPU",
                "1 GB",
                1,
                2,
                80,
                "latest",
                true,
                "Rambutan123",
                "OBoxaQKuPoPm/c73dxOA3NUP3RC7xc8hn6SYBuci9Qs="
        );
    }

    public static EnvironmentConfig prod() {
        return new EnvironmentConfig(
                "prod",
                "CeylonAdsProd",
                InstanceSize.SMALL,
                20,
                100,
                7,
                true,
                true,
                0,
                "0.25 vCPU",
                "1 GB",
                1,
                4,
                80,
                "latest",
                // Initial-stage only: allows direct/public connections to run setup SQL.
                // Set to false once the app is stable and lock RDS back to VPC-only access.
                true,
                "Rambutan123",
                "rC4Si1sWTMOZO1VLMjbUrklVQOGgh+oSSpAlOHDHwe0="
        );
    }
}
