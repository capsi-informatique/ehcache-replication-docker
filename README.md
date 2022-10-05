# ehcache-replication-docker
Ehcache peer provider for docker environments

Actual : Docker Swarm environments supports only via Docker api for service inspection. 

This register classical rmi cache peers with adresses on cluster
Cache peers are refreshed every seconds

Usage :

	<dependency>
		<groupId>io.github.capsi-informatique</groupId>
		<artifactId>ehcache-replication-docker</artifactId>
		<version>1.0.0</version>
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
	
Upcoming soon, kubernetes support...