[![Build Status](https://travis-ci.org/StackVista/akka-http-pac4j.svg?branch=master)](https://travis-ci.org/StackVista/akka-http-pac4j)

## akka-http-pac4j ##
A wrapper around pac4j (www.pac4j.org) built using AkkaHTTP

## how to use it ##

Here is a demo project showing how to configure akka-http-pac4j for SAML & LDAP: https://github.com/vidma/akka-http-pac4j-demo

Many other auth methods supported by pac4j should work too, see  https://github.com/pac4j/play-pac4j-scala-demo/blob/master/app/modules/SecurityModule.scala for more examples

## One time publishing configuration

- Set Sonatype account information
    - Place in `$HOME/.sbt/(sbt-version 0.13 or 1.0)/sonatype.sbt` the following configuration:
    ```
    credentials += Credentials("Sonatype Nexus Repository Manager",
            "oss.sonatype.org",
            "(Sonatype user name)",
            "(Sonatype password)")
    ```
    - Create a PGP key pair:
        - `sbt pgp-cmd gen-key` which should generate an output like...        
        ```
        Please enter the name associated with the key: Developer
        Please enter the email associated with the key: developer@gmail.com
        Please enter the passphrase for the key: ***************
        Please re-enter the passphrase for the key: ***************
        [info] Creating a new PGP key, this could take a long time.
        [info] Public key := /Users/developer/.sbt/gpg/pubring.asc
        [info] Secret key := /Users/developer/.sbt/gpg/secring.asc
        [info] Please do not share your secret key.   Your public key is free to share.
        ```
    - Send your key to a public Key Server
        - Obtain your key id using `sbt pgp-cmd list-keys` which should output...
        ```
        /Users/developer/.sbt/gpg/pubring.asc
        -------------------------------------
        pub	RSA@2048/2bb8c19d	2018-12-03
        uid	                	Developer <developer@gmail.com>
         ```
         - Export your key using `sbt pgp-cmd send-key 2bb8c19d hkp://keyserver.ubuntu.com`
         ```
         Sending PublicKeyRing(PublicKey(9884fa162bb8c19d, Developer <developer@gmail.com>, RSA@2048)) to HkpServer(http://keyserver.ubuntu.com:11371)
         ```
- Support links
    - https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html
    - https://www.scala-sbt.org/sbt-pgp/usage.html

## Publishing artifacts
- Modify the version in `build.sbt`. If the project version has "SNAPSHOT" suffix, your project will be published to the snapshot repository of Sonatype 
    ```
    version      := "0.4.1-SNAPSHOT"
    ```
- Publish using `sbt publishSigned`
