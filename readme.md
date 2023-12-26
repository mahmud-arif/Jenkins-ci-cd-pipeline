# Jenkins CI/CD Pipeline for Java Maven App

This Jenkins pipeline script automates the build, test, and deployment process for a Java Maven application. The pipeline is defined using the Groovy-based Jenkins Pipeline DSL.

## Overview

<img src="ci-cd.png" alt="Pipeline Illustration" width="50%"/>

## Jenkins Pipeline Stages

The pipeline consists of the following stages:

1. **Check Commit Author**: Checks the author of the latest commit and aborts the build if the author is in the exclusion list.

2. **Increment Version**: Increments the application version using Maven build-helper plugin and sets the new version in the project's POM file.

3. **Build App**: Cleans and packages the application using Maven.

4. **Build Image**: Builds a Docker image for the application, tags it with the version, and pushes it to Docker Hub.

5. **Deploy**: Deploys the Docker image to a EKS cluster using environment-specific deployment and service configurations.

6. **Commit Version Update**: Commits the updated version to the Git repository, triggering a new build.

## Prerequisites

- Jenkins environment configured with necessary plugins (Maven, Docker, Kubectl, aws-cli, gettext-base, etc.).
- Docker Hub credentials configured in Jenkins (`docker-hub-repo`).
- AWS User with credentials for deployment (`jenkins_aws_access_key_id`, `jenkins_aws_secret_access_key`),  (`kube-config`) file necessary  role and role-binding for that user in EKS cluster
- 
- GitHub credentials for version update commit (`github-credentials`).

## Pipeline Script

The pipeline script is written in Groovy and is located in the `Jenkinsfile`. The script defines stages for each step of the build and deployment process.

## Usage

1. Copy the contents of this repository, including the Jenkinsfile and other necessary configuration files.

2. Configure Jenkins to use this pipeline script by creating a new pipeline job and pointing it to the Jenkinsfile.

3. Configure Jenkins credentials for Docker Hub, AWS, and GitHub as mentioned in the Prerequisites section.

4. Trigger the pipeline manually or configure it to trigger automatically on each commit.


```groovy
#!/usr/bin/env groovy

pipeline {
    agent any
    // tools {
    //     maven 'Maven'
    // }
    environment {
        EXCLUDED_AUTHORS = 'jenkins@example.com'
    }
    stages {
         stage('Check Commit Author') {
            steps {
                script {
                    def currentCommitAuthor = sh(script: 'git log -1 --pretty=format:%ae', returnStdout: true).trim()

                    if (EXCLUDED_AUTHORS.split(',').contains(currentCommitAuthor)) {
                        echo "Skipping build for commit author: ${currentCommitAuthor}"
                        currentBuild.result = 'ABORTED'
                        error("Build aborted for excluded commit author.")
                    } else {
                        echo "Proceeding with the build for commit author: ${currentCommitAuthor}"
                    }
                }
            }
        }
        stage('increment version') {
            steps {
                script {
                    echo 'incrementing app version...'
                    sh 'mvn build-helper:parse-version versions:set \
                        -DnewVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}.\\\${parsedVersion.nextIncrementalVersion} \
                        versions:commit'
                    def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
                    def version = matcher[0][1]
                    env.IMAGE_NAME = "$version-$BUILD_NUMBER"
                }
            }
        }
        stage('build app') {
            steps {
               script {
                   echo "building the application..."
                   sh 'mvn clean package'
               }
            }
        }
        stage('build image') {
            steps {
                script {
                    echo "building the docker image..."
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-repo', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                        sh "docker build -t mahmudarif/demo-app:${IMAGE_NAME} ."
                        sh "echo $PASS | docker login -u $USER --password-stdin"
                        sh "docker push mahmudarif/demo-app:${IMAGE_NAME}"
                    }
                }
            }
        }
        stage('deploy') {
            environment {
                AWS_ACCESS_KEY_ID = credentials('jenkins_aws_access_key_id')
                AWS_SECRET_ACCESS_KEY = credentials('jenkins_aws_secret_access_key')
                APP_NAME = 'java-maven-app'
            }
            steps {
                script {
                    echo 'deploying docker image...'
                    sh 'envsubst < kubernetes/deployment.yaml | kubectl apply -f -'
                    sh 'envsubst < kubernetes/service.yaml | kubectl apply -f -'
                }
            }
        }
        stage('commit version update') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'github-credentials', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                        sh 'git config user.email "jenkins@example.com"'
                        sh 'git config user.name "Jenkins"'
                        sh "git remote set-url origin https://${USER}:${PASS}@github.com/mahmud-arif/java-maven-app.git"
                        sh 'git add .'
                        sh 'git commit -m "ci: version bump"'
                        sh 'git push origin HEAD:master'
                    }
                }
            }
        }
    }
}
```

<img src="jenkins.png" alt="Pipeline Illustration" width="50%"/>


