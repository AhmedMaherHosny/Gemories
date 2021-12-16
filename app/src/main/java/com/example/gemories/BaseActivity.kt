package com.example.gemories

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity(){
    fun createDialog(msg:String, namePositiveBtn:String, actionPositiveBtn:DialogInterface.OnClickListener,
                     nameNegativeBtn:String, actionNegativeBtn: DialogInterface.OnClickListener){
        val builder = AlertDialog.Builder(this)
        builder.setMessage(msg)
        builder.setPositiveButton(namePositiveBtn, actionPositiveBtn)
        builder.setNegativeButton(nameNegativeBtn, actionNegativeBtn)
        builder.setCancelable(false)
        builder.show()
    }
}