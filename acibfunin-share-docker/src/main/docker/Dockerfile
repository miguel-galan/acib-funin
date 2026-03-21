FROM ${docker.share.image}:${alfresco.share.docker.version}

ARG TOMCAT_DIR=/usr/local/tomcat

# Download the OOTBee Support Tools Share AMP from Maven Central
RUN curl -fSL \
      "https://repo1.maven.org/maven2/org/orderofthebee/support-tools/support-tools-share/1.2.3.0/support-tools-share-1.2.3.0-amp.amp" \
      -o $TOMCAT_DIR/amps_share/support-tools-share.amp

# Copy Dockerfile to avoid an error if no JARs exist
COPY Dockerfile extensions/*.jar $TOMCAT_DIR/webapps/share/WEB-INF/lib/

# Copy Dockerfile to avoid an error if no AMPs exist
COPY Dockerfile extensions/*.amp $TOMCAT_DIR/amps_share/
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
              $TOMCAT_DIR/amps_share $TOMCAT_DIR/webapps/share -directory -nobackup -force

COPY share-config-custom.xml $TOMCAT_DIR/shared/classes/alfresco/web-extension

COPY log4j2.properties $TOMCAT_DIR/webapps/share/WEB-INF/classes
COPY hotswap-agent.properties $TOMCAT_DIR/webapps/share/WEB-INF/classes
