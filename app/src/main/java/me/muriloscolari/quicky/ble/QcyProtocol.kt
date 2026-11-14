package me.muriloscolari.quicky.ble

import java.util.UUID

enum class AncCycleMode(val label: String, val meloBudsCmdId: Int, val legacyCmdId: Int) {
    ANC("ANC", 65794, 1),
    AMBIENT("Ambiente", 196868, 3),
    OFF("OFF", 131072, 0);

    fun next(): AncCycleMode = when (this) {
        ANC -> AMBIENT
        AMBIENT -> OFF
        OFF -> ANC
    }
}

enum class AncSubScene(val label: String, val startCmdId: Int, val endCmdId: Int, val defaultCmdId: Int) {
    ADAPTIVE("Adaptativo", 66816, 66816, 66816),
    INDOOR("Interno", 65792, 65794, 65794),
    COMMUTING("Transporte", 66048, 66050, 66050),
    NOISY("Ruidoso", 66304, 66306, 66306),
    ANTI_WIND("Anti-vento", 66560, 66560, 66560)
}

enum class FindEarbudSide(val label: String, val leftGain: Float, val rightGain: Float) {
    LEFT("Esquerdo", 1f, 0f),
    BOTH("Ambos", 1f, 1f),
    RIGHT("Direito", 0f, 1f)
}

enum class QcyKeyId(val keyId: Int, val label: String, val defaultFuncId: Int) {
    LEFT_SINGLE(0x01, "1 toque · Esquerdo", 0x00),
    RIGHT_SINGLE(0x02, "1 toque · Direito", 0x00),
    LEFT_DOUBLE(0x03, "2 toques · Esquerdo", 0x01),
    RIGHT_DOUBLE(0x04, "2 toques · Direito", 0x01),
    LEFT_TRIPLE(0x05, "3 toques · Esquerdo", 0x04),
    RIGHT_TRIPLE(0x06, "3 toques · Direito", 0x03),
    LEFT_LONG(0x09, "Segurar · Esquerdo", 0x07),
    RIGHT_LONG(0x0A, "Segurar · Direito", 0x0B)
}

enum class QcyKeyAction(val funcId: Int, val label: String) {
    NONE(0x00, "Desativado"),
    PLAY_PAUSE(0x01, "Play / Pause"),
    PREVIOUS(0x02, "Faixa anterior"),
    NEXT(0x03, "Próxima faixa"),
    VOICE_ASSISTANT(0x04, "Assistente de voz"),
    VOLUME_UP(0x05, "Aumentar volume"),
    VOLUME_DOWN(0x06, "Diminuir volume"),
    GAME_MODE(0x07, "Modo Jogo"),
    ANC_MODE(0x0B, "Modo ANC");

    companion object {
        fun fromFuncId(funcId: Int?): QcyKeyAction? =
            entries.firstOrNull { it.funcId == funcId }

        fun labelFor(funcId: Int?, fallback: String = "Desconhecido"): String =
            fromFuncId(funcId)?.label ?: fallback
    }
}

data class QcyBattery(
    val left: Int = -1,
    val right: Int = -1,
    val caseLevel: Int = -1,
    val leftCharging: Boolean = false,
    val rightCharging: Boolean = false,
    val caseCharging: Boolean = false
)

data class QcyAdvertisement(
    val vendorId: Int,
    val modelTitle: String,
    val battery: QcyBattery,
    val controlMac: String,
    val otherMac: String = ""
)

object QcyProtocol {
    const val COMPANY_ID_QCY = 0x521C
    const val VENDOR_MELOBUDS_PRO = 19786

    val SERVICE_UUID: UUID = UUID.fromString("0000a001-0000-1000-8000-00805f9b34fb")
    val COMMAND_UUID: UUID = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb")
    val NOTIFY_UUID: UUID = UUID.fromString("00001002-0000-1000-8000-00805f9b34fb")
    val KEY_FUNC_UUID: UUID = UUID.fromString("0000000d-0000-1000-8000-00805f9b34fb")
    val VERSION_UUID: UUID = UUID.fromString("00000007-0000-1000-8000-00805f9b34fb")
    val BATTERY_UUID: UUID = UUID.fromString("00000008-0000-1000-8000-00805f9b34fb")
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val knownModels = mapOf(
        19786 to "QCY MeloBuds Pro",
        19787 to "QCY HT08 MeloBuds Pro Plus",
        19785 to "QCY MeloBuds Neo",
        19800 to "QCY MeloBuds N50"
    )

    fun modelTitleForVendor(vendorId: Int): String =
        knownModels[vendorId] ?: "QCY Earphones ($vendorId)"

    fun packCommand(opCode: Int, params: ByteArray): ByteArray {
        val bodyLen = 2 + params.size
        val out = ByteArray(2 + bodyLen)
        out[0] = 0xFF.toByte()
        out[1] = bodyLen.toByte()
        out[2] = opCode.toByte()
        out[3] = params.size.toByte()
        System.arraycopy(params, 0, out, 4, params.size)
        return out
    }

    /**
     * Command 0x10 — Sleep Mode (ignores lateral touch controls on the earbuds).
     * 0x01 = Enable (ignore touches), 0x02 = Disable (normal touch controls).
     */
    fun buildSleepModeCommand(enabled: Boolean): ByteArray {
        val state: Byte = if (enabled) 0x01 else 0x02
        return packCommand(0x10, byteArrayOf(state))
    }

    /**
     * Command 0x09 — Low Latency (Gaming Mode).
     * 0x01 = Enable, 0x02 = Disable.
     */
    fun buildLowLatencyCommand(enabled: Boolean): ByteArray {
        val state: Byte = if (enabled) 0x01 else 0x02
        return packCommand(0x09, byteArrayOf(state))
    }

    /**
     * Command 0x23 — LDAC High-Res Codec Switch.
     * 0x01 = Enable, 0x02 = Disable.
     */
    fun buildLdacCommand(enabled: Boolean): ByteArray {
        val state: Byte = if (enabled) 0x01 else 0x02
        return packCommand(0x23, byteArrayOf(state))
    }

    /**
     * Command 0x24 — Dual Device Connection (Multipoint).
     * 0x01 = Enable, 0x02 = Disable.
     */
    fun buildDualConnectionCommand(enabled: Boolean): ByteArray {
        val state: Byte = if (enabled) 0x01 else 0x02
        return packCommand(0x24, byteArrayOf(state))
    }

    /**
     * Command 0x05 — Light Flash (used when finding earbuds).
     * 0x01 = Flashing on, 0x00 = Off.
     */
    fun buildLightFlashCommand(enabled: Boolean): ByteArray {
        val state: Byte = if (enabled) 0x01 else 0x00
        return packCommand(0x05, byteArrayOf(state))
    }

    /**
     * Command 0x3D — Tone Play (triggers built-in notification/search beep on earbuds).
     */
    fun buildTonePlayCommand(toneId: Int = 1): ByteArray {
        return packCommand(0x3D, byteArrayOf((toneId and 0xFF).toByte()))
    }

    /**
     * Command 0xFE — Request current value for any command ID.
     */
    fun buildRequestDataCommand(cmdId: Int): ByteArray {
        return packCommand(0xFE, byteArrayOf((cmdId and 0xFF).toByte()))
    }

    /**
     * Builds ANC command:
     * - For cmdId <= 0xFF: legacy command 0x0C ([0x0C, 0x01, mode])
     * - For cmdId > 0xFF (MeloBuds Pro etc.): command 0x17 ([0x17, 0x03, mode, subScene, noiseValue])
     */
    fun buildAncCommand(cmdId: Int): ByteArray {
        return if (cmdId <= 0xFF) {
            packCommand(0x0C, byteArrayOf((cmdId and 0xFF).toByte()))
        } else {
            val mode = ((cmdId ushr 16) and 0xFF).toByte()
            val subScene = ((cmdId ushr 8) and 0xFF).toByte()
            val noiseValue = (cmdId and 0xFF).toByte()
            packCommand(0x17, byteArrayOf(mode, subScene, noiseValue))
        }
    }

    fun modeFromCmdId(cmdId: Int): AncCycleMode = when {
        cmdId == 0 || cmdId == 131072 -> AncCycleMode.OFF
        cmdId == 3 || cmdId in 196864..196870 -> AncCycleMode.AMBIENT
        else -> AncCycleMode.ANC
    }

    /**
     * Key Function direct write payload (characteristic 0000000D, without 0xFF framing):
     * [keyId1, funcId1, keyId2, funcId2, ...]
     */
    fun buildKeyFunctionData(pairs: List<Pair<Int, Int>>): ByteArray {
        val out = ByteArray(pairs.size * 2)
        pairs.forEachIndexed { idx, (keyId, funcId) ->
            out[idx * 2] = (keyId and 0xFF).toByte()
            out[idx * 2 + 1] = (funcId and 0xFF).toByte()
        }
        return out
    }

    fun parseKeyFunctionPairs(bytes: ByteArray?): Map<Int, Int> {
        if (bytes == null || bytes.size < 2) return emptyMap()
        val out = LinkedHashMap<Int, Int>(bytes.size / 2)
        var i = 0
        while (i + 1 < bytes.size) {
            val keyId = bytes[i].toInt() and 0xFF
            val funcId = bytes[i + 1].toInt() and 0xFF
            out[keyId] = funcId
            i += 2
        }
        return out
    }

    fun parseBatteryBytes(bytes: ByteArray?): QcyBattery? {
        if (bytes == null || bytes.size < 3) return null
        val l = bytes[0].toInt() and 0xFF
        val r = bytes[1].toInt() and 0xFF
        val c = bytes[2].toInt() and 0xFF
        return QcyBattery(
            left = l and 0x7F,
            right = r and 0x7F,
            caseLevel = c and 0x7F,
            leftCharging = (l and 0x80) != 0,
            rightCharging = (r and 0x80) != 0,
            caseCharging = (c and 0x80) != 0
        )
    }

    fun parseVersionBytes(bytes: ByteArray?): String? {
        if (bytes == null || bytes.size < 3) return null
        val left = "${bytes[0].toInt() and 0xFF}.${bytes[1].toInt() and 0xFF}.${bytes[2].toInt() and 0xFF}"
        return if (bytes.size >= 6) {
            val right = "${bytes[3].toInt() and 0xFF}.${bytes[4].toInt() and 0xFF}.${bytes[5].toInt() and 0xFF}"
            if (left == right) left else "$left / $right"
        } else {
            left
        }
    }

    fun parseManufacturerData(data: ByteArray?): QcyAdvertisement? {
        if (data == null || data.size < 17) return null
        val vendorId = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        val l = data[5].toInt() and 0xFF
        val r = data[6].toInt() and 0xFF
        val c = data[7].toInt() and 0xFF
        val battery = QcyBattery(
            left = l and 0x7F,
            right = r and 0x7F,
            caseLevel = c and 0x7F,
            leftCharging = (l and 0x80) != 0,
            rightCharging = (r and 0x80) != 0,
            caseCharging = (c and 0x80) != 0
        )
        val controlMac = "%02X:%02X:%02X:%02X:%02X:%02X".format(
            data[12].toInt() and 0xFF,
            data[11].toInt() and 0xFF,
            data[13].toInt() and 0xFF,
            data[16].toInt() and 0xFF,
            data[15].toInt() and 0xFF,
            data[14].toInt() and 0xFF
        )
        val otherMac = if (data.size >= 24) {
            "%02X:%02X:%02X:%02X:%02X:%02X".format(
                data[19].toInt() and 0xFF,
                data[18].toInt() and 0xFF,
                data[20].toInt() and 0xFF,
                data[23].toInt() and 0xFF,
                data[22].toInt() and 0xFF,
                data[21].toInt() and 0xFF
            ).takeUnless { it == "00:00:00:00:00:00" }.orEmpty()
        } else {
            ""
        }
        return QcyAdvertisement(
            vendorId = vendorId,
            modelTitle = modelTitleForVendor(vendorId),
            battery = battery,
            controlMac = controlMac,
            otherMac = otherMac
        )
    }
}
