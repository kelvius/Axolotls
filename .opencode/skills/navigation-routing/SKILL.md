---
name: navigation-routing
description: Compose Navigation setup and best practices
---

# Navigation & Routing

## Tech Stack
- Jetpack Navigation Compose
- Koin for DI

## Required Patterns

1. **Define routes as sealed classes or objects** - never use raw strings
2. **Use `navController.navigate(route)`** with type-safe arguments via `NavType`
3. **Pass arguments via `Bundle`** or use Navigation Compose SafeArgs plugin
4. **Handle back stack** properly - use `popUpTo` to avoid duplicate destinations
5. **Use NavHost** as the single source of navigation truth

## Route Definition

```kotlin
// ✅ Good - Sealed class routes
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail/{itemId}") {
        fun createRoute(itemId: String) = "detail/$itemId"
    }
}

// ❌ Bad - Raw strings
navController.navigate("detail/$id")
```

## Navigation Graph

```kotlin
NavHost(
    navController = navController,
    startDestination = Screen.Home.route
) {
    composable(Screen.Home.route) {
        HomeScreen(
            onNavigateToDetail = { itemId ->
                navController.navigate(Screen.Detail.createRoute(itemId))
            }
        )
    }
    composable(
        route = Screen.Detail.route,
        arguments = listOf(navArgument("itemId") { type = NavType.StringType })
    ) { backStackEntry ->
        val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
        DetailScreen(itemId = itemId)
    }
}
```

## Prohibited Patterns

1. **Never navigate from ViewModel** - pass navigation callbacks to UI layer
2. **Don't pass heavy objects** via navigation arguments - pass IDs only
3. **Avoid nested NavHosts** when possible

## Deep Links

```kotlin
composable(
    route = Screen.Detail.route,
    deepLinks = listOf(navDeepLink { uriPattern = "https://app.com/detail/{itemId}" })
)
```

## Testing

- Test navigation with `createNavController()` in tests
- Verify back stack after navigation actions
