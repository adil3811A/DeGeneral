package com.example.de_general.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The icons these screens need, built from Material Symbols Outlined path data.
 *
 * Carried here rather than pulled from a library on purpose: `material-icons-extended` stopped at
 * Compose 1.7.3 and this project is on 1.12, and material3 brings in no icons of its own. Two dozen
 * glyphs is a smaller thing to own than an unmaintained dependency or a multi-megabyte icon font.
 *
 * Path data is verbatim from google/material-design-icons (Apache 2.0). The string literals are
 * split at fixed column positions rather than on whitespace — the spaces are significant to the
 * path grammar, so a word-wrapping split silently corrupts the shape.
 *
 * Material Symbols draw on a 960x960 grid whose origin sits at the *bottom* left
 * (`viewBox="0 -960 960 960"`), which [ImageVector] has no field for — hence the translated group
 * in [symbol]. Miss that and every icon renders just off the top of its box, which is the usual
 * way this bites.
 */
object MindfulIcons

/**
 * Vectors are cached because each one parses a path string, and these sit inside rows that
 * recompose on every download progress tick.
 */
private val cache = mutableMapOf<String, ImageVector>()

private fun symbol(name: String, pathData: String): ImageVector = cache.getOrPut(name) {
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    ).apply {
        group(translationY = 960f) {
            // In Compose Multiplatform, addPath takes the parsed nodes inside its pathData parameter
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                fill = SolidColor(Color.Black)
            )
        }
    }.build()
}


val MindfulIcons.VerifiedUser: ImageVector
    get() = symbol(
        name = "verified_user",
        pathData =
            "m438-338 226-226-57-57-169 169-84-84-57 57 141 141Zm42 258q-139-35-229.5-159.5T160-516" +
            "v-244l320-120 320 120v244q0 152-90.5 276.5T480-80Zm0-84q104-33 172-132t68-220v-189l-24" +
            "0-90-240 90v189q0 121 68 220t172 132Zm0-316Z",
    )

val MindfulIcons.Lock: ImageVector
    get() = symbol(
        name = "lock",
        pathData =
            "M240-80q-33 0-56.5-23.5T160-160v-400q0-33 23.5-56.5T240-640h40v-80q0-83 58.5-141.5T480" +
            "-920q83 0 141.5 58.5T680-720v80h40q33 0 56.5 23.5T800-560v400q0 33-23.5 56.5T720-80H24" +
            "0Zm0-80h480v-400H240v400Zm240-120q33 0 56.5-23.5T560-360q0-33-23.5-56.5T480-440q-33 0-" +
            "56.5 23.5T400-360q0 33 23.5 56.5T480-280ZM360-640h240v-80q0-50-35-85t-85-35q-50 0-85 3" +
            "5t-35 85v80ZM240-160v-400 400Z",
    )

val MindfulIcons.Memory: ImageVector
    get() = symbol(
        name = "memory",
        pathData =
            "M360-360v-240h240v240H360Zm80-80h80v-80h-80v80Zm-80 320v-80h-80q-33 0-56.5-23.5T200-28" +
            "0v-80h-80v-80h80v-80h-80v-80h80v-80q0-33 23.5-56.5T280-760h80v-80h80v80h80v-80h80v80h8" +
            "0q33 0 56.5 23.5T760-680v80h80v80h-80v80h80v80h-80v80q0 33-23.5 56.5T680-200h-80v80h-8" +
            "0v-80h-80v80h-80Zm320-160v-400H280v400h400ZM480-480Z",
    )

val MindfulIcons.DeveloperBoard: ImageVector
    get() = symbol(
        name = "developer_board",
        pathData =
            "M160-120q-33 0-56.5-23.5T80-200v-560q0-33 23.5-56.5T160-840h560q33 0 56.5 23.5T800-760" +
            "v80h80v80h-80v80h80v80h-80v80h80v80h-80v80q0 33-23.5 56.5T720-120H160Zm0-80h560v-560H1" +
            "60v560Zm80-80h200v-160H240v160Zm240-280h160v-120H480v120Zm-240 80h200v-200H240v200Zm24" +
            "0 200h160v-240H480v240ZM160-760v560-560Z",
    )

val MindfulIcons.HardDrive: ImageVector
    get() = symbol(
        name = "hard_drive",
        pathData =
            "M160-280h640v-240H160v240Zm520-60q25 0 42.5-17.5T740-400q0-25-17.5-42.5T680-460q-25 0-" +
            "42.5 17.5T620-400q0 25 17.5 42.5T680-340Zm200-260H767l-80-80H273l-80 80H80l137-137q11-" +
            "11 25.5-17t30.5-6h414q16 0 30.5 6t25.5 17l137 137ZM160-200q-33 0-56.5-23.5T80-280v-320" +
            "h800v320q0 33-23.5 56.5T800-200H160Z",
    )

val MindfulIcons.Thermostat: ImageVector
    get() = symbol(
        name = "thermostat",
        pathData =
            "M520-520v-80h200v80H520Zm0-160v-80h320v80H520ZM320-120q-83 0-141.5-58.5T120-320q0-48 2" +
            "1-89.5t59-70.5v-240q0-50 35-85t85-35q50 0 85 35t35 85v240q38 29 59 70.5t21 89.5q0 83-5" +
            "8.5 141.5T320-120ZM200-320h240q0-29-12.5-54T392-416l-32-24v-280q0-17-11.5-28.5T320-760" +
            "q-17 0-28.5 11.5T280-720v280l-32 24q-23 17-35.5 42T200-320Z",
    )

val MindfulIcons.CheckCircle: ImageVector
    get() = symbol(
        name = "check_circle",
        pathData =
            "m424-296 282-282-56-56-226 226-114-114-56 56 170 170Zm56 216q-83 0-156-31.5T197-197q-5" +
            "4-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-76" +
            "3q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-" +
            "93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z",
    )

val MindfulIcons.Warning: ImageVector
    get() = symbol(
        name = "warning",
        pathData =
            "m40-120 440-760 440 760H40Zm138-80h604L480-720 178-200Zm302-40q17 0 28.5-11.5T520-280q" +
            "0-17-11.5-28.5T480-320q-17 0-28.5 11.5T440-280q0 17 11.5 28.5T480-240Zm-40-120h80v-200" +
            "h-80v200Zm40-100Z",
    )

val MindfulIcons.ErrorCircle: ImageVector
    get() = symbol(
        name = "error",
        pathData =
            "M480-280q17 0 28.5-11.5T520-320q0-17-11.5-28.5T480-360q-17 0-28.5 11.5T440-320q0 17 11" +
            ".5 28.5T480-280Zm-40-160h80v-240h-80v240Zm40 360q-83 0-156-31.5T197-197q-54-54-85.5-12" +
            "7T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5" +
            " 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-" +
            "134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z",
    )

val MindfulIcons.ArrowForward: ImageVector
    get() = symbol(
        name = "arrow_forward",
        pathData =
            "M647-440H160v-80h487L423-744l57-56 320 320-320 320-57-56 224-224Z",
    )

val MindfulIcons.ChevronRight: ImageVector
    get() = symbol(
        name = "chevron_right",
        pathData =
            "M504-480 320-664l56-56 240 240-240 240-56-56 184-184Z",
    )

val MindfulIcons.Download: ImageVector
    get() = symbol(
        name = "download",
        pathData =
            "M480-320 280-520l56-58 104 104v-326h80v326l104-104 56 58-200 200ZM240-160q-33 0-56.5-2" +
            "3.5T160-240v-120h80v120h480v-120h80v120q0 33-23.5 56.5T720-160H240Z",
    )

val MindfulIcons.Schedule: ImageVector
    get() = symbol(
        name = "schedule",
        pathData =
            "m612-292 56-56-148-148v-184h-80v216l172 172ZM480-80q-83 0-156-31.5T197-197q-54-54-85.5" +
            "-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 8" +
            "5.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-400Zm0 320q133 0 226.5" +
            "-93.5T800-480q0-133-93.5-226.5T480-800q-133 0-226.5 93.5T160-480q0 133 93.5 226.5T480-" +
            "160Z",
    )

val MindfulIcons.PieChart: ImageVector
    get() = symbol(
        name = "pie_chart",
        pathData =
            "M520-520h278q-15-110-91.5-186.5T520-798v278Zm-80 358v-636q-121 15-200.5 105.5T160-480q" +
            "0 122 79.5 212.5T440-162Zm80 0q110-14 187-91t91-187H520v278Zm-40-318Zm0 400q-83 0-156-" +
            "31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 " +
            "155.5 31.5t127 86q54.5 54.5 86 127T880-480q0 82-31.5 155T763-197.5q-54 54.5-127 86T480" +
            "-80Z",
    )

val MindfulIcons.BatteryCharging: ImageVector
    get() = symbol(
        name = "battery_charging_full",
        pathData =
            "M660-80v-120H560l140-200v120h100L660-80Zm-300-80Zm-40 80q-17 0-28.5-11.5T280-120v-640q" +
            "0-17 11.5-28.5T320-800h80v-80h160v80h80q17 0 28.5 11.5T680-760v280q-21 0-41 3.5T600-46" +
            "6v-254H360v560h94q8 23 19.5 43T501-80H320Z",
    )

val MindfulIcons.Lightbulb: ImageVector
    get() = symbol(
        name = "lightbulb",
        pathData =
            "M480-80q-33 0-56.5-23.5T400-160h160q0 33-23.5 56.5T480-80ZM320-200v-80h320v80H320Zm10-" +
            "120q-69-41-109.5-110T180-580q0-125 87.5-212.5T480-880q125 0 212.5 87.5T780-580q0 81-40" +
            ".5 150T630-320H330Zm24-80h252q45-32 69.5-79T700-580q0-92-64-156t-156-64q-92 0-156 64t-" +
            "64 156q0 54 24.5 101t69.5 79Zm126 0Z",
    )

val MindfulIcons.PauseCircle: ImageVector
    get() = symbol(
        name = "pause_circle",
        pathData =
            "M360-320h80v-320h-80v320Zm160 0h80v-320h-80v320ZM480-80q-83 0-156-31.5T197-197q-54-54-" +
            "85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 " +
            "54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93" +
            "-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z",
    )

val MindfulIcons.PlayCircle: ImageVector
    get() = symbol(
        name = "play_circle",
        pathData =
            "m380-300 280-180-280-180v360ZM480-80q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-8" +
            "3 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480" +
            "q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-" +
            "227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z",
    )

val MindfulIcons.WifiOff: ImageVector
    get() = symbol(
        name = "wifi_off",
        pathData =
            "M790-56 414-434q-47 11-87.5 33T254-346l-84-86q32-32 69-56t79-42l-90-90q-41 21-76.5 46." +
            "5T84-516L0-602q32-32 66.5-57.5T140-708l-84-84 56-56 736 736-58 56Zm-310-64q-42 0-71-29" +
            ".5T380-220q0-42 29-71t71-29q42 0 71 29t29 71q0 41-29 70.5T480-120Zm236-238-29-29-29-29" +
            "-144-144q81 8 151.5 41T790-432l-74 74Zm160-158q-77-77-178.5-120.5T480-680q-21 0-40.5 1" +
            ".5T400-674L298-776q44-12 89.5-18t92.5-6q142 0 265 53t215 145l-84 86Z",
    )

val MindfulIcons.Check: ImageVector
    get() = symbol(
        name = "check",
        pathData =
            "M382-240 154-468l57-57 171 171 367-367 57 57-424 424Z",
    )

val MindfulIcons.ChatBubble: ImageVector
    get() = symbol(
        name = "chat_bubble",
        pathData =
            "M80-80v-720q0-33 23.5-56.5T160-880h640q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800" +
            "-240H240L80-80Zm126-240h594v-480H160v525l46-45Zm-46 0v-480 480Z",
    )

val MindfulIcons.AutoStories: ImageVector
    get() = symbol(
        name = "auto_stories",
        pathData =
            "M480-160q-48-38-104-59t-116-21q-42 0-82.5 11T100-198q-21 11-40.5-1T40-234v-482q0-11 " +
            "5.5-21T62-752q46-24 96-36t102-12q58 0 113.5 15T480-740v484q51-32 107-48t113-16q36 0 " +
            "70.5 6t69.5 18v-480q15 5 29.5 10.5T898-752q11 5 16.5 15t5.5 21v482q0 23-19.5 35t-40." +
            "5 1q-37-20-77.5-31T700-240q-60 0-116 21t-104 59Zm80-200v-380l200-200v400L560-360Zm-1" +
            "60 65v-396q-33-14-68.5-21.5T260-720q-37 0-72 7t-68 21v397q35-13 69.5-19t70.5-6q36 0 " +
            "70.5 6t69.5 19Zm0 0v-396 396Z",
    )

val MindfulIcons.Settings: ImageVector
    get() = symbol(
        name = "settings",
        pathData =
            "m370-80-16-128q-13-5-24.5-12T307-235l-119 50L78-375l103-78q-1-7-1-13.5v-27q0-6.5 1-1" +
            "3.5L78-585l110-190 119 50q11-8 23-15t24-12l16-128h220l16 128q13 5 24.5 12t22.5 15l11" +
            "9-50 110 190-103 78q1 7 1 13.5v27q0 6.5-2 13.5l103 78-110 190-118-50q-11 8-23 15t-24" +
            " 12L590-80H370Zm70-80h79l14-106q31-8 57.5-23.5T639-327l99 41 39-68-86-65q5-14 7-29.5" +
            "t2-31.5q0-16-2-31.5t-7-29.5l86-65-39-68-99 42q-22-23-48.5-38.5T533-694l-13-106h-79l-" +
            "14 106q-31 8-57.5 23.5T321-633l-99-41-39 68 86 64q-5 15-7 30t-2 32q0 16 2 31t7 30l-8" +
            "6 65 39 68 99-42q22 23 48.5 38.5T427-266l13 106Zm42-180q58 0 99-41t41-99q0-58-41-99t" +
            "-99-41q-59 0-99.5 41T342-480q0 58 40.5 99t99.5 41Zm-2-140Z",
    )

val MindfulIcons.Edit: ImageVector
    get() = symbol(
        name = "edit",
        pathData =
            "M200-200h57l391-391-57-57-391 391v57Zm-80 80v-170l528-527q12-11 26.5-17t30.5-6q16 0 " +
            "31 6t26 18l55 56q12 11 17.5 26t5.5 30q0 16-5.5 30.5T817-647L290-120H120Zm640-584-56-" +
            "56 56 56Zm-141 85-28-29 57 57-29-28Z",
    )
