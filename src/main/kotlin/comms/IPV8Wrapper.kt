package comms

import ShootPreferences
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.strategy.PeriodicSimilarity
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomChurn
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import java.net.InetAddress
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToInt

class IPV8Wrapper(private val preferences: ShootPreferences) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val logger = KotlinLogging.logger {}

    fun run() {
        if (!preferences.testing) startIpv8()
    }

    private fun createDiscoveryCommunity(): OverlayConfiguration<DiscoveryCommunity> {
        val randomWalk = RandomWalk.Factory(timeout = 3.0, peers = 20)
        val randomChurn = RandomChurn.Factory()
        val periodicSimilarity = PeriodicSimilarity.Factory()
        return OverlayConfiguration(
            DiscoveryCommunity.Factory(),
            listOf(randomWalk, randomChurn, periodicSimilarity),
        )
    }

    private fun createTrustChainCommunity(): OverlayConfiguration<TrustChainCommunity> {
        val settings = TrustChainSettings()
        val driver: SqlDriver = JdbcSqliteDriver(IN_MEMORY)
        Database.Schema.create(driver)
        val database = Database(driver)
        val store = TrustChainSQLiteStore(database)
        val randomWalk = RandomWalk.Factory(timeout = 3.0, peers = 20)
        return OverlayConfiguration(
            TrustChainCommunity.Factory(settings, store),
            listOf(randomWalk),
        )
    }

    private val shootCommunityFactory = ShootCommunityFactory(preferences)
    val shootCommunityFlow: Flow<ShootCommunity> = shootCommunityFactory.communityFlow

    val myPeer: CompletableFuture<Peer> = CompletableFuture()

    private fun createShootCommunity(): OverlayConfiguration<ShootCommunity> {
        val randomWalk = RandomWalk.Factory(timeout = 3.0, peers = 20)
        return OverlayConfiguration(
            shootCommunityFactory,
            listOf(randomWalk),
        )
    }

    // Get key from ini file or generate/persist a new one
    private fun getKey(): PrivateKey {
        var keyBytes: ByteArray = preferences["privateKey", ""].hexToBytes()
        if (keyBytes.isEmpty()) {
            logger.info { "Generating/saving new private key" }
            val key = JavaCryptoProvider.generateKey()
            keyBytes = key.keyToBin()
            preferences["privateKey"] = keyBytes.toHex()
        } else {
            logger.info { "Using existing private key" }
        }
        return JavaCryptoProvider.keyFromPrivateBin(keyBytes)
    }

    private fun startIpv8() {
        val myKey = getKey()
        val myPeer = Peer(myKey)
        this.myPeer.complete(myPeer)
        val udpEndpoint = UdpEndpoint(8090, InetAddress.getByName("0.0.0.0"))
        val endpoint = EndpointAggregator(udpEndpoint, null)

        val config = IPv8Configuration(
            overlays = listOf(
                createDiscoveryCommunity(),
                createTrustChainCommunity(),
                createShootCommunity(),
            ),
            walkerInterval = 1.0,
        )

        val ipv8 = IPv8(endpoint, config, myPeer)
        ipv8.start()

        scope.launch {
            while (true) {
                for ((_, overlay) in ipv8.overlays) {
                    printPeersInfo(overlay)
                }
                logger.info { "===" }
                delay(5000)
            }
        }

        while (ipv8.isStarted()) {
            Thread.sleep(1000)
        }
    }

    private fun printPeersInfo(overlay: Overlay) {
        val peers = overlay.getPeers()
        logger.info { overlay::class.simpleName + ": ${peers.size} peers" }
        for (peer in peers) {
            val avgPing = peer.getAveragePing()
            val lastRequest = peer.lastRequest
            val lastResponse = peer.lastResponse

            val lastRequestStr = if (lastRequest != null) {
                "" + ((Date().time - lastRequest.time) / 1000.0).roundToInt() + " s"
            } else {
                "?"
            }

            val lastResponseStr = if (lastResponse != null) {
                "" + ((Date().time - lastResponse.time) / 1000.0).roundToInt() + " s"
            } else {
                "?"
            }

            val avgPingStr = if (!avgPing.isNaN()) "" + (avgPing * 1000).roundToInt() + " ms" else "? ms"
            logger.info { "${peer.mid} (S: $lastRequestStr, R: $lastResponseStr, $avgPingStr)" }
        }
    }

    class ShootCommunityFactory(private val preferences: ShootPreferences) : Overlay.Factory<ShootCommunity>(ShootCommunity::class.java) {
        private val scope = CoroutineScope(Dispatchers.Default)
        private val logger = KotlinLogging.logger {}
        private val sharedFlow = MutableSharedFlow<ShootCommunity>(10)
        val communityFlow = sharedFlow.asSharedFlow()
        override fun create(): ShootCommunity {
            val community = ShootCommunity(preferences)
            scope.launch {
                logger.info { "Emitting community" }
                sharedFlow.emit(community)
            }
            return community
        }
    }
}
