# Android Project — Compose + Hilt + Room

## Build Commands
- `./gradlew assembleDebug` — build debug APK
- `./gradlew assembleRelease` — build release APK
- `./gradlew test` — run unit tests (JVM)
- `./gradlew connectedAndroidTest` — run instrumented tests on device/emulator
- `./gradlew lint` — run lint checks
- `./gradlew kspDebugKotlin` — run KSP (triggers Room/Hilt code generation)
- `./gradlew clean` — clean build outputs

## SDK & Config
- minSdk: 26
- targetSdk: 35
- compileSdk: 35
- Kotlin: 2.x
- AGP: 8.x
- Java: 17

## Architecture: MVVM + Clean Architecture

```
ui/
  screens/          # Composable screens
  components/       # Reusable UI components
  theme/            # MaterialTheme, colors, typography
domain/
  model/            # Pure Kotlin data models
  repository/       # Repository interfaces
  usecase/          # Optional: use cases for complex logic
data/
  local/
    db/             # AppDatabase, DAOs
    entity/         # Room @Entity classes
  remote/           # Retrofit services (if applicable)
  repository/       # Repository implementations
di/                 # Hilt modules
```

## Jetpack Compose Rules
- Screens go in `ui/screens/`, named `<Feature>Screen.kt`
- Every screen takes a `ViewModel` via `hiltViewModel()`
- Screens only observe `uiState: StateFlow<UiState>` — no business logic in Composables
- Composables must be stateless where possible; pass state and lambdas down
- Use `LaunchedEffect` for one-time side effects (navigation, snackbars)
- Previews required for all non-trivial Composables using `@Preview`
- Use `Scaffold`, `TopAppBar`, `SnackbarHost` from Material 3

### UiState Pattern
```kotlin
data class FeatureUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### ViewModel Pattern
```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getItems()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { items -> _uiState.update { it.copy(items = items, isLoading = false) } }
        }
    }
}
```

## Hilt Rules
- All ViewModels use `@HiltViewModel` + `@Inject constructor`
- Modules live in `di/` package, one file per concern (e.g. `DatabaseModule`, `NetworkModule`)
- Use `@Singleton` scope for repositories and database
- Use `@ViewModelScoped` for dependencies tied to ViewModel lifecycle
- Bind interfaces to implementations via `@Binds` in abstract modules
- Application class must be annotated `@HiltAndroidApp`
- Activities/Fragments that host Compose must be annotated `@AndroidEntryPoint`

### Module Pattern
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFeatureRepository(
        impl: FeatureRepositoryImpl
    ): FeatureRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app.db").build()

    @Provides
    fun provideFeatureDao(db: AppDatabase): FeatureDao = db.featureDao()
}
```

## Room Rules
- Entities go in `data/local/entity/`, named `<Feature>Entity.kt`
- DAOs go in `data/local/db/`, named `<Feature>Dao.kt`
- DAOs return `Flow<T>` for queries that the UI observes
- DAOs use `suspend` for insert/update/delete
- Map between Entity ↔ domain model in the repository (never expose entities to the UI layer)
- Increment `version` and provide a `Migration` for every schema change — never use `fallbackToDestructiveMigration()` in production

### DAO Pattern
```kotlin
@Dao
interface FeatureDao {
    @Query("SELECT * FROM feature_table ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FeatureEntity>>

    @Query("SELECT * FROM feature_table WHERE id = :id")
    suspend fun getById(id: Int): FeatureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FeatureEntity)

    @Update
    suspend fun update(entity: FeatureEntity)

    @Delete
    suspend fun delete(entity: FeatureEntity)
}
```

### Entity Pattern
```kotlin
@Entity(tableName = "feature_table")
data class FeatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

## Repository Pattern
- Repositories implement an interface defined in `domain/repository/`
- Implementations live in `data/repository/`
- Use `Flow.map {}` to convert entities to domain models
- Wrap non-Flow operations in `withContext(Dispatchers.IO)`

```kotlin
class FeatureRepositoryImpl @Inject constructor(
    private val dao: FeatureDao
) : FeatureRepository {

    override fun getItems(): Flow<List<Item>> =
        dao.getAll().map { entities -> entities.map { it.toDomainModel() } }

    override suspend fun addItem(item: Item) = withContext(Dispatchers.IO) {
        dao.insert(item.toEntity())
    }
}
```

## Kotlin Conventions
- Kotlin strict mode — no `!!` force-unwrap; use `?: return`, `?.let`, or `requireNotNull`
- Prefer `data class` for models, `sealed class` or `sealed interface` for states/events
- Use `object` for singletons and companion objects for factory methods
- Extension functions for mapping: `fun FeatureEntity.toDomainModel()`, `fun Item.toEntity()`
- All coroutines launched in `viewModelScope` or `Dispatchers.IO`; never `GlobalScope`
- Avoid `runBlocking` outside of tests

## Testing
- Unit tests: JUnit 5 + MockK + Turbine (for Flow testing)
- Use `TestCoroutineDispatcher` / `UnconfinedTestDispatcher` in ViewModel tests
- Room tests: use in-memory database (`Room.inMemoryDatabaseBuilder`)
- UI tests: Compose testing APIs (`createComposeRule`)
- Test file mirrors source: `FeatureViewModel` → `FeatureViewModelTest`

### ViewModel Test Pattern
```kotlin
@ExtendWith(CoroutineTestExtension::class)
class FeatureViewModelTest {
    private val repository: FeatureRepository = mockk()
    private lateinit var viewModel: FeatureViewModel

    @BeforeEach
    fun setup() {
        every { repository.getItems() } returns flowOf(emptyList())
        viewModel = FeatureViewModel(repository)
    }

    @Test
    fun `initial state is empty and not loading`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.items).isEmpty()
            assertThat(state.isLoading).isFalse()
        }
    }
}
```

## Do Not
- Do NOT put business logic in Composables
- Do NOT expose Room entities above the repository layer
- Do NOT use `LiveData` — use `StateFlow` and `Flow`
- Do NOT use `runBlocking` in production code
- Do NOT use `fallbackToDestructiveMigration()` in release builds
- Do NOT skip migrations when changing Room schema
- Do NOT hardcode strings in Composables — use `strings.xml`
- Do NOT suppress lint warnings without a comment explaining why
