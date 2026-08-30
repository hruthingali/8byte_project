# Expense Tracker DevOps Assignment
![Uploading Screenshot 2026-08-30 151216.png…]()

## Project Overview

This project was developed as part of the DevOps Technical Assignment for 8Byte.ai.

The objective of this project is to demonstrate an end-to-end DevOps workflow covering infrastructure provisioning, CI/CD, containerization, Kubernetes deployment, database integration, security scanning, monitoring, logging, and documentation.

The application used for this assignment is an Expense Tracker application. The primary focus of the project is the DevOps implementation rather than the application business logic.

The infrastructure is hosted on AWS and the application is deployed on Amazon EKS. PostgreSQL is used as the application database and is hosted on Amazon RDS.

The project uses Terraform for Infrastructure as Code, Jenkins for Continuous Integration, Docker and Docker Hub for containerization and image management, Trivy for vulnerability scanning, Argo CD for GitOps-based deployment, and Amazon CloudWatch for monitoring and logging.

## Architecture

The overall architecture of the project is:

```text
                         Developer
                             |
                             v
                          GitHub
                             |
                             v
                          Jenkins
                             |
             +---------------+---------------+
             |               |               |
             v               v               v
          Maven Build   Docker Build    Trivy Scan
                                             |
                                             v
                                        Docker Hub
                                             |
                                             v
                                  Update Kubernetes
                                      Manifests
                                             |
                                             v
                                           GitHub
                                             |
                                             v
                                          Argo CD
                                       /           \
                                      /             \
                                     v               v
                                Staging          Production
                               Auto Sync         Manual Sync
                                    |                 |
                                    +--------+--------+
                                             |
                                             v
                                        Amazon EKS
                                             |
                                             v
                                    Expense Tracker
                                             |
                                             v
                                  Amazon RDS PostgreSQL


                         Amazon EKS
                             |
                             v
              CloudWatch Observability Controller
                             |
                             v
                     CloudWatch Agent
                             |
                             v
                     Amazon CloudWatch
                        /          \
                       v            v
                 Dashboard 1    Dashboard 2
                 EKS Metrics    Application &
                                Database Metrics
```

## Technology Stack

| Technology            | Purpose                              |
| --------------------- | ------------------------------------ |
| AWS                   | Cloud infrastructure                 |
| Terraform             | Infrastructure as Code               |
| Amazon VPC            | Network isolation                    |
| Amazon EKS            | Kubernetes application hosting       |
| Amazon RDS PostgreSQL | Persistent application database      |
| Docker                | Application containerization         |
| Docker Hub            | Container image registry             |
| Jenkins               | Continuous Integration               |
| Maven                 | Application build                    |
| Trivy                 | Container vulnerability scanning     |
| Kubernetes            | Container orchestration              |
| Argo CD               | GitOps-based deployment              |
| Amazon CloudWatch     | Monitoring and logging               |
| GitHub                | Source code and Kubernetes manifests |

# Part 1: Infrastructure Provisioning

## AWS Infrastructure

The AWS infrastructure is provisioned using Terraform.

The infrastructure includes:

* VPC
* Public subnets
* Private subnets
* Internet Gateway
* NAT Gateway
* Route tables
* Security groups
* Amazon EKS
* Amazon RDS PostgreSQL
* Load Balancer

The VPC provides network isolation for the application environment.

Public subnets are used for resources that require internet connectivity, while private subnets are used for internal application workloads and database resources.

The Internet Gateway provides internet connectivity for public resources.

The NAT Gateway provides outbound internet connectivity to resources in private subnets without exposing those resources directly to the internet.

## Network Architecture

The VPC uses the CIDR range:

```text
10.0.0.0/16
```

The network is divided into public and private subnets.

The general network structure is:

```text
AWS VPC
|
+-- Public Subnet
|     |
|     +-- Load Balancer
|
+-- Public Subnet
|
+-- Private Subnet
|     |
|     +-- EKS Workloads
|
+-- Private Subnet
      |
      +-- RDS PostgreSQL
```

The RDS database is kept in private networking so that it is not directly exposed to the public internet.

## Terraform

Terraform is used to provision and manage the AWS infrastructure.

The Terraform configuration uses variables for configurable infrastructure parameters.

The Terraform project contains configuration files such as:

```text
8byte_terraform/

    main.tf
    variables.tf
    outputs.tf
    providers.tf
    terraform.tfvars
```

The basic Terraform workflow is:

```bash
terraform init
terraform validate
terraform plan
terraform apply
```

To destroy the infrastructure:

```bash
terraform destroy
```

## Terraform Variables

Configurable infrastructure parameters are maintained in `variables.tf`.

This allows values such as AWS region, VPC CIDR, subnet configuration, and other infrastructure parameters to be changed without modifying the main resource definitions.

## Terraform State Management

Terraform state is used to track the resources managed by Terraform.

Terraform state should not be committed to the GitHub repository because it can contain sensitive infrastructure information.

For a production environment, remote Terraform state using Amazon S3 with state locking would be preferred to allow secure collaboration and prevent concurrent state modifications.

## Terraform Outputs

Terraform outputs are used to expose important infrastructure information.

Examples include:

* VPC ID
* Subnet IDs
* Resource identifiers
* Other important infrastructure information

# Amazon EKS

Amazon EKS is used as the application hosting platform.

The Expense Tracker application runs as Kubernetes workloads inside the EKS cluster.

Kubernetes Deployments are used to manage application Pods and Kubernetes Services are used to expose the application.

Multiple replicas are configured for the application to improve availability.

The EKS cluster can be verified using:

```bash
kubectl get nodes
```

Application Pods can be checked using:

```bash
kubectl get pods -A
```

Services can be checked using:

```bash
kubectl get svc -A
```

# Amazon RDS PostgreSQL

PostgreSQL is used as the persistent database for the Expense Tracker application.

The database is hosted using Amazon RDS for PostgreSQL.

The application running inside EKS connects to the RDS PostgreSQL database.

Using Amazon RDS keeps the database independent from the application containers and provides a managed database service.

## Database Security

The RDS database is protected using AWS Security Groups.

Access to PostgreSQL is restricted to the required application resources.

The database is kept in private networking and is not intended to be directly accessible from the public internet.

Database credentials are provided to the Kubernetes application using Kubernetes Secrets rather than being stored directly in the application source code.

# Part 2: Deployment Automation

## Jenkins CI Pipeline

Jenkins is used to automate the Continuous Integration workflow.

The Jenkins pipeline contains the following stages:

1. Checkout
2. Build
3. Build Docker Image
4. Trivy Scan
5. Push Docker Image
6. Update Kubernetes Manifests

## Source Code Checkout

Jenkins checks out the application source code from the main branch of GitHub.

Repository:

```text
https://github.com/hruthingali/8byte_project.git
```

GitHub credentials are stored securely in Jenkins Credentials.

## Maven Build

The application is built using Maven.

The current Jenkins pipeline executes:

```bash
mvn clean install -DskipTests
```

Tests are skipped during the current Docker deployment build.

## Docker Image Build

Jenkins builds the Docker image using the Jenkins build number as the image tag.

For example:

```text
hruthingali/expensetracker:15
```

Using the Jenkins build number makes each Docker image identifiable by the Jenkins build that produced it.

## Trivy Vulnerability Scan

Trivy is used to scan the Docker image for known vulnerabilities.

The pipeline checks for HIGH and CRITICAL severity vulnerabilities.

The pipeline is configured to fail when vulnerabilities matching the configured criteria are detected.

The scan is performed before the image is pushed to Docker Hub.

The command used by the pipeline is:

```bash
trivy image \
    --severity HIGH,CRITICAL \
    --exit-code 1 \
    --no-progress \
    ${DOCKER_IMAGE}:${BUILD_NUMBER}
```

This provides a security check before the container image is published.

## Docker Hub

After the Trivy scan succeeds, Jenkins pushes the Docker image to Docker Hub.

The Docker Hub repository is:

```text
hruthingali/expensetracker
```

Images are tagged using the Jenkins build number.

For example:

```text
hruthingali/expensetracker:20
```

Docker Hub credentials are stored securely in Jenkins Credentials.

# Kubernetes Manifest Update

After successfully pushing the Docker image, Jenkins updates the image tag in the Kubernetes deployment manifests.

The manifests are maintained separately for staging and production:

```text
k8s/

    staging/
        deployment.yaml

    production/
        deployment.yaml
```

Jenkins replaces the previous image tag with the current Jenkins build number.

The updated Kubernetes manifests are committed and pushed back to GitHub.

This allows GitHub to act as the source of truth for the desired Kubernetes state.

# Argo CD

Argo CD is used for GitOps-based Continuous Deployment.

Argo CD monitors the Kubernetes manifests stored in GitHub.

When Jenkins updates the Kubernetes manifests and pushes the changes to GitHub, Argo CD detects the difference between the desired state in Git and the state running in Kubernetes.

Separate Argo CD applications are configured for staging and production.

# Staging Deployment

The staging Argo CD application has automatic synchronization enabled.

When Jenkins updates the staging Kubernetes deployment manifest and pushes the change to GitHub, Argo CD detects the change and automatically synchronizes the staging environment.

The staging deployment flow is:

```text
Jenkins
    |
    v
Update staging deployment.yaml
    |
    v
GitHub
    |
    v
Argo CD
    |
    v
Automatic Sync
    |
    v
EKS Staging
```

This allows new builds to be automatically deployed to staging.

# Production Deployment

Production is intentionally configured differently from staging.

Automatic synchronization is disabled for the production Argo CD application.

Jenkins updates the production Kubernetes deployment manifest and pushes the change to GitHub, but Argo CD does not automatically deploy the change.

After staging has been verified, the production application can be manually synchronized from the Argo CD interface.

The production deployment flow is:

```text
Jenkins
    |
    v
Update production deployment.yaml
    |
    v
GitHub
    |
    v
Argo CD
    |
    v
Manual Sync
    |
    v
EKS Production
```

This provides an additional control before changes are deployed to production.

A Jenkins manual approval stage was not implemented. Production deployment is controlled through the manual synchronization mechanism in Argo CD.

# Part 3: Monitoring and Logging

## Amazon CloudWatch

Amazon CloudWatch is used for monitoring and logging of the EKS environment.

The Amazon CloudWatch Observability Controller was installed and configured inside the EKS cluster.

The controller manages the CloudWatch observability components required to collect metrics and logs from the Kubernetes environment.

The monitoring architecture is:

```text
Amazon EKS
    |
    v
CloudWatch Observability Controller
    |
    v
CloudWatch Agent
    |
    v
Amazon CloudWatch
```

The CloudWatch components were verified using:

```bash
kubectl get pods -n amazon-cloudwatch
```

The CloudWatch Agent was running successfully inside the EKS cluster.

## Monitoring Metrics

The CloudWatch observability setup provides visibility into the EKS environment and container workloads.

The monitoring data can be used to observe:

* CPU utilization
* Memory utilization
* Container metrics
* Kubernetes workloads
* Node-level metrics
* Application and container logs

Amazon RDS PostgreSQL also provides database monitoring through CloudWatch.

Important database metrics include:

* CPU utilization
* Database connections
* Storage usage
* Read operations
* Write operations
* Network activity

# CloudWatch Dashboards

Two CloudWatch dashboards were configured to provide visibility into the application environment.

## Dashboard 1: EKS Infrastructure Monitoring

The first dashboard focuses on the EKS infrastructure and Kubernetes environment.

It provides visibility into resource utilization and the health of the Kubernetes workloads.

The dashboard includes monitoring for areas such as:

* CPU utilization
* Memory utilization
* Container resource usage
* Kubernetes workload metrics
* Node-level metrics
* EKS infrastructure health

This dashboard helps identify resource utilization problems and infrastructure-related issues.

## Dashboard 2: Application and Database Monitoring

The second dashboard focuses on application and database health.

It provides visibility into application/container activity and the RDS PostgreSQL database.

The dashboard includes monitoring for areas such as:

* Application and container logs
* Application resource usage
* RDS CPU utilization
* RDS database connections
* RDS storage usage
* RDS read activity
* RDS write activity

This dashboard helps identify application and database-related issues.

## Logging

Application and container logs are collected through the CloudWatch observability setup.

Centralized logging makes it possible to investigate issues without manually checking every Kubernetes Pod.

The logs can be used to troubleshoot:

* Application errors
* Pod failures
* Container failures
* Deployment issues
* Runtime issues

# Monitoring Flow

The complete monitoring flow is:

```text
Application
    |
    v
Kubernetes Pod
    |
    v
CloudWatch Observability Controller
    |
    v
CloudWatch Agent
    |
    v
Amazon CloudWatch
    |
    +---- Metrics
    |
    +---- Logs
    |
    +---- Dashboard 1
    |
    +---- Dashboard 2
```

# Part 4: Documentation and Best Practices

## Security Considerations

Security was considered across the infrastructure, CI/CD pipeline, container, Kubernetes environment, and database.

Security Groups are used to control network access.

The RDS database is kept in private networking.

Docker images are scanned using Trivy before being pushed to Docker Hub.

GitHub credentials are stored securely in Jenkins Credentials.

Docker Hub credentials are stored securely in Jenkins Credentials.

Database credentials are stored using Kubernetes Secrets.

Sensitive credentials are not committed to GitHub.

Production Argo CD automatic synchronization is disabled to provide manual control over production deployments.

## Secret Management

Jenkins Credentials are used to securely store:

* GitHub credentials
* Docker Hub credentials

The Jenkins pipeline accesses these credentials only when required.

Database credentials are provided to the Kubernetes application using Kubernetes Secrets.

Sensitive credentials are not stored directly in the application source code.

For a larger production environment, AWS Secrets Manager could be used for centralized secret management.

## Backup Strategy

The application database is hosted on Amazon RDS PostgreSQL.

RDS provides automated backup capabilities.

For a production environment, an appropriate backup retention period should be configured based on the application's recovery requirements.

Database restoration should also be tested periodically to verify that backups can be successfully used during a recovery scenario.

## Cost Optimization

The project was implemented as a technical assignment, so unnecessary AWS resource usage should be avoided.

The following cost optimization practices were considered:

* Use appropriate resource sizes.
* Avoid running unused EKS resources.
* Delete unused Load Balancers.
* Delete unused NAT Gateways when the environment is no longer required.
* Monitor RDS usage.
* Monitor CloudWatch log usage.
* Remove temporary resources after testing.
* Destroy assignment infrastructure after completion when it is no longer required.

Resources such as EKS, RDS, NAT Gateway, Load Balancers, and CloudWatch can generate charges even when the application is not actively being used.

# Challenges Faced and Resolutions

## Terraform Destroy Dependency Issues

During infrastructure cleanup, Terraform was unable to delete the VPC because dependencies still existed.

Terraform reported dependency violations while attempting to delete subnets, the Internet Gateway, and the VPC.

AWS CLI was used to identify the resources that were still associated with the VPC.

## Kubernetes Load Balancers

The Kubernetes environment had created AWS Load Balancers.

These Load Balancers created Elastic Network Interfaces inside the VPC.

The network interfaces prevented the associated subnets from being deleted.

The Load Balancers were identified using AWS CLI.

After confirming that they were no longer required, the Load Balancers were deleted.

The associated network interfaces were then removed.

## Kubernetes Security Groups

Kubernetes-created security groups were also present in the VPC.

They were identified using Kubernetes-related tags such as:

```text
kubernetes.io/cluster/ekscluster
```

After confirming that the Load Balancers had been removed and the security groups were no longer required, the unused security groups were deleted.

## NAT Gateway

The NAT Gateway was checked during the cleanup process.

Its state was verified using the AWS CLI.

The NAT Gateway was confirmed to be in the deleted state before continuing with the remaining VPC cleanup.

## Terraform State and External Resources

Another challenge was that not all AWS resources visible in the VPC were managed by the current Terraform state.

The Terraform state contained:

```text
aws_vpc.main
```

while some Kubernetes-created resources, such as Load Balancers and their security groups, were outside the Terraform state.

This resulted in dependencies during Terraform destroy.

AWS CLI was used to identify these resources and remove resources that were no longer required.

This highlighted the importance of clearly defining resource ownership and maintaining consistent Terraform state management.

# Verification Commands

## Check EKS Cluster

```bash
aws eks list-clusters --region us-east-1
```

## Configure kubectl

```bash
aws eks update-kubeconfig \
    --region us-east-1 \
    --name ekscluster
```

## Check Kubernetes Nodes

```bash
kubectl get nodes
```

## Check Kubernetes Pods

```bash
kubectl get pods -A
```

## Check Kubernetes Services

```bash
kubectl get svc -A
```

## Check Argo CD

```bash
kubectl get pods -n argocd
```

```bash
kubectl get applications -n argocd
```

## Check CloudWatch Components

```bash
kubectl get pods -n amazon-cloudwatch
```

## Check Application Logs

```bash
kubectl logs <pod-name>
```

# Project Structure

The application repository contains the following major components:

```text
8byte_project/

    src/

    pom.xml

    Dockerfile

    Jenkinsfile

    k8s/
        staging/
            deployment.yaml
        production/
            deployment.yaml

    argocd/

    README.md
```

The Terraform infrastructure is maintained separately:

```text
8byte_terraform/

    main.tf
    variables.tf
    outputs.tf
    providers.tf
    terraform.tfvars
```

# Assignment Requirement Coverage

| Assignment Requirement               | Implementation                             |
| ------------------------------------ | ------------------------------------------ |
| VPC                                  | Implemented using Terraform                |
| Public and private subnets           | Implemented                                |
| Application hosting                  | Amazon EKS                                 |
| PostgreSQL database                  | Amazon RDS PostgreSQL                      |
| Security Groups                      | Implemented                                |
| Load Balancer                        | Implemented                                |
| variables.tf                         | Implemented                                |
| Terraform state management           | Implemented                                |
| Terraform outputs                    | Implemented                                |
| CI/CD pipeline                       | Jenkins                                    |
| Docker image build                   | Implemented                                |
| Container registry                   | Docker Hub                                 |
| Container vulnerability scanning     | Trivy                                      |
| Staging deployment                   | Argo CD                                    |
| Staging automatic synchronization    | Enabled                                    |
| Production deployment                | Argo CD                                    |
| Production automatic synchronization | Disabled                                   |
| Production deployment control        | Manual Argo CD synchronization             |
| Infrastructure monitoring            | Amazon CloudWatch                          |
| EKS monitoring                       | CloudWatch Observability Controller        |
| Centralized logging                  | Amazon CloudWatch                          |
| Database monitoring                  | Amazon RDS and CloudWatch                  |
| CloudWatch dashboards                | Two dashboards                             |
| Secret management                    | Jenkins Credentials and Kubernetes Secrets |
| Backup strategy                      | Amazon RDS                                 |
| Cost optimization                    | Documented                                 |
| Challenges and resolutions           | Documented                                 |

# Limitations and Future Improvements

The following improvements could be added to make the solution more suitable for a production environment:

* Add unit tests to the Jenkins pipeline.
* Add integration tests.
* Configure Jenkins to trigger automatically for Pull Requests.
* Add a manual approval stage in Jenkins before production deployment.
* Configure Slack or email notifications for pipeline failures.
* Add custom application metrics for request rate, error rate, and latency.
* Configure CloudWatch alarms for important infrastructure and application metrics.
* Improve CloudWatch dashboards with additional application-specific metrics.
* Use AWS Secrets Manager for production secrets.
* Enable HTTPS using an SSL/TLS certificate.
* Configure Kubernetes Horizontal Pod Autoscaling.
* Implement a complete disaster recovery strategy.
* Use separate AWS accounts for staging and production.
* Add stricter security policies for production deployments.

# Conclusion

This project demonstrates an end-to-end DevOps implementation for an Expense Tracker application.

Terraform is used to provision the AWS infrastructure, including the VPC, networking components, security groups, and supporting AWS resources.

Jenkins automates the application build, Docker image creation, vulnerability scanning, Docker image publishing, and Kubernetes manifest updates.

Docker is used for application containerization and Docker Hub is used as the container registry.

Amazon EKS provides the Kubernetes platform for running the application.

Argo CD provides GitOps-based Continuous Deployment. Staging is configured with automatic synchronization, while production automatic synchronization is disabled. Production deployment is manually synchronized through Argo CD after staging verification.

Amazon RDS PostgreSQL provides persistent database storage for the application.

Amazon CloudWatch Observability Controller is configured inside the EKS cluster to provide monitoring and logging capabilities through Amazon CloudWatch.

Two CloudWatch dashboards provide visibility into the EKS infrastructure and application/database environment.

The project also involved troubleshooting real AWS and Kubernetes resource dependency issues during infrastructure cleanup, including Load Balancers, Network Interfaces, Security Groups, NAT Gateway resources, and Terraform state.

Overall, the project demonstrates practical experience with Infrastructure as Code, CI/CD, Docker, Kubernetes, AWS, GitOps, container security, database integration, monitoring, logging, and troubleshooting.
