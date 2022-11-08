# ehcache-replication-docker
Ehcache peer provider for docker environments

Actual : 
 - Docker Swarm environments supports via Docker api for service inspection. 
 - K8s environments supports only via k8s api . 

This register classical rmi cache peers with adresses on cluster
Cache peers are refreshed every seconds

Usage Docker swarm :

	<dependency>
		<groupId>io.github.capsi-informatique</groupId>
		<artifactId>ehcache-replication-docker</artifactId>
		<version>1.0.1</version>
	</dependency>
	<dependency>
		<groupId>com.github.docker-java</groupId>
		<artifactId>docker-java</artifactId>
		<version>3.2.13</version>
	</dependency>

And in you ehcache.xml :

	<cacheManagerPeerProviderFactory
			class="net.sf.ehcache.distribution.DockerCacheManagerPeerProviderFactory"
			properties="dockerSwarmServiceName=ehcachetest_app"
			/>

	<cacheManagerPeerListenerFactory
			class="net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory"
			properties="socketTimeoutMillis=20000,port=40001"
			propertySeparator="," />
			
	<!-- Cache par défaut -->
	<defaultCache
			maxElementsInMemory="1000"
			eternal="false"
			timeToIdleSeconds="1800"
			timeToLiveSeconds="3600"
			overflowToDisk="false"
			diskPersistent="false"
			memoryStoreEvictionPolicy="LRU"
			statistics="true">
		<cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory" properties="replicateAsynchronously=true,replicatePuts=false,replicateRemovals=true,replicateUpdates=true,replicateUpdatesViaCopy=false,asynchronousReplicationIntervalMillis=1000"/>
	</defaultCache>
	

Usage K8S :

	<dependency>
		<groupId>io.github.capsi-informatique</groupId>
		<artifactId>ehcache-replication-docker</artifactId>
		<version>1.0.1</version>
	</dependency>
	<dependency>
	    <groupId>io.kubernetes</groupId>
	    <artifactId>client-java</artifactId>
	    <version>11.0.4</version>
	</dependency>

And in you ehcache.xml :

	<cacheManagerPeerProviderFactory
			class="net.sf.ehcache.distribution.DockerCacheManagerPeerProviderFactory"
			properties="k8sAppSelector=..."
			/>

Other k8s properties availables : k8sUrl, k8sUsername, k8sPassword, k8sNamespace, k8sToken, k8sValidateSSL
