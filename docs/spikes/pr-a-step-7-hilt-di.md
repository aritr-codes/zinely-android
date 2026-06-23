# PR-A Step 7 — Hilt DI Wiring (Design Proposal)

**Status:** Codex gate = **GO** (design approved). **Implemented** on branch `feat/s2b-autosave-coordinator` — awaiting Codex **code** review before commit. **Not committed.**

> **Implementation note (R4 materialized).** The predicted AGP-9-built-in-Kotlin × KSP friction occurred: KSP registers generated sources via the `kotlin.sourceSets` DSL, which AGP 9 rejects ("Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin"). Resolved with the documented flag `android.disallowKotlinSourceSets=false` in `gradle.properties`. Pinned versions (`hilt 2.59.2`, `ksp 2.2.10-2.0.2`) worked as-is; no version bump needed.
**Scope:** Wiring only. Introduces **no** new durability, lifecycle, concurrency, or persistence behavior — all of that is frozen by PR-A Steps 2–6. This step makes `:app` able to resolve the full autosave stack from the Hilt graph at runtime.
**Source of truth:** ADR-026 (autosave ownership), ADR-025 (Android adapter split), ADR-021 (single writer / atomic commit), ADR-024 (desugaring). This proposal records **no** new decision and modifies **no** ADR.

> Codex peer-review pass already run on this draft (read-only). Its findings are folded in and called out inline as **[Codex]**. This document is the artifact for the formal Step-7 design gate.

---

## 0. Inputs (verified against the actual tree)

No Hilt/KSP exists anywhere yet (catalog, plugins, modules all absent). Toolchain: AGP `9.2.1`, Kotlin `2.2.10`, `minSdk 24`, `java.nio` via core-library desugaring (ADR-024). Module direction `:app → :data-android → :core:*` is already wired; `:app` has a plain `MainActivity` and **no** `Application` subclass.

The five frozen adapters are **plain classes with zero DI annotations** today, and Step 7 keeps them that way (see §11/§12). Their real constructors:

| Type (frozen) | Constructor | Notes |
|---|---|---|
| `AndroidFileSystemOps : FileSystemOps` | `()` no-arg | Production wiring of `Os.fsync` dir flush |
| `AtomicFileStore` | `(fs: FileSystemOps)` | `require`s `atomicReplace && fileFsync` caps at construction |
| `DocumentRepositoryImpl : DocumentRepository` | `(rootDir: Path, store: AtomicFileStore, serializer=default, validator=default)` | path-safety chokepoint inside |
| `InMemorySaveFailureSink : SaveFailureSink` | `()` no-arg | app-scoped, in-memory only |
| `AutosaveCoordinatorFactory` | `(autosaveScope: CoroutineScope, ioDispatcher: CoroutineDispatcher, repository: DocumentRepository, failureSink: SaveFailureSink, config: AutosaveConfig = default)` | **`requireNotNull(autosaveScope.coroutineContext[Job])`** |
| `EditorAutosaveBinder` | `(factory, projectId: String, snapshotProvider: DocumentSnapshotProvider, autosaveScope: CoroutineScope, failureSink: SaveFailureSink)` | ctor **eagerly** calls `factory.create(...)` |

Two frozen behaviors drive the whole DI shape:

- **B1 — `autosaveScope` must carry a `Job` *and* the IO dispatcher.** The factory `requireNotNull`s the `Job`. The binder's `flushFromLifecycle()` and `closeProject()` call `autosaveScope.launch { … }` **directly** (not through the factory's per-project IO scope), so those flushes execute on whatever dispatcher the app scope carries. The class comments state they expect IO and must never run on the lifecycle/main thread.
- **B2 — constructing an `EditorAutosaveBinder` is side-effecting.** Its constructor immediately calls `factory.create(projectId, snapshotProvider)`, registering the project in the single-writer registry and launching a failure collector. A binder therefore must be created **only** with a real, open `projectId` — never eagerly by the graph.

---

## 1. Hilt ownership model

- **`:app` owns the root.** Add a `@HiltAndroidApp` `ZinelyApplication` and register it via `android:name=".ZinelyApplication"` in the manifest. `MainActivity` is left unchanged for now (it injects nothing yet; editor-screen `@AndroidEntryPoint` wiring is later, post-PR-A).
- **`:data-android` owns the bindings.** All `@Module`/`@Provides` live in a new `com.aritr.zinely.data.android.di` package, installed in `SingletonComponent`. Bindings sit beside the adapters they assemble.
- **`:core:*` stays DI-free and Android-free** (ADR-025). No Hilt symbol, not even a qualifier annotation, enters core.

**[Codex] A:** co-locating modules in `:data-android` is preferred over dumping them all in `:app`; wiring stays beside the adapters and `:app` stays thin. Trade-off acknowledged in §14.

## 2. Scope design

Everything is a process-lifetime singleton in `SingletonComponent`:

`@IoDispatcher CoroutineDispatcher`, `@AutosaveScope CoroutineScope`, `FileSystemOps`, `AtomicFileStore`, `DocumentRepository`, `SaveFailureSink`, `AutosaveCoordinatorFactory`, `EditorAutosaveBinderFactory`.

`EditorAutosaveBinder` is **deliberately not a Hilt binding and not scoped** — see B2. It is produced on demand by `EditorAutosaveBinderFactory.create(projectId, snapshotProvider)` and owned/disposed by the editor layer (Step 6 lifecycle contract). Making it any kind of Hilt-instantiated binding would eagerly register a bogus project at graph init.

## 3. Qualifier strategy

Two `@Qualifier @Retention(BINARY)` annotations in `:data-android/di`:

- `@IoDispatcher` on `CoroutineDispatcher`
- `@AutosaveScope` on `CoroutineScope`

Rationale: `CoroutineDispatcher` and `CoroutineScope` are framework types with multiple legitimate future bindings (Default/Main dispatchers, other scopes); unqualified bindings would be ambiguous and silently fragile. Only the bindings actually needed now are defined; the pattern extends cleanly. Qualifiers live in `:data-android` (not core) so no DI concept leaks into the pure modules.

## 4. CoroutineScope provisioning

```
@Provides @Singleton @AutosaveScope
fun provideAutosaveScope(@IoDispatcher io: CoroutineDispatcher): CoroutineScope =
    CoroutineScope(SupervisorJob() + io + CoroutineName("autosave"))
```

- `SupervisorJob()` satisfies the factory's `requireNotNull(Job)` (B1) and isolates per-project failures from one another and from the app scope.
- **`+ io` is load-bearing, not cosmetic** (B1): the binder launches lifecycle/teardown flushes directly on this scope. **[Codex] F: confirmed — a non-IO scope is a real correctness bug**, running flush/`close` work on Main/Default despite the factory using IO for its own per-project scopes.
- Process-lifetime; never explicitly cancelled. Teardown of individual projects is the binder's job (`closeProject`), not scope cancellation. **[Codex] D: acceptable**, but it relocates the leak risk to binder ownership (B2): a binder that is never closed leaks its coordinator + collector for the process lifetime. That ownership lives in Step 6 / the editor and is out of Step 7's scope.

## 5. CoroutineDispatcher provisioning

```
@Provides @IoDispatcher
fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
```

Provided behind the qualifier so tests can override it and so the IO dispatcher is a single shared definition consumed by both the app scope (§4) and the factory.

## 6. SaveFailureSink bindings

```
@Provides @Singleton
fun provideSaveFailureSink(): SaveFailureSink = InMemorySaveFailureSink()
```

`@Provides` (not `@Binds` + `@Inject` constructor) so the frozen `InMemorySaveFailureSink` gains no DI annotation. Singleton because the sink is the single app-wide failure surface the editor observes.

## 7. DocumentRepository bindings

```
@Provides @Singleton
fun provideDocumentRepository(
    @ApplicationContext ctx: Context,
    store: AtomicFileStore,
): DocumentRepository =
    DocumentRepositoryImpl(rootDir = ctx.filesDir.toPath(), store = store)
```

- **[Codex] #1 fix folded in:** `AtomicFileStore` is an **explicit provider parameter**, injected from the graph (§8) — not constructed inline — so there is exactly one store instance and the binding compiles.
- `rootDir = ctx.filesDir.toPath()` pins storage to **app-private internal storage** (ADR-025 / ADR-026); no external/cache dirs. Avoids a bare `Path` binding (which would be ambiguous) by deriving it inside the method from `@ApplicationContext`.
- One repository instance serves all projects; per-call path resolution + the factory's per-project single-writer guard keep that safe.

## 8. FileSystemOps bindings

```
@Provides @Singleton
fun provideFileSystemOps(): FileSystemOps = AndroidFileSystemOps()

@Provides @Singleton
fun provideAtomicFileStore(fs: FileSystemOps): AtomicFileStore = AtomicFileStore(fs)
```

`AndroidFileSystemOps` advertises `dirFsync = true`; `AtomicFileStore`'s constructor `require` (atomicReplace + fileFsync caps) is satisfied at graph construction, so a mis-wire fails fast at first injection.

## 9. AutosaveCoordinatorFactory provisioning

```
@Provides @Singleton
fun provideAutosaveCoordinatorFactory(
    @AutosaveScope scope: CoroutineScope,
    @IoDispatcher io: CoroutineDispatcher,
    repository: DocumentRepository,
    failureSink: SaveFailureSink,
): AutosaveCoordinatorFactory =
    AutosaveCoordinatorFactory(scope, io, repository, failureSink)  // config = frozen default
```

`config` is left at its frozen default; making `AutosaveConfig` tunable is explicitly **not** in this step (would need its own ADR).

## 10. EditorAutosaveBinder creation mechanism

A **new, hand-written** factory (Step-7 code, not a frozen class):

```
class EditorAutosaveBinderFactory @Inject constructor(
    private val factory: AutosaveCoordinatorFactory,
    @AutosaveScope private val autosaveScope: CoroutineScope,
    private val failureSink: SaveFailureSink,
) {
    fun create(projectId: String, snapshotProvider: DocumentSnapshotProvider): EditorAutosaveBinder =
        EditorAutosaveBinder(factory, projectId, snapshotProvider, autosaveScope, failureSink)
}
```

Constructor-injected (the `@Inject` is on this **new** class, never on a frozen one). `projectId` and `snapshotProvider` are the runtime parameters; the three singletons come from the graph. The editor calls `create(...)` when a project opens and disposes the returned binder when it closes (B2).

## 11. Assisted injection requirements

**None.** The manual factory in §10 is chosen **over** Hilt `@AssistedInject` / `@AssistedFactory` deliberately: the Hilt route requires annotating the **frozen** `EditorAutosaveBinder` constructor (`@AssistedInject`, `@Assisted`) and coupling that class to `dagger.assisted`. The hand-written factory yields identical ergonomics with zero edits to frozen files and no DI coupling in the adapter. Trade-off in §14.

## 12. Module boundaries

- All DI lives in `:data-android/di` (modules, qualifiers, the binder factory, the entry point) **+** `:app` (the `@HiltAndroidApp` Application).
- `:core:*` remains Android-free and DI-free (ADR-025).
- **`@Provides`-only, no `@Binds`, no `@Inject` on frozen adapters.** **[Codex] E: correct and not wasteful** for annotation-free frozen classes — `@Binds` would itself demand an injectable implementation binding, buying nothing here. The only `@Inject` constructor in the step is on the new `EditorAutosaveBinderFactory`.

## 13. Graph validation / testing strategy

Per the handoff ("TDD where meaningful; otherwise rely on compile-time graph verification — decide in the design"), the decision is:

> **[Codex Fix #1, High] Correction to the prior draft.** The earlier draft claimed compile-time graph validation "runs in CI." That was **wrong**: CI runs with `ZINELY_CORE_ONLY=true`, and `settings.gradle.kts` gates **both** `:app` and `:data-android` out under that flag — so the Android/Hilt graph is **never compiled in CI today** and no current job would catch a broken binding. Step 7 must therefore **add** the validating job; it is not pre-existing.

1. **Primary gate — Android-SDK compile job (NEW Step-7 deliverable, required acceptance gate).** Add a CI job that:
   - runs with **`ZINELY_CORE_ONLY` unset** (so `:app` and `:data-android` are included), on a runner with an Android SDK;
   - executes **`:app:compileDebugKotlin`** (sufficient — KSP/Hilt annotation processing and graph validation run during Kotlin compilation; `:app:assembleDebug` also works but is heavier and needs no extra coverage here);
   - **fails the build on any missing, duplicate, or cyclic binding** across the merged `SingletonComponent` graph.
   This job is the binding-correctness gate. Without it, the EntryPoint below and every `@Provides` are unverified.
2. **Compile-validation surface — a main-source `@EntryPoint`** (**[Codex] #2**). In `:data-android` main source, an entry point enumerating the key bindings:
   ```
   @EntryPoint @InstallIn(SingletonComponent::class)
   interface AutosaveGraph {
       fun documentRepository(): DocumentRepository
       fun saveFailureSink(): SaveFailureSink
       fun coordinatorFactory(): AutosaveCoordinatorFactory
       fun binderFactory(): EditorAutosaveBinderFactory
       @AutosaveScope fun autosaveScope(): CoroutineScope
   }
   ```
   The graph must satisfy this entry point, so **every** listed binding — notably `EditorAutosaveBinderFactory`, which has no production injection site yet — is forced into the graph and validated. **This mechanism is only meaningful once the job in (1) actually compiles the Android modules**: the `@EntryPoint` is processed during `:app`'s Kotlin/KSP compilation, which the core-only CI never triggers. (1) + (2) are a pair — the EntryPoint defines *what* is validated; the new CI job is *when* it is validated.
3. **Supplemental — device-only `@HiltAndroidTest` smoke test (NOT an acceptance gate).** Resolve `AutosaveGraph`, assert bindings non-null and that `autosaveScope` carries a `Job` + IO dispatcher (guards B1), and do one `repository.save` → `load` round-trip under `filesDir`. This is **supplemental only**: CI has no emulator (instrumented tests are already excluded, like Step 2's real-`Os` durability checks) and the project forbids Robolectric, so there is no JVM Hilt test. The device smoke test runs locally / when an emulator is available and **does not gate Step-7 acceptance unless and until emulator CI exists**. Mandatory graph correctness rests entirely on (1) + (2).

## 14. Risks, alternatives, and tradeoffs

| # | Risk / decision | Resolution |
|---|---|---|
| R1 | **Non-IO app scope** would silently run lifecycle/teardown flushes off-IO (B1, **[Codex] F**). | `@AutosaveScope` is built with `+ io`; the smoke test asserts the scope's dispatcher/Job. Documented as load-bearing. |
| R2 | **Eager binder = bogus registration** (B2). | Binder is never a Hilt binding; only produced via `EditorAutosaveBinderFactory.create(realProjectId, …)`. |
| R3 | **`EditorAutosaveBinderFactory` binding unverified** if nothing requests it (**[Codex] #2**). | Main-source `@EntryPoint` lists it → compile-validated in CI. |
| R4 | **Hilt/KSP × AGP 9.2.1 × Kotlin 2.2.10** (note: `:data-android` uses AGP-9 built-in Kotlin, applies **no** `kotlin-android` plugin). **[Codex] #4 / C; Codex Fix #2.** | **Design baseline — exact pins, not a floor:** `hilt = 2.59.2`, `ksp = 2.2.10-2.0.2`. (2.59 added AGP 9 support; .1/.2 fixed AGP 9 Jetifier/plugin issues. `2.2.10-2.0.2` is the AGP-9 built-in-Kotlin KSP baseline.) Implementation uses **exactly these** unless it deliberately verifies and pins a newer compatible pair — no "verify later / unpinned" wording. |
| R5 | **Hilt gradle plugin in `:data-android`** (modules-only, no `@AndroidEntryPoint`) — needed or just `ksp(hilt-compiler)`? **[Codex] B/#3.** | Apply the **Hilt gradle plugin + KSP** in `:data-android` (not ksp alone) and set `enableAggregatingTask = true`; the "ksp-only is enough" path is fragile. |
| R6 | App-scoped scope never cancelled + binder launches on it (**[Codex] D**). | Acceptable (process lifetime); leak risk is binder ownership, owned by Step 6 / editor — out of Step 7 scope. |
| R7 | `DocumentRepository` provider must take `AtomicFileStore` as a parameter (**[Codex] #1**). | Done in §7. |
| **Alt-A** | Put all `@Module`s in `:app`, keep `:data-android` Hilt-free. | **Rejected** — makes `:app` a wiring dump and divorces bindings from the adapters; co-location in `:data-android` is idiomatic multi-module Hilt and `:app` stays thin. |
| **Alt-B** | Hilt `@AssistedInject`/`@AssistedFactory` for the binder. | **Rejected** — annotates the **frozen** `EditorAutosaveBinder` and couples it to `dagger.assisted`; the manual factory (§10) is behavior-identical with zero frozen-file edits. |

### Build changes implied (catalog + Gradle)
- `gradle/libs.versions.toml`: add `hilt = "2.59.2"` and `ksp = "2.2.10-2.0.2"` versions; `hilt-android` + `hilt-compiler` libraries; `com.google.dagger.hilt.android` + `com.google.devtools.ksp` plugins.
- `:app/build.gradle.kts`: apply hilt + ksp plugins; `implementation(hilt-android)`, `ksp(hilt-compiler)`.
- `:data-android/build.gradle.kts`: apply hilt + ksp plugins (`enableAggregatingTask = true`); `implementation(hilt-android)`, `ksp(hilt-compiler)`.
- **CI (`.github/workflows`, or equivalent):** add the Android-SDK job from §13.1 (`ZINELY_CORE_ONLY` unset, `:app:compileDebugKotlin`) as a **required** check. The existing `ZINELY_CORE_ONLY=true` core-only job stays as-is.
- **Privacy invariant intact:** Hilt/Dagger/KSP are build/DI-time only — no networking, account, cloud, or analytics dependency added.

### Acceptance criteria (Step-7 implementation gate)
A Step-7 implementation is accepted only when **all** hold:
1. **Mandatory:** a CI job with `ZINELY_CORE_ONLY` **unset** runs `:app:compileDebugKotlin` (or `:app:assembleDebug`) on an Android-SDK runner and **passes** — this is the binding-correctness gate (§13.1).
2. **Mandatory:** the `AutosaveGraph` `@EntryPoint` (§13.2) exists in `:data-android` main source and lists all key bindings, so that job actually validates them.
3. **Mandatory:** the core-only CI job (`ZINELY_CORE_ONLY=true`) still passes — Step 7 adds no Android dependency to any `:core:*` module (ADR-025).
4. **Mandatory:** tooling pinned to `hilt = 2.59.2`, `ksp = 2.2.10-2.0.2` (or a deliberately-verified newer compatible pair).
5. **Mandatory:** no networking/account/cloud/analytics dependency introduced (privacy invariant).
6. **Supplemental, NOT a gate:** the device-only `@HiltAndroidTest` smoke test (§13.3) — required only if/when emulator CI exists.

---

## Codex review record

**Pass 1 (pre-gate, read-only draft review).** Four findings + answers A–F. All four **ACCEPTED and folded in**: #1 (explicit `AtomicFileStore` param → §7), #2 (main-source `@EntryPoint` → §13), #3 (Hilt plugin + `enableAggregatingTask` in `:data-android` → R5), #4 (version pins → R4). Answers A/E/F confirmed the chosen design; B/C/D folded into §1/R4/R6.

**Pass 2 (formal design gate): verdict = GO WITH FIXES.** Architecture approved, no redesign required. Three fixes, all **ACCEPTED and folded in**:
- **Fix #1 (High) — Android graph validation must be a CI gate.** The prior draft wrongly assumed CI already compiles the Android graph; CI runs `ZINELY_CORE_ONLY=true`, which gates out `:app` **and** `:data-android`, so the Hilt graph is never compiled. → §13 rewritten: a NEW Android-SDK CI job (`ZINELY_CORE_ONLY` unset, `:app:compileDebugKotlin`) is now a required acceptance gate; the EntryPoint is meaningful only when that job runs. Acceptance criteria added.
- **Fix #2 (Medium) — pin exact versions.** → R4 + build block now state `hilt = 2.59.2`, `ksp = 2.2.10-2.0.2` as the baseline (no "verify later" wording).
- **Clarification (Low) — device smoke test is supplemental.** → §13.3 + acceptance criteria: CI graph compile is mandatory; `@HiltAndroidTest` is supplemental and not a gate unless emulator CI exists.

No disagreements outstanding. Ready for a **short final Codex confirmation** before implementation.
