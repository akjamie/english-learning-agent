// Jenkinsfile — OpenSpec-based Agentic SDLC CI gate.
// Designed for a MULTI-BRANCH PIPELINE job: it runs on every PR/branch.
//
// Job configuration (see jenkins/README.md):
//   1. Create a Multibranch Pipeline, point it at this repo.
//   2. Add a credential 'asdlc-coordination' (SSH/username-password) for the
//      coordination store, and set the global env var COORDINATION_URL.
//   3. Optional: set ASDLC_REPO (repository name) as a global env var;
//      defaults to the Jenkins job name.

pipeline {
  agent { docker { image 'node:20' label 'docker' } }

  options {
    buildDiscarder(logRotator(numToKeepStr: '30'))
    disableConcurrentBuilds()
    timestamps()
  }

  environment {
    ASDLC_REPO = "${env.ASDLC_REPO ?: "${env.JOB_BASE_NAME}"}"
    COORDINATION_URL = "${env.COORDINATION_URL}"
  }

  stages {
    stage('Checkout coordination store') {
      steps {
        dir('coordination') {
          // The authority (manifests + approval records) lives here, NOT in
          // the repo under test. Gate reads it read-only.
          checkout([
            $class: 'GitSCM',
            branches: [[name: '*/main']],
            userRemoteConfigs: [[
              url: "${env.COORDINATION_URL}",
              credentialsId: 'asdlc-coordination'
            ]]
          ])
        }
      }
    }

    stage('Install tooling') {
      steps {
        sh 'npm install -g @fission-ai/openspec@latest agentic-sdlc@latest'
      }
    }

    stage('Derive change id') {
      steps {
        script {
          // Convention: branch `asdlc/<changeId>` -> CHANGE_ID (e.g. TECH-1234).
          // Override this block for your own branch/PR metadata convention.
          def branch = env.CHANGE_BRANCH ?: env.BRANCH_NAME
          env.ASDLC_CHANGE_ID = branch.tokenize('/').last().toUpperCase()
          echo "Gating change: ${env.ASDLC_CHANGE_ID}"
        }
      }
    }

    stage('Gate') {
      steps {
        sh """
          asdlc gate "${env.ASDLC_CHANGE_ID}" \\
            --repo "${env.ASDLC_REPO}" \\
            --coordination "${WORKSPACE}/coordination" \\
            --repo-root "${WORKSPACE}"
        """
      }
    }
  }

  post {
    failure {
      // Integrate with your chat tool / issue tracker here.
      echo "asdlc gate FAILED for ${env.ASDLC_CHANGE_ID}"
    }
    success {
      echo "asdlc gate PASSED for ${env.ASDLC_CHANGE_ID}"
    }
  }
}