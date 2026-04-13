---
name: data-api-integration
description: Data layer with Retrofit, Room, Koin, and repositories
---

# Data & API Integration

## Tech Stack
- Retrofit + OkHttp for networking
- Room for local database
- Koin for dependency injection
- Kotlin Coroutines + Flow
- Repository pattern

## Required Patterns

### Repository Pattern

```kotlin
// Interface in domain/data layer
interface UserRepository {
    fun getUsers(): Flow<Result<List<User>>>
    suspend fun getUserById(id: String): Result<User>
}

// Implementation in data layer
class UserRepositoryImpl(
    private val api: UserApi,
    private val dao: UserDao
) : UserRepository {
    override fun getUsers(): Flow<Result<List<User>>> = flow {
        emit(Result.Loading)
        try {
            val users = api.getUsers()
            dao.insertAll(users)
            emit(Result.Success(users))
        } catch (e: Exception) {
            // Try cache on network failure
            val cached = dao.getAll()
            if (cached.isNotEmpty()) {
                emit(Result.Success(cached))
            } else {
                emit(Result.Error(e))
            }
        }
    }.flowOn(Dispatchers.IO)
}
```

### Koin Modules

```kotlin
// Define modules in Koin modules
val networkModule = module {
    single { get<Retrofit>().create(UserApi::class.java) }
    single { HttpLoggingInterceptor().apply { level = Level.BODY } }
}

val databaseModule = module {
    single { Room.databaseBuilder(androidContext(), AppDatabase::class.java, "app.db").build() }
    single { get<AppDatabase>().userDao() }
}

val repositoryModule = module {
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get()) }
}

// Start Koin in Application class
startKoin {
    androidContext(this@MyApplication)
    modules(listOf(networkModule, databaseModule, repositoryModule, viewModelModule))
}
```

## Prohibited Patterns

1. **Never put API keys in code** - use `BuildConfig` or secure storage
2. **Don't make network calls on main thread** - always use `Dispatchers.IO`
3. **Never inject Activity/Fragment** into singletons - use `androidContext()` for Application context
4. **Don't return `null` from repositories** - use `Result`, `Optional`, or sealed classes

## Error Handling

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

## Coroutines Best Practices

- Use `viewModelScope.launch` for UI-triggered operations
- Use `lifecycleScope.launch` only for lifecycle-bound operations
- Pass `CoroutineContext` as constructor parameter, don't hardcode
- Use `withContext` instead of `async().await()` for sequential operations
- Cancel coroutines when ViewModel is cleared (default with viewModelScope)

## Testing

- Mock repositories in unit tests
- Use `runTest` for coroutine testing
- Test success, error, and loading states
