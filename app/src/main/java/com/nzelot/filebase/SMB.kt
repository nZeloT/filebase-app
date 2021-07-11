package com.nzelot.filebase

import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.security.bc.BCSecurityProvider
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.common.SMBRuntimeException
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.nzelot.filebase.data.Result
import com.nzelot.filebase.data.model.Credentials
import com.nzelot.filebase.data.model.SMBConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object SMB {

    suspend fun testShareLogin(config: SMBConfiguration): Result<Boolean> {
        return withContext(Dispatchers.IO) { testShareLoginBlocking(config) }
    }

    fun testShareLoginBlocking(config: SMBConfiguration): Result<Boolean> {
        return doOnShare(config) { Result.Success(true) }
    }

    suspend fun testShareLogin(hostname: String, shareName: String, creds: Credentials): Result<Boolean> {
        return testShareLogin(SMBConfiguration(hostname, shareName, creds))
    }

    fun <T : Any> doOnShare(
        config: SMBConfiguration,
        block: (DiskShare) -> Result<T>
    ): Result<T> {
        val smbConf = SmbConfig.builder().withSecurityProvider(BCSecurityProvider()).build()
        val creds = config.credentials
        val client = SMBClient(smbConf)
        var result: Result<T>

        var connection: Connection? = null
        var session: Session? = null
        var share: DiskShare? = null
        try {
            connection = client.connect(config.address)
            session = connection.authenticate(AuthenticationContext(creds.username, creds.password, creds.domain))
            share = session.connectShare(config.shareName) as DiskShare

            result = block(share)

        } catch (e: Exception) {
            if (e is IllegalArgumentException || e is SMBApiException || e is SMBRuntimeException || e is IOException) {
                result = Result.Error(e)
            } else {
                throw e
            }
        } finally {
            share?.close()
            session?.close()
            connection?.close()
        }

        return result
    }

}