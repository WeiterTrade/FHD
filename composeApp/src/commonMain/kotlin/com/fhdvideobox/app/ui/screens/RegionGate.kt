package com.fhdvideobox.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fhdvideobox.app.data.Region
import com.fhdvideobox.app.ui.AppController
import com.fhdvideobox.app.ui.components.Wordmark

private sealed interface GateState {
    data object Detecting : GateState
    data class Confirm(val region: Region) : GateState
    data class Choose(val reason: String) : GateState
}

/**
 * Первый вход:
 * 1) определяем страну по IP;
 * 2) если это Казахстан или Россия — спрашиваем «Вы находитесь в …?»;
 * 3) «Да» — подключаемся к серверу этой страны; «Нет» / не удалось определить — ручной выбор.
 */
@Composable
fun RegionGate(app: AppController) {
    var state by remember { mutableStateOf<GateState>(GateState.Detecting) }

    LaunchedEffect(Unit) {
        val code = app.geo.detectCountry()
        val region = Region.fromCountry(code)
        state = when {
            region != null -> GateState.Confirm(region)
            code == null -> GateState.Choose("Не удалось определить ваше местоположение.")
            else -> GateState.Choose("Сервис пока работает в Казахстане и России.\nВыберите ближайший сервер.")
        }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Телевизор (≈540 dp по высоте) и альбомный телефон: компактная раскладка
        val compact = maxHeight < 640.dp
        val wide = maxWidth > maxHeight && compact
        val logo: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(app.platform.logo(), null, Modifier.height(if (compact) 110.dp else 150.dp))
                Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
                Wordmark(big = !compact)
            }
        }
        val body: @Composable () -> Unit = {
            when (val s = state) {
                GateState.Detecting -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Определяем ваш регион…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is GateState.Confirm -> ConfirmCard(
                    question = s.region.question,
                    onYes = { app.connect(s.region) },
                    onNo = { state = GateState.Choose("Выберите страну, в которой вы находитесь.") },
                )
                is GateState.Choose -> ChooseCard(s.reason) { app.connect(it) }
            }
        }

        if (wide) {
            // ТВ: логотип слева, вопрос справа — всё помещается на один экран
            Row(
                Modifier.fillMaxSize().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { logo() }
                Box(Modifier.weight(1f).verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                    Box(Modifier.widthIn(max = 460.dp)) { body() }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                logo()
                Spacer(Modifier.height(if (compact) 16.dp else 40.dp))
                Box(Modifier.widthIn(max = 520.dp)) { body() }
            }
        }
    }
}

@Composable
private fun ConfirmCard(question: String, onYes: () -> Unit, onNo: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(question, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Мы подключим вас к ближайшему серверу — так видео будет загружаться быстрее.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onYes, modifier = Modifier.width(140.dp).focusRequester(focus)) { Text("Да") }
                OutlinedButton(onClick = onNo, modifier = Modifier.width(140.dp)) { Text("Нет") }
            }
        }
    }
}

@Composable
private fun ChooseCard(reason: String, onPick: (Region) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(reason, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Region.entries.forEachIndexed { i, r ->
                Button(
                    onClick = { onPick(r) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        .then(if (i == 0) Modifier.focusRequester(focus) else Modifier),
                ) { Text(r.title) }
            }
        }
    }
}
