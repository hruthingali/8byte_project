# 8byte_project
<img width="1097" height="394" alt="image" src="https://github.com/user-attachments/assets/51cba3e1-1ae0-4c21-be9c-c000f3a3b6c2" />
# Expense Tracker DevOps Project

## Overview

This project was developed as part of the DevOps technical assignment for Octa Byte AI Pvt Ltd.

The objective of the project is to demonstrate an end-to-end DevOps workflow including infrastructure provisioning, application containerization, continuous integration, security scanning, Kubernetes deployment, GitOps-based deployment, database integration, monitoring, logging, and operational troubleshooting.

The application used for the assignment is an Expense Tracker application. The main focus of the project is the DevOps implementation rather than the application business logic.

The infrastructure is hosted on AWS and the application is deployed on Amazon EKS. PostgreSQL is used as the application database and is hosted on Amazon RDS.

## Architecture

The overall deployment flow is:

```text
Developer
    |
    v
GitHub
    |
    v
Jenkins
    |
    +----------------------+
    |                      |
    v                      v
Maven Build          Docker Build
                           |
                           v
                     Trivy Scan
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
                       /      \
                      /        \
                     v          v
                Staging      Production
                Auto Sync    Manual Sync
                     \          /
                      \        /
                       v      v
                      Amazon EKS
                           |
                           v
                    Expense Tracker
                           |
                           v
                  Amazon RDS PostgreSQL
```

CloudWatch monitoring is configured using the Amazon CloudWatch Observability Controller inside the EKS cluster.

## Technology Stack

The following technologies are used in the project:

| Technology            | Purpose                              |
| --------------------- | ------------------------------------ |
| AWS                   | Cloud infrastructure                 |
| Terraform             | Infrastructure as Code               |
| Amazon VPC            | Network isolation                    |
| Amazon EKS            | Kubernetes cluster                   |
| Amazon RDS PostgreSQL | Persistent database                  |
| Docker                | Application containerization         |
| Docker Hub            | Container image registry             |
| Jenkins               | Continuous Integration               |
| Maven                 | Application build                    |
| Trivy                 | Container vulnerability scanning     |
| Kubernetes            | Container orchestration              |
| Argo CD               | GitOps-based Continuous Deployment   |
| Amazon CloudWatch     | Monitoring and logging               |
| GitHub                | Source code and Kubernetes manifests |

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

Public subnets are used for internet-facing resources, while private subnets are used for internal workloads and database resources.

The Internet Gateway provides internet connectivity for resources in public subnets.

The NAT Gateway provides outbound internet connectivity for resources in private subnets without exposing them directly to the internet.

## Network Architecture

The VPC uses the CIDR range:

```text
10.0.0.0/16
```

The network is divided into public and private subnets across Availability Zones.

The general network structure is:

```text
AWS VPC
|
+-- Public Subnet
|     |
|     +-- Load Balancer
|     |
|     +-- Internet Gateway
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

The database is kept in private networking so that it is not directly exposed to the public internet.

## Infrastructure as Code

Terraform is used to provision the AWS infrastructure.

The Terraform configuration uses variables so that infrastructure values can be changed without modifying the main resource definitions.

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

Terraform outputs are used to expose important infrastructure information such as VPC IDs, subnet IDs, and other resource information.

## Terraform State Management

Terraform state is used to maintain information about the infrastructure managed by Terraform.

For a shared or production environment, Terraform state can be stored remotely using Amazon S3 with state locking.

Terraform state files should not be committed to the GitHub repository because they can contain sensitive infrastructure information.

During the project, an important troubleshooting issue was encountered where some AWS resources existed in the environment but were not present in the current Terraform state.

This was handled by identifying the resources using AWS CLI and cleaning up the dependent resources before deleting the infrastructure.

## Application

The application used for the assignment is an Expense Tracker application.

The application allows users to manage expense-related information.

The application uses PostgreSQL for persistent data storage.

The application is packaged into a Docker container and deployed to Amazon EKS.

The application does not depend on local container storage for persistent database information.

## Docker

Docker is used to package the application and its dependencies into a container image.

The Docker image is built by Jenkins.

The Docker Hub repository used for the project is:

```text
hruthingali/expensetracker
```

The Jenkins pipeline creates a unique image tag using the Jenkins build number.

For example:

```text
hruthingali/expensetracker:15
```

Using the Jenkins build number makes it possible to identify the exact image associated with a Jenkins build.

## Jenkins CI Pipeline

Jenkins is used to automate the Continuous Integration process.

The Jenkins pipeline contains the following stages:

1. Checkout
2. Build
3. Build Docker Image
4. Trivy Scan
5. Push Docker Image
6. Update Kubernetes Manifests

## Checkout

Jenkins checks out the application source code from the main branch of GitHub.

The repository used for the project is:

```text
https://github.com/hruthingali/8byte_project.git
```

GitHub credentials are stored securely in Jenkins Credentials and are not hardcoded as plain passwords in the pipeline.

## Application Build

The application is built using Maven.

The current pipeline uses:

```bash
mvn clean install -DskipTests
```

Tests are skipped during the current Docker deployment build.

## Docker Image Build

Jenkins builds the Docker image using the Jenkins build number.

The command used is equivalent to:

```bash
docker build -t hruthingali/expensetracker:${BUILD_NUMBER} .
```

This creates a uniquely tagged image for every Jenkins build.

## Trivy Security Scan

Trivy is used to scan the Docker image for known vulnerabilities.

The pipeline checks for HIGH and CRITICAL vulnerabilities.

The scan is configured to fail the pipeline when vulnerabilities matching the configured severity are detected.

The command used is:

```bash
trivy image \
    --severity HIGH,CRITICAL \
    --exit-code 1 \
    --no-progress \
    hruthingali/expensetracker:${BUILD_NUMBER}
```

This provides a security check before the image is pushed to Docker Hub.

## Push Docker Image

After a successful Trivy scan, Jenkins authenticates with Docker Hub using credentials stored in Jenkins.

The image is then pushed to Docker Hub.

The image format is:

```text
hruthingali/expensetracker:<BUILD_NUMBER>
```

Docker Hub credentials are not stored directly inside the Jenkinsfile.

## Update Kubernetes Manifests

After the Docker image is pushed, Jenkins updates the Docker image tag in both the staging and production Kubernetes deployment files.

The files are:

```text
k8s/staging/deployment.yaml

k8s/production/deployment.yaml
```

Jenkins replaces the previous image tag with the current Jenkins build number.

The updated Kubernetes manifests are committed and pushed back to the main branch of GitHub.

This allows GitHub to contain the desired state of the Kubernetes deployments.

## CI/CD Flow

The implemented CI/CD workflow is:

```text
Developer
    |
    v
GitHub
    |
    v
Jenkins
    |
    +-- Checkout
    |
    +-- Maven Build
    |
    +-- Docker Build
    |
    +-- Trivy Scan
    |
    +-- Push Docker Image
    |
    +-- Update Kubernetes Manifests
    |
    v
GitHub
    |
    v
Argo CD
```

## GitOps with Argo CD

Argo CD is used for Continuous Deployment using the GitOps approach.

The Kubernetes manifests are stored in GitHub.

Jenkins updates the image tag in the Kubernetes manifests and pushes the changes to GitHub.

Argo CD monitors the repository and detects changes to the Kubernetes configuration.

The desired Kubernetes state is therefore maintained in Git.

## Staging Deployment

The staging Argo CD application is configured with automatic synchronization enabled.

When Jenkins updates the staging deployment manifest and pushes the change to GitHub, Argo CD detects the change and automatically synchronizes the staging application.

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

This allows new builds to be automatically deployed to the staging environment.

## Production Deployment

Production is intentionally configured differently from staging.

Automatic synchronization is disabled for the production Argo CD application.

Jenkins still updates the production Kubernetes manifest and pushes the change to GitHub.

However, Argo CD does not automatically deploy the change to production.

After the staging environment has been verified, the production application can be manually synchronized from Argo CD.

The production flow is:

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

This provides an additional control before a change is deployed to production.

A manual approval stage inside Jenkins was not implemented. Production deployment is controlled through the manual synchronization option in Argo CD.

## Amazon EKS

Amazon EKS is used as the Kubernetes platform for running the Expense Tracker application.

The EKS cluster hosts the application workloads.

The application is deployed using Kubernetes Deployments and Services.

Multiple replicas are used for the application to improve availability.

The cluster can be checked using:

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

Deployments can be checked using:

```bash
kubectl get deployments
```

## Kubernetes Deployment

The Kubernetes configuration is separated into staging and production environments.

The structure is:

```text
k8s/

    staging/
        deployment.yaml

    production/
        deployment.yaml
```

The Kubernetes deployment defines the application container and the number of replicas.

The Service provides network access to the application.

Kubernetes Secrets are used for sensitive application configuration.

## Amazon RDS PostgreSQL

PostgreSQL is used as the application's persistent database.

The database is hosted using Amazon RDS for PostgreSQL.

The application running inside EKS connects to the RDS PostgreSQL database.

The database stores the application's persistent expense data.

Using RDS instead of running PostgreSQL inside a Kubernetes Pod provides a managed database service and separates persistent database storage from the application containers.

## Database Security

The RDS database is protected using AWS Security Groups.

The database should only accept PostgreSQL traffic from the required application resources.

The database is kept in private networking and is not intended to be directly accessible from the public internet.

Database credentials are not stored directly in the application source code.

Sensitive database configuration is provided to Kubernetes using Secrets.

## Monitoring and Logging

Amazon CloudWatch is used for monitoring and logging of the EKS environment.

The Amazon CloudWatch Observability Controller was installed and configured inside the EKS cluster.

The controller manages the CloudWatch observability components required to collect telemetry from the Kubernetes environment.

The CloudWatch components were verified using:

```bash
kubectl get pods -n amazon-cloudwatch
```

The monitoring flow is:

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

The monitoring setup provides visibility into Kubernetes and container-level metrics and logs.

## CloudWatch Metrics

The CloudWatch setup provides visibility into the EKS environment.

The monitoring information can be used to observe:

* CPU utilization
* Memory utilization
* Container metrics
* Kubernetes workloads
* Application and container logs

This information can be used to troubleshoot application and infrastructure issues.

## CloudWatch Dashboards

CloudWatch provides a centralized location for viewing the collected EKS metrics and logs.

The CloudWatch Observability Controller is responsible for collecting and sending the required observability data from EKS to CloudWatch.

Additional dashboards and alarms can be configured based on the available metrics and the monitoring requirements of the application.

## Security

Security was considered across the infrastructure, CI/CD pipeline, container, Kubernetes environment, and database.

Security Groups are used to restrict network access.

The RDS database is kept in private networking.

Docker images are scanned using Trivy before being pushed to Docker Hub.

GitHub credentials are stored in Jenkins Credentials.

Docker Hub credentials are stored in Jenkins Credentials.

Database credentials are handled using Kubernetes Secrets.

Sensitive credentials are not committed to GitHub.

## Secret Management

Jenkins Credentials are used to securely store:

* GitHub credentials
* Docker Hub credentials

The credentials are accessed by Jenkins only when required by the pipeline.

Database credentials are provided to the Kubernetes application using Kubernetes Secrets.

Sensitive values should not be stored directly in the source code or committed to GitHub.

## Backup Strategy

The application database is hosted on Amazon RDS PostgreSQL.

RDS provides managed database capabilities and supports automated backup functionality.

For a production environment, an appropriate backup retention period should be configured according to the required recovery objectives.

Database restoration should also be tested periodically in a production environment.

## Cost Optimization

The project was implemented as an assignment environment, so cost management is important.

The following practices were considered:

* Use appropriate AWS resource sizes.
* Avoid running unused resources.
* Delete unused Load Balancers.
* Delete unused NAT Gateways.
* Remove temporary EKS resources after testing.
* Monitor RDS usage.
* Monitor CloudWatch log usage.
* Remove the environment after completing the assignment when it is no longer required.

AWS resources such as EKS, RDS, NAT Gateway, Load Balancers, and CloudWatch can generate charges even when they are not actively being used.

## Challenges Faced

### Terraform Resource Dependencies

During Terraform destroy, some resources could not be deleted because dependencies still existed inside the VPC.

Terraform initially reported dependency errors while attempting to delete the subnets and VPC.

AWS CLI was used to identify the remaining resources and determine what was preventing deletion.

### Kubernetes Load Balancers

Kubernetes-created Load Balancers created Elastic Network Interfaces inside the VPC.

These network interfaces prevented the associated subnets from being deleted.

The Load Balancers were identified using AWS CLI.

After confirming that they were no longer required, they were deleted.

The associated network interfaces were then removed automatically.

### Kubernetes Security Groups

Security groups created by Kubernetes for the Load Balancers also remained in the VPC.

They were identified using Kubernetes-related tags such as:

```text
kubernetes.io/cluster/ekscluster
```

After confirming that the Load Balancers had been removed and the security groups were no longer required, the unused security groups were deleted.

### NAT Gateway

The NAT Gateway was also checked during the cleanup process.

Its state was verified using:

```bash
aws ec2 describe-nat-gateways \
    --region us-east-1 \
    --filter Name=vpc-id,Values=<VPC_ID>
```

The NAT Gateway was confirmed to be in the deleted state.

### Terraform State and Manually Created Resources

Another important issue was that some AWS resources existed in the environment but were not present in the current Terraform state.

For example, the Terraform state contained:

```text
aws_vpc.main
```

while some Kubernetes-created AWS resources such as Load Balancers and their security groups were outside the Terraform state.

This caused dependencies during Terraform destroy.

The AWS CLI was used to identify and remove resources that were no longer required.

This highlighted the importance of keeping Terraform state synchronized with the resources that are intended to be managed by Terraform.

## Verification Commands

### Check AWS EKS Clusters

```bash
aws eks list-clusters --region us-east-1
```

### Configure kubectl

```bash
aws eks update-kubeconfig \
    --region us-east-1 \
    --name ekscluster
```

### Check Kubernetes Nodes

```bash
kubectl get nodes
```

### Check Kubernetes Pods

```bash
kubectl get pods -A
```

### Check Kubernetes Services

```bash
kubectl get svc -A
```

### Check Argo CD Pods

```bash
kubectl get pods -n argocd
```

### Check Argo CD Applications

```bash
kubectl get applications -n argocd
```

### Check CloudWatch Components

```bash
kubectl get pods -n amazon-cloudwatch
```

### Check Application Logs

```bash
kubectl logs <pod-name>
```

## Project Structure

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

## Assignment Requirement Coverage

| Assignment Requirement               | Implementation                             |
| ------------------------------------ | ------------------------------------------ |
| VPC with public and private subnets  | Implemented using Terraform                |
| EC2, ECS or EKS                      | Amazon EKS                                 |
| RDS PostgreSQL                       | Amazon RDS PostgreSQL                      |
| Security Groups                      | AWS Security Groups                        |
| Load Balancer                        | AWS/Kubernetes Load Balancer               |
| variables.tf                         | Implemented                                |
| Terraform state management           | Terraform state                            |
| Terraform outputs                    | Implemented                                |
| CI/CD pipeline                       | Jenkins                                    |
| Tests on PR creation                 | Not implemented                            |
| Unit and integration tests           | Not implemented in current pipeline        |
| Docker image build                   | Implemented                                |
| Container registry                   | Docker Hub                                 |
| Container vulnerability scanning     | Trivy                                      |
| Staging deployment                   | Argo CD                                    |
| Staging automatic synchronization    | Enabled                                    |
| Production deployment                | Argo CD                                    |
| Production automatic synchronization | Disabled                                   |
| Production deployment control        | Manual Argo CD synchronization             |
| Jenkins production approval stage    | Not implemented                            |
| Slack/email notifications            | Not implemented                            |
| Infrastructure monitoring            | Amazon CloudWatch                          |
| EKS monitoring                       | CloudWatch Observability Controller        |
| Container monitoring                 | CloudWatch                                 |
| Application/container logs           | Amazon CloudWatch                          |
| Database                             | Amazon RDS PostgreSQL                      |
| Secret management                    | Jenkins Credentials and Kubernetes Secrets |
| Backup strategy                      | Amazon RDS                                 |
| Cost optimization                    | Implemented and documented                 |
| Challenges and resolutions           | Documented                                 |

## Future Improvements

The following improvements could be added to make the solution more suitable for a production environment:

* Add unit tests to the Jenkins pipeline.
* Add integration tests to the CI pipeline.
* Configure Jenkins to run automatically for Pull Requests.
* Add a manual Jenkins approval stage before production deployment.
* Add Slack or email notifications for pipeline failures.
* Configure dedicated CloudWatch dashboards for infrastructure and application monitoring.
* Configure CloudWatch alarms for important metrics.
* Add application-level metrics for request rate, error rate, and latency.
* Use AWS Secrets Manager for production database credentials.
* Enable HTTPS using an SSL/TLS certificate.
* Configure Kubernetes Horizontal Pod Autoscaling.
* Implement a complete disaster recovery strategy.
* Use separate AWS accounts for staging and production environments.
* Implement stricter vulnerability policies for production deployments.

## Conclusion

This project demonstrates an end-to-end DevOps workflow for an Expense Tracker application.

Terraform is used to provision the AWS infrastructure.

Jenkins automates the application build, Docker image creation, vulnerability scanning, image publishing, and Kubernetes manifest updates.

Docker is used for application containerization and Docker Hub is used as the container registry.

Argo CD provides GitOps-based Continuous Deployment to Amazon EKS.

Staging is configured with automatic Argo CD synchronization, while production automatic synchronization is disabled. Production changes are manually synchronized through Argo CD after staging has been verified.

Amazon RDS PostgreSQL provides persistent database storage for the application.

Amazon CloudWatch Observability Controller is configured inside EKS to provide monitoring and logging for the Kubernetes environment.

The project also involved troubleshooting real AWS and Kubernetes dependency issues, including Load Balancers, Network Interfaces, Security Groups, NAT Gateway cleanup, and Terraform state management.

Overall, the project demonstrates practical experience with Infrastructure as Code, CI/CD, Docker, Kubernetes, AWS EKS, GitOps, database integration, container security, monitoring, and troubleshooting.

**screenshots :**
<img width="1477" height="735" alt="Screenshot 2026-08-30 105933" src="https://github.com/user-attachments/assets/633d5d9d-6d4f-4be5-a5e5-24f4352f4689" />
<img width="1898" height="942" alt="Screenshot 2026-08-30 132626" src="https://github.com/user-attachments/assets/86d129c1-da63-4333-8b23-a07630cc30cd" />



