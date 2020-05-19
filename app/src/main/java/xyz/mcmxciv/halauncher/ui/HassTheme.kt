package xyz.mcmxciv.halauncher.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import org.json.JSONException
import org.json.JSONObject
import xyz.mcmxciv.halauncher.LauncherApplication
import xyz.mcmxciv.halauncher.R
import xyz.mcmxciv.halauncher.utils.ResourceProvider
import javax.inject.Inject

class HassTheme private constructor(
    val primaryTextColor: Int,
    val secondaryTextColor: Int,
    val textPrimaryColor: Int,
    val disabledTextColor: Int,
    val primaryColor: Int,
    val darkPrimaryColor: Int,
    val lightPrimaryColor: Int,
    val accentColor: Int,
    val errorColor: Int,
    val cardBackgroundColor: Int,
    val primaryBackgroundColor: Int,
    val secondaryBackgroundColor: Int
) {
    @Inject
    lateinit var resourceProvider: ResourceProvider

    private val inputStateList: ColorStateList

    val appListBackground: Drawable
        get() {
            val drawable = resourceProvider.getDrawable(R.drawable.top_rounded_background)?.mutate()
            drawable?.setTint(secondaryBackgroundColor)
            return drawable!!
        }

    init {
        LauncherApplication.instance.component.inject(this)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_pressed),
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_focused, android.R.attr.state_pressed),
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf()
        )

        val colors = intArrayOf(
            accentColor,
            primaryTextColor,
            accentColor,
            ColorUtils.setAlphaComponent(primaryTextColor, 192),
            primaryTextColor
        )

        inputStateList = ColorStateList(states, colors)
    }

    companion object {
        private val hexPattern = Regex("^(#[a-fA-F0-9]{6}|#[a-fA-F0-9]{3})\$")
        private val hexAlphaPattern = Regex("^(#[a-fA-F0-9]{8}|#[a-fA-F0-9]{4})\$")
        private val rgbPattern = Regex(
            "rgb *\\( *([0-9]{1,3})%? *, *([0-9]{1,3})%? *, *([0-9]{1,3})%? *\\)"
        )
        private val rgbaPattern = Regex(
            """
                rgba *\( *([0-9]{1,3}%?) *,
                 *([0-9]{1,3}%?) *, *([0-9]{1,3}%?) *,
                 *([0-9]{1,3}%|0?\.[0-9]+|[01]) *\)
            """.trimIndent()
        )
        private val varPattern = Regex("^var\\(--(\\S+)\\)\$")

        fun createDefaultTheme(context: Context): HassTheme =
            HassTheme(
                context.getColor(R.color.primary_text_color),
                context.getColor(R.color.secondary_text_color),
                context.getColor(R.color.text_primary_color),
                context.getColor(R.color.disabled_text_color),
                context.getColor(R.color.primary_color),
                context.getColor(R.color.dark_primary_color),
                context.getColor(R.color.light_primary_color),
                context.getColor(R.color.accent_color),
                context.getColor(R.color.error_color),
                context.getColor(R.color.card_background_color),
                context.getColor(R.color.primary_background_color),
                context.getColor(R.color.secondary_background_color)
            )

        fun createFromString(theme: String, context: Context): HassTheme {
            val default = createDefaultTheme(context)
            val json = try {
                JSONObject(theme)
            } catch (ex: JSONException) {
                JSONObject()
            }

            val styles = if (json.has("styles"))
                    json.getJSONObject("styles")
                else JSONObject()

            return HassTheme(
                styles.getColor("primary-text-color", default.primaryTextColor),
                styles.getColor("secondary-text-color", default.secondaryTextColor),
                styles.getColor("text-primary-color", default.textPrimaryColor),
                styles.getColor("disabled-text-color", default.disabledTextColor),
                styles.getColor("primary-color", default.primaryColor),
                styles.getColor("dark-primary-color", default.darkPrimaryColor),
                styles.getColor("light-primary-color", default.lightPrimaryColor),
                styles.getColor("accent-color", default.accentColor),
                styles.getColor("error-color", default.errorColor),
                styles.getColor("card-background-color", default.cardBackgroundColor),
                styles.getColor("primary-background-color", default.primaryBackgroundColor),
                styles.getColor("secondary-background-color", default.secondaryBackgroundColor)
            )
        }

        private fun JSONObject.getColor(key: String, default: Int): Int {
            return if (has(key)) {
                try {
                    parseColor(key)
                } catch (ex: IllegalArgumentException) {
                    default
                }
            } else default
        }

        // Patterns inpired by answers to this question:
        // https://stackoverflow.com/questions/1636350/how-to-identify-a-given-string-is-hex-color-format
        private fun JSONObject.parseColor(key: String): Int {
            val color = getString(key)

            return when (true) {
                hexPattern.matches(color) -> Color.parseColor(color)
                hexAlphaPattern.matches(color) -> parseHexAlphaColor(color)
                rgbPattern.matches(color) -> parseRgbColor(color)
                rgbaPattern.matches(color) -> parseRgbColor(color, true)
                varPattern.matches(color) -> parseColor(getVariableKey(color))
                else -> throw IllegalArgumentException()
            }
        }

        private fun parseHexAlphaColor(color: String): Int {
            val hexValue = color.removePrefix("#")
            val splitList = if (hexValue.length == 4) {
                listOf(hexValue.substring(0, 3), hexValue.substring(3))
            } else {
                listOf(hexValue.substring(0, 6), hexValue.substring(6))
            }
            val parsedHexValue = "#${splitList[1]}${splitList[0]}"
            return Color.parseColor(parsedHexValue)
        }

        private fun parseRgbColor(color: String, hasAlpha: Boolean = false): Int {
            val matches = if (hasAlpha) rgbaPattern.find(color)!! else rgbPattern.find(color)!!
            val red = convertToNumber(matches.groups[1]!!.value)
            val green = convertToNumber(matches.groups[2]!!.value)
            val blue = convertToNumber(matches.groups[3]!!.value)

            return if (hasAlpha) {
                val alpha = convertToNumber(matches.groups[3]!!.value)
                Color.argb(alpha, red, green, blue)
            } else Color.rgb(red, green, blue)
        }

        private fun getVariableKey(variable: String): String {
            return varPattern.find(variable)!!.groups[1]!!.value
        }

        private fun convertToNumber(value: String): Float =
            value.removeSuffix("%").toFloat() * 255 / 100
    }
}
