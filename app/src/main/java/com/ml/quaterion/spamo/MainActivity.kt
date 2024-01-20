package com.ml.quaterion.spamo

import android.app.ProgressDialog
import android.os.Bundle
import android.os.PersistableBundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.ml.quaterion.spamo.ui.theme.AppTheme
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var classifier: Classifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ActivityUI()
        }

        // Init the classifier.
        classifier = Classifier( this )

        // Start vocab processing, show a ProgressDialog to the user.
        val progressDialog = ProgressDialog( this )
        progressDialog.setMessage( "Loading model from assets" )
        progressDialog.setCancelable( false )
        progressDialog.show()

        classifier.load( "model.tflite" , "word_dict.json" ) {
            progressDialog.dismiss()
        }
    }

    @Composable
    private fun ActivityUI() {
        AppTheme {
            Surface(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxSize() ,
            ) {
                MessageInput()
            }
        }
    }

    @Composable
    private fun MessageInput() {
        var message by remember{ mutableStateOf( "" ) }
        var resultText by remember{ mutableStateOf( "" ) }
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth() ,
                value = message,
                onValueChange = { message = it } )
            Button(onClick = {
                if ( !TextUtils.isEmpty( message ) ){
                    classifier.classify( message ) {
                        resultText = "SPAM : $it[0]\nNOT SPAM : $it[1] "
                    }
                }
                else{
                    Toast.makeText( this@MainActivity, "Please enter a message.", Toast.LENGTH_LONG).show();
                }
            }) {
                Text(text = "Classify")
            }
            Text(text = resultText)
        }
    }


}
