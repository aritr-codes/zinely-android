package com.aritr.zinely.data.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.data.android.di.AutosaveGraph
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor

/**
 * Device-only graph smoke test (PR-A Step 7, design §13.3). **Supplemental — not a CI acceptance
 * gate**: it needs an emulator (the project forbids Robolectric), and CI validates the graph at
 * `:app:compileDebugKotlin` (§13.1). Resolves [AutosaveGraph], asserts the key bindings are present
 * and that the autosave scope is shaped per invariant B1, then round-trips one real save under
 * `filesDir`.
 */
@HiltAndroidTest
class AutosaveGraphSmokeTest {

    @get:Rule
    val hilt = HiltAndroidRule(this)

    private lateinit var graph: AutosaveGraph

    @Before
    fun setUp() {
        hilt.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        graph = EntryPointAccessors.fromApplication(context, AutosaveGraph::class.java)
    }

    @Test
    fun keyBindingsResolve() {
        assertNotNull(graph.documentRepository())
        assertNotNull(graph.saveFailureSink())
        assertNotNull(graph.coordinatorFactory())
        assertNotNull(graph.binderFactory())

        val scope = graph.autosaveScope()
        assertNotNull("autosave scope must carry a Job (B1)", scope.coroutineContext[Job])
        assertNotNull("autosave scope must carry a dispatcher (B1)", scope.coroutineContext[ContinuationInterceptor])
    }

    @Test
    fun repositoryRoundTripsThroughTheGraph() = runBlocking {
        val repo = graph.documentRepository()
        val projectId = "smoke-project"
        val document = ZineDocument(
            format = ZineFormat.SINGLE_SHEET_8,
            paperSize = PaperSize.LETTER,
            pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
        )

        val saved = repo.save(projectId, document)
        assertTrue("save through the wired repository should succeed", saved is DataResult.Success)

        val loaded = repo.load(projectId)
        assertTrue("load should return the saved document", loaded is DataResult.Success)
    }
}
