grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
	}

	dependencies {
		String nettyVersion = '4.0.0.Beta2'

		compile "io.netty:netty-buffer:$nettyVersion", {
			excludes 'easymock', 'easymockclassextension', 'hamcrest-library', 'javassist', 'jmock-junit4',
			         'junit', 'logback-classic', 'netty-common'
		}

		compile "io.netty:netty-codec:$nettyVersion", {
			excludes 'easymock', 'easymockclassextension', 'hamcrest-library', 'javassist',
			         'jboss-marshalling', 'jboss-marshalling-river', 'jboss-marshalling-serial',
			         'jmock-junit4', 'junit', 'jzlib', 'logback-classic', 'netty-transport', 'protobuf-java'
		}

		compile "io.netty:netty-common:$nettyVersion", {
			excludes 'commons-logging', 'easymock', 'easymockclassextension', 'hamcrest-library', 'javassist',
			         'jmock-junit4', 'junit', 'log4j', 'logback-classic', 'slf4j-api'
		}

		compile "io.netty:netty-handler:$nettyVersion", {
			excludes 'easymock', 'easymockclassextension', 'hamcrest-library', 'javassist', 'jmock-junit4',
			         'junit', 'logback-classic', 'netty-buffer', 'netty-transport'
		}

		compile "io.netty:netty-codec-http:$nettyVersion", {
			excludes 'easymock', 'easymockclassextension', 'hamcrest-library', 'javassist', 'jmock-junit4',
			         'junit', 'jzlib', 'logback-classic', 'netty-codec', 'netty-handler'
		}

		compile "io.netty:netty-transport:$nettyVersion", {
			excludes 'easymock', 'easymockclassextension', 'hamcrest-library', 'javassist', 'jmock-junit4',
			         'junit', 'logback-classic', 'netty-buffer'
		}
	}

	plugins {
		build ':release:2.2.1', ':rest-client-builder:1.0.3', {
			export = false
		}
	}
}
