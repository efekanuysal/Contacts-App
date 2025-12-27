package com.example.contactsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.compose.AsyncImage
import coil.request.CachePolicy
import com.example.contactsapp.data.remote.RetrofitClient
import com.example.contactsapp.presentation.contract.ContactEvent
import com.example.contactsapp.presentation.screens.AddContactScreen
import com.example.contactsapp.presentation.screens.ContactDetailScreen
import com.example.contactsapp.presentation.screens.ContactListScreen
import com.example.contactsapp.presentation.screens.SuccessScreen
import com.example.contactsapp.presentation.viewmodel.ContactViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val viewModel: ContactViewModel = viewModel()
            val state = viewModel.state.collectAsState().value

            NavHost(navController = navController, startDestination = "contact_list") {
                composable("contact_list") {
                    ContactListScreen(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onNavigateToAdd = { navController.navigate("add_contact") },
                        onContactClick = { contact ->
                            viewModel.onEvent(ContactEvent.OnContactSelected(contact))
                            navController.navigate("contact_detail")
                        },
                        onNavigateToDetail = {
                            navController.navigate("contact_detail")
                        }
                    )
                }
                composable("contact_detail") {
                    ContactDetailScreen(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("add_contact") {
                    AddContactScreen(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateSuccess = {
                            navController.navigate("success") {
                                popUpTo("contact_list")
                            }
                        }
                    )
                }
                composable("success") {
                    SuccessScreen(
                        onNavigateHome = {
                            navController.navigate("contact_list") {
                                popUpTo("contact_list") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }

}