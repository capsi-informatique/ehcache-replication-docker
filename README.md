# ehcache-replication-docker
Ehcache peer provider for docker env


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