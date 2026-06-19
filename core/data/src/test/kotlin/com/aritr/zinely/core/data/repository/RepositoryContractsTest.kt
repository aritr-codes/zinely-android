package com.aritr.zinely.core.data.repository

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The repository contracts must be usable with the sealed `DataResult` boundary and coroutine/Flow
 * surface (S2 spike §1). A tiny in-memory fake proves the interfaces compose end-to-end — the real
 * Room/file-backed implementations arrive in S2B.
 */
class RepositoryContractsTest {

    private class FakeRepository : ProjectRepository, DocumentRepository {
        private val docs = LinkedHashMap<String, ZineDocument>()
        private val summaries = MutableStateFlow<List<ProjectSummary>>(emptyList())

        override fun observeProjects(): Flow<List<ProjectSummary>> = summaries

        override suspend fun createProject(
            title: String,
            format: ZineFormat,
            paperSize: PaperSize,
        ): DataResult<ProjectSummary> {
            val summary = ProjectSummary(
                id = "p${summaries.value.size + 1}",
                title = title,
                format = format,
                paperSize = paperSize,
                createdAtEpochMs = 0L,
                updatedAtEpochMs = 0L,
                documentSchemaVersion = 1,
            )
            summaries.value = summaries.value + summary
            docs[summary.id] = ZineDocument(format = format, paperSize = paperSize)
            return DataResult.Success(summary)
        }

        override suspend fun getProject(id: String): DataResult<ProjectSummary> =
            summaries.value.firstOrNull { it.id == id }?.let { DataResult.Success(it) }
                ?: DataResult.Failure(DataError.NotFound(id))

        override suspend fun renameProject(id: String, title: String): DataResult<Unit> {
            val exists = summaries.value.any { it.id == id }
            if (!exists) return DataResult.Failure(DataError.NotFound(id))
            summaries.value = summaries.value.map { if (it.id == id) it.copy(title = title) else it }
            return DataResult.Success(Unit)
        }

        override suspend fun duplicateProject(id: String): DataResult<ProjectSummary> {
            val source = summaries.value.firstOrNull { it.id == id }
                ?: return DataResult.Failure(DataError.NotFound(id))
            val copy = source.copy(id = "p${summaries.value.size + 1}", title = "${source.title} copy")
            summaries.value = summaries.value + copy
            docs[copy.id] = docs.getValue(id)
            return DataResult.Success(copy)
        }

        override suspend fun deleteProject(id: String): DataResult<Unit> {
            docs.remove(id)
            summaries.value = summaries.value.filterNot { it.id == id }
            return DataResult.Success(Unit)
        }

        override suspend fun load(projectId: String): DataResult<ZineDocument> =
            docs[projectId]?.let { DataResult.Success(it) } ?: DataResult.Failure(DataError.NotFound(projectId))

        override suspend fun save(projectId: String, document: ZineDocument): DataResult<Unit> {
            if (projectId !in docs) return DataResult.Failure(DataError.NotFound(projectId))
            docs[projectId] = document
            return DataResult.Success(Unit)
        }
    }

    @Test
    fun `create then load round-trips through the result boundary`() = runTest {
        val repo = FakeRepository()
        val created = repo.createProject("My Zine", ZineFormat.SINGLE_SHEET_8, PaperSize.A4)
        assertTrue(created.isSuccess)
        val id = created.getOrNull()!!.id

        val loaded = repo.load(id)
        assertEquals(PaperSize.A4, loaded.getOrNull()?.paperSize)
        assertEquals(listOf("My Zine"), repo.observeProjects().first().map { it.title })
    }

    @Test
    fun `loading a missing project is a typed NotFound failure`() = runTest {
        val repo = FakeRepository()
        val result = repo.load("nope")
        assertEquals(DataError.NotFound("nope"), result.errorOrNull())
    }

    @Test
    fun `delete removes the project from the observable list`() = runTest {
        val repo = FakeRepository()
        val id = repo.createProject("X", ZineFormat.SINGLE_SHEET_8, PaperSize.LETTER).getOrNull()!!.id
        repo.deleteProject(id)
        assertTrue(repo.observeProjects().first().isEmpty())
    }
}
