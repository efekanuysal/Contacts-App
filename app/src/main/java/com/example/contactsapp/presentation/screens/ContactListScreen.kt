package com.example.contactsapp.presentation.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.contactsapp.domain.model.Contact
import com.example.contactsapp.presentation.components.SearchBar
import com.example.contactsapp.presentation.components.SuccessMessageOverlay
import com.example.contactsapp.presentation.contract.ContactEvent
import com.example.contactsapp.presentation.contract.ContactState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ContactListScreen(
    state: ContactState,
    onEvent: (ContactEvent) -> Unit,
    onNavigateToAdd: () -> Unit,
    onContactClick: (Contact) -> Unit,
    onNavigateToDetail: () -> Unit
) {
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }

    // Added FocusManager to handle keyboard dismissal
    val focusManager = LocalFocusManager.current

    // Advanced search logic: Supports "First Name space Last Name"
    val filteredContacts = remember(state.contacts, state.searchQuery) {
        if (state.searchQuery.isBlank()) {
            state.contacts
        } else {
            state.contacts.filter { contact ->
                val fullName = "${contact.firstName} ${contact.lastName}"
                fullName.contains(state.searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    val groupedContacts = remember(filteredContacts) {
        filteredContacts
            .sortedBy { it.firstName }
            .groupBy { it.firstName.firstOrNull()?.uppercaseChar() ?: '#' }
    }

    if (contactToDelete != null) {
        DeleteConfirmationSheet(
            onDismiss = { contactToDelete = null },
            onConfirm = {
                onEvent(ContactEvent.OnContactSelected(contactToDelete!!))
                onEvent(ContactEvent.OnDeleteContact)
                contactToDelete = null
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            ContactsHeader(
                onAddClick = onNavigateToAdd,
                onTitleClick = {
                    // Reset logic: Clear query, clear focus (closes history), dismiss keyboard
                    onEvent(ContactEvent.OnSearchQueryChanged(""))
                    onEvent(ContactEvent.OnSearchFocusChanged(false))
                    focusManager.clearFocus()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                // Optional: Clear focus when clicking background to close keyboard/history
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChanged = { onEvent(ContactEvent.OnSearchQueryChanged(it)) },
                onFocusChanged = { onEvent(ContactEvent.OnSearchFocusChanged(it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // State Logic (Preserved Exactly as requested):
            // 1. Search Active & Empty Query -> Show History
            // 2. Search Active & Query Exists & No Results -> Show Error
            // 3. Search Active & Query Exists & Results -> Show Results
            // 4. Default -> Show All Contacts

            when {
                state.isSearchActive && state.searchQuery.isEmpty() && state.previousSearches.isNotEmpty() -> {
                    PreviousSearchesList(
                        searches = state.previousSearches,
                        onSearchClick = { onEvent(ContactEvent.OnSearchQueryChanged(it)) },
                        onRemoveClick = { onEvent(ContactEvent.OnRemoveSearchHistoryItem(it)) },
                        onClearAllClick = { onEvent(ContactEvent.OnClearSearchHistory) }
                    )
                }

                state.searchQuery.isNotEmpty() && filteredContacts.isEmpty() -> {
                    NoMatchesFoundView()
                }

                else -> {
                    if (filteredContacts.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            groupedContacts.forEach { (initial, contacts) ->
                                item {
                                    ContactGroup(
                                        initial = initial,
                                        contacts = contacts,
                                        onContactClick = { contact ->
                                            onContactClick(contact)
                                        },
                                        onEditClick = { contact ->
                                            onEvent(ContactEvent.OnContactSelected(contact))
                                            onEvent(ContactEvent.OnToggleEditMode)
                                            onNavigateToDetail()
                                        },
                                        onDeleteClick = { contact ->
                                            contactToDelete = contact
                                        }
                                    )
                                }
                            }
                        }
                    } else if (state.contacts.isEmpty() && !state.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No contacts yet.", color = Color.Gray)
                        }
                    }
                }
            }

            if (state.globalSuccessMessage != null) {
                SuccessMessageOverlay(
                    message = state.globalSuccessMessage,
                    onDismiss = { onEvent(ContactEvent.OnClearGlobalSuccessMessage) }
                )
            }
        }
    }
}

@Composable
fun PreviousSearchesList(
    searches: List<String>,
    onSearchClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    onClearAllClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Previous searches",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )
            Text(
                text = "Clear All",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.clickable { onClearAllClick() }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searches.reversed()) { search -> // Show most recent first
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .clickable { onSearchClick(search) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = search, fontSize = 16.sp, color = Color.Black)
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.LightGray,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onRemoveClick(search) }
                    )
                }
            }
        }
    }
}

@Composable
fun NoMatchesFoundView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Used Spacer logic to push content slightly higher as requested in previous turn
        Spacer(modifier = Modifier.weight(0.3f))

        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No results found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try checking for typos or using a different name.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.weight(0.7f))
    }
}

@Composable
fun ContactsHeader(
    onAddClick: () -> Unit,
    onTitleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Contacts",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null 
            ) { onTitleClick() }
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF007AFF))
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Contact",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ContactGroup(
    initial: Char,
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onEditClick: (Contact) -> Unit,
    onDeleteClick: (Contact) -> Unit
) {
    Column {
        Text(
            text = initial.toString(),
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                contacts.forEachIndexed { index, contact ->
                    SwipeableContactItem(
                        contact = contact,
                        onContactClick = onContactClick,
                        onEditClick = { onEditClick(contact) },
                        onDeleteClick = { onDeleteClick(contact) }
                    )
                    if (index < contacts.lastIndex) {
                        HorizontalDivider(
                            color = Color(0xFFE5E5EA),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 76.dp)
                        )
                    }
                }
            }
        }
    }
}

private enum class SwipeState { Closed, Open }

@Composable
fun SwipeableContactItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val density = LocalDensity.current
    val actionWidth = 140.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val scope = rememberCoroutineScope()

    val decayAnimationSpec = rememberSplineBasedDecay<Float>()

    val state = remember {
        AnchoredDraggableState(
            initialValue = SwipeState.Closed,
            anchors = DraggableAnchors {
                SwipeState.Closed at 0f
                SwipeState.Open at -actionWidthPx
            },
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = decayAnimationSpec
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(actionWidth)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.End
        ) {
            SwipeActionButton(
                color = Color(0xFF007AFF),
                icon = Icons.Default.Edit,
                onClick = {
                    scope.launch { state.animateTo(SwipeState.Closed) }
                    onEditClick()
                }
            )
            SwipeActionButton(
                color = Color(0xFFFF3B30),
                icon = Icons.Default.Delete,
                onClick = {
                    scope.launch { state.animateTo(SwipeState.Closed) }
                    onDeleteClick()
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .anchoredDraggable(state, Orientation.Horizontal)
                .background(Color.White)
        ) {
            ContactItem(contact = contact, onClick = { onContactClick(contact) })
        }
    }
}

@Composable
private fun SwipeActionButton(
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(70.dp)
            .fillMaxHeight()
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
fun ContactItem(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(contact)
        Spacer(modifier = Modifier.width(16.dp))
        ContactInfo(contact)
    }
}

@Composable
private fun ContactAvatar(contact: Contact) {
    Box(contentAlignment = Alignment.BottomEnd) {
        if (contact.photoUri != null && contact.photoUri.toString().isNotEmpty()) {
            AsyncImage(
                model = contact.photoUri,
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                error = painterResource(id = android.R.drawable.ic_menu_report_image)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE1F5FE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.firstName.take(1).uppercase(),
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        if (contact.isSavedLocally) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactInfo(contact: Contact) {
    Column {
        Text(
            text = "${contact.firstName} ${contact.lastName}",
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = contact.phoneNumber,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}