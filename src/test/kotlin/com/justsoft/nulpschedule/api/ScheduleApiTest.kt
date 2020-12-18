package com.justsoft.nulpschedule.api

import com.justsoft.nulpschedule.api.model.ScheduleType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.*
import org.junit.jupiter.params.provider.Arguments.of
import java.io.FileInputStream
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.stream.Stream
import javax.net.ssl.*


internal class ScheduleApiTest {

    private lateinit var scheduleApi: ScheduleApi

    @BeforeEach
    fun setUp() {
        scheduleApi = getScheduleApi()
    }

    @Test
    fun getInstitutesSelective() {
        val institutes = scheduleApi.getInstitutes(scheduleType = ScheduleType.SELECTIVE)
        assert(institutes.isFailure)
        assert(institutes.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun getInstitutesPostgraduate() {
        val institutes = scheduleApi.getInstitutes(scheduleType = ScheduleType.POSTGRADUATE)
        assert(institutes.isFailure)
        assert(institutes.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun getInstitutesPostgraduatePartTime() {
        val institutes = scheduleApi.getInstitutes(scheduleType = ScheduleType.POSTGRADUATE_PART_TIME)
        assert(institutes.isFailure)
        assert(institutes.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun getInstitutesStudent() {
        val institutes = scheduleApi.getInstitutes(scheduleType = ScheduleType.STUDENT)
        assert(institutes.isSuccess)
        assert(!institutes.getOrNull().isNullOrEmpty())
    }

    @Test
    fun getInstitutesStudentPartTime() {
        val institutes = scheduleApi.getInstitutes(scheduleType = ScheduleType.STUDENT_PART_TIME)
        assert(institutes.isSuccess)
        assert(!institutes.getOrNull().isNullOrEmpty())
    }

    @Test
    fun getSchedule() {
        val scheduleRequestResult = scheduleApi.getSchedule("ІКНІ", "ПЗ-14")
        assert(scheduleRequestResult.isSuccess)
        assert(scheduleRequestResult.getOrNull() != null)
    }

    class GroupTest {

        private val scheduleApi = getScheduleApi()

        @ParameterizedTest(name = "{index}: ordinal = {0}, institute = \'{1}\'")
        @ArgumentsSource(value = GroupArgumentsProvider::class)
        fun getGroups(ordinal: Int, institute: String) {
            val groups = scheduleApi.getGroups(institute, ScheduleType.values()[ordinal])
            assert(groups.isSuccess)
            assert(!groups.getOrNull().isNullOrEmpty())
        }

        class GroupArgumentsProvider : ArgumentsProvider {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    of(0, "ІКНІ"),
                    of(1, ""),
                    of(2, ""),
                    of(3, "ІКНІ"),
                    of(4, "")
                )
            }

        }
    }

    companion object {
        private fun getScheduleApi(): ScheduleApi {
            return ScheduleApi(SSLSocketFactoryWithAdditionalKeyStores(loadKeystore()))
        }

        private fun loadKeystore(): KeyStore {
            val ks = KeyStore.getInstance("JKS")
            ks.load(null, null)
            val factory = CertificateFactory.getInstance("X.509")
            val fis = FileInputStream("nulp_ua_certificate.cer")
            val cert = factory.generateCertificate(fis)
            ks.setCertificateEntry("nulp", cert)
            return ks
        }


    }

    internal class SSLSocketFactoryWithAdditionalKeyStores(
        vararg keyStores: KeyStore
    ) : SSLSocketFactory() {
        private val sslContext = SSLContext.getInstance("TLS")

        init {
            sslContext.init(
                null,
                arrayOf<TrustManager>(AdditionalKeyStoresTrustManager(*keyStores)),
                null
            )
        }

        override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket =
            sslContext.socketFactory.createSocket(s, host, port, autoClose)

        override fun createSocket(host: String?, port: Int): Socket =
            sslContext.socketFactory.createSocket(host, port)

        override fun createSocket(
            host: String?,
            port: Int,
            localHost: InetAddress?,
            localPort: Int
        ): Socket = sslContext.socketFactory.createSocket(host, port, localHost, localPort)

        override fun createSocket(host: InetAddress?, port: Int): Socket =
            sslContext.socketFactory.createSocket(host, port)

        override fun createSocket(
            address: InetAddress?,
            port: Int,
            localAddress: InetAddress?,
            localPort: Int
        ): Socket = sslContext.socketFactory.createSocket(address, port, localAddress, localPort)

        override fun getDefaultCipherSuites(): Array<String> =
            sslContext.defaultSSLParameters.cipherSuites


        override fun getSupportedCipherSuites(): Array<String> =
            sslContext.supportedSSLParameters.cipherSuites

        class AdditionalKeyStoresTrustManager(vararg additionalKeyStores: KeyStore) : X509TrustManager {
            private val x509TrustManagers = mutableListOf<X509TrustManager>()

            init {
                val factories = mutableListOf<TrustManagerFactory>()

                val originalFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                originalFactory.init(null as KeyStore?)
                factories.add(originalFactory)

                for (keyStore in additionalKeyStores) {
                    val additionalCerts =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    additionalCerts.init(keyStore)
                    factories.add(additionalCerts)
                }

                for (tmf in factories)
                    for (tm in tmf.trustManagers)
                        if (tm is X509TrustManager)
                            x509TrustManagers.add(tm)


                if (x509TrustManagers.size == 0) throw RuntimeException("Couldn't find any X509TrustManagers")
            }

            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                x509TrustManagers.first().checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                for (tm in x509TrustManagers) {
                    try {
                        tm.checkServerTrusted(chain, authType)
                        return
                    } catch (ignored: CertificateException) {
                    }
                }
                throw CertificateException()
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                val list = mutableListOf<X509Certificate>()
                for (tm in x509TrustManagers)
                    list.addAll(tm.acceptedIssuers)
                return list.toTypedArray()
            }
        }
    }
}