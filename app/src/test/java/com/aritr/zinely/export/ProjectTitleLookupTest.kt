package com.aritr.zinely.export

import androidx.lifecycle.SavedStateHandle
import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.ProjectRepository
import com.aritr.zinely.core.data.repository.ProjectSummary
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The real title lookup behind the saved file's name: Proof route argument → [ProjectRepository] → title. */
@RunWith(RobolectricTestRunner::class)
class ProjectTitleLookupTest {

    private fun repo(result: (String) -> DataResult<ProjectSummary>): ProjectRepository =
        java.lang.reflect.Proxy.newProxyInstance(
            ProjectRepository::class.java.classLoader,
            arrayOf(ProjectRepository::class.java),
        ) { _, method, args ->
            check(method.name == "getProject") { "unexpected ${method.name}" }
            result(args[0] as String)
        } as ProjectRepository

    private fun summary(id: String, title: String) =
        ProjectSummary(id, title, ZineFormat.SINGLE_SHEET_8, PaperSize.A4, 0L, 0L, 1)

    private fun handle(id: String) = SavedStateHandle(mapOf("projectId" to id))

    @Test
    fun readsTheTitleOfTheRoutesProject() = runTest {
        val titleOf = projectTitleOf(handle("p1"), repo { DataResult.Success(summary(it, "My Zine")) })
        assertEquals("My Zine", titleOf())
    }

    @Test
    fun missingZineFallsBackToZine() = runTest {
        val titleOf = projectTitleOf(handle("p1"), repo { DataResult.Failure(DataError.NotFound(it)) })
        assertEquals("zine", ExportNaming.sanitizeBase(titleOf()))
    }

    @Test
    fun failingReadFallsBackToZine() = runTest {
        val titleOf = projectTitleOf(handle("p1"), repo { throw java.io.IOException("disk") })
        assertEquals("zine", ExportNaming.sanitizeBase(titleOf()))
    }

    @Test
    fun missingRouteArgumentFallsBackToZine() = runTest {
        val titleOf = projectTitleOf(SavedStateHandle(), repo { DataResult.Success(summary(it, "x")) })
        assertEquals("zine", ExportNaming.sanitizeBase(titleOf()))
    }
}
