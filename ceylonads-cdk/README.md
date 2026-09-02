# CeylonAds AWS CDK Infrastructure

Java AWS CDK project for two CeylonAds environments in **Mumbai (`ap-south-1`)**.

## Architecture

Each environment has two CloudFormation stacks so that the ECR repository can be created **before** App Runner tries to start from an image.

```text
CeylonAdsDevFoundation              CeylonAdsDevApp
├── VPC                             ├── App Runner 0.25 vCPU / 1 GB
├── RDS PostgreSQL db.t4g.micro     ├── App Runner VPC connector
├── S3 public-read media bucket     ├── App Runner scaling config
└── ECR repository                  └── IAM roles

CeylonAdsProdFoundation             CeylonAdsProdApp
├── VPC                             ├── App Runner 0.25 vCPU / 1 GB
├── RDS PostgreSQL db.t4g.small     ├── App Runner VPC connector
├── S3 public-read media bucket     ├── App Runner scaling config
└── ECR repository                  └── IAM roles
```

Conceptually you still have only **DEV** and **PROD**. The foundation/app split solves the initial ECR-image bootstrapping problem and makes data resources safer to manage.

## Defaults

| Setting | Dev | Prod |
|---|---:|---:|
| RDS | db.t4g.micro | db.t4g.small |
| PostgreSQL | 18 major version | 18 major version |
| Storage | 20 GB gp3 | 20 GB gp3 |
| Storage autoscale max | 50 GB | 100 GB |
| Multi-AZ | No | No |
| DB backup retention | 1 day | 7 days |
| DB deletion protection | No | Yes |
| App Runner | 0.25 vCPU / 1 GB | 0.25 vCPU / 1 GB |
| Minimum App Runner instances | 1 | 1 |
| Maximum App Runner instances | 2 | 4 |
| S3 public read access | Open (bucket policy) | Open (bucket policy) |
| RDS public access | Open (initial stage) | Open (initial stage) |
| S3 versioning | No | Yes |
| NAT Gateway | 0 | 0 |
| Region | ap-south-1 | ap-south-1 |

## Why NAT Gateway is disabled

App Runner uses a VPC connector so it can reach the private RDS instance. Once App Runner sends outbound traffic through the VPC, public-internet access requires a NAT path. NAT Gateway has a material fixed hourly cost, so this starter stack deliberately creates **zero NAT Gateways**.

With the current configuration:

- App Runner can reach private RDS.
- App Runner can reach S3 using the S3 VPC gateway endpoint.
- App Runner itself remains publicly reachable from the web.
- App Runner **cannot initiate calls to arbitrary public internet endpoints through the VPC**.

When CeylonAds needs outbound payment gateways, SMS providers, email APIs, etc., change `natGateways` from `0` to `1` in `EnvironmentConfig.prod()` (and dev if needed), then deploy the foundation and app stacks again. This is intentionally not enabled early because of cost.

## RDS access (initial stage: open)

For the initial stage, RDS is deployed in the **public** subnet with `publiclyAccessible=true`, and port 5432 is open to `0.0.0.0/0` in addition to the App Runner VPC-connector security group (`EnvironmentConfig.databasePubliclyAccessible = true`). This makes it possible to connect directly from a local machine to run SQL scripts and verify data.

This is intentionally permissive and **not** a long-term production posture. To lock it down later:

1. Set `databasePubliclyAccessible = false` in `EnvironmentConfig.prod()` (and dev if desired).
2. Redeploy the foundation stack (`cdk deploy CeylonAdsProdFoundation`). This moves RDS back into the private subnet and removes the `0.0.0.0/0:5432` ingress rule.
3. For any future local access, prefer a controlled path such as SSM/bastion or a VPN instead of reopening the security group.

## Prerequisites

Install:

1. Java 21+ (a JDK 21+ on `PATH` — the Gradle wrapper below handles Gradle itself)
2. Node.js/npm
3. AWS CLI v2
4. AWS CDK CLI v2
5. Docker Desktop / Docker Engine

No separate Gradle install is required — always use the committed wrapper (`./gradlew`), which downloads the pinned Gradle version on first run.

### Install CDK

```bash
npm install -g aws-cdk
cdk --version
```

### Configure AWS CLI

```bash
aws configure
aws sts get-caller-identity
```

The AWS identity used for the first setup needs enough permission to bootstrap and create IAM, VPC, RDS, S3, ECR and App Runner resources. For a personal account, using an administrative deployment identity during initial setup is simplest; tighten permissions later.

## First-time bootstrap

From this project directory:

```bash
./scripts/bootstrap.sh
```

Equivalent manual command:

```bash
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
cdk bootstrap aws://$ACCOUNT_ID/ap-south-1
```

Bootstrap is normally once per AWS account/region combination.

## Review before deploying

```bash
cdk list
cdk synth CeylonAdsDevFoundation
cdk diff CeylonAdsDevFoundation
```

Expected stacks:

```text
CeylonAdsDevFoundation
CeylonAdsDevApp
CeylonAdsProdFoundation
CeylonAdsProdApp
```

## First DEV deployment

### 1. Create the infrastructure foundation

```bash
./scripts/deploy-foundation.sh dev
```

This creates VPC, RDS, S3 and ECR resources.

### 2. Push your CeylonAds Docker image

Your CeylonAds project needs a Dockerfile whose container listens on port `8080`.

```bash
./scripts/push-image.sh dev ../ceylonads latest
```

On Apple Silicon the script deliberately builds `linux/amd64`, avoiding an architecture mismatch with App Runner.

### 3. Deploy App Runner

```bash
./scripts/deploy-app.sh dev
```

CDK prints an `AppRunnerServiceUrl` output. Open that URL to reach the application.

### One-command equivalent

After CDK has been bootstrapped:

```bash
./scripts/deploy-env.sh dev ../ceylonads
```

## First PROD deployment

Use exactly the same flow:

```bash
./scripts/deploy-foundation.sh prod
./scripts/push-image.sh prod ../ceylonads latest
./scripts/deploy-app.sh prod
```

Or:

```bash
./scripts/deploy-env.sh prod ../ceylonads
```

## Application configuration supplied by App Runner

The stack supplies these runtime environment variables:

```text
SPRING_PROFILES_ACTIVE=aws,dev|prod
DB_HOST=<RDS endpoint>
DB_PORT=5432
DB_NAME=ceylonads
DB_USERNAME=ceylonads
DB_PASSWORD=<fixed value from EnvironmentConfig.databasePassword>
S3_BUCKET=<public-read media bucket>
S3_PUBLIC_BASE_URL=<https://<bucket>.s3.<region>.amazonaws.com>
AWS_REGION=ap-south-1
```

`application-aws.yml` binds directly to these:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:ceylonads}}
    username: ${DB_USERNAME:ceylonads}
    password: ${DB_PASSWORD:}

ceylonads:
  media:
    s3:
      bucket: ${S3_BUCKET:ceylonads-media}
      public-base-url: ${S3_PUBLIC_BASE_URL:https://ceylonads-media.s3.ap-south-1.amazonaws.com}
      region: ${AWS_REGION:ap-south-1}
```

For your storage code, use `S3_BUCKET`/`S3_PUBLIC_BASE_URL` and the default AWS credential provider chain. App Runner receives S3 read/write permission through its instance role, so do **not** put AWS access keys into application properties. The bucket itself also allows anonymous `s3:GetObject` via bucket policy, so `S3_PUBLIC_BASE_URL` links work directly in a browser without going through the app.

## Database password

No Secrets Manager is used. The RDS master password is a fixed value set in `EnvironmentConfig.databasePassword()` (currently `Rambutan123` for both dev and prod — note RDS master passwords cannot contain `/`, `"`, `@`, or spaces), passed straight through to App Runner as the plain `DB_PASSWORD` environment variable — matching how `application-aws.yml` reads it directly (`${DB_PASSWORD:}`), with no runtime Secrets Manager lookup in the app.

This is simple but means the password lives in the CDK source and in the CloudFormation template in plain text. To change it, edit `EnvironmentConfig.databasePassword()` and redeploy the foundation stack — note that RDS `ModifyDBInstance` password changes apply immediately by default, so redeploying updates the live password (App Runner env vars are also updated on the next app-stack deploy).

## Updating the application

The App Runner service uses ECR tag `latest` and automatic deployments are enabled.

Build and push a new image:

```bash
./scripts/push-image.sh dev ../ceylonads latest
```

App Runner should detect a new same-account ECR image and deploy it. You normally do not need to run `cdk deploy` for application-code-only changes.

Use immutable version tags later if you want stricter release control.

## Infrastructure changes

When you change CDK code:

```bash
cdk diff CeylonAdsDevFoundation
cdk diff CeylonAdsDevApp
cdk deploy CeylonAdsDevFoundation CeylonAdsDevApp
```

Do the same for prod only after testing dev.

## Destroying DEV

The dev configuration intentionally allows destructive cleanup:

```bash
./scripts/destroy-dev.sh
```

This removes App Runner first and then the dev foundation. Dev RDS, S3 content and ECR images may be deleted.

## PROD deletion safety

Production has stronger protection:

- RDS deletion protection is enabled.
- RDS uses snapshot retention behavior.
- S3 uses `RETAIN`.
- ECR uses `RETAIN`.

Do not use production destruction as a routine operation. If you ever intentionally dismantle prod, take backups and explicitly decide what to retain first.

## Domain name

The domain can be registered anywhere: GoDaddy, Route 53, Cloudflare, etc. The registrar does not have to match AWS hosting.

After App Runner is deployed, add a custom domain to the App Runner service and create the DNS validation/routing records at your DNS provider. This project deliberately does not hard-code a domain because the registrar/DNS zone has not been specified.

A later CDK revision can add Route 53 records automatically if DNS is moved to Route 53.

## S3 media access

The media bucket allows **public read** (`s3:GetObject` via bucket policy, not ACLs) so ad photos are directly browsable at `S3_PUBLIC_BASE_URL/<key>`. Write/modify/delete access is restricted to the application's App Runner instance role (`grantReadWrite`) — nothing else can write to the bucket.

For a higher-traffic production setup, consider adding CloudFront in front of the bucket (cheaper egress, custom domain, caching) while keeping the bucket policy as-is or switching to an OAC-restricted bucket. Not required for the initial stage.

## Cost-sensitive choices made here

This project deliberately avoids initially adding:

- NAT Gateway
- Application Load Balancer
- API Gateway
- CloudFront
- ElastiCache/Redis
- Multi-AZ RDS
- read replicas
- ECS/EKS

Those can be introduced when CeylonAds traffic or requirements justify them.

## Important note about App Runner and ECR

App Runner cannot start an ECR-backed service if the referenced image does not exist yet. That is why the first deployment is:

```text
Foundation -> push Docker image -> App stack
```

After the initial deployment, normal image pushes can update App Runner automatically.

## Files to customize first

`EnvironmentConfig.java`

- RDS size
- backups
- App Runner CPU/RAM
- scaling limits
- NAT Gateway count

`CeylonAdsApplicationStack.java`

- Spring profile/environment variables
- App Runner port/health checks

`CeylonAdsFoundationStack.java`

- VPC/networking
- RDS settings
- S3/ECR retention policies

## Recommended CeylonAds workflow

```text
Local
  Spring Boot + local PostgreSQL
        |
        v
AWS DEV
  App Runner + db.t4g.micro + S3
        |
        v
AWS PROD
  App Runner + db.t4g.small + S3
```

Use DEV to verify Docker, Flyway, S3 and AWS-profile behavior. Keep everyday development local so the cloud dev stack does not have to be running continuously.
