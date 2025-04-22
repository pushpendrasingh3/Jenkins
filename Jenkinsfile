@Library('github.com/pushpendrasingh3/jenkins@master') _

pipeline {
  agent any


  triggers {
    pollSCM('H/10 * * * *')
    cron('H 10 * * 1-5')
  }

  environment {
    ENV = 'dev'
    APP = 'www'
    VERSION = "$GIT_COMMIT"
    GITOPS_REPO = 'git@github.com/HariSekhon/app-k8s'
    CI = true

    GIT_USERNAME = 'pushpendrasingh3'
    GIT_EMAIL = 'pushpendra.singh3@tcs.com'
    
    GITHUB_SSH_KNOWN_HOSTS = credentials('github-ssh-known-hosts')

    GCP_SERVICEACCOUNT_KEY = credentials('gcp-serviceaccount-key')
    GOOGLE_APPLICATION_CREDENTIALS = "$WORKSPACE_TMP/.gcloud/application-credentials.json.$BUILD_TAG"
    GITHUB_TOKEN = credentials('github-token')


    CLOUDSDK_CORE_PROJECT = 'mycompany-dev'
    CLOUDSDK_COMPUTE_REGION = 'europe-west2'
    GCR_REGISTRY = 'eu.gcr.io'


  }

  stages {
    stage('Checkout') {
      steps {
        node('checkout') {  // Move node block here

          milestone(ordinal: null, label: "Milestone: Checkout")
          checkout(
            [
              $class: 'GitSCM',
              userRemoteConfigs: [
                [
                  url: 'https://github.com/HariSekhon/DevOps-Bash-tools',
                  credentialsId: '' // **Important:  Replace with your actual credential ID if needed!**
                ]
              ],
              //doGenerateSubmoduleConfigurations: false,
              //extensions: [],
              //submoduleCfg: [],
            ]
          )
        } // End node block
      }
    }
  }

  post {
    always {
      echo 'Always'
    }
    success {
      echo 'SUCCESS!'
    }
    fixed {
      echo "FIXED!"
      Notify()
    }
    failure {
      node('checkout') {
        echo 'Failure!'
        echo 'FAILURE!'
      }
    }
    unstable {
      echo 'UNSTABLE!'
    }
    changed {
      echo 'Pipeline state change! (success vs failure)'
    }
  }
}
