node {
  def mvnHome
  
  stage('Preparation') {
    git 'https://github.com/DARIAH-DE/colreg.git'
    mvnHome = tool 'Maven 3.0.4'
  }

  stage('Build') {

    echo "Building ${env.BUILD_ID} on ${env.JENKINS_URL}"
    def username = 'fu'
    echo "I said, Hello Mr ${username} ${env.POM_VERSION}"

    sh "'${mvnHome}/bin/mvn' -U -Pdariah.deb -Dmaven.test.failure.ignore clean package"
  }

  stage('Publish') {
    sh "aptly repo add snapshots colreg-ui/target/*.deb"
    sh "aptly publish update trusty"
    sh "rm colreg-ui/target/*.deb"
  }
}
