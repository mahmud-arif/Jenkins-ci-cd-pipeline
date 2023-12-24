# Java Maven App Jenkins Pipeline

This branch contains a Jenkins pipeline written in Groovy for a Java Maven application. The pipeline automates the process of version incrementation, building the application, creating a Docker image. Additionally, the pipeline includes a step for committing the version update back to the Git repository.

## Jenkins Pipeline Stages
<img src="pipeline-1.png" alt="Pipeline Illustration" width="50%"/>

Pipline starts when changes push to the branch jenkins-version-bump

### 1. Increment Version

- Increments the application version based on the Maven versioning schema.
- Parses the current version from the `pom.xml` file and increments the next incremental version.
- Commits the updated version to the Git repository.

### 2. Build App

- Cleans and packages the Java Maven application using the `mvn clean package` command.

### 3. Build Image

- Builds a Docker image for the application.
- Uses Docker Hub credentials for authentication.
- Tags the Docker image with the incremented version and pushes it to Docker Hub.

<!-- ### 4. Deploy

- Deploys the Docker image to an EC2 instance (Note: Implementation details need to be added). -->

### 5. Commit Version Update

- Commits the version update back to the Git repository.
- Configures Git user information for Jenkins.
- Uses GitHub credentials for authentication.

## Pipeline Execution Flow

1. **Increment Version:**
   - Increments the version in `pom.xml`.
   - Commits the version change to a dedicated branch (`jenkins-version-bump`) in the Git repository.

2. **Build App:**
   - Cleans and packages the Java Maven application.

3. **Build Image:**
   - Builds a Docker image with the incremented version.
   - Logs in to Docker Hub using provided credentials.
   - Pushes the Docker image to the Docker Hub repository.

4. **Deploy:**
   - Deploys the Docker image to an EC2 instance (Implementation details need to be added).

5. **Commit Version Update:**
   - Commits the version change back to the Git repository on a separate branch.

## Prerequisites

- Jenkins environment set up.
- Maven tool configured in Jenkins.
- Docker installed on the Jenkins agent.
- Docker Hub credentials configured in Jenkins.
- GitHub credentials configured in Jenkins.

**Note:** Ensure that necessary environment variables and configurations are set up in your Jenkins environment before running the pipeline.
```groovy
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
            steps {
                script {
                    echo 'deploying docker image to EC2...'
                }
            }
        }
        stage('commit version update') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'github-credentials', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                        // git config here for the first time run
                        sh 'git config --global user.email "jenkins@example.com"'
                        sh 'git config --global user.name "jenkins"'

                        sh "git remote set-url origin https://${USER}:${PASS}@github.com/mahmud-arif/java-maven-app.git"
                        sh 'git add .'
                        sh 'git commit -m "ci: version bump"'
                        sh 'git push origin HEAD:jenkins-version-bump'
                    }
                }
            }
        }
    }
}
```

<img src="pipleline-jenkins.png" alt="Pipeline Illustration" width="50%"/>
