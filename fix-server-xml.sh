#!/bin/bash

echo Example of using surgixml to update a Tomcat server.xml file for production use
echo This cleanly changes the default port and adds a TLS connector block
echo in the right location relative to comments.

CONNECTOR_XML=$(cat <<EOF

    <Connector port="443" protocol="org.apache.coyote.http11.Http11NioProtocol"
               maxThreads="150" maxPostSize="15000000" SSLEnabled="true" >
        <UpgradeProtocol className="org.apache.coyote.http2.Http2Protocol" />
        <SSLHostConfig>
            <Certificate certificateKeystoreFile="conf/keystore.p12"
                         certificateKeystorePassword="changeit" type="RSA" />
        </SSLHostConfig>
    </Connector>
EOF
		)

./surgixml-1.0-SNAPSHOT --file=/tmp/server.xml \
  --edit-attribute="/Server/Service/Connector[@port='8080']@port=80"  \
  --edit-attribute="/Server/Service/Connector[@port='8080']@redirectPort=443"  \
  --edit-attribute="/Server/Service/Engine/Host/Valve[@directory='logs']@directory=/var/log/tomcat"  \
  --add-attribute="//Server/Listener[@className='org.apache.catalina.core.AprLifecycleListener']: SSLEngine=\"on\" FIPSMode=\"on\""  \
  --insert-after-location="//Service/comment()[contains(., 'port=\"8443\"')]:$CONNECTOR_XML"
