package com.adll.de_general.feature.onboarding.domain

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.toByteString
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

/**
 * The download path, driven end to end against an in-memory filesystem and a scripted server.
 *
 * This is where the risk actually lives. The file is 769 MB in production, so the branches that
 * matter — resume, a server that ignores `Range`, a corrupt body, a pause — are the ones that
 * would otherwise only ever be exercised by a user on a train losing signal.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ModelInstallerTest {

    private val fileSystem = FakeFileSystem()
    private val directory = "/models".toPath()

    /** A stand-in for the real weights: small, but hashed and checked exactly like them. */
    private val body = ByteArray(8_000) { (it % 251).toByte() }

    private val spec = ModelSpec(
        id = "test-model",
        displayName = "Test Model",
        quantization = "Q4_K_M",
        parameterCount = "1B",
        fileName = "test-model.gguf",
        downloadUrl = "https://example.invalid/test-model.gguf",
        sizeBytes = body.size.toLong(),
        sha256 = body.toByteString().sha256().hex(),
        requiredDiskBytes = body.size * 2L,
        requiredRamBytes = 1L,
    )

    private val requests = mutableListOf<HttpRequestData>()

    @AfterTest
    fun tearDown() {
        fileSystem.checkNoOpenFiles()
    }

    private fun files() = ModelFiles(fileSystem, directory, spec)

    private fun installer(
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): ModelInstaller {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        return ModelInstaller(
            httpClient = HttpClient(engine),
            fileSystem = fileSystem,
            directory = directory,
            spec = spec,
            dispatcher = dispatcher,
        )
    }

    /** Serves the file, honouring `Range` the way Hugging Face's CDN does. */
    private fun MockRequestHandleScope.serveWithRanges(request: HttpRequestData) =
        when (val range = request.headers[HttpHeaders.Range]) {
            null -> respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentLength, body.size.toString()),
            )

            else -> {
                val start = range.removePrefix("bytes=").substringBefore('-').toInt()
                val slice = body.copyOfRange(start, body.size)
                respond(
                    content = ByteReadChannel(slice),
                    status = HttpStatusCode.PartialContent,
                    headers = headersOf(
                        HttpHeaders.ContentRange,
                        "bytes $start-${body.size - 1}/${body.size}",
                    ),
                )
            }
        }

    private fun seedPartial(bytes: Int) {
        fileSystem.createDirectories(directory)
        fileSystem.write(files().partial) { write(body, 0, bytes) }
    }

    private fun installedBytes(): ByteArray? =
        fileSystem.takeIf { it.exists(files().target) }?.read(files().target) { readByteArray() }

    // --- the happy path ---------------------------------------------------------------------

    @Test
    fun cleanDownloadVerifiesAndPromotes() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val installer = installer(dispatcher) { serveWithRanges(it) }

        installer.install()

        assertEquals(InstallState.Installed, installer.state.value)
        assertContentEquals(body, installedBytes())
        assertFalse(fileSystem.exists(files().partial), "the .part file should be gone")
        assertEquals(null, requests.single().headers[HttpHeaders.Range], "a fresh download must not send Range")
    }

    @Test
    fun anAlreadyInstalledModelMakesNoRequest() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        fileSystem.createDirectories(directory)
        fileSystem.write(files().target) { write(body) }

        val installer = installer(dispatcher) { serveWithRanges(it) }
        installer.install()

        assertEquals(InstallState.Installed, installer.state.value)
        assertTrue(requests.isEmpty(), "nothing should have been requested")
    }

    // --- resume -----------------------------------------------------------------------------

    @Test
    fun resumeSendsARangeHeaderAndFinishesTheFile() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        seedPartial(3_000)

        val installer = installer(dispatcher) { serveWithRanges(it) }
        installer.install()

        assertEquals("bytes=3000-", requests.single().headers[HttpHeaders.Range])
        assertEquals(InstallState.Installed, installer.state.value)
        assertContentEquals(body, installedBytes())
    }

    @Test
    fun aPartialFileIsReportedAsPausedBeforeAnythingRuns() {
        seedPartial(1_234)

        val installer = installer(StandardTestDispatcher()) { serveWithRanges(it) }

        val state = installer.state.value
        assertIs<InstallState.Paused>(state)
        assertEquals(1_234L, state.progress.bytesDownloaded)
    }

    @Test
    fun aServerThatIgnoresRangeRestartsInsteadOfAppending() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        seedPartial(3_000)

        // Always 200 with the whole file, as some CDNs and captive portals do.
        val installer = installer(dispatcher) {
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentLength, body.size.toString()),
            )
        }
        installer.install()

        assertEquals(2, requests.size, "should have retried once from zero")
        assertEquals("bytes=3000-", requests[0].headers[HttpHeaders.Range])
        assertEquals(null, requests[1].headers[HttpHeaders.Range], "the retry must start clean")
        assertEquals(InstallState.Installed, installer.state.value)
        assertContentEquals(body, installedBytes())
    }

    // --- failure ----------------------------------------------------------------------------

    @Test
    fun aCorruptBodyOfTheRightLengthIsRejected() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val corrupt = body.copyOf().also { it[42] = (it[42] + 1).toByte() }

        val installer = installer(dispatcher) {
            respond(ByteReadChannel(corrupt), HttpStatusCode.OK)
        }
        installer.install()

        val state = installer.state.value
        assertIs<InstallState.Failed>(state)
        assertEquals(InstallFailure.ChecksumMismatch, state.reason)
        assertFalse(fileSystem.exists(files().target), "a bad file must never be promoted")
        assertFalse(fileSystem.exists(files().partial), "and must not be left behind to resume")
    }

    @Test
    fun aTruncatedBodyFailsAsIncompleteRatherThanSilentlyInstalling() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val installer = installer(dispatcher) {
            respond(ByteReadChannel(body.copyOf(5_000)), HttpStatusCode.OK)
        }
        installer.install()

        val state = installer.state.value
        assertIs<InstallState.Failed>(state)
        assertEquals(InstallFailure.ChecksumMismatch, state.reason)
        assertTrue(state.message.contains("incomplete"), "was: ${state.message}")
        assertFalse(fileSystem.exists(files().target))
    }

    @Test
    fun anUnusableStatusIsReportedAsAServerProblem() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val installer = installer(dispatcher) { respond("nope", HttpStatusCode.NotFound) }
        installer.install()

        val state = installer.state.value
        assertIs<InstallState.Failed>(state)
        assertEquals(InstallFailure.Server, state.reason)
    }

    // --- pause ------------------------------------------------------------------------------

    @Test
    fun cancellingKeepsThePartialFileAndReportsPaused() = runTest(timeout = 10.seconds) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        seedPartial(3_000)

        // A server that accepts the request and then never answers, like a stalled mobile link.
        // Note it never responds at all rather than dribbling bytes: MockEngine does not run on
        // the test scheduler, so anything that tries to interleave with it by virtual time is a
        // race. Resuming is covered by resumeSendsARangeHeaderAndFinishesTheFile.
        val installer = installer(dispatcher) { awaitCancellation() }

        val job = launch { installer.install() }
        advanceUntilIdle()
        job.cancelAndJoin()
        advanceUntilIdle()

        val paused = installer.state.value
        assertIs<InstallState.Paused>(paused)
        assertEquals(3_000L, paused.progress.bytesDownloaded, "a pause must not throw bytes away")
        assertEquals(3_000L, fileSystem.metadataOrNull(files().partial)?.size)
        assertFalse(fileSystem.exists(files().target), "a pause must not promote anything")
    }

    @Test
    fun deletingAnInstallClearsBothFiles() {
        fileSystem.createDirectories(directory)
        fileSystem.write(files().target) { write(body) }
        fileSystem.write(files().partial) { write(body, 0, 10) }

        val installer = installer(StandardTestDispatcher()) { serveWithRanges(it) }
        installer.deleteInstall()

        assertEquals(InstallState.NotInstalled, installer.state.value)
        assertFalse(fileSystem.exists(files().target))
        assertFalse(fileSystem.exists(files().partial))
    }
}
