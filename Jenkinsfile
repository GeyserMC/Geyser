pipeline {
    agent any
    tools {
        maven 'Maven 3'
        jdk 'Java 8'
    }
    stages {
        stage ('Build') {
            steps {
                sh 'git submodule update --init --recursive'
                sh 'mvn clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'bootstrap/**/target/*.jar', excludes: 'bootstrap/**/target/original-*.jar', fingerprint: true
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
    }

    post {
        always {
            deleteDir()
            script {
                def changeLogSets = currentBuild.changeSets
                def message = "**Changes:**"

                if (changeLogSets.size() == 0) {
                    message += "\n*No changes.*"
                } else {
                    def repositoryUrl = scm.userRemoteConfigs[0].url  
                    for (int i = 0; i < changeLogSets.size(); i++) {
                        def entries = changeLogSets[i].items
                        for (int j = 0; j < entries.length; j++) {
                            def entry = entries[j]
                            def commitId = entry.commitId.substring(0, 6)
                            message += "\n   - [`${commitId}`](${repositoryUrl}${entry.commitId}) ${entry.msg} - ${entry.author}"
                        }
                    }
                }

                env.changes = message
            }
            withCredentials([string(credentialsId: 'geyser-discord-webhook', variable: 'DISCORD_WEBHOOK')]) {
                discordSend description: "**Build:** [${currentBuild.id}](${env.BUILD_URL})\n**Status:** [${currentBuild.currentResult}](${env.BUILD_URL})\n${changes}\n\n[**Artifacts on Jenkins**](https://ci.nukkitx.com/job/Geyser)", footer: 'NukkitX Jenkins', link: env.BUILD_URL, successful: currentBuild.resultIsBetterOrEqualTo('SUCCESS'), title: "${env.JOB_NAME} #${currentBuild.id}", webhookURL: DISCORD_WEBHOOK
            }
        }
    }
}
