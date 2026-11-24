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
 private val word = StringBuilder()
 private var root: LinearLayout? = null
 private var status: TextView? = null
 private var suggestions: LinearLayout? = null
 private val handler = Handler(Looper.getMainLooper())
 private val translators = mutableMapOf<Boolean, Translator>()
 private val ru = "привет как дела хорошо спасибо пожалуйста сегодня завтра вчера сейчас потом можно нужно хочу буду будет есть нет да тут здесь там это этот эта очень просто почему потому когда где куда откуда который которая которые человек люди время работа домой дома телефон сообщение клавиатура перевод русский английский здравствуйте добрый доброе утро вечер ночь пока конечно понятно отлично нормально немного много больше меньше делать сделать работает работаю напиши написать сказать говорю говорить думаю думаю́ знаю знаю́ ничего что чтобы если или уже ещё еще тоже только всегда никогда вместе друг друзья родной родная давай поехали дальше получилось получается исправить ошибка ошибок текст слово слова предложение предложения приложение приложения установить обновить проверить проверка интернет спасибо большое тебя тебе меня мне мы вы они он она оно мой моя мои твой твоя наш ваше новый новая новое первый первая второй вторая раз два три четыре пять шесть семь восемь девять десять".split(" ")
 private val en = "hello hi how are you i am is it we they he she the a an and or but if then this that these those there here where when what why who which can could would should will have has had do does did not no yes thanks thank please good great excellent fine today tomorrow yesterday now later time work working home phone keyboard translation translate english russian message text word sentence app application install update check internet friend friends want need like love know think understand right wrong first second new old more less much many some any all one two three four five six seven eight nine ten".split(" ")
 private fun dp(n:Int) = (n*resources.displayMetrics.density).toInt()
 private fun bg(color:Int) = GradientDrawable().apply { setColor(color); cornerRadius=dp(5).toFloat() }
 private fun active() = enabled && !sensitive
 private fun stopRepeat() { handler.removeCallbacksAndMessages(null) }
 override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
 super.onStartInput(attribute,restarting)
 stopRepeat(); generation++; pending=false; buffer.clear(); word.clear()
 val type=attribute?.inputType ?: 0
 val variation=type and InputType.TYPE_MASK_VARIATION
 sensitive=(type and InputType.TYPE_MASK_CLASS)!=InputType.TYPE_CLASS_TEXT || variation in listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,InputType.TYPE_TEXT_VARIATION_URI) || (type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)!=0
 shift=!sensitive && (currentInputConnection?.getTextBeforeCursor(100,0)?.toString()?.trimEnd()?.lastOrNull()?.let { it in ".?!" } ?: true)
 render()
 }
 override fun onFinishInput(){ stopRepeat(); generation++; pending=false; buffer.clear(); word.clear(); super.onFinishInput() }
 override fun onCreateInputView():View {
 root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xff151515.toInt()); setPadding(dp(2),dp(2),dp(2),dp(48)) }
 render(); return root!!
 }
 private fun key(label:String,weight:Float=1f,height:Int=44,blue:Boolean=false,alt:String?=null,action:()->Unit):TextView {
 return TextView(this).apply {
 text=if(alt==null) label else "$alt\n$label"
 textSize=if(alt==null) 18f else 16f
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
 controls.addView(key("Перевести",1.5f,30){if(active() && buffer.isNotBlank()) {correctWord(); translate()}})
 panel.addView(controls)
 suggestions=LinearLayout(this); panel.addView(suggestions); refreshSuggestions()
 fun row(block:LinearLayout.()->Unit){panel.addView(LinearLayout(this).apply(block))}
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
 del.setOnTouchListener { _,event ->
 when(event.actionMasked){
 MotionEvent.ACTION_DOWN -> { if(!pending){delete(); handler.postDelayed(object:Runnable{override fun run(){if(!pending){delete(); handler.postDelayed(this,65)}}},400)}; true }
 MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {stopRepeat(); true}
 else -> true
 }
 }
 addView(del)
 }
 } }
 row {
 addView(key(if(symbols) "ABC" else "123"){symbols=!symbols; render()})
 addView(key(if(russian) "ё" else ","){type(if(russian) "ё" else ",")})
 addView(key(",",0.8f){type(",")})
 addView(key(if(russian) "Русский" else "English",4.5f){type(" ")})
 addView(key(".",alt="?"){type(".")})
 addView(key("↵",1.4f,blue=true){if(active() && buffer.isNotBlank()){correctWord(); translate()}else{finishRaw();currentInputConnection?.commitText("\n",1);shift=true;render()}})
 }
 }
 private fun candidates():List<String>{
 val s=word.toString().lowercase()
 if(sensitive || s.isBlank()) return emptyList()
 val dict=if(russian) ru else en
 return (listOf(s)+dict.filter{it.startsWith(s)&&it!=s}+dict.filter{it!=s && it.length>=3 && s.length>=3 && distance(s,it)==1}).distinct().take(3)
 }
 private fun distance(a:String,b:String):Int {
 if(kotlin.math.abs(a.length-b.length)>1)return 2
 var prev=IntArray(b.length+1){it}
 for(i in a.indices){val next=IntArray(b.length+1);next[0]=i+1;for(j in b.indices)next[j+1]=minOf(next[j]+1,prev[j+1]+1,prev[j]+if(a[i]==b[j])0 else 1);prev=next}
 return prev[b.length]
 }
 private fun refreshSuggestions(){
 val line=suggestions ?: return; line.removeAllViews()
 val options=candidates()
 repeat(3){i->val s=options.getOrNull(i) ?: "";line.addView(key(s,height=32){if(s.isNotEmpty()){replaceWord(s);type(" ")}})}
 }
 private fun replaceWord(s:String){
 if(word.isEmpty())return
 val value=if(word.first().isUpperCase())s.replaceFirstChar{it.uppercase()}else s
 val ic=currentInputConnection ?: return
 if(active()){buffer.delete(buffer.length-word.length,buffer.length);buffer.append(value);ic.setComposingText(buffer.toString(),1)}
 else {ic.deleteSurroundingText(word.length,0);ic.commitText(value,1)}
 word.clear();word.append(value)
 }
 private fun correctWord(){
 if(sensitive || word.length<4)return
 val s=word.toString().lowercase();val dict=if(russian)ru else en
 if(s in dict)return
 val matches=dict.filter{distance(s,it)==1}
 if(matches.size==1)replaceWord(matches[0])
 }
 private fun finishRaw(){currentInputConnection?.finishComposingText();buffer.clear();word.clear()}
 private fun delete(){
 val ic=currentInputConnection ?: return
 if(active() && buffer.isNotEmpty()){buffer.deleteCharAt(buffer.length-1);ic.setComposingText(buffer.toString(),1);word.clear();word.append(buffer.takeLastWhile{it.isLetter()})}
 else {ic.deleteSurroundingText(1,0);if(word.isNotEmpty())word.deleteCharAt(word.length-1)}
 refreshSuggestions()
 }
 private fun type(value:String){
 val ic=currentInputConnection ?: return
 val letter=value.all{it.isLetter()}
 val actual=if(letter && shift && !sensitive)value.replaceFirstChar{it.uppercase()}else value
 if(!letter){correctWord();word.clear()}else if(!sensitive)word.append(actual)
 if(active()){buffer.append(actual);ic.setComposingText(buffer.toString(),1)}else ic.commitText(actual,1)
 val old=shift
 if(letter)shift=false
 if(actual in listOf(".","?","!"))shift=!sensitive
 if(active() && actual in listOf(".","?","!") && buffer.length>1){translate();return}
 if(old!=shift)render()else refreshSuggestions()
 }
 private fun translate(){
 val source=buffer.toString();val token=generation;val direction=russian
 pending=true;render()
 val translator=translators.getOrPut(direction){Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(if(direction)TranslateLanguage.RUSSIAN else TranslateLanguage.ENGLISH).setTargetLanguage(if(direction)TranslateLanguage.ENGLISH else TranslateLanguage.RUSSIAN).build())}
 translator.downloadModelIfNeeded().continueWithTask{task->if(!task.isSuccessful)throw(task.exception ?: IllegalStateException("Model unavailable"));translator.translate(source)}.addOnCompleteListener{task->
 if(token!=generation)return@addOnCompleteListener
 pending=false
 if(task.isSuccessful)currentInputConnection?.commitText(task.result,1)else currentInputConnection?.finishComposingText()
 buffer.clear();word.clear();render()
 if(!task.isSuccessful)status?.text="Модель недоступна. Исходный текст сохранён."
 }
 }
 override fun onDestroy(){stopRepeat();generation++;translators.values.forEach{it.close()};super.onDestroy()}
}
