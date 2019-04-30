package com.ml.quaterion.spamo

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max

class Classifier  {

    private var context : Context? = null
    private var filename : String? = null
    private var callback : DataCallback? = null
    private var maxlen : Int? = null
    private var vocabData : HashMap< String , Int >? = null

    constructor( context: Context , jsonFilename : String ){
        this.context = context
        this.filename = jsonFilename
    }


    fun loadData () {
        val loadVocabularyTask = LoadVocabularyTask( callback )
        loadVocabularyTask.execute( loadJSONFromAsset( filename ))
    }

    private fun loadJSONFromAsset(filename : String? ): String? {
        var json: String? = null
        try {
            val inputStream = context!!.assets.open(filename )
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer)
        }
        catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    fun setCallback( callback: DataCallback ) {
        this.callback = callback
    }

    fun tokenize ( message : String ): IntArray {
        val parts : List<String> = message.split(" " )
        val tokenizedMessage = ArrayList<Int>()
        for ( part in parts ) {
            if (part.trim() != ""){
                var index : Int? = 0
                if ( vocabData!![part] == null ) {
                    index = 0
                }
                else{
                    index = vocabData!![part]
                }
                tokenizedMessage.add( index!! )
            }
        }
        return tokenizedMessage.toIntArray()
    }

    fun padSequence ( sequence : IntArray ) : IntArray {
        val maxlen = this.maxlen
        if ( sequence.size > maxlen!!) {
            return sequence.sliceArray( 0..maxlen )
        }
        else if ( sequence.size < maxlen ) {
            val array = ArrayList<Int>()
            array.addAll( sequence.asList() )
            for ( i in array.size until maxlen ){
                array.add(0)
            }
            return array.toIntArray()
        }
        else{
            return sequence
        }
    }

    fun setVocab( data : HashMap<String, Int>? ) {
        this.vocabData = data
    }

    fun setMaxLength( maxlen : Int ) {
        this.maxlen = maxlen
    }

    interface DataCallback {
        fun onDataProcessed( result : HashMap<String, Int>?)
    }

    private inner class LoadVocabularyTask(callback: DataCallback?) : AsyncTask<String, Void, HashMap<String, Int>?>() {

        private var callback : DataCallback? = callback

        override fun doInBackground(vararg params: String?): HashMap<String, Int>? {
            val jsonObject = JSONObject( params[0] )
            val iterator : Iterator<String> = jsonObject.keys()
            val data = HashMap< String , Int >()
            while ( iterator.hasNext() ) {
                val key = iterator.next()
                data.put( key , jsonObject.get( key ) as Int )
            }
            return data
        }

        override fun onPostExecute(result: HashMap<String, Int>?) {
            super.onPostExecute(result)
            callback?.onDataProcessed( result )
        }

    }

}