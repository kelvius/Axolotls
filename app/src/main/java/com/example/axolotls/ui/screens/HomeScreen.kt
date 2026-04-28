package com.example.axolotls.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.axolotls.data.CommunityEvent
import com.example.axolotls.data.EventRepository
import com.example.axolotls.data.FavoritesManager
import com.google.android.gms.location.LocationServices
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Event(
    val id: Int,
    val title: String,
    val date: String,
    val location: String,
    val description: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceKm: Double? = null
)

data class BottomNavItem(
    val title: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Default.Home),
    BottomNavItem("Map", Icons.Default.Map),
    BottomNavItem("Favorites", Icons.Default.Favorite),
    BottomNavItem("Nearby", Icons.Default.NearMe)
)

val dummyEvents = listOf(
    Event(1, "Music Festival", "Apr 20, 2026", "Downtown Plaza", "Live music all day with local bands"),
    Event(2, "Food Fair", "Apr 25, 2026", "Central Park", "Sample cuisines from around the world"),
    Event(3, "Art Exhibition", "May 1, 2026", "City Gallery", "Modern art showcase featuring local artists"),
    Event(4, "Tech Meetup", "May 5, 2026", "Innovation Hub", "Networking for tech enthusiasts"),
    Event(5, "Sports Day", "May 10, 2026", "Sports Complex", "Community sports activities for all ages")
)

@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var mapFocusLat by remember { mutableStateOf<Double?>(null) }
    var mapFocusLng by remember { mutableStateOf<Double?>(null) }
    var mapFocusTitle by remember { mutableStateOf<String?>(null) }
    val navigateToMap: (Double, Double, String) -> Unit = { lat, lng, title ->
        mapFocusLat = lat
        mapFocusLng = lng
        mapFocusTitle = title
        selectedTab = 1
    }

    val context = LocalContext.current
    val favoritesManager = remember { FavoritesManager(context) }

    // Favorites loaded from local storage
    var communityFavoriteIds by remember { mutableStateOf(favoritesManager.getCommunityFavorites()) }
    var nearbyFavoriteIds by remember { mutableStateOf(favoritesManager.getNearbyFavorites()) }

    // Nearby events state
    var nearbyEvents by remember { mutableStateOf(dummyEvents) }
    var isLoadingNearby by remember { mutableStateOf(true) }

    val repository = remember { EventRepository() }

    LaunchedEffect(Unit) {
        var userLat: Double? = null
        var userLng: Double? = null

        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val location = suspendCoroutine { cont ->
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc -> cont.resume(loc) }
                        .addOnFailureListener { cont.resume(null) }
                }
                if (location != null) {
                    userLat = location.latitude
                    userLng = location.longitude
                }
            } catch (_: SecurityException) { }
        }

        val result = repository.getRecentEvents(userLat, userLng)
        result.onSuccess { apiEvents ->
            if (apiEvents.isNotEmpty()) {
                nearbyEvents = apiEvents
            }
        }
        isLoadingNearby = false
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> {
                // Community Events = Main tab
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CommunityEventsScreen(
                        favoriteIds = communityFavoriteIds,
                        onFavoriteToggle = { id ->
                            communityFavoriteIds = favoritesManager.toggleCommunityFavorite(id)
                        },
                        onLogout = onLogout,
                        onNavigateToMap = navigateToMap
                    )
                }
            }
            1 -> {
                // Map
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    MapScreen(
                        focusLat = mapFocusLat,
                        focusLng = mapFocusLng,
                        focusTitle = mapFocusTitle
                    )
                }
            }
            2 -> {
                // Favorites tab -- shows saved community events
                FavoritesScreen(
                    communityFavoriteIds = communityFavoriteIds,
                    onCommunityFavoriteToggle = { id ->
                        communityFavoriteIds = favoritesManager.toggleCommunityFavorite(id)
                    },
                    nearbyFavoriteIds = nearbyFavoriteIds,
                    nearbyEvents = nearbyEvents,
                    onNearbyFavoriteToggle = { id ->
                        nearbyFavoriteIds = favoritesManager.toggleNearbyFavorite(id)
                    },
                    paddingValues = paddingValues
                )
            }
            3 -> {
                // Nearby Events (Winnipeg Open Data)
                NearbyEventsScreen(
                    events = nearbyEvents,
                    isLoading = isLoadingNearby,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    favoriteIds = nearbyFavoriteIds,
                    onFavoriteToggle = { id ->
                        nearbyFavoriteIds = favoritesManager.toggleNearbyFavorite(id)
                    },
                    onLogout = onLogout,
                    paddingValues = paddingValues,
                    onNavigateToMap = navigateToMap
                )
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    communityFavoriteIds: Set<String>,
    onCommunityFavoriteToggle: (String) -> Unit,
    nearbyFavoriteIds: Set<Int>,
    nearbyEvents: List<Event>,
    onNearbyFavoriteToggle: (Int) -> Unit,
    paddingValues: PaddingValues
) {
    val favoriteNearbyEvents = nearbyEvents.filter { nearbyFavoriteIds.contains(it.id) }
    val hasCommunityFavorites = communityFavoriteIds.isNotEmpty()
    val hasNearbyFavorites = favoriteNearbyEvents.isNotEmpty()
    val hasAny = hasCommunityFavorites || hasNearbyFavorites

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            )
    ) {
        Text(
            text = "Your Favorites",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (!hasAny) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No favorites yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the heart icon on any event to save it here",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            // We need to render community favorites inline.
            // They'll be loaded from CalendarRepository when showing this tab.
            // For simplicity, show a note that community favorites are saved by ID
            // and show nearby favorites with full cards.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasCommunityFavorites) {
                    item {
                        Text(
                            text = "Saved Community Events",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    // Show community favorites using FavoriteCommunityEvents composable
                    item {
                        FavoriteCommunityEvents(
                            favoriteIds = communityFavoriteIds,
                            onFavoriteToggle = onCommunityFavoriteToggle
                        )
                    }
                }

                if (hasNearbyFavorites) {
                    item {
                        Text(
                            text = "Saved Nearby Events",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(favoriteNearbyEvents) { event ->
                        NearbyEventCard(
                            event = event,
                            isFavorite = true,
                            onFavoriteClick = { onNearbyFavoriteToggle(event.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shows community favorite events by fetching them and filtering by saved IDs.
 */
@Composable
fun FavoriteCommunityEvents(
    favoriteIds: Set<String>,
    onFavoriteToggle: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { com.example.axolotls.data.CalendarRepository() }
    var allEvents by remember { mutableStateOf<List<CommunityEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result = repository.getCommunityEvents()
        result.onSuccess { allEvents = it }
        isLoading = false
    }

    val favoriteEvents = allEvents.filter { favoriteIds.contains(it.id) }

    if (isLoading) {
        Text(
            text = "Loading saved events...",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(8.dp)
        )
    } else if (favoriteEvents.isEmpty()) {
        Text(
            text = "Saved events may have expired",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(8.dp)
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            favoriteEvents.forEach { event ->
                CommunityEventCard(
                    event = event,
                    isFavorite = true,
                    onFavoriteClick = { onFavoriteToggle(event.id) }
                )
            }
        }
    }
}

@Composable
fun NearbyEventsScreen(
    events: List<Event>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    favoriteIds: Set<Int>,
    onFavoriteToggle: (Int) -> Unit,
    onLogout: () -> Unit,
    paddingValues: PaddingValues,
    onNavigateToMap: (lat: Double, lng: Double, title: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentPage by remember { mutableIntStateOf(0) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var isGeocoding by remember { mutableStateOf(false) }
    var geocodeError by remember { mutableStateOf(false) }

    selectedEvent?.let { event ->
        val hasCoords = event.latitude != null && event.longitude != null
        val hasLocation = event.location.isNotBlank()
        AlertDialog(
            onDismissRequest = {
                selectedEvent = null
                isGeocoding = false
                geocodeError = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (event.description.isNotBlank()) {
                        Text(
                            text = event.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = event.date,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    if (event.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = event.location,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    if (event.distanceKm != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatDistance(event.distanceKm),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    if (geocodeError) {
                        Text(
                            text = "Location could not be found on map",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                if (hasCoords || hasLocation) {
                    TextButton(
                        enabled = !isGeocoding,
                        onClick = {
                            if (hasCoords) {
                                onNavigateToMap(event.latitude!!, event.longitude!!, event.title)
                                selectedEvent = null
                            } else {
                                isGeocoding = true
                                geocodeError = false
                                coroutineScope.launch {
                                    val coords = geocodeLocation(context, event.location)
                                    if (coords != null) {
                                        onNavigateToMap(coords.first, coords.second, event.title)
                                        selectedEvent = null
                                    } else {
                                        geocodeError = true
                                    }
                                    isGeocoding = false
                                }
                            }
                        }
                    ) {
                        if (isGeocoding) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text("View on Map")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedEvent = null
                    isGeocoding = false
                    geocodeError = false
                }) {
                    Text("Close")
                }
            }
        )
    }

    val filteredEvents = if (searchQuery.isEmpty()) {
        events
    } else {
        events.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.location.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalPages = if (filteredEvents.isEmpty()) 0 else (filteredEvents.size + 10 - 1) / 10
    val pagedEvents = filteredEvents.drop(currentPage * 10).take(10)

    // Reset page when search changes
    LaunchedEffect(searchQuery) { currentPage = 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = if (events.firstOrNull()?.distanceKm != null)
                        "Nearby Events" else "Upcoming Events",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onLogout() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search events...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Text(
                text = "Loading events from Winnipeg Open Data...",
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pagedEvents) { event ->
                NearbyEventCard(
                    event = event,
                    isFavorite = favoriteIds.contains(event.id),
                    onFavoriteClick = { onFavoriteToggle(event.id) },
                    onCardClick = {
                        selectedEvent = event
                        isGeocoding = false
                        geocodeError = false
                    }
                )
            }
        }

        // Pagination
        if (totalPages > 1) {
            PaginationBar(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = { currentPage = it }
            )
        }
    }
}

/**
 * Event card for Nearby Events (Winnipeg Open Data).
 * Styled to match CommunityEventCard for consistency.
 */
@Composable
fun NearbyEventCard(
    event: Event,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onCardClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: source badge + favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Winnipeg Open Data",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        modifier = Modifier.size(20.dp),
                        tint = if (isFavorite)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title
            Text(
                text = event.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Description
            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Date",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = event.date,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Location row
            if (event.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.location,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Distance row
            if (event.distanceKm != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Distance",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDistance(event.distanceKm),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

private fun formatDistance(km: Double): String {
    return if (km < 1.0) {
        "${(km * 1000).toInt()} m away"
    } else {
        "${"%.1f".format(km)} km away"
    }
}

private suspend fun geocodeLocation(context: Context, locationName: String): Pair<Double, Double>? =
    withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val results = Geocoder(context).getFromLocationName(locationName, 1)
            results?.firstOrNull()?.let { Pair(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }
