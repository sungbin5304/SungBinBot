package me.sungbin.sungbinbot.util

import org.mozilla.javascript.Context


object RhinoUtil {

    private fun runJs(source: String): String {
        return try {
            val rhino = Context.enter()
            val scope = rhino.initStandardObjects()
            rhino.optimizationLevel = -1
            rhino.languageVersion = Context.VERSION_ES6
            rhino.evaluateString(scope, source, "eval", 1, null).toString()
        } catch (exception: Exception) {
            exception.toString()
        }
    }

    fun checkSameWord(firstWord: String, secondWord: String) =
        runJs(JsCode.KOR + "\n\nKor.checkSameWord(\"$firstWord\", \"$secondWord\")").split(".")
            .first().toInt()

}