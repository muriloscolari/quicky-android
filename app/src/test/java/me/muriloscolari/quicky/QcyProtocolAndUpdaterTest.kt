package me.muriloscolari.quicky

import me.muriloscolari.quicky.ble.AncCycleMode
import me.muriloscolari.quicky.ble.AncSubScene
import me.muriloscolari.quicky.ble.QcyEqPreset
import me.muriloscolari.quicky.ble.QcyProtocol
import me.muriloscolari.quicky.update.AppUpdater
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QcyProtocolAndUpdaterTest {

    @Test
    fun sleepModeCommandFramesCorrectly() {
        val enable = QcyProtocol.buildSleepModeCommand(true)
        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0x03, 0x10, 0x01, 0x01),
            enable
        )

        val disable = QcyProtocol.buildSleepModeCommand(false)
        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0x03, 0x10, 0x01, 0x02),
            disable
        )
    }

    @Test
    fun meloBudsProAncCommandUnpacksTo0x17() {
        val ancPacket = QcyProtocol.buildAncCommand(AncCycleMode.ANC.meloBudsCmdId) // 65794 = 0x010102
        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0x05, 0x17, 0x03, 0x01, 0x01, 0x02),
            ancPacket
        )

        val ambientPacket = QcyProtocol.buildAncCommand(AncCycleMode.AMBIENT.meloBudsCmdId) // 196868 = 0x030104
        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0x05, 0x17, 0x03, 0x03, 0x01, 0x04),
            ambientPacket
        )

        val offPacket = QcyProtocol.buildAncCommand(AncCycleMode.OFF.meloBudsCmdId) // 131072 = 0x020000
        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0x05, 0x17, 0x03, 0x02, 0x00, 0x00),
            offPacket
        )
    }

    @Test
    fun ancCycleOrderIsAncThenAmbientThenOff() {
        var mode = AncCycleMode.ANC
        mode = mode.next()
        assertEquals(AncCycleMode.AMBIENT, mode)
        mode = mode.next()
        assertEquals(AncCycleMode.OFF, mode)
        mode = mode.next()
        assertEquals(AncCycleMode.ANC, mode)
    }

    @Test
    fun batteryBytesParseLevelAndChargingFlag() {
        val bytes = byteArrayOf((80 or 0x80).toByte(), 75.toByte(), 0.toByte())
        val parsed = QcyProtocol.parseBatteryBytes(bytes)
        assertNotNull(parsed)
        assertEquals(80, parsed!!.left)
        assertTrue(parsed.leftCharging)
        assertEquals(75, parsed.right)
        assertFalse(parsed.rightCharging)
        assertEquals(0, parsed.caseLevel)
    }

    @Test
    fun versionComparisonDetectsNewerSemanticVersion() {
        assertTrue(AppUpdater.isNewerVersion("0.1.0", "v0.2.0"))
        assertTrue(AppUpdater.isNewerVersion("v0.1.0", "0.1.1"))
        assertFalse(AppUpdater.isNewerVersion("0.1.0", "v0.1.0"))
        assertFalse(AppUpdater.isNewerVersion("0.2.0", "0.1.9"))
    }

    @Test
    fun keyFunctionPairsBuildAndParseRoundTrip() {
        val raw = QcyProtocol.buildKeyFunctionData(listOf(0x09 to 0x00, 0x0A to 0x0B))
        assertArrayEquals(byteArrayOf(0x09, 0x00, 0x0A, 0x0B), raw)

        val parsed = QcyProtocol.parseKeyFunctionPairs(raw)
        assertEquals(0x00, parsed[0x09])
        assertEquals(0x0B, parsed[0x0A])
    }

    @Test
    fun ancSubSceneAndAmbientLevelMappingsAndLabels() {
        assertEquals(3, AncSubScene.INDOOR.levelCount)
        assertEquals(1, AncSubScene.INDOOR.levelFromCmdId(65792))
        assertEquals(2, AncSubScene.INDOOR.levelFromCmdId(65793))
        assertEquals(3, AncSubScene.INDOOR.levelFromCmdId(65794))
        assertEquals(65793, AncSubScene.INDOOR.cmdIdForLevel(2))

        assertEquals(1, AncSubScene.ADAPTIVE.levelCount)
        assertEquals(1, AncSubScene.ADAPTIVE.levelFromCmdId(66816))
        assertEquals(66816, AncSubScene.ADAPTIVE.cmdIdForLevel(3))

        assertEquals(AncSubScene.COMMUTING, QcyProtocol.subSceneForCmdId(66049))
        assertEquals("Transporte · 2/3", QcyProtocol.formatAncShortLabel(66049))
        assertEquals("Adaptativo", QcyProtocol.formatAncShortLabel(66816))

        assertEquals(1, QcyProtocol.ambientLevelFromCmdId(196864))
        assertEquals(5, QcyProtocol.ambientLevelFromCmdId(196868))
        assertEquals(7, QcyProtocol.ambientLevelFromCmdId(196870))
        assertEquals(196866, QcyProtocol.ambientCmdIdForLevel(3))
        assertEquals("Ambiente · 3/7", QcyProtocol.formatAncShortLabel(196866))
        assertEquals("OFF", QcyProtocol.formatAncShortLabel(AncCycleMode.OFF.meloBudsCmdId))
    }

    @Test
    fun soundBalanceCommandFramesAndClamps() {
        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0x03, 0x16, 0x01, 50),
            QcyProtocol.buildSoundBalanceCommand(50)
        )
        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0x03, 0x16, 0x01, 0),
            QcyProtocol.buildSoundBalanceCommand(-12)
        )
        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0x03, 0x16, 0x01, 100),
            QcyProtocol.buildSoundBalanceCommand(140)
        )
    }

    @Test
    fun eqDirectAndParametricRoundTrip() {
        val sampleBands = listOf(-8, -4, 0, 2, 5, 8, -1, 3, -6, 12) // 12 clamps to 8
        val direct = QcyProtocol.buildEqDirectPayload(QcyEqPreset.CUSTOM.eqType, sampleBands)
        assertEquals(11, direct.size)
        val parsedDirect = QcyProtocol.parseEqDirectPayload(direct)
        assertNotNull(parsedDirect)
        assertEquals(QcyEqPreset.CUSTOM, parsedDirect!!.preset)
        assertEquals(listOf(-8, -4, 0, 2, 5, 8, -1, 3, -6, 8), parsedDirect.bands)

        val v2Packet = QcyProtocol.buildEqParametricCommand(QcyEqPreset.HEAVY_BASS.eqType, QcyEqPreset.HEAVY_BASS.defaultBands, isV2 = true)
        assertEquals(0xFF.toByte(), v2Packet[0])
        assertEquals(0x22.toByte(), v2Packet[2])
        val paramsSlice = v2Packet.copyOfRange(4, v2Packet.size)
        val parsedParam = QcyProtocol.parseEqParametricPayload(paramsSlice, isV2 = true)
        assertNotNull(parsedParam)
        assertEquals(QcyEqPreset.HEAVY_BASS, parsedParam!!.preset)
        assertEquals(QcyEqPreset.HEAVY_BASS.defaultBands, parsedParam.bands)
    }
}
