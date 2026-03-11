package com.example.networkmonitor.vpn

import com.example.networkmonitor.data.NetworkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the firewall blocklist and trusted domains (ignore list).
 * Keeps an in-memory set synced from the DB for zero-latency lookups on the VPN fastpath.
 */
class FirewallManager(private val repository: NetworkRepository) {

    // Concurrent sets for fast, thread-safe access in the VPN loop
    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val blockedIps = ConcurrentHashMap.newKeySet<String>()
    private val ignoredDomains = ConcurrentHashMap.newKeySet<String>()

    fun init(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            syncRules()
        }
    }

    suspend fun syncRules() {
        blockedDomains.clear()
        blockedIps.clear()
        ignoredDomains.clear()

        val rules = repository.getAllRulesSync()
        rules.forEach { rule ->
            if (rule.action == "BLOCK") {
                if (rule.isDomain) blockedDomains.add(rule.target) else blockedIps.add(rule.target)
            } else if (rule.action == "IGNORE") {
                if (rule.isDomain) ignoredDomains.add(rule.target)
            }
        }
    }

    /**
     * Checks if a packet to this IP/Domain should be dropped immediately.
     */
    fun isBlocked(domain: String?, ip: String): Boolean {
        if (blockedIps.contains(ip)) return true
        if (domain != null && blockedDomains.contains(domain)) return true
        return false
    }

    /**
     * Checks if the user has explicitly chosen to "Ignore" alerts for this domain.
     */
    fun isIgnored(domain: String?): Boolean {
        if (domain == null) return false
        return ignoredDomains.contains(domain)
    }
}
