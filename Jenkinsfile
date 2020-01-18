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
                sh 'git submodule update --init --recursive'
                sh 'mvn clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar, bootstrap/bukkit/target/*.jar, bootstrap/bungeecord/target/*.jar, bootstrap/sponge/target/*.jar, bootstrap/standalone/target/*.jar', fingerprint: true
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
            withCredentials([string(credentialsId: 'geyser-discord-webhook', variable: 'DISCORD_WEBHOOK')]) {
                discordSend description: "**Build:** [${currentBuild.id}](${env.BUILD_URL})\n**Status:** [${currentBuild.currentResult}](${env.BUILD_URL})\n\n[**Artifacts on Jenkins**](https://ci.nukkitx.com/job/Geyser)", footer: 'NukkitX Jenkins', link: env.BUILD_URL, successful: currentBuild.resultIsBetterOrEqualTo('SUCCESS'), title: "${env.JOB_NAME} #${currentBuild.id}", webhookURL: DISCORD_WEBHOOK
            }
        }
    }
}
