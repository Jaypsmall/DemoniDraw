package com.jaylizapp.demonidraw.util

import java.io.DataOutputStream
import java.io.IOException

object ShellUtils {
    fun executeCommand(command: String): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        } finally {
            try {
                os?.close()
                process?.destroy()
            } catch (e: IOException) {
            }
        }
    }
}
