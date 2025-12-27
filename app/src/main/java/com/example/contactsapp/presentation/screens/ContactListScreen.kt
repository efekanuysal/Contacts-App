package com.example.contactsapp.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Delete
import coil.compose.AsyncImage
import com.example.contactsapp.R
import com.example.contactsapp.domain.model.Contact
import com.example.contactsapp.presentation.components.SearchBar
import com.example.contactsapp.presentation.contract.ContactEvent
import com.example.contactsapp.presentation.contract.ContactState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableContactItem(
    contact: Contact,
    onContactClick: (Contact) -> Unit,
    onEditClick: (Contact) -> Unit,
    onDeleteClick: (Contact) -> Unit
) {
    // 1. Use rememberSwipeToDismissBoxState instead of rememberDismissState
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            // We return false to prevent the item from actually being dismissed/removed
            // from the UI tree immediately. We just want to reveal the background.
            false
        }
    )

    // 2. Use SwipeToDismissBox instead of SwipeToDismiss
    SwipeToDismissBox(
        state = dismissState,
        // 3. Instead of 'directions = setOf(...)', use these booleans:
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true, // Only allow right-to-left swipe
        backgroundContent = {
            SwipeBackground(
                dismissState = dismissState,
                contact = contact,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick
            )
        },
        content = {
            ContactItem(contact = contact, onClick = { onContactClick(contact) })
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackground(
    dismissState: SwipeToDismissBoxState, // Updated type
    contact: Contact,
    onEditClick: (Contact) -> Unit,
    onDeleteClick: (Contact) -> Unit
) {
    // 4. Check dismissDirection using SwipeToDismissBoxValue
    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit Button (Blue)
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(Color(0xFF007AFF))
                    .clickable { onEditClick(contact) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White
                )
            }

            // Delete Button (Red)
            // Note: You seemed to have cut off the Delete button code in your snippet,
            // but assuming it follows the same pattern:
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .background(Color(0xFFFF3B30))
                    .clickable { onDeleteClick(contact) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }
    }
}
@Composable
fun ContactListScreen(
    state: ContactState,
    onEvent: (ContactEvent) -> Unit,
    onNavigateToAdd: () -> Unit,
    onContactClick: (Contact) -> Unit,
    onNavigateToDetail: () -> Unit
) {
    val filteredContacts = state.contacts.filter {
        it.firstName.contains(state.searchQuery, ignoreCase = true) ||
                it.lastName.contains(state.searchQuery, ignoreCase = true)
    }

    val groupedContacts = remember(filteredContacts) {
        filteredContacts
            .sortedBy { it.firstName }
            .groupBy { it.firstName.firstOrNull()?.uppercaseChar() ?: '#' }
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            ContactsHeader(onAddClick = onNavigateToAdd)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChanged = { onEvent(ContactEvent.OnSearchQueryChanged(it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.contacts.isNotEmpty()) {
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
                                onContactClick = onContactClick,
                                onEditClick = { contact ->
                                    // Select the contact and navigate to detail in edit mode
                                    onEvent(ContactEvent.OnContactSelected(contact))
                                    onEvent(ContactEvent.OnToggleEditMode)
                                    onNavigateToDetail()
                                },
                                onDeleteClick = { contact ->
                                    // Select the contact and trigger delete
                                    onEvent(ContactEvent.OnContactSelected(contact))
                                    onEvent(ContactEvent.OnDeleteContact)
                                }
                            )
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
fun ContactsHeader(onAddClick: () -> Unit) {
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
            color = Color.Black
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
                        onEditClick = onEditClick,
                        onDeleteClick = onDeleteClick
                    )
                    if (index < contacts.lastIndex) {
                        Divider(
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

@Composable
fun ContactItem(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Avatar Logic (unchanged)
            if (contact.photoUri != null && contact.photoUri.toString().isNotEmpty()) {
                AsyncImage(
                    model = contact.photoUri,
                    contentDescription = null,
                    modifier = Modifier.size(46.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                    error = painterResource(id = R.drawable.ic_launcher_foreground)
                )
            } else {
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(Color(0xFFE1F5FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = contact.firstName.take(1).uppercase(), color = Color(0xFF007AFF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            // Phone Icon Overlay for saved contacts
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
                        contentDescription = "Saved to phone",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

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
}

