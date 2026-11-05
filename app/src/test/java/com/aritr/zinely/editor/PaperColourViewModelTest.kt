package com.aritr.zinely.editor

import androidx.lifecycle.SavedStateHandle
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.PaperColour
import com.aritr.zinely.core.data.repository.ProjectRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** AW6 in the editor: the one paper-colour value the Bench and the Proof share, read from and written to meta.json. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PaperColourViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    /** Only the two calls under test; anything else the VM touched would throw and fail the test. */
    private class Repo(
        val stored: CompletableDeferred<PaperColour> = CompletableDeferred(PaperColour.WHITE),
        val result: DataResult<Unit> = DataResult.Success(Unit),
    ) : ProjectRepository by unused() {
        val writes = mutableListOf<Pair<String, PaperColour>>()
        override suspend fun getPaperColour(id: String): PaperColour = stored.await()
        override suspend fun setPaperColour(id: String, colour: PaperColour): DataResult<Unit> {
            writes += id to colour
            return result
        }
    }

    private fun vm(repo: Repo) = PaperColourViewModel(SavedStateHandle(mapOf("projectId" to "z1")), repo)

    @Test
    fun `it reads the zine's stored colour`() {
        val vm = vm(Repo(CompletableDeferred(PaperColour.PASTEL_PINK)))
        assertEquals(PaperColour.PASTEL_PINK, vm.paperColour.value)
    }

    @Test
    fun `a pick shows at once and is written for this zine`() {
        val repo = Repo()
        val vm = vm(repo)
        vm.setPaperColour(PaperColour.GREY)
        assertEquals(PaperColour.GREY, vm.paperColour.value)
        assertEquals(listOf("z1" to PaperColour.GREY), repo.writes)
    }

    @Test
    fun `re-picking the current colour writes nothing`() {
        val repo = Repo()
        vm(repo).setPaperColour(PaperColour.WHITE)
        assertEquals(emptyList<Pair<String, PaperColour>>(), repo.writes)
    }

    @Test
    fun `a refused write puts the stored colour back`() {
        val repo = Repo(result = DataResult.Failure(DataError.Io("disk")))
        val vm = vm(repo)
        vm.setPaperColour(PaperColour.IVORY)
        assertEquals(PaperColour.WHITE, vm.paperColour.value)
    }

    @Test
    fun `a refused write shows what meta json holds, not the colour before the pick`() {
        val stored = CompletableDeferred<PaperColour>()
        val vm = vm(Repo(stored, result = DataResult.Failure(DataError.Io("disk"))))
        vm.setPaperColour(PaperColour.IVORY)
        stored.complete(PaperColour.PASTEL_PINK)
        assertEquals(PaperColour.PASTEL_PINK, vm.paperColour.value)
    }

    @Test
    fun `a pick made before the read returns is not overwritten by the read`() {
        val stored = CompletableDeferred<PaperColour>()
        val vm = vm(Repo(stored))
        vm.setPaperColour(PaperColour.PASTEL_GREEN)
        stored.complete(PaperColour.WHITE)
        assertEquals(PaperColour.PASTEL_GREEN, vm.paperColour.value)
    }
}

private fun unused(): ProjectRepository = Proxy.newProxyInstance(
    ProjectRepository::class.java.classLoader,
    arrayOf(ProjectRepository::class.java),
) { _, method, _ -> throw UnsupportedOperationException(method.name) } as ProjectRepository
