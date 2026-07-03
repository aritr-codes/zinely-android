package com.aritr.zinely.home

import android.content.Context
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.imposition.Imposer
import com.aritr.zinely.data.android.ProjectDocumentLayout
import com.aritr.zinely.data.android.di.IoDispatcher
import com.aritr.zinely.editor.AssetsDir
import com.aritr.zinely.editor.FileAssetBytesSource
import com.aritr.zinely.render.android.BundledFontResolver
import com.aritr.zinely.render.android.CanvasReplayer
import com.aritr.zinely.render.android.ImageBlitter
import com.aritr.zinely.render.android.ThumbnailRenderer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import javax.inject.Singleton

/**
 * App-layer DI for the Home shelf's thumbnail producer (S6.4, ADR-045). The replayer stack is the
 * export stack — [BundledFontResolver] + [ImageBlitter] over [FileAssetBytesSource] on the shared
 * `@AssetsDir` — so a shelf thumbnail is a miniature of the export by construction (ADR-027/028).
 * The PNG cache lives under `cacheDir/thumbnails`: derived, rebuildable, system-purgeable, and
 * outside `projects/<id>/` so `deleteProject`'s recursive delete never races a thumbnail write.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object HomeModule {

    @Provides
    @Singleton
    fun provideShelfThumbnails(
        @ApplicationContext context: Context,
        layout: ProjectDocumentLayout,
        documents: DocumentRepository,
        imposer: Imposer,
        @AssetsDir assetsDir: File,
        @IoDispatcher io: CoroutineDispatcher,
    ): ShelfThumbnails = ShelfThumbnailProducer(
        thumbsDir = File(context.cacheDir, THUMBNAILS_DIR).toPath(),
        layout = layout,
        documents = documents,
        imposer = imposer,
        raster = AndroidThumbnailRaster(
            renderer = ThumbnailRenderer(
                CanvasReplayer(
                    fontResolver = BundledFontResolver(context.assets),
                    imageBlitter = ImageBlitter(FileAssetBytesSource(assetsDir)),
                ),
            ),
            longestEdgePx = THUMBNAIL_LONGEST_EDGE_PX,
        ),
        io = io,
    )

    private const val THUMBNAILS_DIR = "thumbnails"
}
