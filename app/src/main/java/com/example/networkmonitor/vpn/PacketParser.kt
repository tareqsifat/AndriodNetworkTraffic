package com.example.networkmonitor.vpn

import java.nio.ByteBuffer

/**
 * Parses raw IPv4 packet bytes and extracts metadata:
 * - source/destination IP
 * - protocol (TCP=6, UDP=17, ICMP=1)
 * - source/destination ports
 * - DNS query domain (for UDP port 53)
 * - TLS SNI hostname (for TCP port 443)
 */
object PacketParser {

    private const val PROTOCOL_TCP = 6
    private const val PROTOCOL_UDP = 17
    private const val DNS_PORT = 53
    private const val HTTPS_PORT = 443
    private const val HTTP_PORT = 80

    data class ParsedPacket(
        val srcIp: String,
        val dstIp: String,
        val protocol: String,
        val srcPort: Int,
        val dstPort: Int,
        val domain: String?,   // domain from DNS query or SNI
        val uid: Int = -1      // filled later by the service
    )

    fun parse(buffer: ByteArray, length: Int): ParsedPacket? {
        if (length < 20) return null
        val buf = ByteBuffer.wrap(buffer, 0, length)

        val versionAndIhl = buf.get(0).toInt() and 0xFF
        val ipVersion = versionAndIhl ushr 4
        if (ipVersion != 4) return null // only IPv4 for now

        val ihl = (versionAndIhl and 0x0F) * 4
        val protocol = buf.get(9).toInt() and 0xFF

        val srcIp = formatIp(buf, 12)
        val dstIp = formatIp(buf, 16)

        val protocolName = when (protocol) {
            PROTOCOL_TCP -> "TCP"
            PROTOCOL_UDP -> "UDP"
            else -> return null // skip non-TCP/UDP
        }

        if (length <= ihl + 4) return null

        val srcPort = ((buf.get(ihl).toInt() and 0xFF) shl 8) or (buf.get(ihl + 1).toInt() and 0xFF)
        val dstPort = ((buf.get(ihl + 2).toInt() and 0xFF) shl 8) or (buf.get(ihl + 3).toInt() and 0xFF)

        val domain: String? = when {
            protocol == PROTOCOL_UDP && dstPort == DNS_PORT -> parseDnsQuery(buf, ihl + 8, length)
            protocol == PROTOCOL_TCP && dstPort == HTTPS_PORT -> parseTlsSni(buf, ihl, length)
            protocol == PROTOCOL_TCP && dstPort == HTTP_PORT -> parseHttpHost(buf, ihl, length)
            else -> null
        }

        return ParsedPacket(
            srcIp = srcIp,
            dstIp = dstIp,
            protocol = protocolName,
            srcPort = srcPort,
            dstPort = dstPort,
            domain = domain
        )
    }

    private fun formatIp(buf: ByteBuffer, offset: Int): String {
        return "${buf.get(offset).toInt() and 0xFF}." +
                "${buf.get(offset + 1).toInt() and 0xFF}." +
                "${buf.get(offset + 2).toInt() and 0xFF}." +
                "${buf.get(offset + 3).toInt() and 0xFF}"
    }

    /**
     * Parse DNS query to extract the queried domain name.
     * DNS message format: 12-byte header, then questions section.
     */
    private fun parseDnsQuery(buf: ByteBuffer, offset: Int, totalLength: Int): String? {
        return try {
            // DNS header: 12 bytes (ID 2, Flags 2, QDCOUNT 2, ANCOUNT 2, NSCOUNT 2, ARCOUNT 2)
            val questionOffset = offset + 12
            if (questionOffset >= totalLength) return null
            buildDomainName(buf.array(), questionOffset, totalLength)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildDomainName(buf: ByteArray, startOffset: Int, limit: Int): String? {
        val sb = StringBuilder()
        var offset = startOffset
        while (offset < limit) {
            val labelLen = buf[offset].toInt() and 0xFF
            if (labelLen == 0) break
            if (sb.isNotEmpty()) sb.append('.')
            offset++
            if (offset + labelLen > limit) return null
            sb.append(String(buf, offset, labelLen, Charsets.US_ASCII))
            offset += labelLen
        }
        return if (sb.isNotEmpty()) sb.toString() else null
    }

    /**
     * Parse TLS ClientHello to extract the SNI (Server Name Indication) extension.
     * This reveals the target hostname for HTTPS connections without decrypting traffic.
     */
    private fun parseTlsSni(buf: ByteBuffer, ipHeaderLen: Int, totalLength: Int): String? {
        return try {
            // TCP header offset: ipHeaderLen, data offset from TCP data offset field
            val tcpDataOffset = ((buf.get(ipHeaderLen + 12).toInt() and 0xFF) ushr 4) * 4
            val tlsStart = ipHeaderLen + tcpDataOffset
            if (tlsStart + 5 >= totalLength) return null

            // TLS record header: type(1), version(2), length(2)
            val recordType = buf.get(tlsStart).toInt() and 0xFF
            if (recordType != 0x16) return null // not a Handshake record

            val tlsPayloadLen = ((buf.get(tlsStart + 3).toInt() and 0xFF) shl 8) or
                    (buf.get(tlsStart + 4).toInt() and 0xFF)

            val handshakeStart = tlsStart + 5
            if (handshakeStart >= totalLength) return null

            // Handshake: type(1), length(3)
            val handshakeType = buf.get(handshakeStart).toInt() and 0xFF
            if (handshakeType != 0x01) return null // not ClientHello

            // Skip: handshake header (4) + client version (2) + random (32) + session id length (1) + session id
            var pos = handshakeStart + 4 + 2 + 32
            if (pos >= totalLength) return null
            val sessionIdLen = buf.get(pos).toInt() and 0xFF
            pos += 1 + sessionIdLen

            // Cipher suites length
            if (pos + 2 > totalLength) return null
            val cipherSuitesLen = ((buf.get(pos).toInt() and 0xFF) shl 8) or (buf.get(pos + 1).toInt() and 0xFF)
            pos += 2 + cipherSuitesLen

            // Compression methods
            if (pos >= totalLength) return null
            val compressionLen = buf.get(pos).toInt() and 0xFF
            pos += 1 + compressionLen

            // Extensions length
            if (pos + 2 > totalLength) return null
            val extensionsLen = ((buf.get(pos).toInt() and 0xFF) shl 8) or (buf.get(pos + 1).toInt() and 0xFF)
            pos += 2

            val extensionsEnd = pos + extensionsLen
            while (pos + 4 <= extensionsEnd && pos + 4 <= totalLength) {
                val extType = ((buf.get(pos).toInt() and 0xFF) shl 8) or (buf.get(pos + 1).toInt() and 0xFF)
                val extLen = ((buf.get(pos + 2).toInt() and 0xFF) shl 8) or (buf.get(pos + 3).toInt() and 0xFF)
                pos += 4

                if (extType == 0x0000) { // SNI extension type = 0
                    // SNI list length (2), type (1), name length (2), name
                    if (pos + 5 <= totalLength) {
                        val nameLen = ((buf.get(pos + 3).toInt() and 0xFF) shl 8) or (buf.get(pos + 4).toInt() and 0xFF)
                        if (pos + 5 + nameLen <= totalLength) {
                            return String(buf.array(), pos + 5, nameLen, Charsets.US_ASCII)
                        }
                    }
                }
                pos += extLen
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract HTTP Host header from HTTP/1.x requests.
     */
    private fun parseHttpHost(buf: ByteBuffer, ipHeaderLen: Int, totalLength: Int): String? {
        return try {
            val tcpDataOffset = ((buf.get(ipHeaderLen + 12).toInt() and 0xFF) ushr 4) * 4
            val dataStart = ipHeaderLen + tcpDataOffset
            if (dataStart >= totalLength) return null
            val payload = String(buf.array(), dataStart, totalLength - dataStart, Charsets.ISO_8859_1)
            val hostLine = payload.lines().find { it.startsWith("Host:", ignoreCase = true) }
            hostLine?.substringAfter(":")?.trim()?.split(":")?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
