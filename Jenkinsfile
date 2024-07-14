pipeline {
	agent any
	tools { 
        maven 'maven-3.6.0' 
        jdk 'jdk7'
    }
    stages {
        stage('Build - Windows Only') {
            steps {
                bat 'mvn -V -Dhttps.protocols=TLSv1.2 -Dmaven.test.failure.ignore clean package'
            }
            post {
                success {
                    junit '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }
    }
}
