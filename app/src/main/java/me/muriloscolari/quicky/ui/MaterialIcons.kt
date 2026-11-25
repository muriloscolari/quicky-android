package me.muriloscolari.quicky.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight Material Symbols Rounded icons implemented as pure ImageVectors
 * so Release APKs remain compact without pulling in material-icons-extended.
 */
internal object QuickyIcons {

    private fun icon(name: String, pathData: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(pathData).build()

    private val fill = SolidColor(Color.Black)

    val Headphones: ImageVector by lazy {
        icon("Headphones") {
            path(fill = fill) {
                moveTo(12f, 3f)
                curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
                verticalLineTo(17f)
                curveTo(3f, 18.66f, 4.34f, 20f, 6f, 20f)
                horizontalLineTo(9f)
                verticalLineTo(12f)
                horizontalLineTo(5f)
                curveTo(5f, 8.13f, 8.13f, 5f, 12f, 5f)
                curveTo(15.87f, 5f, 19f, 8.13f, 19f, 12f)
                horizontalLineTo(15f)
                verticalLineTo(20f)
                horizontalLineTo(18f)
                curveTo(19.66f, 20f, 21f, 18.66f, 21f, 17f)
                verticalLineTo(12f)
                curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
                close()
            }
        }
    }

    val Equalizer: ImageVector by lazy {
        icon("Equalizer") {
            path(fill = fill) {
                moveTo(10f, 20f)
                horizontalLineTo(14f)
                verticalLineTo(4f)
                horizontalLineTo(10f)
                verticalLineTo(20f)
                close()
                moveTo(4f, 20f)
                horizontalLineTo(8f)
                verticalLineTo(12f)
                horizontalLineTo(4f)
                verticalLineTo(20f)
                close()
                moveTo(16f, 9f)
                verticalLineTo(20f)
                horizontalLineTo(20f)
                verticalLineTo(9f)
                horizontalLineTo(16f)
                close()
            }
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            path(fill = fill) {
                moveTo(19.14f, 12.94f)
                curveTo(19.18f, 12.64f, 19.2f, 12.33f, 19.2f, 12f)
                curveTo(19.2f, 11.68f, 19.18f, 11.36f, 19.13f, 11.06f)
                lineTo(21.16f, 9.48f)
                curveTo(21.34f, 9.34f, 21.39f, 9.07f, 21.28f, 8.87f)
                lineTo(19.36f, 5.55f)
                curveTo(19.24f, 5.33f, 18.99f, 5.26f, 18.77f, 5.33f)
                lineTo(16.38f, 6.29f)
                curveTo(15.88f, 5.91f, 15.35f, 5.59f, 14.76f, 5.35f)
                lineTo(14.4f, 2.81f)
                curveTo(14.36f, 2.57f, 14.16f, 2.4f, 13.92f, 2.4f)
                horizontalLineTo(10.08f)
                curveTo(9.84f, 2.4f, 9.65f, 2.57f, 9.61f, 2.81f)
                lineTo(9.25f, 5.35f)
                curveTo(8.66f, 5.59f, 8.12f, 5.92f, 7.63f, 6.29f)
                lineTo(5.24f, 5.33f)
                curveTo(5.02f, 5.25f, 4.77f, 5.33f, 4.65f, 5.55f)
                lineTo(2.74f, 8.87f)
                curveTo(2.62f, 9.08f, 2.66f, 9.34f, 2.86f, 9.48f)
                lineTo(4.89f, 11.06f)
                curveTo(4.84f, 11.36f, 4.8f, 11.69f, 4.8f, 12f)
                curveTo(4.8f, 12.31f, 4.82f, 12.64f, 4.87f, 12.94f)
                lineTo(2.84f, 14.52f)
                curveTo(2.66f, 14.66f, 2.61f, 14.93f, 2.72f, 15.13f)
                lineTo(4.64f, 18.45f)
                curveTo(4.76f, 18.67f, 5.01f, 18.74f, 5.23f, 18.67f)
                lineTo(7.62f, 17.71f)
                curveTo(8.12f, 18.09f, 8.65f, 18.41f, 9.24f, 18.65f)
                lineTo(9.6f, 21.19f)
                curveTo(9.65f, 21.43f, 9.84f, 21.6f, 10.08f, 21.6f)
                horizontalLineTo(13.92f)
                curveTo(14.16f, 21.6f, 14.36f, 21.43f, 14.39f, 21.19f)
                lineTo(14.75f, 18.65f)
                curveTo(15.34f, 18.41f, 15.88f, 18.09f, 16.37f, 17.71f)
                lineTo(18.76f, 18.67f)
                curveTo(18.98f, 18.75f, 19.23f, 18.67f, 19.35f, 18.45f)
                lineTo(21.27f, 15.13f)
                curveTo(21.39f, 14.91f, 21.34f, 14.66f, 21.15f, 14.52f)
                lineTo(19.14f, 12.94f)
                close()
                moveTo(12f, 15.6f)
                curveTo(10.02f, 15.6f, 8.4f, 13.98f, 8.4f, 12f)
                curveTo(8.4f, 10.02f, 10.02f, 8.4f, 12f, 8.4f)
                curveTo(13.98f, 8.4f, 15.6f, 10.02f, 15.6f, 12f)
                curveTo(15.6f, 13.98f, 13.98f, 15.6f, 12f, 15.6f)
                close()
            }
        }
    }

    val Refresh: ImageVector by lazy {
        icon("Refresh") {
            path(fill = fill) {
                moveTo(17.65f, 6.35f)
                curveTo(16.2f, 4.9f, 14.21f, 4f, 12f, 4f)
                curveTo(7.58f, 4f, 4.01f, 7.58f, 4.01f, 12f)
                curveTo(4.01f, 16.42f, 7.58f, 20f, 12f, 20f)
                curveTo(15.73f, 20f, 18.84f, 17.45f, 19.73f, 14f)
                horizontalLineTo(17.65f)
                curveTo(16.83f, 16.33f, 14.61f, 18f, 12f, 18f)
                curveTo(8.69f, 18f, 6f, 15.31f, 6f, 12f)
                curveTo(6f, 8.69f, 8.69f, 6f, 12f, 6f)
                curveTo(13.66f, 6f, 15.14f, 6.69f, 16.22f, 7.78f)
                lineTo(13f, 11f)
                horizontalLineTo(20f)
                verticalLineTo(4f)
                lineTo(17.65f, 6.35f)
                close()
            }
        }
    }

    val Bluetooth: ImageVector by lazy {
        icon("Bluetooth") {
            path(fill = fill) {
                moveTo(17.71f, 7.71f)
                lineTo(12f, 2f)
                horizontalLineTo(11f)
                verticalLineTo(9.59f)
                lineTo(6.41f, 5f)
                lineTo(5f, 6.41f)
                lineTo(10.59f, 12f)
                lineTo(5f, 17.59f)
                lineTo(6.41f, 19f)
                lineTo(11f, 14.41f)
                verticalLineTo(22f)
                horizontalLineTo(12f)
                lineTo(17.71f, 16.29f)
                lineTo(13.41f, 12f)
                lineTo(17.71f, 7.71f)
                close()
                moveTo(13f, 5.83f)
                lineTo(14.88f, 7.71f)
                lineTo(13f, 9.59f)
                verticalLineTo(5.83f)
                close()
                moveTo(14.88f, 16.29f)
                lineTo(13f, 18.17f)
                verticalLineTo(14.41f)
                lineTo(14.88f, 16.29f)
                close()
            }
        }
    }

    val NoiseCancel: ImageVector by lazy {
        icon("NoiseCancel") {
            path(fill = fill) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 20f)
                curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
                curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
                curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f)
                curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f)
                close()
                moveTo(12f, 7f)
                curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f)
                curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f)
                curveTo(14.76f, 17f, 17f, 14.76f, 17f, 12f)
                curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f)
                close()
                moveTo(12f, 15f)
                curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
                curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f)
                curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
                curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
                close()
            }
        }
    }

    val Bedtime: ImageVector by lazy {
        icon("Bedtime") {
            path(fill = fill) {
                moveTo(12.34f, 2.02f)
                curveTo(6.59f, 1.82f, 2f, 6.42f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(15.71f, 22f, 18.94f, 19.98f, 20.66f, 16.99f)
                curveTo(21.02f, 16.36f, 20.52f, 15.58f, 19.8f, 15.68f)
                curveTo(19.22f, 15.76f, 18.62f, 15.79f, 18f, 15.76f)
                curveTo(13.66f, 15.53f, 10.22f, 11.96f, 10.17f, 7.62f)
                curveTo(10.15f, 5.96f, 10.67f, 4.42f, 11.57f, 3.15f)
                curveTo(11.99f, 2.56f, 13.06f, 2.05f, 12.34f, 2.02f)
                close()
            }
        }
    }

    val Lock: ImageVector by lazy {
        icon("Lock") {
            path(fill = fill) {
                moveTo(18f, 8f)
                horizontalLineTo(17f)
                verticalLineTo(6f)
                curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
                curveTo(9.24f, 1f, 7f, 3.24f, 7f, 6f)
                verticalLineTo(8f)
                horizontalLineTo(6f)
                curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
                verticalLineTo(20f)
                curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
                horizontalLineTo(18f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                verticalLineTo(10f)
                curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f)
                close()
                moveTo(12f, 17f)
                curveTo(10.9f, 17f, 10f, 16.1f, 10f, 15f)
                curveTo(10f, 13.9f, 10.9f, 13f, 12f, 13f)
                curveTo(13.1f, 13f, 14f, 13.9f, 14f, 15f)
                curveTo(14f, 16.1f, 13.1f, 17f, 12f, 17f)
                close()
                moveTo(15.1f, 8f)
                horizontalLineTo(8.9f)
                verticalLineTo(6f)
                curveTo(8.9f, 4.29f, 10.29f, 2.9f, 12f, 2.9f)
                curveTo(13.71f, 2.9f, 15.1f, 4.29f, 15.1f, 6f)
                verticalLineTo(8f)
                close()
            }
        }
    }

    val Gamepad: ImageVector by lazy {
        icon("Gamepad") {
            path(fill = fill) {
                moveTo(21f, 6f)
                horizontalLineTo(3f)
                curveTo(1.9f, 6f, 1f, 6.9f, 1f, 8f)
                verticalLineTo(16f)
                curveTo(1f, 17.1f, 1.9f, 18f, 3f, 18f)
                horizontalLineTo(21f)
                curveTo(22.1f, 18f, 23f, 17.1f, 23f, 16f)
                verticalLineTo(8f)
                curveTo(23f, 6.9f, 22.1f, 6f, 21f, 6f)
                close()
                moveTo(11f, 13f)
                horizontalLineTo(8f)
                verticalLineTo(16f)
                horizontalLineTo(6f)
                verticalLineTo(13f)
                horizontalLineTo(3f)
                verticalLineTo(11f)
                horizontalLineTo(6f)
                verticalLineTo(8f)
                horizontalLineTo(8f)
                verticalLineTo(11f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                close()
                moveTo(15.5f, 15f)
                curveTo(14.67f, 15f, 14f, 14.33f, 14f, 13.5f)
                curveTo(14f, 12.67f, 14.67f, 12f, 15.5f, 12f)
                curveTo(16.33f, 12f, 17f, 12.67f, 17f, 13.5f)
                curveTo(17f, 14.33f, 16.33f, 15f, 15.5f, 15f)
                close()
                moveTo(19.5f, 12f)
                curveTo(18.67f, 12f, 18f, 11.33f, 18f, 10.5f)
                curveTo(18f, 9.67f, 18.67f, 9f, 19.5f, 9f)
                curveTo(20.33f, 9f, 21f, 9.67f, 21f, 10.5f)
                curveTo(21f, 11.33f, 20.33f, 12f, 19.5f, 12f)
                close()
            }
        }
    }

    val Devices: ImageVector by lazy {
        icon("Devices") {
            path(fill = fill) {
                moveTo(4f, 6f)
                horizontalLineTo(22f)
                verticalLineTo(4f)
                horizontalLineTo(4f)
                curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
                verticalLineTo(17f)
                horizontalLineTo(0f)
                verticalLineTo(20f)
                horizontalLineTo(14f)
                verticalLineTo(17f)
                horizontalLineTo(4f)
                verticalLineTo(6f)
                close()
                moveTo(23f, 8f)
                horizontalLineTo(17f)
                curveTo(16.45f, 8f, 16f, 8.45f, 16f, 9f)
                verticalLineTo(19f)
                curveTo(16f, 19.55f, 16.45f, 20f, 17f, 20f)
                horizontalLineTo(23f)
                curveTo(23.55f, 20f, 24f, 19.55f, 24f, 19f)
                verticalLineTo(9f)
                curveTo(24f, 8.45f, 23.55f, 8f, 23f, 8f)
                close()
                moveTo(22f, 17f)
                horizontalLineTo(18f)
                verticalLineTo(10f)
                horizontalLineTo(22f)
                verticalLineTo(17f)
                close()
            }
        }
    }

    val HighQuality: ImageVector by lazy {
        icon("HighQuality") {
            path(fill = fill) {
                moveTo(19f, 4f)
                horizontalLineTo(5f)
                curveTo(3.89f, 4f, 3f, 4.9f, 3f, 6f)
                verticalLineTo(18f)
                curveTo(3f, 19.1f, 3.89f, 20f, 5f, 20f)
                horizontalLineTo(19f)
                curveTo(20.1f, 20f, 21f, 19.1f, 21f, 18f)
                verticalLineTo(6f)
                curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f)
                close()
                moveTo(11f, 15f)
                horizontalLineTo(9.5f)
                verticalLineTo(13f)
                horizontalLineTo(7.5f)
                verticalLineTo(15f)
                horizontalLineTo(6f)
                verticalLineTo(9f)
                horizontalLineTo(7.5f)
                verticalLineTo(11.5f)
                horizontalLineTo(9.5f)
                verticalLineTo(9f)
                horizontalLineTo(11f)
                verticalLineTo(15f)
                close()
                moveTo(18f, 14f)
                curveTo(18f, 14.55f, 17.55f, 15f, 17f, 15f)
                horizontalLineTo(13f)
                verticalLineTo(9f)
                horizontalLineTo(17f)
                curveTo(17.55f, 9f, 18f, 9.45f, 18f, 10f)
                verticalLineTo(14f)
                close()
                moveTo(14.5f, 13.5f)
                horizontalLineTo(16.5f)
                verticalLineTo(10.5f)
                horizontalLineTo(14.5f)
                verticalLineTo(13.5f)
                close()
            }
        }
    }

    val TouchApp: ImageVector by lazy {
        icon("TouchApp") {
            path(fill = fill) {
                moveTo(9f, 11.24f)
                verticalLineTo(7.5f)
                curveTo(9f, 6.12f, 10.12f, 5f, 11.5f, 5f)
                curveTo(12.88f, 5f, 14f, 6.12f, 14f, 7.5f)
                verticalLineTo(11.24f)
                curveTo(15.21f, 10.43f, 16f, 9.06f, 16f, 7.5f)
                curveTo(16f, 5.01f, 13.99f, 3f, 11.5f, 3f)
                curveTo(9.01f, 3f, 7f, 5.01f, 7f, 7.5f)
                curveTo(7f, 9.06f, 7.79f, 10.43f, 9f, 11.24f)
                close()
                moveTo(18.84f, 15.87f)
                lineTo(14.3f, 13.61f)
                curveTo(14.13f, 13.54f, 13.95f, 13.5f, 13.76f, 13.5f)
                horizontalLineTo(13f)
                verticalLineTo(7.5f)
                curveTo(13f, 6.67f, 12.33f, 6f, 11.5f, 6f)
                curveTo(10.67f, 6f, 10f, 6.67f, 10f, 7.5f)
                verticalLineTo(18.24f)
                lineTo(6.57f, 17.52f)
                curveTo(6.16f, 17.43f, 5.73f, 17.56f, 5.44f, 17.85f)
                lineTo(4.65f, 18.65f)
                lineTo(9.59f, 23.59f)
                curveTo(9.96f, 23.96f, 10.47f, 24.17f, 11f, 24.17f)
                horizontalLineTo(17.79f)
                curveTo(18.79f, 24.17f, 19.63f, 23.43f, 19.77f, 22.44f)
                lineTo(20.52f, 17.17f)
                curveTo(20.62f, 16.45f, 20.19f, 15.76f, 18.84f, 15.87f)
                close()
            }
        }
    }

    val VolumeUp: ImageVector by lazy {
        icon("VolumeUp") {
            path(fill = fill) {
                moveTo(3f, 9f)
                verticalLineTo(15f)
                horizontalLineTo(7f)
                lineTo(12f, 20f)
                verticalLineTo(4f)
                lineTo(7f, 9f)
                horizontalLineTo(3f)
                close()
                moveTo(16.5f, 12f)
                curveTo(16.5f, 10.23f, 15.48f, 8.71f, 14f, 7.97f)
                verticalLineTo(16.02f)
                curveTo(15.48f, 15.29f, 16.5f, 13.77f, 16.5f, 12f)
                close()
                moveTo(14f, 3.23f)
                verticalLineTo(5.29f)
                curveTo(16.89f, 6.15f, 19f, 8.83f, 19f, 12f)
                curveTo(19f, 15.17f, 16.89f, 17.85f, 14f, 18.71f)
                verticalLineTo(20.77f)
                curveTo(18.01f, 19.86f, 21f, 16.28f, 21f, 12f)
                curveTo(21f, 7.72f, 18.01f, 4.14f, 14f, 3.23f)
                close()
            }
        }
    }

    val Widgets: ImageVector by lazy {
        icon("Widgets") {
            path(fill = fill) {
                moveTo(13f, 13f)
                verticalLineTo(21f)
                horizontalLineTo(21f)
                verticalLineTo(13f)
                horizontalLineTo(13f)
                close()
                moveTo(3f, 21f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                horizontalLineTo(3f)
                verticalLineTo(21f)
                close()
                moveTo(3f, 3f)
                verticalLineTo(11f)
                horizontalLineTo(11f)
                verticalLineTo(3f)
                horizontalLineTo(3f)
                close()
                moveTo(16.66f, 1.69f)
                lineTo(11f, 7.34f)
                lineTo(16.66f, 13f)
                lineTo(22.32f, 7.34f)
                lineTo(16.66f, 1.69f)
                close()
            }
        }
    }

    val Notifications: ImageVector by lazy {
        icon("Notifications") {
            path(fill = fill) {
                moveTo(12f, 22f)
                curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
                horizontalLineTo(10f)
                curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
                close()
                moveTo(18f, 16f)
                verticalLineTo(11f)
                curveTo(18f, 7.93f, 16.37f, 5.36f, 13.5f, 4.68f)
                verticalLineTo(4f)
                curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f)
                curveTo(11.17f, 2.5f, 10.5f, 3.17f, 10.5f, 4f)
                verticalLineTo(4.68f)
                curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f)
                verticalLineTo(16f)
                lineTo(4f, 18f)
                verticalLineTo(19f)
                horizontalLineTo(20f)
                verticalLineTo(18f)
                lineTo(18f, 16f)
                close()
            }
        }
    }

    val Layers: ImageVector by lazy {
        icon("Layers") {
            path(fill = fill) {
                moveTo(11.99f, 18.54f)
                lineTo(4.62f, 12.81f)
                lineTo(3f, 14.07f)
                lineTo(12f, 21.07f)
                lineTo(21f, 14.07f)
                lineTo(19.37f, 12.8f)
                lineTo(11.99f, 18.54f)
                close()
                moveTo(12f, 16f)
                lineTo(19.36f, 10.27f)
                lineTo(21f, 9f)
                lineTo(12f, 2f)
                lineTo(3f, 9f)
                lineTo(4.63f, 10.27f)
                lineTo(12f, 16f)
                close()
            }
        }
    }

    val Palette: ImageVector by lazy {
        icon("Palette") {
            path(fill = fill) {
                moveTo(12f, 3f)
                curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
                curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
                curveTo(12.83f, 21f, 13.5f, 20.33f, 13.5f, 19.5f)
                curveTo(13.5f, 19.11f, 13.35f, 18.76f, 13.11f, 18.49f)
                curveTo(12.88f, 18.23f, 12.73f, 17.88f, 12.73f, 17.5f)
                curveTo(12.73f, 16.67f, 13.4f, 16f, 14.23f, 16f)
                horizontalLineTo(16f)
                curveTo(18.76f, 16f, 21f, 13.76f, 21f, 11f)
                curveTo(21f, 6.58f, 16.97f, 3f, 12f, 3f)
                close()
                moveTo(6.5f, 12f)
                curveTo(5.67f, 12f, 5f, 11.33f, 5f, 10.5f)
                curveTo(5f, 9.67f, 5.67f, 9f, 6.5f, 9f)
                curveTo(7.33f, 9f, 8f, 9.67f, 8f, 10.5f)
                curveTo(8f, 11.33f, 7.33f, 12f, 6.5f, 12f)
                close()
                moveTo(9.5f, 8f)
                curveTo(8.67f, 8f, 8f, 7.33f, 8f, 6.5f)
                curveTo(8f, 5.67f, 8.67f, 5f, 9.5f, 5f)
                curveTo(10.33f, 5f, 11f, 5.67f, 11f, 6.5f)
                curveTo(11f, 7.33f, 10.33f, 8f, 9.5f, 8f)
                close()
                moveTo(14.5f, 8f)
                curveTo(13.67f, 8f, 13f, 7.33f, 13f, 6.5f)
                curveTo(13f, 5.67f, 13.67f, 5f, 14.5f, 5f)
                curveTo(15.33f, 5f, 16f, 5.67f, 16f, 6.5f)
                curveTo(16f, 7.33f, 15.33f, 8f, 14.5f, 8f)
                close()
                moveTo(17.5f, 12f)
                curveTo(16.67f, 12f, 16f, 11.33f, 16f, 10.5f)
                curveTo(16f, 9.67f, 16.67f, 9f, 17.5f, 9f)
                curveTo(18.33f, 9f, 19f, 9.67f, 19f, 10.5f)
                curveTo(19f, 11.33f, 18.33f, 12f, 17.5f, 12f)
                close()
            }
        }
    }

    val SystemUpdate: ImageVector by lazy {
        icon("SystemUpdate") {
            path(fill = fill) {
                moveTo(17f, 1.01f)
                lineTo(7f, 1f)
                curveTo(5.9f, 1f, 5f, 1.9f, 5f, 3f)
                verticalLineTo(21f)
                curveTo(5f, 22.1f, 5.9f, 23f, 7f, 23f)
                horizontalLineTo(17f)
                curveTo(18.1f, 23f, 19f, 22.1f, 19f, 21f)
                verticalLineTo(3f)
                curveTo(19f, 1.9f, 18.1f, 1.01f, 17f, 1.01f)
                close()
                moveTo(17f, 19f)
                horizontalLineTo(7f)
                verticalLineTo(5f)
                horizontalLineTo(17f)
                verticalLineTo(19f)
                close()
                moveTo(16f, 13f)
                horizontalLineTo(13f)
                verticalLineTo(8f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                horizontalLineTo(8f)
                lineTo(12f, 17f)
                lineTo(16f, 13f)
                close()
            }
        }
    }

    val Bolt: ImageVector by lazy {
        icon("Bolt") {
            path(fill = fill) {
                moveTo(11f, 21f)
                horizontalLineTo(10f)
                lineTo(11f, 14f)
                horizontalLineTo(7.5f)
                curveTo(6.92f, 14f, 6.93f, 13.68f, 7.12f, 13.34f)
                lineTo(13f, 3f)
                horizontalLineTo(14f)
                lineTo(13f, 10f)
                horizontalLineTo(16.5f)
                curveTo(17.08f, 10f, 17.07f, 10.33f, 16.88f, 10.66f)
                lineTo(11f, 21f)
                close()
            }
        }
    }

    val Tune: ImageVector by lazy {
        icon("Tune") {
            path(fill = fill) {
                moveTo(3f, 17f)
                verticalLineTo(19f)
                horizontalLineTo(9f)
                verticalLineTo(17f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 5f)
                verticalLineTo(7f)
                horizontalLineTo(13f)
                verticalLineTo(5f)
                horizontalLineTo(3f)
                close()
                moveTo(13f, 21f)
                verticalLineTo(19f)
                horizontalLineTo(21f)
                verticalLineTo(17f)
                horizontalLineTo(13f)
                verticalLineTo(15f)
                horizontalLineTo(11f)
                verticalLineTo(21f)
                horizontalLineTo(13f)
                close()
                moveTo(7f, 9f)
                verticalLineTo(11f)
                horizontalLineTo(3f)
                verticalLineTo(13f)
                horizontalLineTo(7f)
                verticalLineTo(15f)
                horizontalLineTo(9f)
                verticalLineTo(9f)
                horizontalLineTo(7f)
                close()
                moveTo(21f, 13f)
                verticalLineTo(11f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                horizontalLineTo(21f)
                close()
                moveTo(15f, 9f)
                horizontalLineTo(17f)
                verticalLineTo(7f)
                horizontalLineTo(21f)
                verticalLineTo(5f)
                horizontalLineTo(17f)
                verticalLineTo(3f)
                horizontalLineTo(15f)
                verticalLineTo(9f)
                close()
            }
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") {
            path(fill = fill) {
                moveTo(9f, 16.17f)
                lineTo(4.83f, 12f)
                lineTo(3.41f, 13.41f)
                lineTo(9f, 19f)
                lineTo(21f, 7f)
                lineTo(19.59f, 5.59f)
                lineTo(9f, 16.17f)
                close()
            }
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight") {
            path(fill = fill) {
                moveTo(10f, 6f)
                lineTo(8.59f, 7.41f)
                lineTo(13.17f, 12f)
                lineTo(8.59f, 16.59f)
                lineTo(10f, 18f)
                lineTo(16f, 12f)
                lineTo(10f, 6f)
                close()
            }
        }
    }

    val Shield: ImageVector by lazy {
        icon("Shield") {
            path(fill = fill) {
                moveTo(12f, 1f)
                lineTo(3f, 5f)
                verticalLineTo(11f)
                curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
                curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
                verticalLineTo(5f)
                lineTo(12f, 1f)
                close()
                moveTo(10f, 17f)
                lineTo(6f, 13f)
                lineTo(7.41f, 11.59f)
                lineTo(10f, 14.17f)
                lineTo(16.59f, 7.58f)
                lineTo(18f, 9f)
                lineTo(10f, 17f)
                close()
            }
        }
    }

    val LinkOff: ImageVector by lazy {
        icon("LinkOff") {
            path(fill = fill) {
                moveTo(17f, 7f)
                horizontalLineTo(13f)
                verticalLineTo(8.9f)
                horizontalLineTo(17f)
                curveTo(18.71f, 8.9f, 20.1f, 10.29f, 20.1f, 12f)
                curveTo(20.1f, 13.71f, 18.71f, 15.1f, 17f, 15.1f)
                horizontalLineTo(13f)
                verticalLineTo(17f)
                horizontalLineTo(17f)
                curveTo(19.76f, 17f, 22f, 14.76f, 22f, 12f)
                curveTo(22f, 9.24f, 19.76f, 7f, 17f, 7f)
                close()
                moveTo(11f, 15.1f)
                horizontalLineTo(7f)
                curveTo(5.29f, 15.1f, 3.9f, 13.71f, 3.9f, 12f)
                curveTo(3.9f, 10.29f, 5.29f, 8.9f, 7f, 8.9f)
                horizontalLineTo(11f)
                verticalLineTo(7f)
                horizontalLineTo(7f)
                curveTo(4.24f, 7f, 2f, 9.24f, 2f, 12f)
                curveTo(2f, 14.76f, 4.24f, 17f, 7f, 17f)
                horizontalLineTo(11f)
                verticalLineTo(15.1f)
                close()
                moveTo(8f, 11f)
                horizontalLineTo(16f)
                verticalLineTo(13f)
                horizontalLineTo(8f)
                verticalLineTo(11f)
                close()
            }
        }
    }
}
