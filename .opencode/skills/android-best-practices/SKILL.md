---
name: android-best-practices
description: Android architecture, Koin DI, coroutines, and security best practices
---

# Android Best Practices

## Architecture

Follow [Android Architecture Guide](https://developer.android.com/topic/architecture/recommendations):

- **UI Layer**: Compose UI + ViewModel
- **Domain Layer**: Use cases (optional for simple apps)
- **Data Layer**: Repositories + Data Sources (Remote/Local)

```
UI (Composables) -> ViewModel -> Repository -> DataSource
```

### ViewModel Pattern

```kotlin
@KoinViewModel
class HomeViewModel(
    private val repository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun loadUsers() {
        viewModelScope.launch {
            repository.getUsers().collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> _uiState.value.copy(isLoading = true)
                    is Result.Success -> _uiState.value.copy(
                        isLoading = false,
                        users = result.data
                    )
                    is Result.Error -> _uiState.value.copy(
                        isLoading = false,
                        error = result.exception.message
                    )
                }
            }
        }
    }
}
```

## Koin Best Practices

### Memory Management

```kotlin
// ✅ Good - Application context in singleton
module {
    single { SomeService(androidContext()) }
}

// ❌ Bad - Activity leak
module {
    single { SomeService(get<Activity>()) }
}

// ✅ Good - Activity scope for activity-bound dependencies
module {
    activityScope { scoped { MyScopedService() } }
}
```

### Close Scopes Properly

```kotlin
// Use ScopeActivity for automatic scope management
class MyActivity : ScopeActivity() {
    override val scope: Scope by activityScope()
    // Scope automatically closed in onDestroy
}
```

## Coroutines Best Practices

See [official docs](https://developer.android.com/kotlin/coroutines/coroutines-best-practices):

1. **Don't pass `CoroutineScope` to repository/manager classes** - pass `CoroutineContext` instead
2. **Use `viewModelScope`** for ViewModel coroutines
3. **Use `lifecycleScope`** only for lifecycle-bound operations
4. **Prefer `withContext`** over `async().await()`
5. **Avoid `GlobalScope`**

```kotlin
// ✅ Good - Pass context
class UserRepository(
    private val context: CoroutineContext = Dispatchers.IO
) {
    suspend fun getUser(): User = withContext(context) { api.getUser() }
}

// ❌ Bad - Hardcoded scope
class UserRepository {
    suspend fun getUser() = viewModelScope.launch { /* ... */ }
}
```

## Security

1. **Never hardcode secrets** - use `BuildConfig` or encrypted `SharedPreferences`
2. **Use `OkHttp` interceptors** for auth tokens from secure storage
3. **Enable network security config** for production

```kotlin
// ✅ Good - Secrets from BuildConfig
module {
    single {
        Retrofit.Builder()
            .addInterceptor { chain ->
                val token = get<SecurePreferences>().getToken()
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                )
            }
            .build()
    }
}

// ❌ Bad - Hardcoded secrets
module {
    single { ApiService("super-secret-key") }
}
```

## Android Scopes with Koin

```kotlin
// Activity-scoped
class MainActivity : ScopeActivity() {
    override val scope: Scope by activityScope()
    
    private val viewModel: HomeViewModel by inject()
}

// Fragment-scoped
class MyFragment : ScopeFragment() {
    override val scope: Scope by fragmentScope()
}

// WorkManager
class MyWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    val koin = KoinApplication.application.koin
    val repository: UserRepository by koin.inject()
}
```

## Clean Code

- Keep ViewModels thin - delegate to repositories
- Use sealed classes for UI states and events
- Avoid `!!` - use safe calls or `checkNotNull`
- Use `@Suppress` only when absolutely necessary

## Testing

- Test ViewModels with fake repositories
- Use `runTest` for coroutine testing
- Test error paths, not just happy paths
