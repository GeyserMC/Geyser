pipeline {
    agent any
    tools {
        gradle 'Gradle 6'
        jdk 'Java 8'
    }
    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '20'))
    }
    stages {
        stage ('Build') {
            steps {
                sh 'gradle clean build --refresh-dependencies'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'build/libs/*.jar', excludes: 'build/libs/Geyser-Fabric-*-sources*.jar,build/libs/Geyser-Fabric-*-all*.jar', fingerprint: true
                }
            }
        }

        stage ('Deploy') {
                    when {
                        branch "master"
                    }

                    steps {
                        sh 'mvn javadoc:jar source:jar deploy -DskipTests'
                        rtGradleDeployer(
                                id: "GRADLE_DEPLOYER",
                                serverId: "opencollab-artifactory",
                                releaseRepo: "maven-releases",
                                snapshotRepo: "maven-snapshots"
                        )
                        rtGradleResolver(
                                id: "GRADLE_RESOLVER",
                                serverId: "opencollab-artifactory",
                                releaseRepo: "release",
                                snapshotRepo: "snapshot"
                        )
                        rtGradleRun (
                                usesPlugin: true,
                                tool: GRADLE_TOOL,
                                rootDir: "/",
                                buildFile: 'build.gradle',
                                tasks: 'clean artifactoryPublish',
                                deployerId: "GRADLE_DEPLOYER",
                                resolverId: "GRADLE_RESOLVER"
                        )
                        rtPublishBuildInfo(
                                serverId: "opencollab-artifactory"
                        )
                    }
                }
    }

    post {
        always {
            script {
                def changeLogSets = currentBuild.changeSets
                def message = "**Changes:**"

                if (changeLogSets.size() == 0) {
                    message += "\n*No changes.*"
                } else {
                    def repositoryUrl = scm.userRemoteConfigs[0].url.replace(".git", "")
                    def count = 0;
                    def extra = 0;
                    for (int i = 0; i < changeLogSets.size(); i++) {
                        def entries = changeLogSets[i].items
                        for (int j = 0; j < entries.length; j++) {
                            if (count <= 10) {
                                def entry = entries[j]
                                def commitId = entry.commitId.substring(0, 6)
                                message += "\n   - [`${commitId}`](${repositoryUrl}/commit/${entry.commitId}) ${entry.msg}"
                                count++
                            } else {
                                extra++;
                            }
                        }
                    }

                    if (extra != 0) {
                        message += "\n   - ${extra} more commits"
                    }
                }

                env.changes = message
            }
            deleteDir()
            withCredentials([string(credentialsId: 'geyser-discord-webhook', variable: 'DISCORD_WEBHOOK')]) {
                discordSend description: "**Build:** [${currentBuild.id}](${env.BUILD_URL})\n**Status:** [${currentBuild.currentResult}](${env.BUILD_URL})\n${changes}\n\n[**Artifacts on Jenkins**](https://ci.nukkitx.com/job/Geyser)", footer: 'Cloudburst Jenkins', link: env.BUILD_URL, successful: currentBuild.resultIsBetterOrEqualTo('SUCCESS'), title: "${env.JOB_NAME} #${currentBuild.id}", webhookURL: DISCORD_WEBHOOK
            }
        }
    }
}
