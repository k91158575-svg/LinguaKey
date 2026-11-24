package com.linguakey
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.text.InputType
import android.widget.*
import com.google.mlkit.nl.translate.*
class LinguaService : InputMethodService() {
 private var russian = true
 private var enabled = true
 private var shift = false
 private var symbols = false
 private var pending = false
 private var generation = 0
 private var sensitive = false
 private val buffer = StringBuilder()
 private var root: LinearLayout? = null
 private var status: TextView? = null
 private val translators = mutableMapOf<Boolean, Translator>()
 override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
 super.onStartInput(attribute, restarting)
 generation++; pending = false; buffer.clear()
 val type = attribute?.inputType ?: 0
 val variation = type and InputType.TYPE_MASK_VARIATION
 sensitive = (type and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT ||
 variation in listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_URI)
 render()
 }
 override fun onFinishInput() { generation++; pending = false; buffer.clear(); super.onFinishInput() }
 override fun onCreateInputView(): View {
 root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xff202124.toInt()); setPadding(4,4,4,4) }
 render()
 return root!!
 }
 private fun render() {
 val panel = root ?: return
 panel.removeAllViews()
 status = TextView(this).apply { setTextColor(-1); text = if (pending) "Перевод…" else if (enabled && !sensitive) if (russian) "RU → EN" else "EN → RU" else "Обычный набор" }
 panel.addView(status)
 fun row(keys: List<String>, action: (String)->Unit) {
 val line = LinearLayout(this)
 keys.forEach { key -> line.addView(Button(this).apply { text = key; textSize = 14f; isAllCaps = false; minWidth = 0; minimumWidth = 0; setPadding(0,0,0,0); setOnClickListener { if (!pending) action(key) } }, LinearLayout.LayoutParams(0, (48 * resources.displayMetrics.density).toInt(),1f)) }
 panel.addView(line)
 }
 row(listOf("RU/EN","Перевод","⇧","123","⌫")) { key ->
 when(key) {
 "RU/EN" -> { finishRaw(); russian = !russian }
 "Перевод" -> { finishRaw(); enabled = !enabled }
 "⇧" -> shift = !shift
 "123" -> symbols = !symbols
 "⌫" -> { if(buffer.isNotEmpty()) { buffer.deleteCharAt(buffer.length-1); currentInputConnection?.setComposingText(buffer.toString(),1) } else currentInputConnection?.deleteSurroundingText(1,0) }
 }; render()
 }
 val rows = if(symbols) listOf("1234567890","@#%&*()-+","/:;,.?!") else if(russian) listOf("йцукенгшщзх","фывапролджэ","ячсмитьбюъё") else listOf("qwertyuiop","asdfghjkl","zxcvbnm")
 rows.forEach { chars -> row(chars.map { if(shift) it.uppercase() else it.toString() }) { type(it); if(shift) { shift=false; render() } } }
 row(listOf(",", "Пробел", ".", "?", "!", "↵")) { key -> if(key=="↵") { finishRaw(); currentInputConnection?.commitText("\n",1) } else type(if(key=="Пробел") " " else key) }
 }
 private fun finishRaw() { currentInputConnection?.finishComposingText(); buffer.clear() }
 private fun type(value: String) {
 val ic = currentInputConnection ?: return
 if(!enabled || sensitive) { ic.commitText(value,1); return }
 buffer.append(value); ic.setComposingText(buffer.toString(),1)
 if(value in listOf(".","?","!") && buffer.length > 1) translate()
 }
 private fun translate() {
 val source = buffer.toString()
 val token = generation
 val direction = russian
 pending = true; render()
 val translator = translators.getOrPut(direction) {
 Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(if(direction) TranslateLanguage.RUSSIAN else TranslateLanguage.ENGLISH).setTargetLanguage(if(direction) TranslateLanguage.ENGLISH else TranslateLanguage.RUSSIAN).build())
 }
 translator.downloadModelIfNeeded().continueWithTask { task -> if(!task.isSuccessful) throw (task.exception ?: IllegalStateException("Model unavailable")); translator.translate(source) }
 .addOnCompleteListener { task ->
 if(token != generation) return@addOnCompleteListener
 pending = false
 if(task.isSuccessful) currentInputConnection?.commitText(task.result,1) else currentInputConnection?.finishComposingText()
 buffer.clear(); render()
 if(!task.isSuccessful) status?.text = "Модель недоступна. Исходный текст сохранён."
 }
 }
 override fun onDestroy() { generation++; translators.values.forEach { it.close() }; super.onDestroy() }
}