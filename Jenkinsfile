pipeline {

    agent any

    parameters {

        string(
            name: 'AWS_REGION',
            defaultValue: 'ap-south-1',
            description: 'AWS Region'
        )

        string(
            name: 'ECR_REPOSITORY',
            defaultValue: 'devops-E2E-Project',
            description: 'ECR Repository Name'
        )

        string(
            name: 'APPLICATION_PORT',
            defaultValue: '8080',
            description: 'Application Port'
        )
    }

    environment {

        CONTAINER_NAME = 'devops-training-app'

        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {

        stage('Get AWS Account ID') {

            steps {

                script {

                    env.AWS_ACCOUNT_ID = sh(
                        script: '''
                            aws sts get-caller-identity \
                            --query Account \
                            --output text
                        ''',
                        returnStdout: true
                    ).trim()

                    env.ECR_URL =
                        "${env.AWS_ACCOUNT_ID}.dkr.ecr.${params.AWS_REGION}.amazonaws.com"

                    env.IMAGE_NAME =
                        "${env.ECR_URL}/${params.ECR_REPOSITORY}:${env.IMAGE_TAG}"

                    echo "AWS Account: ${env.AWS_ACCOUNT_ID}"
                    echo "AWS Region: ${params.AWS_REGION}"
                    echo "ECR Repository: ${params.ECR_REPOSITORY}"
                    echo "Image: ${env.IMAGE_NAME}"
                }
            }
        }

        stage('Checkout') {

            steps {
                checkout scm
            }
        }

        stage('Maven Dependencies') {

            steps {
                sh 'mvn dependency:resolve'
            }
        }

        stage('Unit Test') {

            steps {
                sh 'mvn test'
            }
        }

        stage('SonarQube') {
    steps {
        catchError(
            buildResult: 'SUCCESS',
            stageResult: 'UNSTABLE'
        ) {
            withSonarQubeEnv('sonarqube') {
                sh '''
                    mvn clean verify \
                    org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                    -Dsonar.projectKey=devops-training-app \
                    -Dsonar.projectName=devops-training-app
                '''
                }
            }
        }

        stage('Docker Version') {

            steps {

                sh '''
                    docker --version
                    docker compose version || true
                '''
            }
        }

        stage('Docker Build') {

            steps {

                sh '''
                    docker build \
                    -t ${ECR_REPOSITORY}:${IMAGE_TAG} .
                '''
            }
        }

        stage('Docker Run') {

            steps {

                sh '''
                    docker rm -f ${CONTAINER_NAME} 2>/dev/null || true

                    docker run -d \
                    --name ${CONTAINER_NAME} \
                    -p ${APPLICATION_PORT}:8080 \
                    ${ECR_REPOSITORY}:${IMAGE_TAG}
                '''
            }
        }

        stage('Application Health Check') {

            steps {

                sh '''
                    sleep 10

                    docker ps

                    curl -f \
                    http://localhost:${APPLICATION_PORT}/health
                '''

                echo 'Application container is running successfully!'
            }
        }

        stage('Stop Test Container') {

            steps {

                sh '''
                    docker stop ${CONTAINER_NAME}
                    docker rm ${CONTAINER_NAME}
                '''
            }
        }

        stage('ECR Login') {

            steps {

                sh '''
                    aws ecr get-login-password \
                    --region ${AWS_REGION} | \
                    docker login \
                    --username AWS \
                    --password-stdin \
                    ${ECR_URL}
                '''
            }
        }

        stage('Tag Image') {

            steps {

                sh '''
                    docker tag \
                    ${ECR_REPOSITORY}:${IMAGE_TAG} \
                    ${IMAGE_NAME}
                '''
            }
        }

        stage('Push Image to ECR') {

            steps {

                sh '''
                    docker push ${IMAGE_NAME}
                '''

                echo "Image pushed successfully: ${env.IMAGE_NAME}"
            }
        }

        stage('Deploy on AWS EC2') {

            steps {

                sh '''
                    docker rm -f ${CONTAINER_NAME} 2>/dev/null || true

                    docker pull ${IMAGE_NAME}

                    docker run -d \
                    --name ${CONTAINER_NAME} \
                    --restart unless-stopped \
                    -p ${APPLICATION_PORT}:8080 \
                    ${IMAGE_NAME}
                '''
            }
        }

        stage('Final Verification') {

            steps {

                sh '''
                    sleep 10

                    echo "===== CONTAINER ====="
                    docker ps

                    echo "===== HEALTH ====="
                    curl -f \
                    http://localhost:${APPLICATION_PORT}/health

                    echo ""

                    echo "===== APPLICATION ====="
                    curl -f \
                    http://localhost:${APPLICATION_PORT}/
                '''
            }
        }
    }

    post {

        success {

            echo '''
            ==========================================
            DEPLOYMENT SUCCESSFUL
            ==========================================
            Application deployed successfully.
            ==========================================
            '''
        }

        failure {

            echo '''
            ==========================================
            DEPLOYMENT FAILED
            ==========================================
            Check the failed Jenkins stage.
            ==========================================
            '''
        }
    }
}
