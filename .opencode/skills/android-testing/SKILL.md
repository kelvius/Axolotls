---
name: android-testing
description: Android testing with JUnit, Espresso, and Compose Test
---

# Testing

## Tech Stack
- JUnit 5 for unit tests
- AndroidX Test
- Compose UI Testing
- MockK for mocking
- Turbine for Flow testing

## Test Structure

```
app/src/test/java/     # Unit tests (JVM only)
app/src/androidTest/   # Instrumented tests (device/emulator)
```

## Required Patterns

### Unit Tests (ViewModels, Repositories)

```kotlin
class HomeViewModelTest {
    private lateinit var viewModel: HomeViewModel
    private lateinit var repository: FakeUserRepository

    @Before
    fun setup() {
        repository = FakeUserRepository()
        viewModel = HomeViewModel(repository)
    }

    @Test
    fun `uiState is updated when users are loaded`() = runTest {
        repository.setUsers(listOf(User("1", "Test")))
        
        viewModel.loadUsers()
        
        assertEquals(
            HomeUiState(users = listOf(User("1", "Test"))),
            viewModel.uiState.value
        )
    }
}
```

### Flow Testing with Turbine

```kotlin
@Test
fun `getUsers returns flow of results`() = runTest {
    val repo = UserRepositoryImpl(fakeApi, fakeDao)
    
    repo.getUsers().test {
        awaitItem() // Loading
        val success = awaitItem() as Result.Success
        assertEquals(1, success.data.size)
        awaitComplete()
    }
}
```

### Compose UI Tests

```kotlin
@ComposeTest
fun `displays loading indicator while loading`() {
    composeTestRule.setContent {
        MyScreen(uiState = HomeUiState(isLoading = true))
    }
    
    composeTestRule.onNodeWithTag("loading").assertIsDisplayed()
}

@ComposeTest
fun `clicking button triggers action`() {
    composeTestRule.setContent {
        MyScreen(
            uiState = HomeUiState(),
            onButtonClick = { wasClicked = true }
        )
    }
    
    composeTestRule.onNodeWithText("Submit").performClick()
    assert(wasClicked)
}
```

## Prohibited Patterns

1. **Don't test private methods** - test behavior, not implementation
2. **Don't use Thread.sleep** - use `await` from coroutines-test
3. **Don't mock data classes** - create real instances or use fakes
4. **Avoid `any()` matchers in MockK** - use `just Runs` or `coJust Runs` for Unit functions

## Running Tests

```bash
# Unit tests only
./gradlew test

# Unit tests for specific variant
./gradlew testDebugUnitTest

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Compose UI tests
./gradlew testDebugUnitTest -PtestInstrumentationRunnerArguments.androidx.compose.ui.util.enableTracing=true
```

## Coverage

- Target 70%+ line coverage for ViewModels and Repositories
- Cover success, loading, and error states
- Use `Fake` implementations over mocks for simpler cases
