def buildJar(){
        echo "bulding the application"
        sh 'mvn package'
}


def buildImage(){
        echo "bulding the application"
        withCredentials([usernamePassword(credentialsId: 'docker-hub-repo', passwordVariable: 'PASS', usernameVariable: 'USER')]){
           sh 'docker build -t mahmudarif/welcome-app:jma-2.0 .'
           sh "echo $PASS | docker login -u $USER --password-stdin" 
           sh 'docker push mahmudarif/welcome-app:jma-2.0' 
        }
}


return this;