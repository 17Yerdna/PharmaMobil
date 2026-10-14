package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import pe.edu.upeu.pharmamobil.presentation.theme.AppIcons
import pe.edu.upeu.pharmamobil.presentation.theme.SemanticWarning
import pe.edu.upeu.pharmamobil.presentation.theme.SemanticWarningContainerLight
import pharmamobil.shared.generated.resources.Res
import pharmamobil.shared.generated.resources.pharmamobil_logo

/**
 * Pantalla de inicio de sesión con validación de credenciales JWT,
 * soporte de visibilidad de contraseña, banners reactivos y chips de selección de usuario para laboratorio.
 */
@Composable
fun LoginScreen(
    viewModel: SesionViewModel,
    sesionExpirada: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo institucional
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 3.dp,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(Res.drawable.pharmamobil_logo),
                        contentDescription = "Logo PharmaMobil",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "PharmaMobil",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Acceso seguro con token JWT y control de roles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Banner informativo de sesión expirada (401 global)
            AnimatedVisibility(visible = sesionExpirada) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SemanticWarningContainerLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Warning,
                            contentDescription = "Sesión Expirada",
                            tint = SemanticWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tu sesión ha expirado o el token ya no es válido. Ingresa tus credenciales nuevamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Banner de Error de Login
            AnimatedVisibility(visible = uiState.mensajeError != null) {
                uiState.mensajeError?.let { mensaje ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = mensaje,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.limpiarError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Close,
                                    contentDescription = "Descartar",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Formulario en Card estilizada
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Campo Usuario
                    OutlinedTextField(
                        value = uiState.usuario,
                        onValueChange = viewModel::onUsuarioChanged,
                        label = { Text("Usuario") },
                        placeholder = { Text("Ej: admin") },
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Campo Contraseña
                    OutlinedTextField(
                        value = uiState.contrasenia,
                        onValueChange = viewModel::onContraseniaChanged,
                        label = { Text("Contraseña") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = viewModel::toggleContraseniaVisible) {
                                Icon(
                                    imageVector = if (uiState.contraseniaVisible) AppIcons.VisibilityOff else AppIcons.Visibility,
                                    contentDescription = if (uiState.contraseniaVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (uiState.contraseniaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.iniciarSesion()
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botón de Inicio de Sesión
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.iniciarSesion()
                        },
                        enabled = !uiState.cargando,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Autenticando...", fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                text = "Iniciar Sesión",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Selector Rápido de Usuarios para Laboratorio (Sin contraseñas quemadas)
            Text(
                text = "Usuarios de prueba (Laboratorio)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Selecciona un usuario para rellenar el campo; digita su contraseña manualmente.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("admin", "vendedor", "almacen").forEach { roleUser ->
                    val isSelected = uiState.usuario == roleUser
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.seleccionarUsuarioDebug(roleUser) },
                        label = { Text(roleUser, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}
