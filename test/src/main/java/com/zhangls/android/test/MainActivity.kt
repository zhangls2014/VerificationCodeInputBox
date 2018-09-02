package com.zhangls.android.test

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputBox.onInputComplete = {
            Toast.makeText(this, "验证码是：$it", Toast.LENGTH_SHORT).show()
        }
    }
}
