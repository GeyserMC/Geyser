pipeline {
    agent any
    tools {
        maven 'Maven 3'
        jdk 'Java 8'
    }
    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '5'))
    }
    stages {
        stage ('Build') {
            steps {
                sh 'mvn clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/Geyser.jar', fingerprint: true
                }
            }
        }

        stage ('Deploy') {
            when {
                branch "master"
            }
            steps {
                sh 'mvn javadoc:jar source:jar deploy -DskipTests'
            }
        }

        stage ('Javadoc') {
            when {
                branch "master"
            }
            steps {
                sh 'mvn javadoc:javadoc -DskipTests -pl api'
                step([$class: 'JavadocArchiver',
                        javadocDir: 'api/target/site/apidocs',
                        keepAll: false])
            }
        }
    }

    post {
        always {
            deleteDir()
        }
    }
}