package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSoftBg
import com.example.ui.theme.StrawberryRed
import com.example.ui.theme.StrawberrySoftBg
import com.example.ui.theme.StrawberryTint

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        StrawberrySoftBg,
                        Color.White,
                        EmeraldSoftBg
                    )
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Logo Badge
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_brand_logo),
                            contentDescription = "স্ট্রবেরি আড়ত প্রো লোগো",
                            modifier = Modifier.size(76.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "স্ট্রবেরি আড়ত প্রো",
                    style = MaterialTheme.typography.headlineMedium,
                    color = StrawberryRed,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ডিজিটাল কার্টুন হিসাব, খাতা ও এক্সেল রিপোর্ট",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Mode Switcher Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { if (state.isSignUpMode) viewModel.toggleMode() },
                            shape = RoundedCornerShape(12.dp),
                            color = if (!state.isSignUpMode) Color.White else Color.Transparent,
                            shadowElevation = if (!state.isSignUpMode) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("login_tab_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "লগইন করুন",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (!state.isSignUpMode) StrawberryRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = { if (!state.isSignUpMode) viewModel.toggleMode() },
                            shape = RoundedCornerShape(12.dp),
                            color = if (state.isSignUpMode) Color.White else Color.Transparent,
                            shadowElevation = if (state.isSignUpMode) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("signup_tab_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "নতুন অ্যাকাউন্ট",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (state.isSignUpMode) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auth Form Bento Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedVisibility(visible = state.isSignUpMode) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = state.displayName,
                                    onValueChange = viewModel::updateDisplayName,
                                    label = { Text("আপনার নাম") },
                                    placeholder = { Text("যেমন: করিম সুপারভাইজার") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Person, contentDescription = null)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("name_input")
                                )

                                OutlinedTextField(
                                    value = state.aratName,
                                    onValueChange = viewModel::updateAratName,
                                    label = { Text("আড়তের নাম (ঐচ্ছিক)") },
                                    placeholder = { Text("যেমন: মা-বাবা স্ট্রবেরি আড়ত") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Storefront, contentDescription = null)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("arat_name_input")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.email,
                            onValueChange = viewModel::updateEmail,
                            label = { Text("ইমেইল ঠিকানা") },
                            placeholder = { Text("example@gmail.com") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Email, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )

                        OutlinedTextField(
                            value = state.password,
                            onValueChange = viewModel::updatePassword,
                            label = { Text("পাসওয়ার্ড") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = "পাসওয়ার্ড দেখুন"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (state.isSignUpMode) ImeAction.Next else ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        AnimatedVisibility(visible = state.isSignUpMode) {
                            OutlinedTextField(
                                value = state.confirmPassword,
                                onValueChange = viewModel::updateConfirmPassword,
                                label = { Text("পুনরায় পাসওয়ার্ড দিন") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Lock, contentDescription = null)
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_password_input")
                            )
                        }

                        if (!state.isSignUpMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = viewModel::sendPasswordReset) {
                                    Text("পাসওয়ার্ড ভুলে গেছেন?")
                                }
                            }
                        }

                        state.errorMessage?.let { err ->
                            Surface(
                                color = StrawberryTint,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        state.infoMessage?.let { info ->
                            Surface(
                                color = EmeraldSoftBg,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = info,
                                    color = EmeraldGreen,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = viewModel::submit,
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isSignUpMode) EmeraldGreen else StrawberryRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("auth_submit_button")
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = if (state.isSignUpMode) "অ্যাকাউন্ট তৈরি করুন" else "প্রবেশ করুন",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "আপনার প্রতিটি হিসাব নিরাপদে আপনার নিজস্ব অ্যাকাউন্টে সংরক্ষিত থাকবে",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
