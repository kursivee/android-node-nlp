package com.kursivee.nodejs

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL
import android.content.res.AssetManager
import android.system.Os
import java.io.*
import java.nio.file.Files.delete
import java.nio.file.Files.isDirectory
import java.nio.file.Files.exists






class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Os.setenv("TMPDIR", this.getCacheDir().getAbsolutePath(),true)
        Thread(Runnable {
            //The path where we expect the node project to be at runtime.
            val nodeDir = applicationContext.filesDir.absolutePath + "/nodejs"
            val node_modules = applicationContext.filesDir.absolutePath + "/node_modules"
            //Recursively delete any existing nodejs-project.
            val nodeDirReference = File(nodeDir)
            if (nodeDirReference.exists()) {
                deleteFolderRecursively(File(nodeDir))
            }
            //Copy the node project from assets into the application's data path.
            copyAssetFolder(applicationContext.assets, "nodejs", nodeDir)
//            copyAssetFolder(applicationContext.assets, "nodejs/node_modules/node-nlp/dist", node_modules)
            startNodeWithArguments(arrayOf("node", "$nodeDir/index.android.js"))
        }).start()

        tv_message.setOnClickListener {
            val task = @Suppress("StaticFieldLeak")
            object : AsyncTask<Void, Void, String>() {
                override fun doInBackground(vararg params: Void): String {
                    var nodeResponse = ""
                    try {
                        val localNodeServer = URL("http://localhost:3000/")
                        val `in` = BufferedReader(
                            InputStreamReader(localNodeServer.openStream())
                        )
                        var inputLine: String
                        `in`.forEachLine {
                            nodeResponse = nodeResponse + it
                        }
                        `in`.close()
                    } catch (ex: Exception) {
                        nodeResponse = ex.toString()
                    }

                    return nodeResponse
                }

                override fun onPostExecute(result: String) {
                    println(result)
                }
            }.execute()
        }
    }

    external fun startNodeWithArguments(arguments: Array<String>): Int?

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
            System.loadLibrary("node")
        }
    }

    private fun deleteFolderRecursively(file: File): Boolean {
        try {
            var res = true
            for (childFile in file.listFiles()) {
                if (childFile.isDirectory()) {
                    res = res and deleteFolderRecursively(childFile)
                } else {
                    res = res and childFile.delete()
                }
            }
            res = res and file.delete()
            return res
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    private fun copyAssetFolder(assetManager: AssetManager, fromAssetPath: String, toPath: String): Boolean {
        try {
            val files = assetManager.list(fromAssetPath)
            var res = true

            if (files!!.size == 0) {
                //If it's a file, it won't have any assets "inside" it.
                res = res and copyAsset(
                    assetManager,
                    fromAssetPath,
                    toPath
                )
            } else {
                File(toPath).mkdirs()
                for (file in files)
                    res = res and copyAssetFolder(
                        assetManager,
                        "$fromAssetPath/$file",
                        "$toPath/$file"
                    )
            }
            return res
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    private fun copyAsset(assetManager: AssetManager, fromAssetPath: String, toPath: String): Boolean {
        var `in`: InputStream? = null
        var out: OutputStream? = null
        try {
            `in` = assetManager.open(fromAssetPath)
            File(toPath).createNewFile()
            out = FileOutputStream(toPath)
            copyFile(`in`!!, out)
            `in`!!.close()
            `in` = null
            out!!.flush()
            out!!.close()
            out = null
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    @Throws(IOException::class)
    private fun copyFile(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int = `in`.read(buffer)
        while(read != -1) {
            out.write(buffer, 0, read)
            read = `in`.read(buffer)
        }
    }
}
