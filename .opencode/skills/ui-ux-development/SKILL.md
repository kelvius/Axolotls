---
name: ui-ux-development
description: UI/UX development for Jetpack Compose with Material 3
---

# UI/UX Development

## Tech Stack
- Jetpack Compose (BOM latest stable)
- Material 3 (Material You)
- Kotlin

## Required Patterns

1. **Always use Material 3 components** - `MaterialTheme`, `Surface`, `Card`, `Button` from `androidx.compose.material3`
2. **Use `remember` and `rememberSaveable`** for UI state that should survive configuration changes
3. **Use `LaunchedEffect` and `rememberCoroutineScope`** for side effects in Composables
4. **Always handle loading and error states** in UI
5. **Use `Modifier` chaining** - apply modifiers in order: padding -> alignment -> clickable
6. **Use `Box`, `Column`, `Row`, `LazyColumn`/`LazyRow`** for layouts
7. **Extract composable functions** for reusable UI elements (max ~50 lines per function)

## Prohibited Patterns

1. **Never hardcode colors** - use `MaterialTheme.colorScheme` (primary, secondary, surface, etc.)
2. **Never use dp literals in calculations** - use `with(LocalDensity.current)` for conversions
3. **Avoid `GlobalScope`** in ViewModels or Composables
4. **Don't put business logic in Composables** - delegate to ViewModel

## Theming

```kotlin
// Use MaterialTheme.colorScheme.primary, not hardcoded colors
Text("Hello", color = MaterialTheme.colorScheme.primary)

// Custom colors via Material 3 ColorScheme
MaterialTheme(
    colorScheme = lightColorScheme(
        primary = Color(0xFF...),
        // ...
    )
)
```

## State Management

```kotlin
// ViewModel -> UI state flow
data class UiState(val items: List<Item> = emptyList(), val isLoading: Boolean = false)

// In Composable
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

## Testing

- Use `composeTestRule` for UI testing
- Test loading, error, and success states separately
- Use `testTag` for element identification
