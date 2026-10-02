pipeline {

    agent any

    environment {

        // ==============================
        // AWS CONFIGURATION
        // ==============================

        AWS_REGION = 'ap-south-1'

        AWS_ACCOUNT_ID = 'YOUR_AWS_ACCOUNT_ID'

        ECR_REPOSITORY = 'devops-training-app'

        // Jenkins BUILD_NUMBER gives every image a unique version
        IMAGE_TAG = "${BUILD_NUMBER}"

        ECR_URL = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

        IMAGE_NAME = "${ECR_URL}/${ECR_REPOSITORY}:${IMAGE_TAG}"

        // Local Docker container
        CONTAINER_NAME = 'devops-training-app'

        APPLICATION_PORT = '8080'

    }

    stages {

        // ==================================================
        // 1. CHECKOUT
        // ==================================================

        stage('Checkout') {

            steps {

                echo '========================================'
                echo 'CHECKING OUT SOURCE CODE'
                echo '========================================'

                checkout scm
            }
        }


        // ==================================================
        // 2. MAVEN DEPENDENCIES
        // ==================================================

        stage('Maven Dependency Resolve') {

            steps {

                echo '========================================'
                echo 'RESOLVING MAVEN DEPENDENCIES'
                echo '========================================'

                sh '''
                    mvn dependency:resolve
                '''
            }
        }


        // ==================================================
        // 3. UNIT TEST
        // ==================================================

        stage('Unit Test') {

            steps {

                echo '========================================'
                echo 'RUNNING UNIT TESTS'
                echo '========================================'

                sh '''
                    mvn test
                '''
            }

            post {

                always {

                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml'
                }
            }
        }


        // ==================================================
        // 4. SONARQUBE
        // ==================================================

        stage('SonarQube Analysis') {

            steps {

                echo '========================================'
                echo 'RUNNING SONARQUBE ANALYSIS'
                echo '========================================'

                withSonarQubeEnv('sonarqube') {

                    sh '''
                        mvn clean verify sonar:sonar \
                        -Dsonar.projectKey=devops-training-app \
                        -Dsonar.projectName=devops-training-app
                    '''
                }
            }
        }


        // ==================================================
        // 5. DOCKER VERSION
        // ==================================================

        stage('Docker Version Check') {

            steps {

                echo '========================================'
                echo 'CHECKING DOCKER VERSION'
                echo '========================================'

                sh '''
                    docker --version
                    docker compose version || true
                '''
            }
        }


        // ==================================================
        // 6. DOCKER BUILD
        // ==================================================

        stage('Docker Build') {

            steps {

                echo '========================================'
                echo 'BUILDING DOCKER IMAGE'
                echo '========================================'

                sh '''
                    docker build \
                    -t ${ECR_REPOSITORY}:${IMAGE_TAG} \
                    .
                '''

                echo "Docker image created successfully."
                echo "Image: ${ECR_REPOSITORY}:${IMAGE_TAG}"
            }
        }


        // ==================================================
        // 7. DOCKER RUN
        // ==================================================

        stage('Docker Run') {

            steps {

                echo '========================================'
                echo 'STARTING DOCKER CONTAINER'
                echo '========================================'

                sh '''
                    docker rm -f ${CONTAINER_NAME} 2>/dev/null || true

                    docker run -d \
                    --name ${CONTAINER_NAME} \
                    -p ${APPLICATION_PORT}:8080 \
                    ${ECR_REPOSITORY}:${IMAGE_TAG}
                '''

                echo 'Docker container created successfully!'
            }
        }


        // ==================================================
        // 8. CONTAINER HEALTH CHECK
        // ==================================================

        stage('Container Health Check') {

            steps {

                echo '========================================'
                echo 'CHECKING CONTAINER'
                echo '========================================'

                sh '''
                    sleep 10

                    docker ps

                    echo "----------------------------------------"
                    echo "Container Logs"
                    echo "----------------------------------------"

                    docker logs ${CONTAINER_NAME}

                    echo "----------------------------------------"
                    echo "Testing Application"
                    echo "----------------------------------------"

                    curl -f http://localhost:${APPLICATION_PORT}/health
                '''

                echo '========================================'
                echo 'APPLICATION IS RUNNING SUCCESSFULLY!'
                echo '========================================'
            }
        }


        // ==================================================
        // 9. STOP LOCAL TEST CONTAINER
        // ==================================================

        stage('Stop Test Container') {

            steps {

                echo 'Stopping temporary test container...'

                sh '''
                    docker stop ${CONTAINER_NAME}
                    docker rm ${CONTAINER_NAME}
                '''
            }
        }


        // ==================================================
        // 10. AWS ECR LOGIN
        // ==================================================

        stage('AWS ECR Login') {

            steps {

                echo '========================================'
                echo 'LOGIN TO AWS ECR'
                echo '========================================'

                sh '''
                    aws sts get-caller-identity

                    aws ecr get-login-password \
                    --region ${AWS_REGION} | \
                    docker login \
                    --username AWS \
                    --password-stdin \
                    ${ECR_URL}
                '''
            }
        }


        // ==================================================
        // 11. TAG IMAGE FOR ECR
        // ==================================================

        stage('Tag Docker Image') {

            steps {

                echo '========================================'
                echo 'TAGGING IMAGE FOR ECR'
                echo '========================================'

                sh '''
                    docker tag \
                    ${ECR_REPOSITORY}:${IMAGE_TAG} \
                    ${IMAGE_NAME}
                '''

                echo "Image tagged as:"
                echo "${IMAGE_NAME}"
            }
        }


        // ==================================================
        // 12. PUSH TO ECR
        // ==================================================

        stage('Push Image to ECR') {

            steps {

                echo '========================================'
                echo 'PUSHING IMAGE TO AWS ECR'
                echo '========================================'

                sh '''
                    docker push ${IMAGE_NAME}
                '''

                echo 'Docker image successfully pushed to AWS ECR.'
            }
        }


        // ==================================================
        // 13. DEPLOY TO AWS EC2
        // ==================================================

        stage('Deploy to AWS EC2') {

            steps {

                echo '========================================'
                echo 'DEPLOYING APPLICATION ON AWS EC2'
                echo '========================================'

                sh '''
                    docker rm -f ${CONTAINER_NAME} 2>/dev/null || true

                    docker pull ${IMAGE_NAME}

                    docker run -d \
                    --name ${CONTAINER_NAME} \
                    --restart unless-stopped \
                    -p ${APPLICATION_PORT}:8080 \
                    ${IMAGE_NAME}
                '''

                echo '========================================'
                echo 'AWS DEPLOYMENT SUCCESSFUL!'
                echo '========================================'
            }
        }


        // ==================================================
        // 14. FINAL APPLICATION TEST
        // ==================================================

        stage('Final Application Verification') {

            steps {

                echo '========================================'
                echo 'FINAL APPLICATION VERIFICATION'
                echo '========================================'

                sh '''
                    sleep 10

                    echo "Running Containers:"
                    docker ps

                    echo ""
                    echo "Application Health:"
                    curl -f http://localhost:${APPLICATION_PORT}/health

                    echo ""
                    echo "Application Response:"
                    curl -f http://localhost:${APPLICATION_PORT}/
                '''

                echo '========================================'
                echo 'DEPLOYMENT COMPLETED SUCCESSFULLY!'
                echo '========================================'
            }
        }
    }


    // ======================================================
    // POST ACTIONS
    // ======================================================

    post {

        success {

            echo '''
            ================================================
            DEPLOYMENT SUCCESSFUL
            ================================================

            Application has been:

            1. Checked out from GitHub
            2. Maven dependencies resolved
            3. Unit tests executed
            4. SonarQube analysis completed
            5. Docker image built
            6. Container tested
            7. Image pushed to AWS ECR
            8. Application deployed on AWS EC2
            9. Application health verified

            ================================================
            '''
        }

        failure {

            echo '''
            ================================================
            DEPLOYMENT FAILED
            ================================================

            Check the failed Jenkins stage and console log.

            ================================================
            '''
        }

        always {

            echo 'Pipeline execution completed.'
        }
    }
}
