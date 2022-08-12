def gv

pipeline {
  agent any
  tools {
    maven 'maven3.8' 
  }

  stages {
    stage("init"){
      steps {
        script {
          gv = load "script.grovy"
        }
      }
    }
    stage("build jar") {
      steps {
        script {
          gv.buildJar()
        }
      }
    }

    stage("build image") {
      when {
        expression {
          BRANCH_NAME == 'master'
        }
      }
      steps {
        script {
          gv.buildImage()
        }
      
      }
    }
   
  }
}