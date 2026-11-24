package com.linguakey
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.*
class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
 super.onCreate(savedInstanceState)
 val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,80,32,32) }
 layout.addView(TextView(this).apply { text = "LinguaKey 0.1\nВключите клавиатуру и выберите её. Для первой загрузки моделей нужен интернет."; textSize = 20f })
 layout.addView(Button(this).apply { text = "Включить клавиатуру"; setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) } })
 layout.addView(Button(this).apply { text = "Выбрать клавиатуру"; setOnClickListener { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() } })
 setContentView(layout)
 }
}