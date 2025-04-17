@Library('github.com/pushpendrasingh3/jenkins@master') _

pipeline {
  agent any

  options {
    timestamps()
    timeout(time: 2, unit: 'HOURS')
    parallelsAlwaysFailFast()
    rateLimitBuilds(throttle: [count: 3, durationName: 'minute', userBoost: false])
    buildDiscarder(logRotator(numToKeepStr: '100'))
  }

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

    GIT_USERNAME = 'Jenkins'
    GIT_EMAIL = 'platform-engineering@MYCOMPANY.CO.UK'
    
    GITHUB_SSH_KNOWN_HOSTS = credentials('github-ssh-known-hosts')
    GITLAB_SSH_KNOWN_HOSTS = credentials('gitlab-ssh-known-hosts')
    AZURE_DEVOPS_SSH_KNOWN_HOSTS = credentials('azure-devops-ssh-known-hosts')
    BITBUCKET_SSH_KNOWN_HOSTS = credentials('bitbucket-ssh-known-hosts')
    SSH_KNOWN_HOSTS = """
      $GITHUB_SSH_KNOWN_HOSTS
      $GITLAB_SSH_KNOWN_HOSTS
      $AZURE_DEVOPS_SSH_KNOWN_HOSTS
      $BITBUCKET_SSH_KNOWN_HOSTS
    """

    AWS_ACCESS_KEY_ID = credentials('aws-secret-key-id')
    AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
    GCP_SERVICEACCOUNT_KEY = credentials('gcp-serviceaccount-key')
    GOOGLE_APPLICATION_CREDENTIALS = "$WORKSPACE_TMP/.gcloud/application-credentials.json.$BUILD_TAG"
    DIGITALOCEAN_ACCESS_TOKEN = credentials('digitalocean-access-token')
    GITHUB_TOKEN = credentials('github-token')
    AWS_ACCESS_KEY = credentials('aws')

    AWS_DEFAULT_REGION = 'eu-west-2'
    AWS_ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
    AWS_EKS_CLUSTER = 'mycluster'

    CLOUDSDK_CORE_PROJECT = 'mycompany-dev'
    CLOUDSDK_COMPUTE_REGION = 'europe-west2'
    GCR_REGISTRY = 'eu.gcr.io'

    CLOUDFLARE_API_KEY = credentials('cloudflare-api-key')
    DOCKER_BUILDKIT = 1
    DOCKER_IMAGE = "$AWS_ECR_REGISTRY/$APP"
    DOCKER_TAG = "$GIT_COMMIT"

    ARTIFACTORY_URL = 'http://x.x.x.x:8082/artifactory/'
    ARTIFACTORY_ACCESS_TOKEN = credentials('artifactory-access-token')

    ARGOCD_SERVER = 'argocd.domain.com'
    ARGOCD_AUTH_TOKEN = credentials('argocd-auth-token')

    TF_IN_AUTOMATION = 1
    HERMIT_ENV_VARS = sh(returnStdout: true, script: './bin/hermit env --raw').trim()

    THREAD_COUNT = 6

    SLACK_MESSAGE = "Pipeline <${env.JOB_DISPLAY_URL}|${env.JOB_NAME}> - <${env.RUN_DISPLAY_URL}|Build #${env.BUILD_NUMBER}>"
    
    SEMGREP_BASELINE_REF = "origin/${env.CHANGE_TARGET}"
    SEMGREP_TIMEOUT = "300"
  }

  stages {
    stage('Checkout') {
      steps {
        milestone(ordinal: null, label: "Milestone: Checkout")
        checkout([
          $class: 'GitSCM',
          userRemoteConfigs: [[
            url: 'https://github.com/pushpendrasingh3/DevOps-Bash-tools',
            credentialsId: '',
          ]],
          branches: [[name: '*/master']]
        ])
      }
    }

    stage('Git Merge') {
      when {
        beforeAgent true
        branch '*/staging'
      }
      steps {
        lock(resource: 'Git Merge Staging to Dev', inversePrecedence: true) {
          milestone(ordinal: null, label: "Milestone: Git Merge")
          timeout(time: 5, unit: 'MINUTES') {
            sshagent(credentials: ['jenkins-ssh-key-for-github']) {
              retry(2) {
                sh 'path/to/git_merge_branch.sh staging dev'
              }
            }
          }
        }
      }
    }

    stage('Setup') {
      steps {
        milestone(ordinal: null, label: "Milestone: Setup")
        
        container('jq') {
          sh "wget -qO- ifconfig.co/json | jq -r '.ip'"
          sh 'script_using_jq.sh'
        }

        script {
          currentBuild.displayName = "$BUILD_DISPLAY_NAME (${GIT_COMMIT.take(8)})"
          workspace = "$env.WORKSPACE"
          
          env.DOCKER_IMAGES = [
            "$APP-php",
            "$APP-nginx",
            "$APP-cache",
            "$APP-sql-proxy",
          ].collect{"$GCR_REGISTRY/$GCR_PROJECT/$it"}.join(',')

          env.DOCKER_IMAGES_TAGS = env.DOCKER_IMAGES.split(',').collect{"$it:$VERSION"}.join(',')
        }
        printEnv()
      }
    }

    stage('Auth') {
      steps {
        gcpActivateServiceAccount()
        printAuth()
      }
    }

    stage("Generate AWS_ACCOUNT_ID") {
      steps {
        script {
          env.AWS_ACCOUNT_ID = sh(script:'aws sts get-caller-identity | jq -r .Account', returnStdout: true).trim()
        }
      }
    }

    stage('Test') {
      steps {
        milestone(ordinal: null, label: "Milestone: Test")
        timeout(time: 60, unit: 'MINUTES') {
          sh 'make test'
        }
      }
    }

    stage('Run Tests in Parallel') {
      parallel {
        stage('Desktop Tests') {
          steps {
            sh "./mvnw test -DselenoidUrl='$SELENIUM_HUB_URL' -Dgroups=com.mydomain.category.interfaces.DesktopTests -DthreadCount='$THREAD_COUNT'"
          }
        }
        stage('Mobile Tests') {
          steps {
            sh "./mvnw test -DselenoidUrl='$SELENIUM_HUB_URL' -Dgroups=com.mydomain.category.interfaces.MobileTests -Dmobile=true -DthreadCount='$THREAD_COUNT'"
          }
        }
      }
    }

    stage('SonarQube Scan') {
      steps {
        withSonarQubeEnv(installationName: 'mysonar') {
          sh './mvnw clean org.sonarsource.scanner.maven:sonar-maven-plugin:3.9.1.2184:sonar'
        }
      }
    }

    stage('SonarQube Quality Gate') {
      steps {
        timeout(time: 2, unit: 'MINUTES') {
          waitForQualtityGate abortPipeline: true
        }
      }
    }

    stage('Build') {
      environment {
        DEBUG = '1'
      }
      steps {
        milestone(ordinal: null, label: "Milestone: Build")
        
        cloudBuild(timeout_minutes: 40)

        gcpCloudBuild(
          args: '--project="$GCR_PROJECT" --substitutions="_REGISTRY=$GCR_REGISTRY,_IMAGE_VERSION=$GIT_COMMIT,_GIT_BRANCH=${GIT_BRANCH##*/}"',
          timeoutMinutes: 90,
          skipIfDockerImagesExist: env.DOCKER_IMAGES.tokenize(',').collect { "$it:$VERSION" }
        )

        timeout(time: 60, unit: 'MINUTES') {
          sh 'make'
        }
      }
    }

    stage('Docker Build') {
      agent { label 'docker-builder' }
      steps {
        milestone(ordinal: null, label: "Milestone: Docker Build")
        timeout(time: 60, unit: 'MINUTES') {
          sh "docker build -t '$DOCKER_IMAGE':'$DOCKER_TAG' --build-arg=BUILDKIT_INLINE_CACHE=1 --cache-from '$DOCKER_IMAGE':'$DOCKER_TAG' ."
        }
      }
    }

    stage('Trivy') {
      steps {
        milestone(ordinal: null, label: "Milestone: Trivy")
        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
          trivy('fs . ')
          trivy("image --no-progress --timeout 20m --exit-code 1 $DOCKER_IMAGE:$DOCKER_TAG")
        }
      }
    }

    stage('Docker Push') {
      agent { label 'docker-builder' }
      steps {
        milestone(ordinal: null, label: "Milestone: Docker Push")
        timeout(time: 15, unit: 'MINUTES') {
          sh "docker push '$DOCKER_IMAGE':'$DOCKER_TAG'"
        }
      }
    }

    stage('Terraform Plan') {
      steps {
        terraformPlan()
      }
    }

    stage('Approval') {
      when {
        beforeAgent true
        beforeInput true
        branch '*/production'
      }
      steps {
        approval(
          submitter: "$DEPLOYERS",
          ok: 'Deploy',
          timeout: 2,
          timeoutUnits: 'HOURS'
        )
      }
    }

    stage('Terraform Apply') {
      steps {
        terraformApply()
      }
    }

    stage('ArgoCD Deploy') {
      steps {
        lock(resource: "ArgoCD Deploy - App: $APP, Environment: $ENVIRONMENT", inversePrecedence: true) {
          milestone(ordinal: null, label: "Milestone: ArgoCD Deploy")

          container('git-kustomize') {
            sshagent(credentials: ['my-ssh-key'], ignoreMissing: false) {
              sshKnownHostsGitHub()
              gitKustomizeImage(
                dockerImages: env.DOCKER_IMAGES.tokenize(','),
                repo: "$GITOPS_REPO",
                branch: "$ENVIRONMENT",
                dir: "$APP/$ENVIRONMENT",
                version: "$VERSION"
              )
            }
          }

          argoDeploy("$APP-$ENVIRONMENT")
        }
      }
    }

    stage('Deploy') {
      steps {
        lock(resource: "Deploy - App: $APP, Environment: $ENVIRONMENT", inversePrecedence: true) {
          milestone(ordinal: null, label: "Milestone: Deploy")
          timeout(time: 15, unit: 'MINUTES') {
            sh 'make deploy'
          }
        }
      }
    }

    stage('Cloudflare Cache Purge') {
      steps {
        CloudflarePurgeCache()
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
      echo 'FAILURE!'
      Notify()
    }
    unstable {
      echo 'UNSTABLE!'
    }
    changed {
      echo 'Pipeline state change! (success vs failure)'
    }
  }
}
