package com.linguakey
import android.inputmethodservice.InputMethodService
import android.view.*
import android.view.inputmethod.EditorInfo
import android.text.InputType
import android.widget.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import com.google.mlkit.nl.translate.*
class LinguaService : InputMethodService() {
 private var russian = true
 private var enabled = true
 private var shift = true
 private var symbols = false
 private var pending = false
 private var generation = 0
 private var sensitive = false
 private val buffer = StringBuilder()
 private var root: LinearLayout? = null
 private var status: TextView? = null
 private val handler = Handler(Looper.getMainLooper())
 private val translators = mutableMapOf<Boolean, Translator>()
 private fun dp(n:Int) = (n*resources.displayMetrics.density).toInt()
 private fun bg(color:Int) = GradientDrawable().apply { setColor(color); cornerRadius=dp(5).toFloat() }
 private fun active() = enabled && !sensitive
 private fun stopRepeat() { handler.removeCallbacksAndMessages(null) }
 override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
 super.onStartInput(attribute,restarting)
 stopRepeat(); generation++; pending=false; buffer.clear()
 val type=attribute?.inputType ?: 0
 val variation=type and InputType.TYPE_MASK_VARIATION
 sensitive=(type and InputType.TYPE_MASK_CLASS)!=InputType.TYPE_CLASS_TEXT || variation in listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,InputType.TYPE_TEXT_VARIATION_URI) || (type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)!=0
 shift=!sensitive && (currentInputConnection?.getTextBeforeCursor(100,0)?.toString()?.trimEnd()?.lastOrNull()?.let { it in ".?!" } ?: true)
 render()
 }
 override fun onFinishInput(){ stopRepeat(); generation++; pending=false; buffer.clear(); super.onFinishInput() }
 override fun onCreateInputView():View {
 root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xff151515.toInt()); setPadding(dp(2),dp(2),dp(2),dp(48)) }
 render(); return root!!
 }
 private fun key(label:String,weight:Float=1f,height:Int=44,blue:Boolean=false,alt:String?=null,action:()->Unit):TextView {
 return TextView(this).apply {
 text=if(alt==null) label else android.text.SpannableString("$alt\n$label").apply { setSpan(android.text.style.RelativeSizeSpan(0.65f),0,alt.length,0); setSpan(android.text.style.ForegroundColorSpan(0xffaaaaaa.toInt()),0,alt.length,0) }
 textSize=18f; includeFontPadding=false; maxLines=2
 setTextColor(Color.WHITE); gravity=Gravity.CENTER
 background=bg(if(blue) 0xff3d5de8.toInt() else 0xff292929.toInt())
 layoutParams=LinearLayout.LayoutParams(0,dp(height),weight).apply { setMargins(dp(2),dp(2),dp(2),dp(2)) }
 setOnClickListener { if(!pending) action() }
 if(alt!=null) setOnLongClickListener { if(!pending) type(alt); true }
 }
 }
 private fun render(){
 val panel=root ?: return
 stopRepeat(); panel.removeAllViews()
 status=TextView(this).apply { textSize=12f; setTextColor(0xffcccccc.toInt()); gravity=Gravity.CENTER; text=if(pending) "Перевод…" else if(active()) if(russian) "RU → EN" else "EN → RU" else "Обычный набор" }
 panel.addView(status)
 val controls=LinearLayout(this)
 controls.addView(key("RU/EN",height=30){finishRaw(); russian=!russian; render()})
 controls.addView(key("Режим",height=30){finishRaw(); enabled=!enabled; render()})
 controls.addView(key("Перевести",1.5f,30){if(active() && buffer.isNotBlank()) {translate()}})
 panel.addView(controls)
 fun row(block:LinearLayout.()->Unit){panel.addView(LinearLayout(this).apply { setBaselineAligned(false); gravity=Gravity.CENTER_VERTICAL; block() })}
 row { "1234567890".forEach { ch -> addView(key(ch.toString(),height=35){type(ch.toString())}) } }
 val rows=if(symbols) listOf("@#%&*()-+","/:;,.?!","[]{}<>=") else if(russian) listOf("йцукенгшщзх","фывапролджэ","ячсмитьбю") else listOf("qwertyuiop","asdfghjkl","zxcvbnm")
 val alts=listOf("%^~|[]<>{}№","@#&*-+=()×÷","—₽\"':;ъ$€")
 rows.forEachIndexed { index,chars -> row {
 if(index==2) addView(key("⇧"){shift=!shift; render()})
 chars.forEachIndexed { j,ch ->
 val label=if(shift) ch.uppercase() else ch.toString()
 val alt=if(!symbols && russian) alts[index].getOrNull(j)?.toString() else null
 addView(key(label,alt=alt){type(label)})
 }
 if(index==2) {
 val del=key("⌫"){delete()}
 var held=false
 val repeat=object:Runnable { override fun run(){ if(held && del.isPressed && del.isAttachedToWindow && !pending){delete();handler.postDelayed(this,80)} } }
 del.setOnTouchListener { v,event ->
 when(event.actionMasked){
 MotionEvent.ACTION_DOWN -> {stopRepeat(); held=true; v.isPressed=true; if(!pending){delete();handler.postDelayed(repeat,450)}; true}
 MotionEvent.ACTION_MOVE -> {if(event.x<0 || event.y<0 || event.x>=v.width || event.y>=v.height){held=false;v.isPressed=false;handler.removeCallbacks(repeat)};true}
 MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {held=false;v.isPressed=false;handler.removeCallbacks(repeat);true}
 else -> true
 }
 }
 del.addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener{
 override fun onViewAttachedToWindow(v:View){}
 override fun onViewDetachedFromWindow(v:View){held=false;handler.removeCallbacks(repeat)}
 })
 addView(del)
 }
 } }
 row {
 addView(key(if(symbols) "ABC" else "123"){symbols=!symbols; render()})
 addView(key(if(russian) "ё" else ","){type(if(russian) "ё" else ",")})
 addView(key(",",0.8f){type(",")})
 addView(key(if(russian) "Русский" else "English",4.5f){type(" ")})
 addView(key(".",alt="?"){type(".")})
 addView(key("↵",1.4f,blue=true){if(active() && buffer.isNotBlank()){translate()}else{finishRaw();currentInputConnection?.commitText("\n",1);shift=true;render()}})
 }
 }
 private fun finishRaw(){currentInputConnection?.finishComposingText();buffer.clear()}
 private fun delete(){
 val ic=currentInputConnection ?: return
 if(active() && buffer.isNotEmpty()){buffer.deleteCharAt(buffer.length-1);ic.setComposingText(buffer.toString(),1)}
 else if(!ic.commitTextIfSelection()) ic.deleteSurroundingText(1,0)
 }
 private fun android.view.inputmethod.InputConnection.commitTextIfSelection():Boolean {
 if(getSelectedText(0).isNullOrEmpty())return false
 return commitText("",1)
 }
 private fun type(value:String){
 val ic=currentInputConnection ?: return
 val letter=value.all{it.isLetter()}
 val actual=if(letter && shift && !sensitive)value.replaceFirstChar{it.uppercase()}else value
 if(active()){buffer.append(actual);ic.setComposingText(buffer.toString(),1)}else ic.commitText(actual,1)
 val old=shift
 if(letter)shift=false
 if(actual in listOf(".","?","!"))shift=!sensitive
 if(active() && actual in listOf(".","?","!") && buffer.length>1){translate();return}
 if(old!=shift)render()
 }
 private fun translate(){
 val source=buffer.toString();val token=generation;val direction=russian
 pending=true;render()
 val translator=translators.getOrPut(direction){Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(if(direction)TranslateLanguage.RUSSIAN else TranslateLanguage.ENGLISH).setTargetLanguage(if(direction)TranslateLanguage.ENGLISH else TranslateLanguage.RUSSIAN).build())}
 translator.downloadModelIfNeeded().continueWithTask{task->if(!task.isSuccessful)throw(task.exception ?: IllegalStateException("Model unavailable"));translator.translate(source)}.addOnCompleteListener{task->
 if(token!=generation)return@addOnCompleteListener
 pending=false
 if(task.isSuccessful)currentInputConnection?.commitText(task.result,1)else currentInputConnection?.finishComposingText()
 buffer.clear();render()
 if(!task.isSuccessful)status?.text="Модель недоступна. Исходный текст сохранён."
 }
 }
 override fun onDestroy(){stopRepeat();generation++;translators.values.forEach{it.close()};super.onDestroy()}
}
